package com.example.projsaude

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (aberto == null) "Meus orçamentos" else "Detalhes do orçamento",
            style = MaterialTheme.typography.headlineMedium
        )

        if (mensagem.isNotBlank()) {
            Text(mensagem)
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                carregando -> item { CircularProgressIndicator() }

                erroAoCarregar != null -> item {
                    Text(erroAoCarregar.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { recarga++ }) { Text("Tentar novamente") }
                }

                aberto != null -> {
                    item {
                        Text(aberto.orcamento.procedimento?.nome ?: "Orçamento")
                        Text("Status: ${aberto.orcamento.status ?: "Não informado"}")
                        aberto.salvo_em?.let {
                            Text("Composição de ${dataComposicaoPaciente(it)}")
                        }
                        aberto.orcamento.data_prevista?.let {
                            Text("Data prevista: ${dataOrcamentoPaciente(it)}")
                        }
                    }

                    items(aberto.periodos_internacao, key = { "periodo-${it.id}" }) { periodo ->
                        Text(
                            "${periodo.tipo_acomodacao}: " +
                                    "${dataOrcamentoPaciente(periodo.data_entrada)} a " +
                                    dataOrcamentoPaciente(periodo.data_saida)
                        )
                    }

                    items(aberto.itens_composicao, key = { "item-${it.id}" }) { item ->
                        val unitario = item.valor_unitario.content.toBigDecimalOrNull()
                        val subtotal = item.valor_total.content.toBigDecimalOrNull()

                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(item.descricao, style = MaterialTheme.typography.titleMedium)
                                if (unitario != null && subtotal != null) {
                                    Text(
                                        "${item.quantidade.content} × ${moeda.format(unitario)} " +
                                                "= ${moeda.format(subtotal)}"
                                    )
                                } else {
                                    Text("Valor indisponível")
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            totalAberto?.let { "Total: ${moeda.format(it)}" }
                                ?: "Total indisponível",
                            style = MaterialTheme.typography.titleLarge
                        )

                        when (aberto.orcamento.status) {
                            "Aceito" -> Text("Aceite registrado.")
                            "Em espera" -> Text("O orçamento está em espera.")
                            else -> Unit
                        }
                    }
                }

                composicoes.isEmpty() -> item {
                    Text("Você ainda não tem orçamentos disponíveis.")
                }

                else -> items(composicoes, key = { it.orcamento.id }) { composicao ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                composicao.orcamento.procedimento?.nome ?: "Orçamento",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("Status: ${composicao.orcamento.status ?: "Não informado"}")
                            composicao.salvo_em?.let {
                                Text("Composição de ${dataComposicaoPaciente(it)}")
                            }
                            composicao.total()?.let {
                                Text("Total: ${moeda.format(it)}")
                            }

                            TextButton(
                                onClick = {
                                    abertoId = composicao.orcamento.id
                                    mensagem = ""
                                }
                            ) { Text("Ver orçamento") }
                        }
                    }
                }
            }
        }

        if (!carregando && erroAoCarregar == null &&
            aberto != null && aberto.orcamento.status == "Pendente"
        ) {
            if (confirmando && totalAberto != null) {
                Text("Confirme o aceite do orçamento de ${moeda.format(totalAberto)}.")

                Button(
                    onClick = { enviarAceite(aberto.orcamento.id) },
                    enabled = !aceitando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirmar aceite agora")
                }

                TextButton(onClick = { confirmando = false }) {
                    Text("Cancelar")
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (aceitando) "Registrando aceite…" else "Revisar e aceitar orçamento")
                }
            }
        }

        if (!carregando && erroAoCarregar == null) {
            TextButton(
                onClick = { recarga++ },
                enabled = !aceitando && !confirmando
            ) {
                Text("Atualizar")
            }
        }

        OutlinedButton(
            onClick = ::voltar,
            enabled = !aceitando && (!carregando || aceitePendenteId == null),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar")
        }
    }
}