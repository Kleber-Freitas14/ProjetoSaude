
package com.example.projsaude

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class ResultadoCadastroPaciente {
    data class Sucesso(val mensagem: String) :
        ResultadoCadastroPaciente()

    data class AguardandoConfirmacao(val mensagem: String) :
        ResultadoCadastroPaciente()
}

@Serializable
private data class NovoPaciente(
    @SerialName("usuario_id")
    val usuarioId: String,

    @SerialName("nome_completo")
    val nomeCompleto: String,

    val cpf: String,

    @SerialName("data_nascimento")
    val dataNascimento: String,

    val telefone: String
)

@Serializable
private data class PacienteCriado(
    val id: String
)

@Serializable
private data class AtualizacaoFoto(
    @SerialName("foto_pedido_medico_path")
    val caminho: String
)

object CadastroPacienteRepository {

    suspend fun cadastrarPaciente(
        context: Context,
        nome: String,
        cpf: String,
        nascimento: String,
        telefone: String,
        email: String,
        senha: String,
        fotoPedido: Uri?
    ): ResultadoCadastroPaciente {

        val cliente = SupabaseConfig.client

        val dataNascimento = try {
            LocalDate.parse(
                nascimento.trim(),
                DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale("pt", "BR"))
            ).toString()
        } catch (e: Exception) {
            throw Exception("Data de nascimento inválida. Use DD/MM/AAAA.")
        }

        val cpfLimpo = cpf.filter { it.isDigit() }

        if (cpfLimpo.length != 11) {
            throw Exception("O CPF deve conter 11 números.")
        }

        if (nome.isBlank() || telefone.isBlank()) {
            throw Exception("Preencha todos os campos obrigatórios.")
        }

        if (senha.length < 6) {
            throw Exception("A senha deve possuir pelo menos 6 caracteres.")
        }

        // 1. Cria a conta no Supabase Auth.
        cliente.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = senha

            data = buildJsonObject {
                put("nome", nome.trim())
            }
        }

        // 2. Verifica se o Supabase liberou uma sessão.
        val usuarioId = cliente.auth.currentUserOrNull()?.id

        if (usuarioId == null) {
            return ResultadoCadastroPaciente.AguardandoConfirmacao(
                "Conta criada! Confirme seu e-mail para concluir o cadastro."
            )
        }

        // 3. Insere os dados do paciente.
        val paciente = cliente
            .from("pacientes")
            .insert(
                NovoPaciente(
                    usuarioId = usuarioId,
                    nomeCompleto = nome.trim(),
                    cpf = cpfLimpo,
                    dataNascimento = dataNascimento,
                    telefone = telefone.trim()
                )
            ) {
                select()
            }
            .decodeSingle<PacienteCriado>()

        // 4. Envia a foto, se houver.
        if (fotoPedido != null) {

            val tamanho = context.contentResolver
                .openAssetFileDescriptor(fotoPedido, "r")
                ?.use { it.length }
                ?: -1L

            if (tamanho > 10L * 1024L * 1024L) {
                throw Exception("A imagem deve ter no máximo 10 MB.")
            }

            val tipo = context.contentResolver.getType(fotoPedido)

            if (tipo == null || !tipo.startsWith("image/")) {
                throw Exception("Selecione um arquivo de imagem válido.")
            }

            val bytes = context.contentResolver
                .openInputStream(fotoPedido)
                ?.use { it.readBytes() }
                ?: throw Exception("Não foi possível ler a imagem.")

            if (bytes.size > 10 * 1024 * 1024) {
                throw Exception("A imagem deve ter no máximo 10 MB.")
            }

            val extensao = when (tipo.lowercase()) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> throw Exception("Formato de imagem não permitido.")
            }

            val caminho = "${paciente.id}/pedido_medico.$extensao"

            // 5. Envia para o bucket privado.
            cliente.storage
                .from("pedidos-medicos")
                .upload(caminho, bytes) {
                    upsert = true
                }

            // 6. Salva o caminho da foto no cadastro.
            cliente
                .from("pacientes")
                .update(AtualizacaoFoto(caminho)) {
                    filter {
                        eq("id", paciente.id)
                    }
                }
        }

        return ResultadoCadastroPaciente.Sucesso(
            "Cadastro do paciente realizado com sucesso!"
        )
    }
}
