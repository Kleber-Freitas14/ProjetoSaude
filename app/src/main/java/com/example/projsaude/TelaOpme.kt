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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private data class MaterialOpmeSalvo(
    val id: String,
    val nome: String,
    val descricao: String? = null,
    val fabricante: String? = null,
    val codigo: String? = null,
    val quantidade_estoque: Int? = null,
    val quantidade_reservada: Int? = null
)

@Serializable
private data class NovoMaterialOpme(
    val nome: String,
    val descricao: String,
    val fabricante: String,
    val codigo: String?,
    val quantidade_estoque: Int,
    val quantidade_reservada: Int,
    val estoque_minimo: Int,
    val ativo: Boolean
)

@Composable
fun TelaOpme(aoVoltar: () -> Unit) {
    val cliente = SupabaseConfig.client
    val escopo = rememberCoroutineScope()
    val estadoLista = rememberLazyListState()
    val cores = MaterialTheme.colorScheme
    val formas = MaterialTheme.shapes

    var busca by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var fabricante by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var materiais by remember { mutableStateOf(emptyList<MaterialOpmeSalvo>()) }
    var carregando by remember { mutableStateOf(true) }
    var salvando by remember { mutableStateOf(false) }
    var erroLista by remember { mutableStateOf(false) }
    var recarga by remember { mutableIntStateOf(0) }
    var mensagem by remember { mutableStateOf("") }
    var cadastroConfirmado by remember { mutableStateOf(false) }

    val termo = busca.trim()
    val encontrados = materiais.filter {
        it.nome.contains(termo, ignoreCase = true) ||
                it.codigo.orEmpty().contains(termo, ignoreCase = true)
    }
    val materiaisVisiveis = if (!carregando && !erroLista) encontrados else emptyList()

    BackHandler(enabled = salvando) {}

    LaunchedEffect(recarga) {
        carregando = true
        erroLista = false

        try {
            materiais = cliente.from("opme")
                .select(
                    Columns.raw(
                        "id,nome,descricao,fabricante,codigo," +
                                "quantidade_estoque,quantidade_reservada"
                    )
                ) {
                    filter { eq("ativo", true) }
                }
                .decodeList<MaterialOpmeSalvo>()
                .sortedBy { it.nome.lowercase() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            erroLista = true
        } finally {
            carregando = false
        }
    }

    fun cadastrarMaterial() {
        if (salvando) return
        cadastroConfirmado = false

        if (nome.isBlank() || descricao.isBlank()) {
            mensagem = "Informe o nome e a descrição do material."
            return
        }

        val novo = NovoMaterialOpme(
            nome = nome.trim(),
            descricao = descricao.trim(),
            fabricante = fabricante.trim(),
            codigo = codigo.trim().takeIf { it.isNotEmpty() },
            quantidade_estoque = 0,
            quantidade_reservada = 0,
            estoque_minimo = 0,
            ativo = true
        )

        salvando = true
        mensagem = ""

        escopo.launch {
            try {
                cliente.from("opme").insert(novo)
                nome = ""
                descricao = ""
                fabricante = ""
                codigo = ""
                busca = ""
                cadastroConfirmado = true
                mensagem = "Material salvo no banco."
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
            state = estadoLista,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().fillMaxHeight(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "cabecalho") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF10345E), Color(0xFF064FB5))
                            ),
                            shape = formas.large
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "OPME",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Materiais e estoque",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE1EEFC)
                    )
                }
            }

            item(key = "busca") {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = formas.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = busca,
                            onValueChange = { busca = it },
                            label = { Text("Buscar pelo nome ou código") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            singleLine = true
                        )

                        OutlinedButton(
                            onClick = {
                                escopo.launch {
                                    estadoLista.scrollToItem(2 + materiaisVisiveis.size)
                                }
                            },
                            enabled = !salvando,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cadastrar material")
                        }

                        TextButton(
                            onClick = { recarga++ },
                            enabled = !carregando && !salvando
                        ) {
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
                                Text("Carregando materiais…")
                            }

                            erroLista -> AvisoOpme(
                                "Não foi possível carregar os materiais do banco.",
                                erro = true
                            )

                            materiais.isEmpty() ->
                                AvisoOpme("Nenhum material ativo encontrado.")

                            encontrados.isEmpty() ->
                                AvisoOpme("Nenhum material encontrado para essa busca.")
                        }
                    }
                }
            }

            items(materiaisVisiveis, key = { "material-${it.id}" }) { material ->
                CartaoMaterialOpme(material)
            }

            item(key = "cadastro") {
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
                            text = "Cadastrar material",
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = nome,
                            onValueChange = { nome = it },
                            label = { Text("Nome do material *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            enabled = !salvando
                        )

                        OutlinedTextField(
                            value = descricao,
                            onValueChange = { descricao = it },
                            label = { Text("Descrição *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            enabled = !salvando
                        )

                        OutlinedTextField(
                            value = fabricante,
                            onValueChange = { fabricante = it },
                            label = { Text("Fabricante") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            enabled = !salvando
                        )

                        OutlinedTextField(
                            value = codigo,
                            onValueChange = { codigo = it },
                            label = { Text("Código ou referência") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            enabled = !salvando
                        )

                        if (mensagem.isNotEmpty()) {
                            AvisoOpme(mensagem, erro = !cadastroConfirmado)
                        }

                        Button(
                            onClick = ::cadastrarMaterial,
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
                            Text(if (salvando) "Salvando…" else "Salvar material")
                        }
                    }
                }
            }

            item(key = "voltar") {
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
private fun CartaoMaterialOpme(material: MaterialOpmeSalvo) {
    val cores = MaterialTheme.colorScheme
    val formas = MaterialTheme.shapes
    val estoque = material.quantidade_estoque ?: 0
    val reservado = material.quantidade_reservada ?: 0
    val saldo = estoque - reservado

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
                text = material.nome,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            material.descricao?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = cores.onSurfaceVariant)
            }
            material.fabricante?.takeIf { it.isNotBlank() }?.let {
                Text("Fabricante: $it", color = cores.onSurfaceVariant)
            }
            material.codigo?.takeIf { it.isNotBlank() }?.let {
                Text("Código: $it", color = cores.onSurfaceVariant)
            }

            Spacer(Modifier.height(2.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = formas.small,
                color = if (saldo > 0) cores.primaryContainer else cores.surfaceVariant,
                contentColor = if (saldo > 0) cores.onPrimaryContainer else cores.onSurfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Saldo disponível: $saldo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Estoque total: $estoque · Reservado: $reservado",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun AvisoOpme(texto: String, erro: Boolean = false) {
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