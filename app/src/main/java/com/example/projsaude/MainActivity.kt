package com.example.projsaude

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.projsaude.ui.theme.ProjSaudeTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val LIMITE_FOTO_BYTES = 10 * 1024 * 1024

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ProjSaudeTheme {
                val navegacao = rememberNavController()
                var ehAdministrador by rememberSaveable { mutableStateOf(false) }
                val voltar: () -> Unit = { navegacao.popBackStack() }

                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(navController = navegacao, startDestination = "inicio") {
                        composable("inicio") {
                            TelaInicial(
                                aoAcessarColaborador = { navegacao.navigate("login") },
                                aoAcessarPaciente = { navegacao.navigate("acessoPaciente") }
                            )
                        }

                        composable("acessoPaciente") {
                            TelaAcessoPaciente(
                                aoEntrar = { navegacao.navigate("loginPaciente") },
                                aoCadastrar = { navegacao.navigate("cadastroPaciente") },
                                aoVoltar = voltar
                            )
                        }

                        composable("loginPaciente") {
                            TelaLogin(
                                titulo = "Login do paciente",
                                aoVoltar = voltar,
                                autenticar = { email, senha ->
                                    autenticarUsuario(email, senha, acessoPaciente = true)
                                },
                                aoEntrar = {
                                    navegacao.navigate("menuPaciente") {
                                        popUpTo("acessoPaciente")
                                    }
                                }
                            )
                        }

                        composable("cadastroPaciente") {
                            TelaCadastroPaciente(
                                aoVoltar = voltar,
                                aoEnviar = {
                                    navegacao.navigate("loginPaciente") {
                                        popUpTo("acessoPaciente")
                                    }
                                }
                            )
                        }

                        composable("menuPaciente") {
                            PaginaPaciente("Área do paciente") {
                                Button(
                                    onClick = { navegacao.navigate("novoPedido") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Novo pedido")
                                }

                                OutlinedButton(
                                    onClick = { navegacao.navigate("historicoPaciente") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Acompanhar pedidos")
                                }

                                OutlinedButton(
                                    onClick = { navegacao.navigate("orcamentosPaciente") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Meus orçamentos")
                                }

                                TextButton(onClick = voltar) {
                                    Text("Voltar")
                                }
                            }
                        }

                        composable("orcamentosPaciente") {
                            TelaOrcamentosPaciente(aoVoltar = voltar)
                        }

                        composable("novoPedido") {
                            TelaNovoPedido(
                                aoVoltar = voltar,
                                aoEnviar = {
                                    navegacao.navigate("historicoPaciente") {
                                        popUpTo("menuPaciente")
                                    }
                                }
                            )
                        }

                        composable("historicoPaciente") {
                            TelaHistoricoPedidos(aoVoltar = voltar)
                        }

                        composable("login") {
                            TelaLogin(
                                aoVoltar = voltar,
                                aoEntrar = { administrador ->
                                    ehAdministrador = administrador
                                    navegacao.navigate("principal") {
                                        popUpTo("inicio")
                                    }
                                }
                            )
                        }

                        composable("principal") {
                            TelaPrincipal(
                                aoSair = {
                                    ehAdministrador = false
                                    navegacao.popBackStack("inicio", inclusive = false)
                                },
                                aoAbrirPacientes = { navegacao.navigate("pacientes") },
                                aoAbrirConsultas = { navegacao.navigate("consultas") },
                                aoAbrirProcedimentos = { navegacao.navigate("procedimentos") },
                                aoAbrirOrcamentos = { navegacao.navigate("orcamentos") },
                                aoAbrirMedicos = { navegacao.navigate("medicos") },
                                aoAbrirOpme = { navegacao.navigate("opme") },
                                aoAbrirCadastroValores = {
                                    if (ehAdministrador) navegacao.navigate("cadastroValores")
                                },
                                ehAdministrador = ehAdministrador
                            )
                        }

                        composable("cadastroValores") {
                            if (ehAdministrador) {
                                TelaCadastroValores(aoVoltar = voltar)
                            } else {
                                PaginaPaciente("Acesso restrito ao administrador") {
                                    TextButton(onClick = voltar) {
                                        Text("Voltar")
                                    }
                                }
                            }
                        }

                        composable("pacientes") { TelaPacientes(aoVoltar = voltar) }
                        composable("consultas") { TelaConsultas(aoVoltar = voltar) }
                        composable("procedimentos") { TelaProcedimentos(aoVoltar = voltar) }
                        composable("orcamentos") { TelaOrcamentos(aoVoltar = voltar) }
                        composable("medicos") { TelaMedicos(aoVoltar = voltar) }
                        composable("opme") { TelaOpme(aoVoltar = voltar) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaginaPaciente(
    titulo: String,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(titulo, style = MaterialTheme.typography.headlineSmall)
        conteudo()
    }
}

@Composable
private fun TelaNovoPedido(aoVoltar: () -> Unit, aoEnviar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var foto by remember { mutableStateOf<Uri?>(null) }
    var caminhoEnviado by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    val seletor = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            foto = uri
            erro = null
        }
    }

    BackHandler {
        if (!enviando) aoVoltar()
    }

    PaginaPaciente("Novo pedido") {
        OutlinedButton(
            onClick = { seletor.launch("image/*") },
            enabled = !enviando && caminhoEnviado == null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (foto == null) "Selecionar foto do pedido" else "Trocar foto")
        }

        if (foto != null) {
            Text("Foto selecionada")
        }

        Button(
            enabled = foto != null && !enviando,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val uri = foto ?: return@Button
                enviando = true
                erro = null

                scope.launch {
                    try {
                        val cliente = SupabaseConfig.client
                        val caminho = caminhoEnviado ?: run {
                            val pacienteId = obterIdDoPaciente()
                            val mime = context.contentResolver.getType(uri)
                            val extensao = when (mime) {
                                "image/jpeg" -> "jpg"
                                "image/png" -> "png"
                                else -> throw IllegalArgumentException(
                                    "Selecione uma foto JPG ou PNG."
                                )
                            }

                            val bytes = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.use { entrada ->
                                    val saida = java.io.ByteArrayOutputStream()
                                    val buffer = ByteArray(8192)
                                    var quantidade = entrada.read(buffer)

                                    while (quantidade != -1) {
                                        require(saida.size() + quantidade <= LIMITE_FOTO_BYTES) {
                                            "A foto deve ter até 10 MB."
                                        }
                                        saida.write(buffer, 0, quantidade)
                                        quantidade = entrada.read(buffer)
                                    }

                                    saida.toByteArray().also {
                                        require(it.isNotEmpty()) { "A foto está vazia." }
                                    }
                                } ?: throw IllegalArgumentException("Não foi possível abrir a foto.")
                            }

                            val novoCaminho = "$pacienteId/${UUID.randomUUID()}.$extensao"
                            cliente.storage.from("pedidos-medicos").upload(novoCaminho, bytes) {
                                upsert = false
                                contentType = ContentType.parse(mime)
                            }
                            caminhoEnviado = novoCaminho
                            novoCaminho
                        }

                        cliente.postgrest.rpc(
                            "registrar_pedido_medico",
                            buildJsonObject { put("p_foto_path", caminho) }
                        )
                        aoEnviar()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: IllegalArgumentException) {
                        erro = e.message ?: "Verifique a foto selecionada."
                    } catch (_: Exception) {
                        erro = if (caminhoEnviado == null) {
                            "Não foi possível enviar a foto. Tente novamente."
                        } else {
                            "A foto foi enviada. Toque em Registrar pedido para tentar concluir."
                        }
                    } finally {
                        enviando = false
                    }
                }
            }
        ) {
            Text(
                when {
                    enviando -> "Enviando…"
                    caminhoEnviado != null -> "Registrar pedido"
                    else -> "Enviar pedido"
                }
            )
        }

        erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        TextButton(onClick = aoVoltar, enabled = !enviando) {
            Text("Voltar")
        }
    }
}

private suspend fun obterIdDoPaciente(): String {
    val cliente = SupabaseConfig.client
    val usuarioId = cliente.auth.currentUserOrNull()?.id
        ?: throw IllegalArgumentException("Entre novamente para acessar seus pedidos.")

    val pacientes = cliente.from("pacientes").select {
        filter { eq("usuario_id", usuarioId) }
    }.decodeList<JsonObject>()

    return pacientes.singleOrNull()?.get("id")?.jsonPrimitive?.content
        ?: throw IllegalArgumentException("Não foi possível localizar seu cadastro de paciente.")
}

@Composable
private fun TelaHistoricoPedidos(aoVoltar: () -> Unit) {
    var pedidos by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf(false) }
    var tentativa by remember { mutableStateOf(0) }

    LaunchedEffect(tentativa) {
        carregando = true
        erro = false
        try {
            val pacienteId = obterIdDoPaciente()
            pedidos = SupabaseConfig.client.from("pedidos_medicos").select {
                filter { eq("paciente_id", pacienteId) }
            }.decodeList<JsonObject>().sortedByDescending {
                it["criado_em"]?.jsonPrimitive?.content.orEmpty()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            erro = true
        } finally {
            carregando = false
        }
    }

    PaginaPaciente("Acompanhar pedidos") {
        when {
            carregando -> CircularProgressIndicator()
            erro -> {
                Text("Não foi possível carregar seus pedidos.")
                OutlinedButton(onClick = { tentativa++ }) {
                    Text("Tentar novamente")
                }
            }
            pedidos.isEmpty() -> Text("Você ainda não tem pedidos registrados.")
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pedidos, key = { it.getValue("id").jsonPrimitive.content }) { pedido ->
                    val data = pedido.getValue("criado_em").jsonPrimitive.content
                        .take(10).split("-").reversed().joinToString("/")
                    val status = pedido.getValue("status").jsonPrimitive.content

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Pedido de $data", style = MaterialTheme.typography.titleMedium)
                            Text("Status: $status")
                        }
                    }
                }
            }
        }

        TextButton(onClick = aoVoltar) {
            Text("Voltar")
        }
    }
}