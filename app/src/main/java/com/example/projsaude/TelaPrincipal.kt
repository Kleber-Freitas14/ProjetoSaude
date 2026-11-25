package com.example.projsaude

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val AzulPrincipal = Color(0xFF085DD3)

private enum class PerfilFuncionario(val codigo: String, val rotulo: String) {
    COLABORADOR("colaborador", "Colaborador"),
    ADMINISTRADOR("administrador", "Administrador")
}

private data class ModuloPrincipal(
    val titulo: String,
    val descricao: String,
    val aoAbrir: () -> Unit
)

@Composable
fun TelaPrincipal(
    aoSair: () -> Unit,
    aoAbrirPacientes: () -> Unit,
    aoAbrirConsultas: () -> Unit,
    aoAbrirProcedimentos: () -> Unit,
    aoAbrirOrcamentos: () -> Unit,
    aoAbrirMedicos: () -> Unit,
    aoAbrirOpme: () -> Unit,
    aoAbrirCadastroValores: () -> Unit,
    ehAdministrador: Boolean
) {
    var mostrarCadastro by remember { mutableStateOf(false) }
    var mensagemMenu by remember { mutableStateOf("") }

    if (ehAdministrador && mostrarCadastro) {
        TelaCadastroFuncionario(
            aoVoltar = { mostrarCadastro = false },
            aoCadastrar = { perfil ->
                mensagemMenu = "Funcionário cadastrado com o perfil ${perfil.rotulo}."
                mostrarCadastro = false
            }
        )
        return
    }

    val modulos = listOf(
        ModuloPrincipal("Pacientes", "Cadastro e consulta", aoAbrirPacientes),
        ModuloPrincipal("Consultas", "Agenda de atendimentos", aoAbrirConsultas),
        ModuloPrincipal("Orçamentos", "Pedidos e composição", aoAbrirOrcamentos),
        ModuloPrincipal("Procedimentos", "Cadastro de procedimentos", aoAbrirProcedimentos),
        ModuloPrincipal("Médicos", "Cadastro da equipe médica", aoAbrirMedicos),
        ModuloPrincipal("OPME", "Materiais e estoque", aoAbrirOpme)
    )
    val cores = MaterialTheme.colorScheme

    Box(
        modifier = Modifier.fillMaxSize().background(cores.surface).safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(listOf(Color(0xFF12345B), AzulPrincipal)),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Projeto Saúde",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (ehAdministrador) "Acesso administrativo" else "Acesso do colaborador",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE2EDFF)
                )
            }

            if (mensagemMenu.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cores.primaryContainer,
                    contentColor = cores.onPrimaryContainer
                ) {
                    Text(
                        text = mensagemMenu,
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(
                text = "Atendimento",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge
            )

            val escalaFonte = LocalDensity.current.fontScale
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val colunas = if (maxWidth >= 320.dp && escalaFonte <= 1.3f) 2 else 1

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    modulos.chunked(colunas).forEach { linha ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            linha.forEach { modulo ->
                                CartaoModuloPrincipal(modulo, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (ehAdministrador) {
                Text(
                    text = "Administração",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CartaoModuloPrincipal(
                        ModuloPrincipal(
                            "Cadastro de valores",
                            "Preços usados nos orçamentos",
                            aoAbrirCadastroValores
                        ),
                        Modifier.fillMaxWidth()
                    )
                    CartaoModuloPrincipal(
                        ModuloPrincipal("Cadastrar funcionário", "Acesso de um novo usuário") {
                            mensagemMenu = ""
                            mostrarCadastro = true
                        },
                        Modifier.fillMaxWidth()
                    )
                    BotaoGerenciarFuncionarios()
                }
            }

            OutlinedButton(onClick = aoSair, modifier = Modifier.fillMaxWidth()) {
                Text("Sair")
            }
        }
    }
}

@Composable
private fun CartaoModuloPrincipal(modulo: ModuloPrincipal, modifier: Modifier) {
    val cores = MaterialTheme.colorScheme

    OutlinedCard(
        onClick = modulo.aoAbrir,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = cores.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = modulo.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = modulo.descricao,
                style = MaterialTheme.typography.bodySmall,
                color = cores.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TelaCadastroFuncionario(
    aoVoltar: () -> Unit,
    aoCadastrar: (PerfilFuncionario) -> Unit
) {
    val scope = rememberCoroutineScope()
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var perfil by remember { mutableStateOf(PerfilFuncionario.COLABORADOR) }
    var mensagem by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }

    BackHandler {
        if (!enviando) aoVoltar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8FC))
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Cadastrar funcionário",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome completo") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !enviando,
            singleLine = true
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !enviando,
            singleLine = true
        )

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha inicial") },
            supportingText = { Text("Pelo menos 8 caracteres") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            enabled = !enviando,
            singleLine = true
        )

        Text("Perfil de acesso", style = MaterialTheme.typography.titleMedium)

        Column(Modifier.selectableGroup()) {
            PerfilFuncionario.entries.forEach { opcao ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = perfil == opcao,
                            onClick = { perfil = opcao },
                            enabled = !enviando,
                            role = Role.RadioButton
                        )
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = perfil == opcao,
                        onClick = null,
                        enabled = !enviando
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(opcao.rotulo)
                }
            }
        }

        if (mensagem.isNotEmpty()) {
            Text(mensagem, color = MaterialTheme.colorScheme.error)
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !enviando,
            colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
            onClick = {
                val nomeInformado = nome.trim()
                val emailInformado = email.trim()
                val senhaInformada = senha
                val perfilSelecionado = perfil

                mensagem = when {
                    nomeInformado.isEmpty() || nomeInformado.length > 120 ->
                        "Informe um nome de até 120 caracteres."
                    emailInformado.length > 254 ||
                            !Patterns.EMAIL_ADDRESS.matcher(emailInformado).matches() ->
                        "Informe um e-mail válido."
                    senhaInformada.trim().length < 8 ||
                            senhaInformada.toByteArray(Charsets.UTF_8).size > 72 ->
                        "A senha precisa ter pelo menos 8 caracteres e até 72 bytes."
                    else -> ""
                }

                if (mensagem.isEmpty()) {
                    enviando = true

                    scope.launch {
                        try {
                            cadastrarFuncionario(
                                nomeInformado,
                                emailInformado,
                                senhaInformada,
                                perfilSelecionado
                            )
                            aoCadastrar(perfilSelecionado)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: IllegalStateException) {
                            mensagem = e.message ?: "Não foi possível concluir o cadastro."
                        } catch (_: Exception) {
                            mensagem =
                                "Falha de comunicação. Verifique a conta no Supabase antes de tentar novamente."
                        } finally {
                            enviando = false
                        }
                    }
                }
            }
        ) {
            Text(if (enviando) "Cadastrando…" else "Cadastrar funcionário")
        }

        OutlinedButton(
            onClick = aoVoltar,
            enabled = !enviando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar")
        }
    }
}

private suspend fun cadastrarFuncionario(
    nome: String,
    email: String,
    senha: String,
    perfil: PerfilFuncionario
) {
    val cliente = SupabaseConfig.client
    val token = cliente.auth.currentSessionOrNull()?.accessToken
        ?: error("Entre novamente como administrador.")

    val endpoint =
        "https://trxdswjajfhrubnedevl.supabase.co/functions/v1/cadastrar-funcionario"

    val corpo = JSONObject()
        .put("nome", nome)
        .put("email", email)
        .put("senha", senha)
        .put("tipo_usuario", perfil.codigo)
        .toString()
        .toByteArray(Charsets.UTF_8)

    withContext(Dispatchers.IO) {
        val conexao = URL(endpoint).openConnection() as HttpURLConnection

        try {
            conexao.requestMethod = "POST"
            conexao.instanceFollowRedirects = false
            conexao.connectTimeout = 15_000
            conexao.readTimeout = 20_000
            conexao.doOutput = true
            conexao.setRequestProperty("Authorization", "Bearer $token")
            conexao.setRequestProperty("apikey", cliente.supabaseKey)
            conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conexao.outputStream.use { it.write(corpo) }

            val status = conexao.responseCode
            if (status != HttpURLConnection.HTTP_CREATED) {
                val resposta = conexao.errorStream?.bufferedReader()?.use {
                    it.readText()
                }
                val mensagem = resposta?.let {
                    runCatching { JSONObject(it).optString("mensagem") }
                        .getOrNull()
                        ?.takeIf(String::isNotBlank)
                }

                error(
                    mensagem ?: when (status) {
                        401 -> "Entre novamente como administrador."
                        403 -> "Somente administradores podem cadastrar funcionários."
                        404 -> "A função cadastrar-funcionario ainda precisa ser publicada."
                        else -> "Não foi possível cadastrar o funcionário."
                    }
                )
            }
        } finally {
            conexao.disconnect()
        }
    }
}