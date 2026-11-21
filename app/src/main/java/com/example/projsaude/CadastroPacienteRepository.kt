
package com.example.projsaude

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
private data class PacienteCadastro(
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

        // Valida e converte a data para o formato do banco.
        val dataNascimento = try {
            LocalDate.parse(
                nascimento.trim(),
                DateTimeFormatter.ofPattern(
                    "dd/MM/uuuu",
                    Locale("pt", "BR")
                )
            ).toString()
        } catch (e: Exception) {
            throw Exception(
                "Data de nascimento inválida. Use DD/MM/AAAA."
            )
        }

        val cpfLimpo = cpf.filter { it.isDigit() }

        if (nome.isBlank()) {
            throw Exception("Informe o nome completo.")
        }

        if (cpfLimpo.length != 11) {
            throw Exception("O CPF deve conter 11 números.")
        }

        if (telefone.isBlank()) {
            throw Exception("Informe o telefone.")
        }

        if (email.isBlank()) {
            throw Exception("Informe o e-mail.")
        }

        if (senha.length < 6) {
            throw Exception(
                "A senha deve possuir pelo menos 6 caracteres."
            )
        }

        // Cria a conta e envia os dados para o gatilho SQL.
        cliente.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = senha

            data = buildJsonObject {
                put("nome", nome.trim())
                put("cpf", cpfLimpo)
                put("data_nascimento", dataNascimento)
                put("telefone", telefone.trim())
            }
        }

        // Verifica se o Supabase liberou uma sessão.
        val usuarioId = cliente.auth.currentUserOrNull()?.id

        if (usuarioId == null) {
            return ResultadoCadastroPaciente.AguardandoConfirmacao(
                "Conta criada com sucesso! " +
                        "Confirme seu e-mail para acessar o aplicativo."
            )
        }

        // Localiza o paciente criado automaticamente pelo gatilho.
        val paciente = cliente
            .from("pacientes")
            .select {
                filter {
                    eq("usuario_id", usuarioId)
                }
            }
            .decodeSingle<PacienteCadastro>()

        // Se não houver foto, o cadastro já está concluído.
        if (fotoPedido == null) {
            return ResultadoCadastroPaciente.Sucesso(
                "Cadastro do paciente realizado com sucesso!"
            )
        }

        // Verifica o tamanho da imagem.
        val tamanho = context.contentResolver
            .openAssetFileDescriptor(fotoPedido, "r")
            ?.use { it.length }
            ?: -1L

        if (tamanho > 10L * 1024L * 1024L) {
            throw Exception("A imagem deve ter no máximo 10 MB.")
        }

        // Verifica o tipo de arquivo.
        val tipo = context.contentResolver.getType(fotoPedido)

        val extensao = when (tipo?.lowercase()) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> throw Exception(
                "Selecione uma imagem JPG, PNG ou WEBP."
            )
        }

        val bytes = context.contentResolver
            .openInputStream(fotoPedido)
            ?.use { it.readBytes() }
            ?: throw Exception("Não foi possível ler a imagem.")

        if (bytes.size > 10 * 1024 * 1024) {
            throw Exception("A imagem deve ter no máximo 10 MB.")
        }

        // O caminho utiliza o UUID do registro do paciente.
        val caminho = "${paciente.id}/pedido_medico.$extensao"

        // Envia a imagem para o bucket privado.
        cliente.storage
            .from("pedidos-medicos")
            .upload(caminho, bytes) {
                upsert = true
            }

        // Atualiza o caminho da imagem no cadastro.
        cliente
            .from("pacientes")
            .update(AtualizacaoFoto(caminho)) {
                filter {
                    eq("id", paciente.id)
                }
            }

        return ResultadoCadastroPaciente.Sucesso(
            "Cadastro do paciente realizado com sucesso!"
        )
    }
}
