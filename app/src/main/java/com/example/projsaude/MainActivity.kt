package com.example.projsaude

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
                            TelaMenuPaciente(
                                aoNovoPedido = { navegacao.navigate("novoPedido") },
                                aoAcompanhar = { navegacao.navigate("historicoPaciente") },
                                aoVerOrcamentos = { navegacao.navigate("orcamentosPaciente") },
                                aoVoltar = voltar
                            )
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
    mostrarLogo: Boolean = false,
    rolavel: Boolean = false,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    val cores = MaterialTheme.colorScheme
    val cantos = MaterialTheme.shapes

    val rolagem = if (rolavel) {
        Modifier.verticalScroll(rememberScrollState())
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cores.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .then(rolagem)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (mostrarLogo) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF10345E), Color(0xFF064FB5))
                            ),
                            shape = cantos.large
                        )
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = cantos.medium,
                        color = Color(0xFFF5F5F5)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo_projeto_saude),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(4.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Projeto Saúde",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFE1EEFC)
                        )
                        Text(
                            text = titulo,
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Text(
                    text = titulo,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = cores.onBackground
                )
            }

            conteudo()
        }
    }
}

@Composable
private fun TelaMenuPaciente(
    aoNovoPedido: () -> Unit,
    aoAcompanhar: () -> Unit,
    aoVerOrcamentos: () -> Unit,
    aoVoltar: () -> Unit
) {
    PaginaPaciente("Área do paciente", mostrarLogo = true, rolavel = true) {
        CartaoAcaoPaciente(
            titulo = "Novo pedido",
            descricao = "Envie a foto do pedido médico para solicitar um orçamento.",
            aoClicar = aoNovoPedido,
            destaque = true
        )

        CartaoAcaoPaciente(
            titulo = "Acompanhar pedidos",
            descricao = "Veja a situação dos pedidos que você já enviou.",
            aoClicar = aoAcompanhar
        )

        CartaoAcaoPaciente(
            titulo = "Meus orçamentos",
            descricao = "Consulte os valores e confira as propostas disponíveis para aceite.",
            aoClicar = aoVerOrcamentos
        )

        TextButton(onClick = aoVoltar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Voltar")
        }
    }
}

@Composable
private fun CartaoAcaoPaciente(
    titulo: String,
    descricao: String,
    aoClicar: () -> Unit,
    destaque: Boolean = false
) {
    val cores = MaterialTheme.colorScheme

    OutlinedCard(
        onClick = aoClicar,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (destaque) cores.primary else cores.surface,
            contentColor = if (destaque) cores.onPrimary else cores.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (destaque) cores.primary else cores.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = descricao,
                style = MaterialTheme.typography.bodyMedium,
                color = if (destaque) cores.onPrimary else cores.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AvisoPaciente(texto: String, erro: Boolean = false) {
    val cores = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.small,
        color = if (erro) cores.errorContainer else cores.primaryContainer,
        contentColor = if (erro) cores.onErrorContainer else cores.onPrimaryContainer
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium
        )
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

    PaginaPaciente("Novo pedido", rolavel = true) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Foto do pedido médico",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Anexe uma foto legível em JPG ou PNG, de até 10 MB.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { seletor.launch("image/*") },
                    enabled = !enviando && caminhoEnviado == null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) {
                    Text(if (foto == null) "Selecionar foto do pedido" else "Trocar foto")
                }

                if (foto != null) {
                    AvisoPaciente(
                        if (caminhoEnviado == null) "Foto selecionada"
                        else "Foto enviada"
                    )
                }

                erro?.let { AvisoPaciente(it, erro = true) }

                Button(
                    enabled = foto != null && !enviando,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
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
                                        } ?: throw IllegalArgumentException(
                                            "Não foi possível abrir a foto."
                                        )
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
                    if (enviando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                    }

                    Text(
                        when {
                            enviando -> "Enviando…"
                            caminhoEnviado != null -> "Registrar pedido"
                            else -> "Enviar pedido"
                        }
                    )
                }
            }
        }

        TextButton(
            onClick = aoVoltar,
            enabled = !enviando,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
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
    var tentativa by remember { mutableIntStateOf(0) }

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
            carregando -> Row(
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Text("Carregando seus pedidos…")
            }

            erro -> {
                AvisoPaciente("Não foi possível carregar seus pedidos.", erro = true)
                OutlinedButton(onClick = { tentativa++ }) {
                    Text("Tentar novamente")
                }
            }

            pedidos.isEmpty() -> AvisoPaciente("Você ainda não tem pedidos registrados.")

            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pedidos, key = { it.getValue("id").jsonPrimitive.content }) { pedido ->
                    val data = pedido.getValue("criado_em").jsonPrimitive.content
                        .take(10)
                        .split("-")
                        .reversed()
                        .joinToString("/")
                    val status = pedido.getValue("status").jsonPrimitive.content

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Pedido de $data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Text(
                                    text = status,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        TextButton(
            onClick = aoVoltar,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Voltar")
        }
    }
}