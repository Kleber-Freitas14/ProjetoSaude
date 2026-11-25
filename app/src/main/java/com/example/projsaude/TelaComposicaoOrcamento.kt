package com.example.projsaude

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class PrecoComposicao(
    val codigo: String,
    val valor: JsonPrimitive? = null
)

@Serializable
private data class OrcamentoComposicao(
    val status: String? = null,
    val hospedagem: String? = null,
    val aceito_em: String? = null,
    val necessita_uti: Boolean? = null
)

@Serializable
private data class ItemComposicaoSalvo(
    val descricao: String,
    val quantidade: JsonPrimitive,
    val valor_unitario: JsonPrimitive,
    val valor_total: JsonPrimitive
)

@Serializable
private data class PeriodoComposicaoSalvo(
    val tipo_acomodacao: String,
    val data_entrada: String,
    val data_saida: String
)

@Serializable
private data class ComposicaoSalva(
    val salvo_em: String? = null,
    val itens_composicao: List<ItemComposicaoSalvo> = emptyList(),
    val periodos_internacao: List<PeriodoComposicaoSalvo> = emptyList()
)

private data class PeriodoParaSalvar(
    val tipo: String,
    val entrada: LocalDate,
    val saida: LocalDate
) {
    val diarias: Long
        get() = ChronoUnit.DAYS.between(entrada, saida) + 1
}

private data class LinhaParaSalvar(
    val codigo: String,
    val nome: String,
    val quantidade: Long,
    val unitario: BigDecimal?
) {
    val subtotal: BigDecimal?
        get() = unitario?.multiply(BigDecimal.valueOf(quantidade))
}

private val dataBrComposicao = DateTimeFormatter
    .ofPattern("dd/MM/uuuu", Locale.forLanguageTag("pt-BR"))
    .withResolverStyle(ResolverStyle.STRICT)

private val acomodacoesParaSalvar = linkedMapOf(
    "Enfermaria" to "diaria_enfermaria",
    "Apartamento Individual" to "diaria_apartamento_individual",
    "Apartamento Duplo" to "diaria_apartamento_duplo"
)

private val portesParaSalvar = linkedMapOf(
    "porte_cirurgico_i" to "Porte I (até 2h)",
    "porte_cirurgico_ii" to "Porte II (2h a 4h)",
    "porte_cirurgico_iii" to "Porte III (4h a 6h)",
    "porte_cirurgico_iv" to "Porte IV (mais de 6h)",
    "porte_cirurgico_v" to "Porte V (grande procedimento)",
    "porte_cirurgico_vi" to "Porte VI (cirurgia especial)",
    "porte_cirurgico_vii" to "Porte VII (cirurgia especial cardíaca)"
)

private fun lerPeriodoComposicao(
    tipo: String,
    entrada: String,
    saida: String
): PeriodoParaSalvar? {
    val inicio = runCatching {
        LocalDate.parse(entrada.trim(), dataBrComposicao)
    }.getOrNull() ?: return null

    val fim = runCatching {
        LocalDate.parse(saida.trim(), dataBrComposicao)
    }.getOrNull() ?: return null

    return if (fim.isBefore(inicio)) null else PeriodoParaSalvar(tipo, inicio, fim)
}

private fun mostrarDataComposicao(data: String): String =
    runCatching { LocalDate.parse(data).format(dataBrComposicao) }.getOrDefault(data)

@Composable
fun TelaComposicaoOrcamento(
    paciente: String,
    procedimento: String,
    aoVoltar: () -> Unit,
    aoMenuPrincipal: () -> Unit,
    orcamentoId: String
) {
    val cliente = SupabaseConfig.client
    val escopo = rememberCoroutineScope()
    val contexto = LocalContext.current
    val rascunhos = remember(contexto.applicationContext) {
        RascunhosOrcamentoDatabase.obter(contexto).rascunhos()
    }
    val moeda = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
    }

    var etapa by rememberSaveable(orcamentoId) { mutableIntStateOf(1) }
    var porte by rememberSaveable(orcamentoId) { mutableStateOf<String?>(null) }
    var mostrarPortes by rememberSaveable(orcamentoId) { mutableStateOf(false) }
    var acomodacao by rememberSaveable(orcamentoId) { mutableStateOf<String?>(null) }
    var entradaUti by rememberSaveable(orcamentoId) { mutableStateOf("") }
    var saidaUti by rememberSaveable(orcamentoId) { mutableStateOf("") }
    var entradaAcomodacao by rememberSaveable(orcamentoId) { mutableStateOf("") }
    var saidaAcomodacao by rememberSaveable(orcamentoId) { mutableStateOf("") }
    var extras by rememberSaveable(orcamentoId) { mutableStateOf("0") }
    var hemoterapia by rememberSaveable(orcamentoId) { mutableStateOf("0") }
    var fisioterapia by rememberSaveable(orcamentoId) { mutableStateOf("0") }

    var orcamento by remember(orcamentoId) { mutableStateOf<OrcamentoComposicao?>(null) }
    var salvo by remember(orcamentoId) { mutableStateOf<ComposicaoSalva?>(null) }
    var precos by remember { mutableStateOf(emptyMap<String, BigDecimal>()) }
    var carregando by remember { mutableStateOf(true) }
    var processando by remember { mutableStateOf(false) }
    var erroCarga by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("") }
    var recarga by remember { mutableIntStateOf(0) }
    var confirmarExclusao by remember(orcamentoId) { mutableStateOf(false) }
    var confirmarAceitePresencial by remember(orcamentoId) { mutableStateOf(false) }
    var rascunhoConsultado by rememberSaveable(orcamentoId) { mutableStateOf(false) }
    var conferindoEnvio by rememberSaveable(orcamentoId) { mutableStateOf(false) }

    val necessitaUti = orcamento?.necessita_uti == true

    fun usuarioAtual(): String = checkNotNull(cliente.auth.currentUserOrNull()?.id)

    fun montarRascunho(usuarioId: String) = RascunhoComposicao(
        usuarioId = usuarioId,
        orcamentoId = orcamentoId,
        etapa = etapa,
        porte = porte,
        acomodacao = acomodacao ?: "Nenhum",
        entradaUti = if (necessitaUti) entradaUti else "",
        saidaUti = if (necessitaUti) saidaUti else "",
        entradaAcomodacao = entradaAcomodacao,
        saidaAcomodacao = saidaAcomodacao,
        extras = extras,
        hemoterapia = hemoterapia,
        fisioterapia = fisioterapia
    )

    fun guardarRascunho() {
        processando = true
        escopo.launch {
            try {
                rascunhos.salvar(montarRascunho(usuarioAtual()))
                mensagem = "Rascunho salvo neste aparelho."
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mensagem = "Não foi possível guardar o rascunho neste aparelho."
            } finally {
                processando = false
            }
        }
    }

    LaunchedEffect(orcamentoId, recarga) {
        carregando = true
        erroCarga = false

        try {
            val registro = cliente.from("orcamentos")
                .select(Columns.raw("status,hospedagem,aceito_em,necessita_uti")) {
                    filter { eq("id", orcamentoId) }
                }.decodeSingle<OrcamentoComposicao>()

            val composicao = cliente.from("composicoes_orcamento")
                .select(
                    Columns.raw(
                        "salvo_em," +
                                "itens_composicao(descricao,quantidade,valor_unitario,valor_total)," +
                                "periodos_internacao(tipo_acomodacao,data_entrada,data_saida)"
                    )
                ) {
                    filter { eq("orcamento_id", orcamentoId) }
                }.decodeList<ComposicaoSalva>().let { resultados ->
                    if (resultados.isEmpty()) null else resultados.single()
                }

            if (composicao == null && registro.status in listOf("Pendente", "Em espera")) {
                precos = cliente.from("valores_hospitalares")
                    .select(Columns.raw("codigo,valor")) {
                        filter { eq("ativo", true) }
                    }.decodeList<PrecoComposicao>()
                    .mapNotNull { preco ->
                        preco.valor?.content?.toBigDecimalOrNull()
                            ?.let { preco.codigo to it }
                    }.toMap()
            }

            orcamento = registro
            salvo = composicao
            if (acomodacao == null) acomodacao = registro.hospedagem ?: "Nenhum"

            val conferiuEnvio = conferindoEnvio
            if (conferiuEnvio) {
                mensagem = if (composicao != null) {
                    "Orçamento salvo no Supabase."
                } else {
                    "Composição ainda não confirmada no Supabase. O rascunho está salvo neste aparelho."
                }
                conferindoEnvio = false
            }

            try {
                if (composicao != null) {
                    rascunhos.remover(usuarioAtual(), orcamentoId)
                } else if (!rascunhoConsultado) {
                    val rascunho = rascunhos.buscar(usuarioAtual(), orcamentoId)
                    if (rascunho != null) {
                        etapa = rascunho.etapa.coerceIn(1, 4)
                        porte = rascunho.porte
                        acomodacao = rascunho.acomodacao

                        if (registro.necessita_uti == true) {
                            entradaUti = rascunho.entradaUti
                            saidaUti = rascunho.saidaUti
                        }

                        entradaAcomodacao = rascunho.entradaAcomodacao
                        saidaAcomodacao = rascunho.saidaAcomodacao
                        extras = rascunho.extras
                        hemoterapia = rascunho.hemoterapia
                        fisioterapia = rascunho.fisioterapia

                        if (!conferiuEnvio) mensagem = "Rascunho recuperado deste aparelho."
                    }
                    rascunhoConsultado = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (mensagem.isBlank()) {
                    mensagem = if (composicao != null) {
                        "Orçamento salvo no Supabase. Não foi possível limpar o rascunho local."
                    } else {
                        "Não foi possível carregar o rascunho neste aparelho."
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            erroCarga = true
        } finally {
            carregando = false
        }
    }

    fun registrarAceitePresencial() {
        if (processando || carregando || erroCarga ||
            orcamento?.status != "Pendente" || salvo?.salvo_em.isNullOrBlank()
        ) return

        processando = true
        mensagem = ""

        escopo.launch {
            try {
                cliente.postgrest.rpc(
                    "registrar_aceite_presencial",
                    buildJsonObject { put("p_orcamento_id", orcamentoId) }
                )

                orcamento = orcamento?.copy(status = "Aceito")
                mensagem = "Aceite presencial registrado. O orçamento está em Orçamentos aprovados."
                carregando = true
                recarga++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val detalhe = e.message.orEmpty()
                mensagem = when {
                    "PGRST202" in detalhe ->
                        "A função de aceite presencial não foi encontrada no Supabase (PGRST202)."
                    "42501" in detalhe ->
                        "Sua conta não tem permissão para registrar o aceite presencial."
                    else ->
                        "Não foi possível confirmar o aceite. Atualize o orçamento e confira o status."
                }
            } finally {
                confirmarAceitePresencial = false
                processando = false
            }
        }
    }

    fun gerenciar(acao: String) {
        if (processando || carregando) return
        processando = true
        mensagem = ""

        escopo.launch {
            try {
                cliente.postgrest.rpc(
                    "gerenciar_orcamento",
                    buildJsonObject {
                        put("p_orcamento_id", orcamentoId)
                        put("p_acao", acao)
                    }
                )

                if (acao == "excluir") {
                    val aindaExiste = cliente.from("orcamentos")
                        .select(Columns.raw("status")) {
                            filter { eq("id", orcamentoId) }
                        }
                        .decodeList<OrcamentoComposicao>()
                        .isNotEmpty()

                    if (aindaExiste) {
                        mensagem = "O Supabase ainda retorna este orçamento. Atualize a tela e confira o status."
                    } else {
                        aoVoltar()
                    }
                } else {
                    carregando = true
                    recarga++
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val detalhe = e.message.orEmpty()
                mensagem = when {
                    acao == "excluir" && "PGRST202" in detalhe ->
                        "A função de exclusão não foi encontrada no Supabase (PGRST202)."
                    acao == "excluir" && "42501" in detalhe ->
                        "Sua conta não tem permissão para excluir este orçamento (42501)."
                    else -> "A ação não foi confirmada. Atualize o orçamento para conferir o status."
                }
            } finally {
                if (acao == "excluir") confirmarExclusao = false
                processando = false
            }
        }
    }

    val voltarOuCancelar: () -> Unit = {
        if (!processando && !carregando) {
            when {
                confirmarExclusao -> confirmarExclusao = false
                confirmarAceitePresencial -> confirmarAceitePresencial = false
                salvo == null && etapa > 1 && !erroCarga -> etapa--
                else -> aoVoltar()
            }
        }
    }

    BackHandler { voltarOuCancelar() }

    val tipoAcomodacao = acomodacao ?: "Nenhum"
    val codigoAcomodacao = acomodacoesParaSalvar[tipoAcomodacao]
    val periodoUti = if (necessitaUti) {
        lerPeriodoComposicao("UTI", entradaUti, saidaUti)
    } else {
        null
    }

    val periodoAcomodacao = if (codigoAcomodacao == null) {
        null
    } else {
        lerPeriodoComposicao(tipoAcomodacao, entradaAcomodacao, saidaAcomodacao)
    }

    val erroUti = necessitaUti &&
            (entradaUti.isNotBlank() || saidaUti.isNotBlank()) &&
            periodoUti == null
    val erroAcomodacao = codigoAcomodacao != null && periodoAcomodacao == null

    fun linha(codigo: String, nome: String, quantidade: Long) =
        LinhaParaSalvar(codigo, nome, quantidade, precos[codigo])

    val taxas = buildList {
        add(linha("taxa_internacao", "Taxa de internação", 1))
        porte?.let { codigo ->
            add(linha(codigo, portesParaSalvar.getValue(codigo), 1))
        }
    }

    val internacao = buildList {
        periodoUti?.let { periodo ->
            val dias = periodo.diarias
            add(linha("diaria_uti", "Diária de UTI", dias))
            add(linha("taxa_uti", "Taxa de UTI", 1))
            add(linha("respirador_volume", "Respirador a volume", dias))
            add(linha("oxigenio_diaria", "Oxigênio (10 unidades por diária de UTI)", dias * 10))
        }

        if (codigoAcomodacao != null && periodoAcomodacao != null) {
            val dias = periodoAcomodacao.diarias
            add(linha(codigoAcomodacao, "Diária de $tipoAcomodacao", dias))
            add(linha("alimentacao_acompanhante", "Alimentação de acompanhante", dias))
        }
    }

    val servicos = listOf(
        linha("procedimento_extra", "Procedimentos extras", extras.toLongOrNull() ?: 0),
        linha("hemoterapia", "Hemoterapia", hemoterapia.toLongOrNull() ?: 0),
        linha("fisioterapia", "Fisioterapia", fisioterapia.toLongOrNull() ?: 0)
    ).filter { it.quantidade > 0 }

    val linhas = taxas + internacao + servicos
    val semPreco = linhas.filter { it.unitario == null }
    val totalPrevisto = linhas.fold(BigDecimal.ZERO) { total, item ->
        total.add(item.subtotal ?: BigDecimal.ZERO)
    }

    fun salvar() {
        val periodos = listOfNotNull(periodoUti, periodoAcomodacao)
        val parametros = buildJsonObject {
            put("p_orcamento_id", orcamentoId)
            put("p_itens", buildJsonArray {
                linhas.forEach { item ->
                    add(buildJsonObject {
                        put("codigo", item.codigo)
                        put("quantidade", item.quantidade)
                    })
                }
            })
            put("p_periodos", buildJsonArray {
                periodos.forEach { periodo ->
                    add(buildJsonObject {
                        put("tipo_acomodacao", periodo.tipo)
                        put("data_entrada", periodo.entrada.toString())
                        put("data_saida", periodo.saida.toString())
                    })
                }
            })
        }

        processando = true
        mensagem = ""
        escopo.launch {
            try {
                rascunhos.salvar(montarRascunho(usuarioAtual()))
                conferindoEnvio = true
                try {
                    cliente.postgrest.rpc("salvar_composicao_orcamento", parametros)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    mensagem = "Envio a confirmar. O rascunho foi mantido neste aparelho."
                }
                carregando = true
                recarga++
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mensagem = "Não foi possível guardar o rascunho para enviar o orçamento."
            } finally {
                processando = false
            }
        }
    }

    val status = orcamento?.status
    val composicaoSalva = salvo
    val podeMontar = status == "Pendente" || status == "Em espera"
    val cores = MaterialTheme.colorScheme
    val rolagem = rememberScrollState()

    LaunchedEffect(etapa, composicaoSalva != null) {
        rolagem.scrollTo(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cores.background)
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rolagem),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CabecalhoComposicao(paciente, procedimento, status)

            when {
                carregando -> CircularProgressIndicator()

                erroCarga -> {
                    AvisoComposicao(
                        "Não foi possível carregar o orçamento ou seus preços.",
                        erro = true
                    )
                    OutlinedButton(onClick = { recarga++ }) { Text("Tentar novamente") }
                }

                composicaoSalva != null -> {
                    val totalSalvo = composicaoSalva.itens_composicao.fold(BigDecimal.ZERO) { total, item ->
                        total.add(item.valor_total.content.toBigDecimal())
                    }
                    val valorSalvo = moeda.format(totalSalvo)
                    CartaoTotalComposicao("Total salvo", valorSalvo)

                    if (composicaoSalva.periodos_internacao.isNotEmpty()) {
                        TituloComposicao("Períodos de internação")
                    }

                    composicaoSalva.periodos_internacao.forEach { periodo ->
                        Text(
                            "${periodo.tipo_acomodacao}: " +
                                    "${mostrarDataComposicao(periodo.data_entrada)} a " +
                                    mostrarDataComposicao(periodo.data_saida)
                        )
                    }

                    if (composicaoSalva.itens_composicao.isNotEmpty()) {
                        TituloComposicao("Itens do orçamento")
                    }

                    composicaoSalva.itens_composicao.forEach { item ->
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = item.descricao,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${item.quantidade.content} × " +
                                            "${moeda.format(item.valor_unitario.content.toBigDecimal())} = " +
                                            moeda.format(item.valor_total.content.toBigDecimal()),
                                    color = cores.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (status == "Aceito") {
                        AvisoComposicao("Aceite do paciente registrado. A composição está bloqueada.")
                    }

                    if (status == "Pendente" && !composicaoSalva.salvo_em.isNullOrBlank()) {
                        if (confirmarAceitePresencial) {
                            AvisoComposicao(
                                "Confirme que $paciente concordou presencialmente com o orçamento de $valorSalvo."
                            )

                            Button(
                                onClick = ::registrarAceitePresencial,
                                enabled = !processando,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (processando) "Registrando…"
                                    else "Confirmar aceite presencial"
                                )
                            }

                            TextButton(
                                onClick = { confirmarAceitePresencial = false },
                                enabled = !processando
                            ) { Text("Cancelar") }
                        } else {
                            Button(
                                onClick = {
                                    mensagem = ""
                                    confirmarAceitePresencial = true
                                },
                                enabled = !processando && !confirmarExclusao,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Registrar aceite presencial") }
                        }
                    }
                }

                !podeMontar -> AvisoComposicao(
                    "Este orçamento não tem composição salva disponível."
                )

                processando -> CircularProgressIndicator()

                else -> {
                    Text(
                        text = "Etapa $etapa de 4",
                        style = MaterialTheme.typography.labelLarge,
                        color = cores.onSurfaceVariant
                    )

                    LinearProgressIndicator(
                        progress = { etapa / 4f },
                        modifier = Modifier.fillMaxWidth()
                    )

                    when (etapa) {
                        1 -> {
                            TituloComposicao("Taxas e porte cirúrgico")

                            OutlinedButton(
                                onClick = { mostrarPortes = !mostrarPortes },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (mostrarPortes) "Fechar opções de porte"
                                    else portesParaSalvar[porte] ?: "Selecionar porte cirúrgico"
                                )
                            }

                            if (mostrarPortes) {
                                TextButton(
                                    onClick = {
                                        porte = null
                                        mostrarPortes = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Sem porte cirúrgico") }

                                portesParaSalvar.forEach { (codigo, nome) ->
                                    TextButton(
                                        onClick = {
                                            porte = codigo
                                            mostrarPortes = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(nome) }
                                }
                            }

                            taxas.forEach { LinhaOrcamentoComposicao(it, moeda) }
                        }

                        2 -> {
                            TituloComposicao("Internação e acomodação")

                            if (necessitaUti) {
                                CampoOrcamentoComposicao("Entrada na UTI", entradaUti) {
                                    entradaUti = it
                                }
                                CampoOrcamentoComposicao("Saída da UTI", saidaUti) {
                                    saidaUti = it
                                }
                                if (erroUti) {
                                    AvisoComposicao(
                                        "Confira as datas da UTI no formato DD/MM/AAAA.",
                                        erro = true
                                    )
                                }
                            }

                            (listOf("Nenhum") + acomodacoesParaSalvar.keys).forEach { opcao ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = tipoAcomodacao == opcao,
                                        onClick = { acomodacao = opcao }
                                    )
                                    Text(opcao)
                                }
                            }

                            if (codigoAcomodacao != null) {
                                CampoOrcamentoComposicao("Entrada na acomodação", entradaAcomodacao) {
                                    entradaAcomodacao = it
                                }
                                CampoOrcamentoComposicao("Saída da acomodação", saidaAcomodacao) {
                                    saidaAcomodacao = it
                                }
                                if (erroAcomodacao) {
                                    AvisoComposicao(
                                        "Preencha as duas datas válidas ou selecione Nenhum.",
                                        erro = true
                                    )
                                }
                            }

                            internacao.forEach { LinhaOrcamentoComposicao(it, moeda) }
                        }

                        3 -> {
                            TituloComposicao("Procedimentos e serviços")

                            CampoOrcamentoComposicao("Procedimentos extras", extras, true) {
                                extras = it
                            }
                            CampoOrcamentoComposicao("Hemoterapia", hemoterapia, true) {
                                hemoterapia = it
                            }
                            CampoOrcamentoComposicao("Fisioterapia", fisioterapia, true) {
                                fisioterapia = it
                            }

                            servicos.forEach { LinhaOrcamentoComposicao(it, moeda) }
                        }

                        4 -> {
                            TituloComposicao("Resumo")

                            val tituloTotal = if (semPreco.isEmpty()) "Total previsto" else "Total parcial"
                            CartaoTotalComposicao(tituloTotal, moeda.format(totalPrevisto))

                            if (semPreco.isNotEmpty()) {
                                AvisoComposicao(
                                    "Cadastre os preços faltantes antes de salvar.",
                                    erro = true
                                )
                            }

                            linhas.forEach { LinhaOrcamentoComposicao(it, moeda) }

                            Text(
                                text = "O banco grava os preços vigentes no momento do salvamento.",
                                style = MaterialTheme.typography.bodySmall,
                                color = cores.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (!carregando && !erroCarga && composicaoSalva != null) {
                if (status == "Pendente" || status == "Em espera") {
                    OutlinedButton(
                        onClick = {
                            gerenciar(if (status == "Pendente") "aguardar" else "retomar")
                        },
                        enabled = !processando && !confirmarExclusao && !confirmarAceitePresencial
                    ) {
                        Text(if (status == "Pendente") "Colocar em espera" else "Retomar")
                    }
                }

                if (status in listOf("Pendente", "Em espera", "Recusado", "Cancelado")) {
                    if (confirmarExclusao) {
                        AvisoComposicao(
                            "Excluir definitivamente este orçamento e sua composição?",
                            erro = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { gerenciar("excluir") },
                                enabled = !processando,
                                colors = ButtonDefaults.textButtonColors(contentColor = cores.error)
                            ) {
                                Text(if (processando) "Excluindo…" else "Confirmar exclusão")
                            }

                            TextButton(
                                onClick = { confirmarExclusao = false },
                                enabled = !processando
                            ) { Text("Cancelar") }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { confirmarExclusao = true },
                            enabled = !processando && !confirmarAceitePresencial,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = cores.error)
                        ) { Text("Excluir orçamento") }
                    }
                }
            }
        }

        if (mensagem.isNotBlank()) {
            AvisoComposicao(mensagem)
        }

        if (!carregando && !erroCarga && composicaoSalva == null && podeMontar) {
            OutlinedButton(
                onClick = ::guardarRascunho,
                enabled = !processando,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar rascunho neste aparelho") }

            if (etapa < 4) {
                Button(
                    onClick = { etapa++ },
                    enabled = !processando && (etapa != 2 || (!erroUti && !erroAcomodacao)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Próximo") }
            } else {
                Button(
                    onClick = { salvar() },
                    enabled = !processando && semPreco.isEmpty() && !erroUti && !erroAcomodacao,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (processando) "Salvando…" else "Salvar orçamento") }
            }
        }

        if (!carregando && !erroCarga && composicaoSalva != null) {
            Button(
                onClick = aoMenuPrincipal,
                enabled = !processando,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Voltar ao menu principal") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = voltarOuCancelar,
                enabled = !processando && !carregando,
                modifier = Modifier.weight(1f)
            ) { Text("Voltar") }

            if (!carregando) {
                TextButton(
                    onClick = {
                        confirmarExclusao = false
                        confirmarAceitePresencial = false
                        recarga++
                    },
                    enabled = !processando
                ) { Text("Atualizar") }
            }
        }
    }
}

@Composable
private fun CabecalhoComposicao(paciente: String, procedimento: String, status: String?) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF10345E), Color(0xFF064FB5))
                ),
                shape = MaterialTheme.shapes.large
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Composição do orçamento",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = paciente,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Text(procedimento, color = Color(0xFFE1EEFC))

        status?.let {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color.White,
                contentColor = Color(0xFF10345E)
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun TituloComposicao(titulo: String) {
    Text(
        text = titulo,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun CartaoTotalComposicao(titulo: String, valor: String) {
    val cores = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = cores.primaryContainer,
        contentColor = cores.onPrimaryContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(titulo, style = MaterialTheme.typography.labelLarge)
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AvisoComposicao(texto: String, erro: Boolean = false) {
    val cores = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth()
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

@Composable
private fun LinhaOrcamentoComposicao(item: LinhaParaSalvar, moeda: NumberFormat) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.nome,
                style = MaterialTheme.typography.titleMedium
            )

            val unitario = item.unitario
            val subtotal = item.subtotal

            Text(
                text = if (unitario == null || subtotal == null) {
                    "Preço indisponível"
                } else {
                    "${item.quantidade} × ${moeda.format(unitario)} = ${moeda.format(subtotal)}"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CampoOrcamentoComposicao(
    titulo: String,
    valor: String,
    quantidade: Boolean = false,
    aoAlterar: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { novo ->
            if (!quantidade || (novo.length <= 9 && novo.all { it in '0'..'9' })) {
                aoAlterar(novo)
            }
        },
        label = { Text(titulo) },
        placeholder = { if (!quantidade) Text("DD/MM/AAAA") },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (quantidade) KeyboardType.Number else KeyboardType.Text
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}