package com.example.projsaude

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
    val estadoLista = rememberLazyListState()
    val cores = MaterialTheme.colorScheme
    val formas = MaterialTheme.shapes

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
    var gravacaoConfirmada by remember { mutableStateOf(false) }

    val precosPorCodigo = remember(precos) { precos.associateBy { it.codigo } }
    val encontrados = procedimentos.filter {
        it.nome.contains(busca.trim(), ignoreCase = true)
    }

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

    fun salvarProcedimento() {
        if (salvando || carregando || erroLista) return
        gravacaoConfirmada = false

        if (nome.isBlank()) {
            mensagem = "Informe o nome do procedimento."
            return
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
                gravacaoConfirmada = true
                mensagem = "Procedimento salvo no banco."
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

    BackHandler(enabled = salvando) {}

    LaunchedEffect(editandoId) {
        if (editandoId != null) estadoLista.scrollToItem(1)
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cores.background)
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = estadoLista,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().fillMaxHeight(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Procedimentos",
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

                    OutlinedTextField(
                        value = busca,
                        onValueChange = { busca = it },
                        label = { Text("Buscar procedimento pelo nome") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = formas.small,
                        enabled = !salvando,
                        singleLine = true
                    )
                }
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
                    item {
                        AvisoProcedimento("Nenhum procedimento encontrado com esse nome.")
                    }
                }
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
                            text = if (editandoId == null) {
                                "Novo procedimento"
                            } else {
                                "Editar procedimento"
                            },
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = nome,
                            onValueChange = { nome = it },
                            label = { Text("Nome do procedimento") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            enabled = !salvando
                        )

                        OutlinedTextField(
                            value = descricao,
                            onValueChange = { descricao = it },
                            label = { Text("Descrição") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            enabled = !salvando
                        )

                        HorizontalDivider(color = cores.outlineVariant)

                        Text(
                            text = "Valor hospitalar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Box {
                            OutlinedButton(
                                onClick = { menuPrecos = true },
                                enabled = !salvando && !carregando && !erroLista,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    precosPorCodigo[codigoPreco]?.nome ?: "Vincular valor hospitalar"
                                )
                            }

                            DropdownMenu(
                                expanded = menuPrecos,
                                onDismissRequest = { menuPrecos = false }
                            ) {
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

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            color = cores.primaryContainer,
                            contentColor = cores.onPrimaryContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Valor vinculado", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = exibirValor(codigoPreco),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (mensagem.isNotEmpty()) {
                            AvisoProcedimento(mensagem, erro = !gravacaoConfirmada)
                        }

                        Button(
                            onClick = ::salvarProcedimento,
                            enabled = !salvando && !carregando && !erroLista,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = formas.small
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
                    }
                }
            }

            item {
                Text(
                    text = "Procedimentos cadastrados",
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
                        Text("Carregando procedimentos…")
                    }

                    erroLista -> AvisoProcedimento(
                        "Não foi possível carregar procedimentos e valores.",
                        erro = true
                    )

                    procedimentos.isEmpty() ->
                        AvisoProcedimento("Nenhum procedimento ativo encontrado.")
                }
            }

            if (!carregando && !erroLista) {
                items(encontrados, key = { it.id }) { procedimento ->
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
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = procedimento.nome,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            procedimento.descricao?.takeIf { it.isNotBlank() }?.let { texto ->
                                Text(texto, color = cores.onSurfaceVariant)
                            }

                            Text(
                                text = exibirValor(procedimento.codigo_valor_hospitalar),
                                color = cores.primary,
                                style = MaterialTheme.typography.titleMedium
                            )

                            TextButton(
                                onClick = { selecionar(procedimento) },
                                enabled = !salvando
                            ) {
                                Text("Editar procedimento")
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
private fun AvisoProcedimento(texto: String, erro: Boolean = false) {
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