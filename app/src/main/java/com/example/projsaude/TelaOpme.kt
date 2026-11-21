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

    val termo = busca.trim()
    val encontrados = materiais.filter {
        it.nome.contains(termo, ignoreCase = true) ||
                it.codigo.orEmpty().contains(termo, ignoreCase = true)
    }

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

    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("OPME", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                label = { Text("Buscar material pelo nome ou código") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextButton(onClick = { recarga++ }, enabled = !carregando && !salvando) {
                Text("Atualizar lista")
            }

            when {
                carregando -> CircularProgressIndicator()
                erroLista -> Text("Não foi possível carregar os materiais do banco.")
                materiais.isEmpty() -> Text("Nenhum material ativo encontrado.")
                encontrados.isEmpty() -> Text("Nenhum material encontrado para essa busca.")
            }
        }

        if (!carregando && !erroLista) {
            items(encontrados, key = { it.id }) { material ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        val estoque = material.quantidade_estoque ?: 0
                        val reservado = material.quantidade_reservada ?: 0

                        Text(material.nome, style = MaterialTheme.typography.titleMedium)
                        material.descricao?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        material.fabricante?.takeIf { it.isNotBlank() }?.let {
                            Text("Fabricante: $it")
                        }
                        material.codigo?.takeIf { it.isNotBlank() }?.let {
                            Text("Código: $it")
                        }
                        Text("Estoque: $estoque · Reservado: $reservado")
                        Text("Saldo disponível: ${estoque - reservado}")
                    }
                }
            }
        }

        item {
            HorizontalDivider()
            Text("Cadastrar material", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome do material") },
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
            OutlinedTextField(
                value = fabricante,
                onValueChange = { fabricante = it },
                label = { Text("Fabricante") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !salvando
            )
            OutlinedTextField(
                value = codigo,
                onValueChange = { codigo = it },
                label = { Text("Código ou referência") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !salvando
            )
        }

        item {
            Button(
                enabled = !salvando,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (nome.isBlank() || descricao.isBlank()) {
                        mensagem = "Informe o nome e a descrição do material."
                        return@Button
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
                            mensagem = "Material salvo no banco."
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
                Text(if (salvando) "Salvando…" else "Cadastrar material")
            }

            if (mensagem.isNotEmpty()) Text(mensagem)
        }

        item {
            OutlinedButton(aoVoltar, Modifier.fillMaxWidth(), enabled = !salvando) {
                Text("Voltar")
            }
        }
    }
}