package com.example.projsaude

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private enum class PaginaOrcamentos(val titulo: String) {
    MENU("Orçamentos"),
    NOVO("Novo orçamento"),
    RASCUNHOS("Retomar composição"),
    ANALISE("Orçamentos em análise"),
    APROVADOS("Orçamentos aprovados")
}

@Serializable
private data class PacienteDoOrcamento(
    val id: String,
    val nome_completo: String,
    val cpf: String? = null
) {
    fun rotulo(): String {
        val finalCpf = cpf.orEmpty().filter { it in '0'..'9' }.takeLast(4)
        return if (finalCpf.isEmpty()) nome_completo
        else "$nome_completo · CPF final $finalCpf"
    }
}

@Serializable
private data class MedicoDoOrcamento(
    val id: String,
    val nome: String,
    val crm: String? = null,
    val uf_crm: String? = null,
    val atuacao: String? = null
) {
    fun rotulo(): String {
        val registro = listOfNotNull(crm, uf_crm)
            .filter { it.isNotBlank() }
            .joinToString("/")
        return if (registro.isEmpty()) nome else "$nome · CRM $registro"
    }

    fun atuacaoNormalizada(): String =
        Normalizer.normalize(atuacao.orEmpty().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
}

@Serializable
private data class ProcedimentoDoOrcamento(val id: String, val nome: String)

@Serializable
private data class OrcamentoDaEquipe(
    val id: String,
    val status: String? = null,
    val data_prevista: String? = null,
    val paciente: PacienteDoOrcamento? = null,
    val procedimento: ProcedimentoDoOrcamento? = null,
    val composicao: JsonElement? = null
) {
    fun composicaoFoiSalva(): Boolean {
        val registro = when (val conteudo = composicao) {
            is JsonObject -> conteudo
            is JsonArray -> conteudo.singleOrNull() as? JsonObject
            else -> null
        }
        val data = registro?.get("salvo_em") as? JsonPrimitive
        return data?.isString == true && data.content.isNotBlank()
    }
}

@Serializable
private data class NovoOrcamentoEquipe(
    val paciente_id: String,
    val medico_cirurgiao_id: String,
    val medico_anestesista_id: String,
    val procedimento_id: String,
    val data_prevista: String,
    val descricao: String,
    val hospedagem: String,
    val necessita_uti: Boolean,
    val criado_por: String,
    val status: String
)

@Serializable
private data class IdOrcamentoCriado(val id: String)

private data class OpcaoOrcamentoEquipe(
    val id: String,
    val nome: String,
    val rotulo: String = nome
)

private data class OrcamentoParaAbrir(
    val id: String,
    val paciente: String,
    val procedimento: String
)

private val dataBrOrcamentoEquipe = DateTimeFormatter.ofPattern("dd/MM/uuuu")

private fun exibirDataOrcamentoEquipe(data: String): String =
    runCatching { LocalDate.parse(data).format(dataBrOrcamentoEquipe) }
        .getOrDefault(data)

@Composable
fun TelaOrcamentos(aoVoltar: () -> Unit) {
    var pagina by remember { mutableStateOf(PaginaOrcamentos.MENU) }
    var aberto by remember { mutableStateOf<OrcamentoParaAbrir?>(null) }

    val orcamento = aberto
    if (orcamento != null) {
        TelaComposicaoOrcamento(
            paciente = orcamento.paciente,
            procedimento = orcamento.procedimento,
            orcamentoId = orcamento.id,
            aoVoltar = {
                aberto = null
                if (pagina == PaginaOrcamentos.NOVO) {
                    pagina = PaginaOrcamentos.ANALISE
                }
            }
        )
        return
    }

    when (pagina) {
        PaginaOrcamentos.MENU -> Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Orçamentos", style = MaterialTheme.typography.headlineMedium)

            listOf(
                PaginaOrcamentos.NOVO,
                PaginaOrcamentos.ANALISE,
                PaginaOrcamentos.APROVADOS
            ).forEach { destino ->
                Button(
                    onClick = { pagina = destino },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(destino.titulo) }
            }

            OutlinedButton(onClick = aoVoltar, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar")
            }
        }

        PaginaOrcamentos.NOVO -> FormularioNovoOrcamentoEquipe(
            aoVoltar = { pagina = PaginaOrcamentos.MENU },
            aoRetomar = { pagina = PaginaOrcamentos.RASCUNHOS },
            aoAbrir = { aberto = it }
        )

        else -> ListaOrcamentosEquipe(
            pagina = pagina,
            aoVoltar = {
                pagina = if (pagina == PaginaOrcamentos.RASCUNHOS) {
                    PaginaOrcamentos.NOVO
                } else {
                    PaginaOrcamentos.MENU
                }
            },
            aoAbrir = { aberto = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioNovoOrcamentoEquipe(
    aoVoltar: () -> Unit,
    aoRetomar: () -> Unit,
    aoAbrir: (OrcamentoParaAbrir) -> Unit
) {
    val cliente = SupabaseConfig.client
    val escopo = rememberCoroutineScope()

    var paciente by remember { mutableStateOf<OpcaoOrcamentoEquipe?>(null) }
    var cirurgiao by remember { mutableStateOf<OpcaoOrcamentoEquipe?>(null) }
    var anestesista by remember { mutableStateOf<OpcaoOrcamentoEquipe?>(null) }
    var procedimento by remember { mutableStateOf<OpcaoOrcamentoEquipe?>(null) }
    var data by remember { mutableStateOf<LocalDate?>(null) }
    var mostrarCalendario by remember { mutableStateOf(false) }
    var descricao by remember { mutableStateOf("") }
    var hospedagem by remember { mutableStateOf("Enfermaria") }
    var necessitaUti by remember { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("") }

    BackHandler { if (!salvando) aoVoltar() }

    suspend fun buscarMedico(termo: String, atuacao: String): List<OpcaoOrcamentoEquipe> =
        cliente.from("medicos")
            .select(Columns.raw("id,nome,crm,uf_crm,atuacao")) {
                filter {
                    eq("ativo", true)
                    ilike("nome", "%$termo%")
                }
            }
            .decodeList<MedicoDoOrcamento>()
            .filter { it.atuacaoNormalizada() == atuacao }
            .sortedBy { it.nome.lowercase() }
            .take(8)
            .map { OpcaoOrcamentoEquipe(it.id, it.nome, it.rotulo()) }

    if (mostrarCalendario) {
        val calendario = rememberDatePickerState(
            initialSelectedDateMillis = data
                ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(
                    enabled = calendario.selectedDateMillis != null,
                    onClick = {
                        data = calendario.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).toLocalDate()
                        }
                        mostrarCalendario = false
                    }
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = calendario, showModeToggle = false)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Novo orçamento", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = aoRetomar, enabled = !salvando) {
                Text("Retomar composição pelo CPF")
            }
        }

        item {
            CampoBuscaEquipe("Nome ou CPF do paciente", paciente, { paciente = it }, !salvando) { termo ->
                val digitos = termo.filter { it in '0'..'9' }
                val buscaPorCpf = termo.all { it in '0'..'9' || it in ".- " }

                if (buscaPorCpf && digitos.length != 11) {
                    throw IllegalArgumentException("Informe os 11 dígitos do CPF.")
                }

                cliente.from("pacientes")
                    .select(Columns.raw("id,nome_completo,cpf")) {
                        filter {
                            eq("ativo", true)
                            if (buscaPorCpf) {
                                val formatado = formatarCpfOrcamento(digitos)
                                or {
                                    eq("cpf", digitos)
                                    eq("cpf", formatado)
                                }
                            } else {
                                ilike("nome_completo", "%$termo%")
                            }
                        }
                        limit(8)
                    }
                    .decodeList<PacienteDoOrcamento>()
                    .map { OpcaoOrcamentoEquipe(it.id, it.nome_completo, it.rotulo()) }
            }
        }

        item {
            CampoBuscaEquipe("Cirurgião", cirurgiao, { cirurgiao = it }, !salvando) {
                buscarMedico(it, "cirurgiao")
            }
        }

        item {
            CampoBuscaEquipe("Anestesista", anestesista, { anestesista = it }, !salvando) {
                buscarMedico(it, "anestesista")
            }
        }

        item {
            CampoBuscaEquipe("Procedimento", procedimento, { procedimento = it }, !salvando) { termo ->
                cliente.from("procedimentos")
                    .select(Columns.raw("id,nome")) {
                        filter {
                            eq("ativo", true)
                            ilike("nome", "%$termo%")
                        }
                        limit(8)
                    }
                    .decodeList<ProcedimentoDoOrcamento>()
                    .map { OpcaoOrcamentoEquipe(it.id, it.nome) }
            }
        }

        item {
            OutlinedButton(
                onClick = { mostrarCalendario = true },
                enabled = !salvando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(data?.format(dataBrOrcamentoEquipe) ?: "Selecionar data prevista")
            }

            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it },
                label = { Text("Descrição / observações") },
                enabled = !salvando,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text("Tipo de hospedagem", style = MaterialTheme.typography.titleMedium)
            listOf("Enfermaria", "Apartamento Individual", "Apartamento Duplo").forEach { opcao ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = hospedagem == opcao,
                        onClick = { hospedagem = opcao },
                        enabled = !salvando
                    )
                    Text(opcao)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = necessitaUti,
                    onCheckedChange = { necessitaUti = it },
                    enabled = !salvando
                )
                Text("Necessita de UTI")
            }
        }

        item {
            if (mensagem.isNotBlank()) {
                Text(mensagem, color = MaterialTheme.colorScheme.error)
            }

            Button(
                enabled = !salvando,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val pessoa = paciente
                    val cirurgiaoId = cirurgiao?.id
                    val anestesistaId = anestesista?.id
                    val cirurgia = procedimento
                    val dataPrevista = data
                    val usuarioId = cliente.auth.currentUserOrNull()?.id

                    if (pessoa == null || cirurgiaoId == null || anestesistaId == null ||
                        cirurgia == null || dataPrevista == null
                    ) {
                        mensagem = "Selecione o paciente, os médicos, o procedimento e a data."
                        return@Button
                    }
                    if (usuarioId == null) {
                        mensagem = "Entre novamente para cadastrar o orçamento."
                        return@Button
                    }

                    val novo = NovoOrcamentoEquipe(
                        paciente_id = pessoa.id,
                        medico_cirurgiao_id = cirurgiaoId,
                        medico_anestesista_id = anestesistaId,
                        procedimento_id = cirurgia.id,
                        data_prevista = dataPrevista.toString(),
                        descricao = descricao.trim(),
                        hospedagem = hospedagem,
                        necessita_uti = necessitaUti,
                        criado_por = usuarioId,
                        status = "Pendente"
                    )

                    salvando = true
                    mensagem = ""
                    escopo.launch {
                        try {
                            val criado = cliente.from("orcamentos")
                                .insert(novo) { select(Columns.raw("id")) }
                                .decodeSingle<IdOrcamentoCriado>()

                            aoAbrir(OrcamentoParaAbrir(criado.id, pessoa.nome, cirurgia.nome))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            mensagem = "A gravação não foi confirmada. Confira em Retomar composição antes de tentar novamente."
                        } finally {
                            salvando = false
                        }
                    }
                }
            ) {
                Text(if (salvando) "Criando orçamento…" else "Criar e abrir composição")
            }

            OutlinedButton(
                onClick = aoVoltar,
                enabled = !salvando,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Voltar") }
        }
    }
}

private fun formatarCpfOrcamento(cpf: String): String =
    "${cpf.substring(0, 3)}.${cpf.substring(3, 6)}.${cpf.substring(6, 9)}-${cpf.substring(9)}"

@Composable
private fun CampoBuscaEquipe(
    titulo: String,
    selecionado: OpcaoOrcamentoEquipe?,
    aoSelecionar: (OpcaoOrcamentoEquipe?) -> Unit,
    habilitado: Boolean,
    buscar: suspend (String) -> List<OpcaoOrcamentoEquipe>
) {
    var texto by remember { mutableStateOf("") }
    var sugestoes by remember { mutableStateOf(emptyList<OpcaoOrcamentoEquipe>()) }
    var mensagem by remember { mutableStateOf("") }

    LaunchedEffect(texto, selecionado) {
        sugestoes = emptyList()
        mensagem = ""
        val termo = texto.trim().replace(Regex("[%*_\\\\]"), "")
        if (selecionado != null || termo.length < 2) return@LaunchedEffect

        delay(300)

        try {
            sugestoes = buscar(termo)
            if (sugestoes.isEmpty()) mensagem = "Nenhum registro ativo encontrado."
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            mensagem = e.message ?: "Confira o valor digitado."
        } catch (_: Exception) {
            mensagem = "Não foi possível consultar o banco."
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = selecionado?.rotulo ?: texto,
            onValueChange = {
                texto = it
                sugestoes = emptyList()
                aoSelecionar(null)
            },
            label = { Text(titulo) },
            modifier = Modifier.fillMaxWidth(),
            enabled = habilitado,
            singleLine = true
        )

        if (mensagem.isNotBlank()) Text(mensagem)

        sugestoes.forEach { opcao ->
            OutlinedButton(
                enabled = habilitado,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    texto = opcao.nome
                    sugestoes = emptyList()
                    aoSelecionar(opcao)
                }
            ) { Text(opcao.rotulo) }
        }
    }
}

@Composable
private fun ListaOrcamentosEquipe(
    pagina: PaginaOrcamentos,
    aoVoltar: () -> Unit,
    aoAbrir: (OrcamentoParaAbrir) -> Unit
) {
    val cliente = SupabaseConfig.client
    var cpf by remember { mutableStateOf("") }
    var cpfConsultado by remember { mutableStateOf<String?>(null) }
    var busca by remember { mutableIntStateOf(0) }
    var carregando by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("") }
    var registros by remember { mutableStateOf(emptyList<OrcamentoDaEquipe>()) }

    BackHandler { aoVoltar() }

    LaunchedEffect(pagina, busca) {
        val digitos = cpfConsultado ?: return@LaunchedEffect
        carregando = true
        registros = emptyList()
        mensagem = ""

        try {
            val pacientes = cliente.from("pacientes")
                .select(Columns.raw("id,nome_completo")) {
                    filter {
                        or {
                            eq("cpf", digitos)
                            eq("cpf", formatarCpfOrcamento(digitos))
                        }
                    }
                }
                .decodeList<PacienteDoOrcamento>()

            if (pacientes.isEmpty()) {
                mensagem = "Nenhum paciente encontrado com esse CPF."
                return@LaunchedEffect
            }
            if (pacientes.size > 1) {
                mensagem = "Há mais de um cadastro com esse CPF. Confira o cadastro do paciente."
                return@LaunchedEffect
            }

            val pacienteId = pacientes.single().id
            val encontrados = cliente.from("orcamentos")
                .select(
                    Columns.raw(
                        "id,status,data_prevista," +
                                "paciente:pacientes(id,nome_completo)," +
                                "procedimento:procedimentos(id,nome)," +
                                "composicao:composicoes_orcamento(salvo_em)"
                    )
                ) {
                    filter {
                        eq("paciente_id", pacienteId)
                        if (pagina == PaginaOrcamentos.APROVADOS) {
                            eq("status", "Aceito")
                        } else {
                            or {
                                eq("status", "Pendente")
                                eq("status", "Em espera")
                            }
                        }
                    }
                }
                .decodeList<OrcamentoDaEquipe>()

            registros = encontrados.filter { orcamento ->
                when (pagina) {
                    PaginaOrcamentos.APROVADOS -> true
                    PaginaOrcamentos.RASCUNHOS -> !orcamento.composicaoFoiSalva()
                    else -> orcamento.composicaoFoiSalva()
                }
            }.sortedByDescending { it.data_prevista }

            if (registros.isEmpty()) mensagem = "Nenhum orçamento nesta situação para esse CPF."
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            mensagem = "Não foi possível buscar os orçamentos no banco."
        } finally {
            carregando = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(pagina.titulo, style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = cpf,
                onValueChange = {
                    cpf = it
                    cpfConsultado = null
                    busca++
                    registros = emptyList()
                    mensagem = ""
                },
                label = { Text("CPF do paciente") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                enabled = !carregando,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val digitos = cpf.filter { it in '0'..'9' }
                    if (digitos.length != 11) {
                        mensagem = "Informe os 11 dígitos do CPF."
                    } else {
                        cpfConsultado = digitos
                        busca++
                    }
                }
            ) { Text("Buscar orçamentos") }

            if (carregando) CircularProgressIndicator()
            if (mensagem.isNotBlank()) Text(mensagem)
        }

        items(registros, key = { it.id }) { registro ->
            val nomePaciente = registro.paciente?.nome_completo ?: "Paciente indisponível"
            val nomeProcedimento = registro.procedimento?.nome ?: "Procedimento indisponível"

            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(nomePaciente, style = MaterialTheme.typography.titleMedium)
                    Text(nomeProcedimento)

                    registro.status?.let { Text("Status: $it") }
                    registro.data_prevista?.let {
                        Text("Data prevista: ${exibirDataOrcamentoEquipe(it)}")
                    }

                    TextButton(
                        onClick = {
                            aoAbrir(OrcamentoParaAbrir(
                                registro.id, nomePaciente, nomeProcedimento
                            ))
                        }
                    ) {
                        Text(
                            if (pagina == PaginaOrcamentos.RASCUNHOS) "Continuar composição"
                            else "Ver orçamento"
                        )
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = aoVoltar, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar")
            }
        }
    }
}