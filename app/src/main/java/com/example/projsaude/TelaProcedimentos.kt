package com.example.projsaude

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
private data class ProcedimentoSalvo(
    val id: String,
    val nome: String,
    val descricao: String? = null,
    val codigo_valor_hospitalar: String? = null
)

@Serializable
private data class DadosProcedimento(
    val nome: String,
    val descricao: String,
    val codigo_valor_hospitalar: String?
)

@Serializable
private data class ValorVinculadoProcedimento(
    val codigo: String,
    val nome: String,
    val valor: JsonPrimitive? = null
)

@Composable
fun TelaProcedimentos(aoVoltar: () -> Unit) {
    val cliente = SupabaseConfig.client
    val escopo = rememberCoroutineScope()
    val moeda = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")) }

    var busca by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var editandoId by remember { mutableStateOf<String?>(null) }
    var codigoPreco by remember { mutableStateOf<String?>(null) }
    var menuPrecos by remember { mutableStateOf(false) }
    var precos by remember { mutableStateOf(emptyList<ValorVinculadoProcedimento>()) }
    var procedimentos by remember { mutableStateOf(emptyList<ProcedimentoSalvo>()) }
    var carregando by remember { mutableStateOf(true) }
    var salvando by remember { mutableStateOf(false) }
    var erroLista by remember { mutableStateOf(false) }
    var recarga by remember { mutableIntStateOf(0) }
    var mensagem by remember { mutableStateOf("") }

    val precosPorCodigo = remember(precos) { precos.associateBy { it.codigo } }
    val encontrados = procedimentos.filter { it.nome.contains(busca.trim(), ignoreCase = true) }

    fun exibirValor(codigo: String?): String {
        if (codigo == null) return "Sem valor vinculado"
        val valor = precosPorCodigo[codigo]?.valor?.content?.toBigDecimalOrNull()
        return if (valor == null) "Valor indisponível" else moeda.format(valor)
    }

    fun selecionar(procedimento: ProcedimentoSalvo) {
        editandoId = procedimento.id
        nome = procedimento.nome
        descricao = procedimento.descricao.orEmpty()
        codigoPreco = procedimento.codigo_valor_hospitalar
        busca = ""
        mensagem = ""
    }

    fun limparFormulario() {
        editandoId = null
        nome = ""
        descricao = ""
        codigoPreco = null
    }

    BackHandler(enabled = salvando) {}

    LaunchedEffect(recarga) {
        carregando = true
        erroLista = false

        try {
            procedimentos = cliente.from("procedimentos")
                .select(Columns.raw("id,nome,descricao,codigo_valor_hospitalar")) {
                    filter { eq("ativo", true) }
                }
                .decodeList<ProcedimentoSalvo>()
                .sortedBy { it.nome.lowercase() }

            precos = cliente.from("valores_hospitalares")
                .select(Columns.raw("codigo,nome,valor")) {
                    filter { eq("ativo", true) }
                }
                .decodeList<ValorVinculadoProcedimento>()
                .sortedBy { it.nome.lowercase() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            erroLista = true
        } finally {
            carregando = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Procedimentos", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                label = { Text("Buscar procedimento pelo nome") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !salvando,
                singleLine = true
            )
        }

        if (busca.isNotBlank() && !carregando && !erroLista) {
            items(encontrados.take(12), key = { "sugestao-${it.id}" }) { procedimento ->
                OutlinedButton(
                    onClick = { selecionar(procedimento) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !salvando
                ) {
                    Text(procedimento.nome)
                }
            }
            if (encontrados.isEmpty()) {
                item { Text("Nenhum procedimento encontrado com esse nome.") }
            }
        }

        item {
            Text(
                if (editandoId == null) "Novo procedimento" else "Procedimento selecionado",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome do procedimento") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !salvando
            )
            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !salvando
            )
        }

        item {
            Box {
                OutlinedButton(
                    onClick = { menuPrecos = true },
                    enabled = !salvando && !carregando && !erroLista,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(precosPorCodigo[codigoPreco]?.nome ?: "Vincular valor hospitalar")
                }

                DropdownMenu(menuPrecos, { menuPrecos = false }) {
                    DropdownMenuItem(
                        text = { Text("Sem vínculo") },
                        onClick = {
                            codigoPreco = null
                            menuPrecos = false
                        }
                    )

                    precos.forEach { preco ->
                        DropdownMenuItem(
                            text = { Text(preco.nome) },
                            onClick = {
                                codigoPreco = preco.codigo
                                menuPrecos = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = exibirValor(codigoPreco),
                onValueChange = {},
                readOnly = true,
                label = { Text("Valor (R$)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                enabled = !salvando && !carregando && !erroLista,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (nome.isBlank()) {
                        mensagem = "Informe o nome do procedimento."
                        return@Button
                    }

                    val id = editandoId
                    val dados = DadosProcedimento(nome.trim(), descricao.trim(), codigoPreco)
                    salvando = true
                    mensagem = ""

                    escopo.launch {
                        try {
                            if (id == null) {
                                cliente.from("procedimentos").insert(dados)
                            } else {
                                cliente.from("procedimentos").update(dados) {
                                    filter { eq("id", id) }
                                    select(Columns.raw("id,nome"))
                                }.decodeSingle<ProcedimentoSalvo>()
                            }

                            limparFormulario()
                            mensagem = "Procedimento salvo no banco."
                            recarga++
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            mensagem = "Não foi possível confirmar a gravação. Atualize a lista antes de tentar novamente."
                        } finally {
                            salvando = false
                        }
                    }
                }
            ) {
                Text(
                    when {
                        salvando -> "Salvando…"
                        editandoId != null -> "Salvar procedimento"
                        else -> "Cadastrar procedimento"
                    }
                )
            }

            if (editandoId != null) {
                TextButton(
                    onClick = {
                        limparFormulario()
                        mensagem = ""
                    },
                    enabled = !salvando
                ) {
                    Text("Cancelar edição")
                }
            }

            if (mensagem.isNotEmpty()) Text(mensagem)
        }

        item {
            HorizontalDivider()
            Text("Procedimentos cadastrados", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { recarga++ }, enabled = !carregando && !salvando) {
                Text("Atualizar lista")
            }
            when {
                carregando -> CircularProgressIndicator()
                erroLista -> Text("Não foi possível carregar procedimentos e valores.")
                procedimentos.isEmpty() -> Text("Nenhum procedimento ativo encontrado.")
            }
        }

        if (!carregando && !erroLista) {
            items(encontrados, key = { it.id }) { procedimento ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(procedimento.nome, style = MaterialTheme.typography.titleMedium)
                        procedimento.descricao?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        Text(exibirValor(procedimento.codigo_valor_hospitalar))
                        TextButton(
                            onClick = { selecionar(procedimento) },
                            enabled = !salvando
                        ) {
                            Text("Selecionar procedimento")
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(aoVoltar, Modifier.fillMaxWidth(), enabled = !salvando) {
                Text("Voltar")
            }
        }
    }
}