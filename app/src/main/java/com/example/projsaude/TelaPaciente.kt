package com.example.projsaude

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

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
    val cores = MaterialTheme.colorScheme
    val formas = MaterialTheme.shapes

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

    fun salvarPaciente() {
        if (salvando) return
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

        if (mensagem.isNotEmpty() || dataNascimento == null) return

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cores.background)
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().fillMaxHeight(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TextButton(onClick = aoVoltar, enabled = !salvando) {
                    Text("← Voltar ao menu")
                }

                Text(
                    text = "Cadastro de pacientes",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF10345E), Color(0xFF064FB5))
                            ),
                            shape = formas.large
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = formas.large,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = cores.surface,
                        contentColor = cores.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Dados do paciente",
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = nome,
                            onValueChange = { nome = it },
                            label = { Text("Nome completo") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            enabled = !salvando,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = cpf,
                            onValueChange = { cpf = it },
                            label = { Text("CPF") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !salvando,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = nascimento,
                            onValueChange = { nascimento = it },
                            label = { Text("Data de nascimento") },
                            placeholder = { Text("DD/MM/AAAA") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            enabled = !salvando,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = telefone,
                            onValueChange = { telefone = it },
                            label = { Text("Telefone") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            enabled = !salvando,
                            singleLine = true
                        )

                        if (mensagem.isNotEmpty()) {
                            AvisoCadastroEquipe(mensagem, erro = !cadastroSalvo)
                        }

                        Button(
                            onClick = ::salvarPaciente,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = formas.small,
                            enabled = !salvando
                        ) {
                            if (salvando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = cores.onSurfaceVariant
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(if (salvando) "Salvando…" else "Salvar paciente")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Pacientes cadastrados",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cores.onBackground
                )

                TextButton(
                    onClick = { recarregar++ },
                    enabled = !carregando && !salvando
                ) {
                    Text("Atualizar lista")
                }

                when {
                    carregando -> Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Carregando pacientes…")
                    }

                    erroLista -> AvisoCadastroEquipe(
                        "Não foi possível carregar a lista do banco.",
                        erro = true
                    )

                    pacientes.isEmpty() ->
                        AvisoCadastroEquipe("Nenhum paciente ativo encontrado.")
                }
            }

            if (!carregando && !erroLista) {
                items(pacientes, key = { it.id }) { paciente ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = formas.medium,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = cores.surface,
                            contentColor = cores.onSurface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = paciente.nome_completo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "CPF: ${paciente.cpf ?: "Não informado"}",
                                color = cores.onSurfaceVariant
                            )
                            Text(
                                text = "Nascimento: ${nascimentoParaExibicao(paciente.data_nascimento)}",
                                color = cores.onSurfaceVariant
                            )
                            Text(
                                text = "Telefone: ${paciente.telefone ?: "Não informado"}",
                                color = cores.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvisoCadastroEquipe(texto: String, erro: Boolean = false) {
    val cores = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.small,
        color = if (erro) cores.errorContainer else cores.tertiaryContainer,
        contentColor = if (erro) cores.onErrorContainer else cores.onTertiaryContainer
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}