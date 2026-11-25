package com.example.projsaude

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private data class PacienteConsulta(
    val id: String,
    val nome_completo: String,
    val cpf: String? = null
) {
    fun rotulo(): String {
        val finalCpf = cpf.orEmpty().filter { it in '0'..'9' }.takeLast(4)
        return if (finalCpf.isEmpty()) nome_completo else "$nome_completo · CPF final $finalCpf"
    }
}

@Serializable
private data class MedicoConsulta(
    val id: String,
    val nome: String,
    val crm: String? = null,
    val uf_crm: String? = null
) {
    fun rotulo(): String {
        val registro = listOfNotNull(crm, uf_crm).filter { it.isNotBlank() }.joinToString("/")
        return if (registro.isEmpty()) nome else "$nome · CRM $registro"
    }
}

@Serializable
private data class ConsultaSalva(
    val id: String,
    val data_consulta: String,
    val horario: String,
    val observacoes: String? = null,
    val paciente: PacienteConsulta? = null,
    val profissional: MedicoConsulta? = null
)

@Serializable
private data class NovaConsulta(
    val paciente_id: String,
    val profissional_id: String,
    val data_consulta: String,
    val horario: String,
    val observacoes: String,
    val criado_por: String
)

@Composable
fun TelaConsultas(aoVoltar: () -> Unit) {
    val cliente = SupabaseConfig.client
    val escopo = rememberCoroutineScope()
    val cores = MaterialTheme.colorScheme
    val formas = MaterialTheme.shapes

    val formatoData = remember {
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    }
    val formatoHora = remember {
        DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT)
    }

    var buscaPaciente by remember { mutableStateOf("") }
    var buscaMedico by remember { mutableStateOf("") }
    var paciente by remember { mutableStateOf<PacienteConsulta?>(null) }
    var medico by remember { mutableStateOf<MedicoConsulta?>(null) }
    var sugestoesPacientes by remember { mutableStateOf(emptyList<PacienteConsulta>()) }
    var sugestoesMedicos by remember { mutableStateOf(emptyList<MedicoConsulta>()) }
    var avisoPaciente by remember { mutableStateOf("") }
    var avisoMedico by remember { mutableStateOf("") }

    var data by remember { mutableStateOf("") }
    var horario by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    var consultaGravada by remember { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }
    var consultas by remember { mutableStateOf(emptyList<ConsultaSalva>()) }
    var carregando by remember { mutableStateOf(true) }
    var erroLista by remember { mutableStateOf(false) }
    var recarga by remember { mutableIntStateOf(0) }

    BackHandler(enabled = salvando) {}

    LaunchedEffect(buscaPaciente, paciente) {
        sugestoesPacientes = emptyList()
        avisoPaciente = ""

        val termo = buscaPaciente.trim().replace(Regex("[*%_]"), "")
        if (paciente != null || termo.length < 2) return@LaunchedEffect

        val pareceCpf = termo.all { it in '0'..'9' || it in ".- " }
        val digitos = termo.filter { it in '0'..'9' }

        if (pareceCpf && digitos.length != 11) {
            avisoPaciente = "Digite os 11 dígitos para buscar pelo CPF."
            return@LaunchedEffect
        }

        try {
            delay(300)

            sugestoesPacientes = cliente.from("pacientes")
                .select(Columns.raw("id,nome_completo,cpf")) {
                    filter {
                        eq("ativo", true)
                        if (pareceCpf) {
                            val formatado = "${digitos.take(3)}.${digitos.substring(3, 6)}." +
                                    "${digitos.substring(6, 9)}-${digitos.takeLast(2)}"
                            or {
                                eq("cpf", digitos)
                                eq("cpf", formatado)
                            }
                        } else {
                            ilike("nome_completo", "%$termo%")
                        }
                    }
                    limit(12)
                }
                .decodeList<PacienteConsulta>()
                .sortedBy { it.nome_completo.lowercase() }

            if (sugestoesPacientes.isEmpty()) {
                avisoPaciente = "Nenhum paciente ativo encontrado."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            avisoPaciente = "Não foi possível buscar pacientes no banco."
        }
    }

    LaunchedEffect(buscaMedico, medico) {
        sugestoesMedicos = emptyList()
        avisoMedico = ""

        val termo = buscaMedico.trim().replace(Regex("[*%_]"), "")
        if (medico != null || termo.length < 2) return@LaunchedEffect

        try {
            delay(300)

            sugestoesMedicos = cliente.from("medicos")
                .select(Columns.raw("id,nome,crm,uf_crm")) {
                    filter {
                        eq("ativo", true)
                        ilike("nome", "%$termo%")
                    }
                    limit(12)
                }
                .decodeList<MedicoConsulta>()
                .sortedBy { it.nome.lowercase() }

            if (sugestoesMedicos.isEmpty()) {
                avisoMedico = "Nenhum médico ativo encontrado."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            avisoMedico = "Não foi possível buscar médicos no banco."
        }
    }

    LaunchedEffect(recarga) {
        carregando = true
        erroLista = false

        try {
            consultas = cliente.from("consultas")
                .select(
                    Columns.raw(
                        "id,data_consulta,horario,observacoes," +
                                "paciente:pacientes(id,nome_completo)," +
                                "profissional:medicos(id,nome,crm,uf_crm)"
                    )
                )
                .decodeList<ConsultaSalva>()
                .sortedWith(
                    compareByDescending<ConsultaSalva> { it.data_consulta }
                        .thenByDescending { it.horario }
                )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            erroLista = true
        } finally {
            carregando = false
        }
    }

    fun cadastrarConsulta() {
        if (salvando) return
        consultaGravada = false

        val pacienteId = paciente?.id
        val medicoId = medico?.id
        val dia = runCatching { LocalDate.parse(data.trim(), formatoData) }.getOrNull()
        val hora = runCatching { LocalTime.parse(horario.trim(), formatoHora) }.getOrNull()
        val autor = cliente.auth.currentUserOrNull()?.id

        if (pacienteId == null || medicoId == null || dia == null || hora == null) {
            mensagem = "Selecione o paciente e o médico e confira a data e o horário."
            return
        }

        if (autor == null) {
            mensagem = "Entre novamente para cadastrar a consulta."
            return
        }

        val nova = NovaConsulta(
            paciente_id = pacienteId,
            profissional_id = medicoId,
            data_consulta = dia.toString(),
            horario = hora.format(formatoHora),
            observacoes = observacoes.trim(),
            criado_por = autor
        )

        salvando = true
        mensagem = ""

        escopo.launch {
            try {
                cliente.from("consultas").insert(nova)
                paciente = null
                medico = null
                buscaPaciente = ""
                buscaMedico = ""
                data = ""
                horario = ""
                observacoes = ""
                consultaGravada = true
                mensagem = "Consulta salva no banco."
                recarga++
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mensagem =
                    "Não foi possível confirmar a gravação. Atualize a lista antes de tentar novamente."
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
                Text(
                    text = "Consultas",
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
                            text = "Nova consulta",
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = buscaPaciente,
                            onValueChange = {
                                paciente = null
                                sugestoesPacientes = emptyList()
                                buscaPaciente = it
                            },
                            label = { Text("Nome ou CPF do paciente") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            enabled = !salvando,
                            singleLine = true
                        )

                        if (avisoPaciente.isNotEmpty()) {
                            Text(
                                text = avisoPaciente,
                                style = MaterialTheme.typography.bodySmall,
                                color = cores.onSurfaceVariant
                            )
                        }

                        sugestoesPacientes.forEach { opcao ->
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !salvando,
                                onClick = {
                                    paciente = opcao
                                    buscaPaciente = opcao.rotulo()
                                    sugestoesPacientes = emptyList()
                                }
                            ) {
                                Text(opcao.rotulo())
                            }
                        }

                        OutlinedTextField(
                            value = buscaMedico,
                            onValueChange = {
                                medico = null
                                sugestoesMedicos = emptyList()
                                buscaMedico = it
                            },
                            label = { Text("Nome do médico") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            enabled = !salvando,
                            singleLine = true
                        )

                        if (avisoMedico.isNotEmpty()) {
                            Text(
                                text = avisoMedico,
                                style = MaterialTheme.typography.bodySmall,
                                color = cores.onSurfaceVariant
                            )
                        }

                        sugestoesMedicos.forEach { opcao ->
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !salvando,
                                onClick = {
                                    medico = opcao
                                    buscaMedico = opcao.rotulo()
                                    sugestoesMedicos = emptyList()
                                }
                            ) {
                                Text(opcao.rotulo())
                            }
                        }

                        HorizontalDivider(color = cores.outlineVariant)

                        OutlinedTextField(
                            value = data,
                            onValueChange = { data = it },
                            label = { Text("Data da consulta") },
                            placeholder = { Text("DD/MM/AAAA") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            enabled = !salvando,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = horario,
                            onValueChange = { horario = it },
                            label = { Text("Horário") },
                            placeholder = { Text("HH:MM") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            enabled = !salvando,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = observacoes,
                            onValueChange = { observacoes = it },
                            label = { Text("Observações") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            enabled = !salvando
                        )

                        if (mensagem.isNotEmpty()) {
                            AvisoConsulta(mensagem, erro = !consultaGravada)
                        }

                        Button(
                            onClick = ::cadastrarConsulta,
                            enabled = !salvando,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = formas.small
                        ) {
                            if (salvando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = cores.onSurfaceVariant
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(if (salvando) "Salvando…" else "Cadastrar consulta")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Consultas cadastradas",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                TextButton(onClick = { recarga++ }, enabled = !carregando && !salvando) {
                    Text(if (erroLista) "Tentar novamente" else "Atualizar lista")
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
                        Text("Carregando consultas…")
                    }

                    erroLista -> AvisoConsulta(
                        "Não foi possível carregar a lista de consultas.",
                        erro = true
                    )

                    consultas.isEmpty() -> AvisoConsulta("Nenhuma consulta cadastrada.")
                }
            }

            if (!carregando && !erroLista) {
                items(consultas, key = { it.id }) { consulta ->
                    val dia = runCatching {
                        LocalDate.parse(consulta.data_consulta).format(formatoData)
                    }.getOrDefault(consulta.data_consulta)

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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = formas.small,
                                color = cores.primaryContainer,
                                contentColor = cores.onPrimaryContainer
                            ) {
                                Text(
                                    text = "$dia às ${consulta.horario.take(5)}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            Text(
                                text = consulta.paciente?.nome_completo ?: "Paciente indisponível",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "Médico: ${consulta.profissional?.rotulo() ?: "Indisponível"}",
                                color = cores.onSurfaceVariant
                            )

                            consulta.observacoes?.takeIf { it.isNotBlank() }?.let { texto ->
                                HorizontalDivider(color = cores.outlineVariant)
                                Text(
                                    text = texto,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cores.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = aoVoltar,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !salvando
                ) {
                    Text("Voltar")
                }
            }
        }
    }
}

@Composable
private fun AvisoConsulta(texto: String, erro: Boolean = false) {
    val cores = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.small,
        color = if (erro) cores.errorContainer else cores.secondaryContainer,
        contentColor = if (erro) cores.onErrorContainer else cores.onSecondaryContainer
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}