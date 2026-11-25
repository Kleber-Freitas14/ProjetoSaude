package com.example.projsaude

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class PacienteDosOrcamentos(val id: String)

@Serializable
private data class ProcedimentoDoPaciente(val nome: String)

@Serializable
private data class OrcamentoDoPaciente(
    val id: String,
    val status: String? = null,
    val data_prevista: String? = null,
    val procedimento: ProcedimentoDoPaciente? = null
)

@Serializable
private data class ItemDoOrcamentoPaciente(
    val id: String,
    val descricao: String,
    val quantidade: JsonPrimitive,
    val valor_unitario: JsonPrimitive,
    val valor_total: JsonPrimitive
)

@Serializable
private data class PeriodoDoOrcamentoPaciente(
    val id: String,
    val tipo_acomodacao: String,
    val data_entrada: String,
    val data_saida: String
)

@Serializable
private data class ComposicaoDoPaciente(
    val orcamento: OrcamentoDoPaciente,
    val salvo_em: String? = null,
    val itens_composicao: List<ItemDoOrcamentoPaciente> = emptyList(),
    val periodos_internacao: List<PeriodoDoOrcamentoPaciente> = emptyList()
) {
    fun total(): BigDecimal? {
        if (itens_composicao.isEmpty()) return null

        var soma = BigDecimal.ZERO
        for (item in itens_composicao) {
            val subtotal = item.valor_total.content.toBigDecimalOrNull() ?: return null
            soma = soma.add(subtotal)
        }
        return soma
    }
}

private val formatoDataOrcamentoPaciente =
    DateTimeFormatter.ofPattern("dd/MM/uuuu")

private val formatoComposicaoPaciente =
    DateTimeFormatter.ofPattern("dd/MM/uuuu 'às' HH:mm")

private fun dataOrcamentoPaciente(valor: String): String =
    runCatching {
        LocalDate.parse(valor).format(formatoDataOrcamentoPaciente)
    }.getOrDefault(valor)

private fun dataComposicaoPaciente(valor: String): String =
    runCatching {
        OffsetDateTime.parse(valor)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(formatoComposicaoPaciente)
    }.getOrDefault(valor)

private fun erroDeAceite(erro: Exception): String {
    val detalhe = erro.message.orEmpty()
    val codigo = Regex("\\b(?:PGRST[0-9]{3}|42501|P0001)\\b").find(detalhe)?.value
    val referencia = codigo ?: erro.javaClass.simpleName
    Log.w("AceiteOrcamento", "Falha na chamada de aceite: $referencia")

    return when {
        "Entre com uma conta de paciente." in detalhe ->
            "Entre novamente com sua conta de paciente para confirmar o aceite."

        "O orçamento não está disponível para aceite." in detalhe ->
            "O orçamento não está disponível para aceite."

        codigo == "PGRST202" ->
            "O Supabase não encontrou a função de aceite (PGRST202)."

        codigo == "42501" ->
            "O Supabase recusou o aceite para esta conta (42501)."

        else -> "O envio do aceite falhou ($referencia)."
    }
}

@Composable
fun TelaOrcamentosPaciente(aoVoltar: () -> Unit) {
    val cliente = SupabaseConfig.client
    val escopo = rememberCoroutineScope()
    val moeda = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
    }

    var composicoes by remember { mutableStateOf(emptyList<ComposicaoDoPaciente>()) }
    var abertoId by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var erroAoCarregar by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var confirmando by remember { mutableStateOf(false) }
    var aceitando by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("") }
    var aceitePendenteId by remember { mutableStateOf<String?>(null) }
    var erroEnvio by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(recarga) {
        carregando = true
        erroAoCarregar = null

        try {
            val usuarioId = cliente.auth.currentUserOrNull()?.id

            if (usuarioId == null) {
                erroAoCarregar = "Entre novamente para consultar seus orçamentos."
                return@LaunchedEffect
            }

            val cadastro = cliente.from("pacientes")
                .select(Columns.raw("id")) {
                    filter {
                        eq("usuario_id", usuarioId)
                        eq("ativo", true)
                    }
                }
                .decodeList<PacienteDosOrcamentos>()
                .singleOrNull()

            if (cadastro == null) {
                erroAoCarregar = "Não foi possível identificar seu cadastro de paciente."
                return@LaunchedEffect
            }

            composicoes = cliente.from("composicoes_orcamento")
                .select(
                    Columns.raw(
                        "salvo_em," +
                                "orcamento:orcamentos!inner(id,status,data_prevista," +
                                "procedimento:procedimentos(nome))," +
                                "itens_composicao(id,descricao,quantidade,valor_unitario,valor_total)," +
                                "periodos_internacao(id,tipo_acomodacao,data_entrada,data_saida)"
                    )
                ) {
                    filter { eq("orcamento.paciente_id", cadastro.id) }
                }
                .decodeList<ComposicaoDoPaciente>()
                .filter { it.salvo_em != null }
                .sortedByDescending { it.salvo_em }

            aceitePendenteId?.let { id ->
                val status = composicoes
                    .firstOrNull { it.orcamento.id == id }
                    ?.orcamento?.status

                mensagem = when {
                    status == "Aceito" -> "Orçamento aceito com sucesso."
                    erroEnvio != null -> erroEnvio.orEmpty()
                    status != null -> "Aceite ainda não confirmado. Status no Supabase: $status."
                    else -> "O orçamento não apareceu na consulta após o envio."
                }
                aceitePendenteId = null
                erroEnvio = null
            }

            if (composicoes.none { it.orcamento.id == abertoId }) {
                abertoId = null
                confirmando = false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            erroAoCarregar = "Não foi possível carregar seus orçamentos."
        } finally {
            carregando = false
        }
    }

    val aberto = composicoes.firstOrNull { it.orcamento.id == abertoId }
    val totalAberto = remember(aberto) { aberto?.total() }

    fun voltar() {
        if (aceitando || (carregando && aceitePendenteId != null)) return

        if (confirmando) {
            confirmando = false
        } else if (abertoId != null) {
            abertoId = null
            mensagem = ""
        } else {
            aoVoltar()
        }
    }

    fun enviarAceite(id: String) {
        if (aceitando || carregando) return

        confirmando = false
        aceitando = true
        mensagem = "Registrando aceite…"
        erroEnvio = null
        aceitePendenteId = id

        escopo.launch {
            try {
                cliente.postgrest.rpc(
                    "aceitar_orcamento",
                    buildJsonObject { put("p_orcamento_id", id) }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                erroEnvio = erroDeAceite(e)
            } finally {
                mensagem = erroEnvio ?: "Conferindo o status no Supabase…"
                carregando = true
                aceitando = false
                recarga++
            }
        }
    }

    BackHandler { voltar() }

    val cores = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cores.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = cores.primary,
                contentColor = cores.onPrimary
            ) {
                Text(
                    text = if (aberto == null) "Meus orçamentos" else "Detalhes do orçamento",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                        .semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (mensagem.isNotBlank()) {
                AvisoOrcamentoPaciente(mensagem)
            }

            key(abertoId) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        carregando -> item {
                            Row(
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    if (aceitePendenteId != null) "Conferindo o aceite…"
                                    else "Carregando seus orçamentos…"
                                )
                            }
                        }

                        erroAoCarregar != null -> item {
                            AvisoOrcamentoPaciente(erroAoCarregar.orEmpty(), erro = true)
                            TextButton(onClick = { recarga++ }) {
                                Text("Tentar novamente")
                            }
                        }

                        aberto != null -> {
                            item {
                                CartaoOrcamentoPaciente {
                                    CabecalhoPropostaPaciente(aberto)

                                    HorizontalDivider(color = cores.outlineVariant)

                                    Text(
                                        text = "Total do orçamento",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = cores.onSurfaceVariant
                                    )
                                    Text(
                                        text = totalAberto?.let { moeda.format(it) }
                                            ?: "Valor indisponível",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = cores.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (aberto.orcamento.status == "Pendente") {
                                item {
                                    if (confirmando && totalAberto != null) {
                                        CartaoOrcamentoPaciente {
                                            Text(
                                                text = "Confirmar aceite",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text("Você confirma o aceite do orçamento de ${moeda.format(totalAberto)}?")

                                            Button(
                                                onClick = { enviarAceite(aberto.orcamento.id) },
                                                enabled = !aceitando,
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                            ) {
                                                Text("Confirmar aceite agora")
                                            }

                                            TextButton(
                                                onClick = { confirmando = false },
                                                modifier = Modifier.align(Alignment.CenterHorizontally)
                                            ) {
                                                Text("Cancelar")
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                if (totalAberto == null) {
                                                    mensagem = "O total do orçamento está indisponível para sua conta."
                                                } else {
                                                    mensagem = ""
                                                    confirmando = true
                                                }
                                            },
                                            enabled = !aceitando,
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                        ) {
                                            Text(
                                                if (aceitando) "Registrando aceite…"
                                                else "Revisar e aceitar orçamento"
                                            )
                                        }
                                    }
                                }
                            }

                            if (aberto.periodos_internacao.isNotEmpty()) {
                                item {
                                    TituloSecaoOrcamentoPaciente("Períodos de internação")
                                }

                                items(
                                    aberto.periodos_internacao,
                                    key = { "periodo-${it.id}" }
                                ) { periodo ->
                                    CartaoOrcamentoPaciente {
                                        Text(
                                            text = periodo.tipo_acomodacao,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "${dataOrcamentoPaciente(periodo.data_entrada)} a " +
                                                    dataOrcamentoPaciente(periodo.data_saida),
                                            color = cores.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (aberto.itens_composicao.isNotEmpty()) {
                                item {
                                    TituloSecaoOrcamentoPaciente("Itens do orçamento")
                                }

                                items(
                                    aberto.itens_composicao,
                                    key = { "item-${it.id}" }
                                ) { item ->
                                    val unitario = item.valor_unitario.content.toBigDecimalOrNull()
                                    val subtotal = item.valor_total.content.toBigDecimalOrNull()

                                    CartaoOrcamentoPaciente {
                                        Text(
                                            text = item.descricao,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        if (unitario != null && subtotal != null) {
                                            Text(
                                                text = "${item.quantidade.content} × ${moeda.format(unitario)}",
                                                color = cores.onSurfaceVariant
                                            )
                                            Text(
                                                text = moeda.format(subtotal),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Text("Valor indisponível", color = cores.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        composicoes.isEmpty() -> item {
                            AvisoOrcamentoPaciente("Você ainda não tem orçamentos disponíveis.")
                        }

                        else -> items(composicoes, key = { it.orcamento.id }) { composicao ->
                            val total = remember(composicao) { composicao.total() }

                            CartaoOrcamentoPaciente {
                                CabecalhoPropostaPaciente(composicao)

                                total?.let {
                                    Text(
                                        text = "Total: ${moeda.format(it)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = cores.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        abertoId = composicao.orcamento.id
                                        mensagem = ""
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Ver orçamento")
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = ::voltar,
                    enabled = !aceitando && (!carregando || aceitePendenteId == null),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Voltar")
                }

                if (!carregando && erroAoCarregar == null) {
                    TextButton(
                        onClick = { recarga++ },
                        enabled = !aceitando && !confirmando,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Atualizar")
                    }
                }
            }
        }
    }
}

@Composable
private fun CartaoOrcamentoPaciente(conteudo: @Composable ColumnScope.() -> Unit) {
    val cores = MaterialTheme.colorScheme

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = cores.surface,
            contentColor = cores.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = conteudo
        )
    }
}

@Composable
private fun CabecalhoPropostaPaciente(composicao: ComposicaoDoPaciente) {
    val cores = MaterialTheme.colorScheme
    val orcamento = composicao.orcamento
    val aceito = orcamento.status == "Aceito"

    Text(
        text = orcamento.procedimento?.nome ?: "Orçamento",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )

    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (aceito) cores.tertiaryContainer else cores.secondaryContainer,
        contentColor = if (aceito) cores.onTertiaryContainer else cores.onSecondaryContainer
    ) {
        Text(
            text = orcamento.status ?: "Situação não informada",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }

    composicao.salvo_em?.let {
        Text(
            text = "Composição de ${dataComposicaoPaciente(it)}",
            style = MaterialTheme.typography.bodyMedium,
            color = cores.onSurfaceVariant
        )
    }

    orcamento.data_prevista?.let {
        Text(
            text = "Data prevista: ${dataOrcamentoPaciente(it)}",
            style = MaterialTheme.typography.bodyMedium,
            color = cores.onSurfaceVariant
        )
    }
}

@Composable
private fun TituloSecaoOrcamentoPaciente(titulo: String) {
    Text(
        text = titulo,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun AvisoOrcamentoPaciente(texto: String, erro: Boolean = false) {
    val cores = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.small,
        color = if (erro) cores.errorContainer else cores.primaryContainer,
        contentColor = if (erro) cores.onErrorContainer else cores.onPrimaryContainer
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}