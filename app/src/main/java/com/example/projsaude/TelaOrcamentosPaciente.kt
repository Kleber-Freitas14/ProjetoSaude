package com.example.projsaude

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

private val formatoDataOrcamentoPaciente = DateTimeFormatter.ofPattern("dd/MM/uuuu")

private fun dataOrcamentoPaciente(valor: String): String =
    runCatching {
        LocalDate.parse(valor).format(formatoDataOrcamentoPaciente)
    }.getOrDefault(valor)

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

            if (composicoes.none { it.orcamento.id == abertoId }) {
                abertoId = null
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

    fun voltar() {
        if (aceitando) return

        if (abertoId != null) {
            abertoId = null
            mensagem = ""
        } else {
            aoVoltar()
        }
    }

    BackHandler { voltar() }

    if (confirmando && aberto != null) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            title = { Text("Aceitar orçamento?") },
            text = {
                Text(
                    "Ao confirmar, seu aceite será registrado. " +
                            "O orçamento ficará disponível para consulta."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = aberto.orcamento.id
                        confirmando = false
                        aceitando = true
                        mensagem = ""

                        escopo.launch {
                            try {
                                cliente.postgrest.rpc(
                                    "aceitar_orcamento",
                                    buildJsonObject { put("p_orcamento_id", id) }
                                )

                                mensagem = "Orçamento aceito com sucesso."
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                mensagem = "Não foi possível confirmar o aceite. Confira o status atualizado."
                            } finally {
                                carregando = true
                                aceitando = false
                                recarga++
                            }
                        }
                    }
                ) { Text("Confirmar aceite") }
            },
            dismissButton = {
                TextButton(onClick = { confirmando = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (aberto == null) "Meus orçamentos" else "Detalhes do orçamento",
            style = MaterialTheme.typography.headlineMedium
        )

        if (mensagem.isNotBlank()) Text(mensagem)

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
                        val total = aberto.total()
                        Text(
                            total?.let { "Total: ${moeda.format(it)}" } ?: "Total indisponível",
                            style = MaterialTheme.typography.titleLarge
                        )

                        when (aberto.orcamento.status) {
                            "Pendente" -> Button(
                                onClick = { confirmando = true },
                                enabled = !aceitando && total != null,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (aceitando) "Registrando aceite…" else "Aceitar orçamento") }

                            "Aceito" -> Text("Aceite registrado.")

                            "Em espera" -> Text("O orçamento está em espera.")
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
                            composicao.total()?.let { Text("Total: ${moeda.format(it)}") }

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

        if (!carregando && erroAoCarregar == null) {
            TextButton(onClick = { recarga++ }, enabled = !aceitando) {
                Text("Atualizar")
            }
        }

        OutlinedButton(
            onClick = ::voltar,
            enabled = !aceitando,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Voltar") }
    }
}