package com.example.projsaude

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.postgrest.from
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

private val AzulPacientes = Color(0xFF085DD3)

private val FormatoNascimento = DateTimeFormatter
    .ofPattern("dd/MM/uuuu")
    .withResolverStyle(ResolverStyle.STRICT)

@Serializable
private data class PacienteSalvo(
    val id: String,
    val nome_completo: String,
    val cpf: String? = null,
    val data_nascimento: String? = null,
    val telefone: String? = null
)

@Serializable
private data class NovoPaciente(
    val nome_completo: String,
    val cpf: String,
    val data_nascimento: String,
    val telefone: String
)

private fun nascimentoParaExibicao(data: String?): String {
    if (data.isNullOrBlank()) return "Não informado"

    return try {
        LocalDate.parse(data).format(FormatoNascimento)
    } catch (_: DateTimeParseException) {
        data
    }
}

@Composable
fun TelaPacientes(aoVoltar: () -> Unit) {
    val escopo = rememberCoroutineScope()
    val cliente = SupabaseConfig.client

    var nome by rememberSaveable { mutableStateOf("") }
    var cpf by rememberSaveable { mutableStateOf("") }
    var nascimento by rememberSaveable { mutableStateOf("") }
    var telefone by rememberSaveable { mutableStateOf("") }

    var mensagem by remember { mutableStateOf("") }
    var cadastroSalvo by remember { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }
    var carregando by remember { mutableStateOf(true) }
    var erroLista by remember { mutableStateOf(false) }
    var recarregar by remember { mutableIntStateOf(0) }
    var pacientes by remember { mutableStateOf<List<PacienteSalvo>>(emptyList()) }

    BackHandler(enabled = salvando) {}

    LaunchedEffect(recarregar) {
        carregando = true
        erroLista = false

        try {
            pacientes = cliente.from("pacientes")
                .select {
                    filter { eq("ativo", true) }
                }
                .decodeList<PacienteSalvo>()
                .sortedBy { it.nome_completo.lowercase() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            erroLista = true
        } finally {
            carregando = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8FC))
            .safeDrawingPadding()
            .imePadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = aoVoltar, enabled = !salvando) {
                Text("← Voltar ao menu")
            }

            Text(
                text = "Cadastro de Pacientes",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = AzulPacientes
            )
        }

        item {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome completo") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !salvando,
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = cpf,
                onValueChange = { cpf = it },
                label = { Text("CPF") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !salvando,
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = nascimento,
                onValueChange = { nascimento = it },
                label = { Text("Data de nascimento (DD/MM/AAAA)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !salvando,
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = telefone,
                onValueChange = { telefone = it },
                label = { Text("Telefone") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = !salvando,
                singleLine = true
            )
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AzulPacientes),
                enabled = !salvando,
                onClick = {
                    cadastroSalvo = false

                    val nomeInformado = nome.trim()
                    val cpfInformado = cpf.filter { it in '0'..'9' }
                    val telefoneInformado = telefone.trim()
                    val dataNascimento = try {
                        LocalDate.parse(nascimento.trim(), FormatoNascimento)
                    } catch (_: DateTimeParseException) {
                        null
                    }

                    mensagem = when {
                        nomeInformado.isBlank() || cpf.isBlank() ||
                                nascimento.isBlank() || telefoneInformado.isBlank() ->
                            "Preencha todos os campos."
                        cpfInformado.length != 11 ->
                            "Informe um CPF com 11 dígitos."
                        dataNascimento == null || dataNascimento.isAfter(LocalDate.now()) ->
                            "Informe uma data de nascimento válida no formato DD/MM/AAAA."
                        else -> ""
                    }

                    if (mensagem.isEmpty() && dataNascimento != null) {
                        val novoPaciente = NovoPaciente(
                            nome_completo = nomeInformado,
                            cpf = cpfInformado,
                            data_nascimento = dataNascimento.toString(),
                            telefone = telefoneInformado
                        )

                        salvando = true
                        escopo.launch {
                            try {
                                cliente.from("pacientes").insert(novoPaciente)

                                nome = ""
                                cpf = ""
                                nascimento = ""
                                telefone = ""
                                cadastroSalvo = true
                                mensagem = "Paciente salvo no banco."
                                recarregar++
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                mensagem = "Não foi possível confirmar o cadastro. " +
                                        "Atualize a lista antes de tentar novamente."
                            } finally {
                                salvando = false
                            }
                        }
                    }
                }
            ) {
                Text(if (salvando) "Salvando…" else "Salvar paciente")
            }

            if (mensagem.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = mensagem,
                    color = if (cadastroSalvo) Color(0xFF16803D)
                    else MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            Text(
                text = "Pacientes cadastrados",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            TextButton(
                onClick = { recarregar++ },
                enabled = !carregando && !salvando
            ) {
                Text("Atualizar lista")
            }

            when {
                carregando -> CircularProgressIndicator()
                erroLista -> Text(
                    "Não foi possível carregar a lista do banco.",
                    color = MaterialTheme.colorScheme.error
                )
                pacientes.isEmpty() -> Text("Nenhum paciente ativo encontrado.")
            }
        }

        if (!carregando && !erroLista) {
            items(pacientes, key = { it.id }) { paciente ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(paciente.nome_completo, fontWeight = FontWeight.Bold)
                        Text("CPF: ${paciente.cpf ?: "Não informado"}")
                        Text("Nascimento: ${nascimentoParaExibicao(paciente.data_nascimento)}")
                        Text("Telefone: ${paciente.telefone ?: "Não informado"}")
                    }
                }
            }
        }
    }
}