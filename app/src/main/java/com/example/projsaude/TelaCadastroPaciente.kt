package com.example.projsaude

import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun TelaCadastroPaciente(
    aoVoltar: () -> Unit,
    aoEnviar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cores = MaterialTheme.colorScheme
    val formas = MaterialTheme.shapes

    var nome by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var nascimento by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }

    var mensagem by remember { mutableStateOf("") }
    var erroCadastro by remember { mutableStateOf(false) }
    var carregando by remember { mutableStateOf(false) }
    var fotoPedido by remember { mutableStateOf<Uri?>(null) }

    val seletorImagem = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        fotoPedido = uri
    }

    BackHandler(enabled = carregando) {}

    fun enviarCadastro() {
        if (carregando) return

        mensagem = when {
            nome.isBlank() || cpf.isBlank() || nascimento.isBlank() ||
                    telefone.isBlank() || email.isBlank() || senha.isBlank() ||
                    confirmarSenha.isBlank() -> "Preencha todos os campos obrigatórios."

            senha != confirmarSenha -> "As senhas não coincidem."

            senha.length < 6 -> "A senha deve possuir pelo menos 6 caracteres."

            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                "Informe um e-mail válido."

            else -> ""
        }

        erroCadastro = mensagem.isNotBlank()
        if (erroCadastro) return

        carregando = true

        scope.launch {
            try {
                when (
                    val resultado = CadastroPacienteRepository.cadastrarPaciente(
                        context = context,
                        nome = nome,
                        cpf = cpf,
                        nascimento = nascimento,
                        telefone = telefone,
                        email = email,
                        senha = senha,
                        fotoPedido = fotoPedido
                    )
                ) {
                    is ResultadoCadastroPaciente.Sucesso -> {
                        mensagem = resultado.mensagem
                        aoEnviar()
                    }

                    is ResultadoCadastroPaciente.AguardandoConfirmacao -> {
                        mensagem = resultado.mensagem
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mensagem = e.message ?: "Não foi possível realizar o cadastro."
                erroCadastro = true
            } finally {
                carregando = false
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
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF10345E), Color(0xFF064FB5))
                        ),
                        shape = formas.large
                    )
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = formas.small,
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
                        text = "Cadastro do paciente",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Text(
                text = "Os campos com asterisco (*) são obrigatórios.",
                style = MaterialTheme.typography.bodyMedium,
                color = cores.onSurfaceVariant
            )

            SecaoCadastroPaciente("Dados pessoais") {
                CampoCadastroPaciente(
                    valor = nome,
                    aoAlterar = { nome = it },
                    rotulo = "Nome completo *",
                    habilitado = !carregando
                )

                CampoCadastroPaciente(
                    valor = cpf,
                    aoAlterar = { cpf = it },
                    rotulo = "CPF *",
                    habilitado = !carregando,
                    teclado = KeyboardType.Number
                )

                CampoCadastroPaciente(
                    valor = nascimento,
                    aoAlterar = { nascimento = it },
                    rotulo = "Data de nascimento *",
                    habilitado = !carregando,
                    exemplo = "DD/MM/AAAA"
                )

                CampoCadastroPaciente(
                    valor = telefone,
                    aoAlterar = { telefone = it },
                    rotulo = "Telefone *",
                    habilitado = !carregando,
                    teclado = KeyboardType.Phone
                )
            }

            SecaoCadastroPaciente("Dados de acesso") {
                CampoCadastroPaciente(
                    valor = email,
                    aoAlterar = { email = it },
                    rotulo = "E-mail *",
                    habilitado = !carregando,
                    teclado = KeyboardType.Email
                )

                CampoCadastroPaciente(
                    valor = senha,
                    aoAlterar = { senha = it },
                    rotulo = "Senha *",
                    habilitado = !carregando,
                    teclado = KeyboardType.Password,
                    ajuda = "Pelo menos 6 caracteres"
                )

                CampoCadastroPaciente(
                    valor = confirmarSenha,
                    aoAlterar = { confirmarSenha = it },
                    rotulo = "Confirmar senha *",
                    habilitado = !carregando,
                    teclado = KeyboardType.Password
                )
            }

            SecaoCadastroPaciente("Pedido médico") {
                Text(
                    text = "Você pode anexar uma foto do pedido médico ao cadastro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cores.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { seletorImagem.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    enabled = !carregando,
                    shape = formas.small
                ) {
                    Text(
                        if (fotoPedido == null) "Selecionar foto do pedido médico"
                        else "Trocar foto do pedido médico"
                    )
                }

                if (fotoPedido != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        shape = formas.small,
                        color = cores.secondaryContainer,
                        contentColor = cores.onSecondaryContainer
                    ) {
                        Text(
                            text = "Foto do pedido médico selecionada.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (mensagem.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    shape = formas.small,
                    color = if (erroCadastro) cores.errorContainer else cores.primaryContainer,
                    contentColor = if (erroCadastro) {
                        cores.onErrorContainer
                    } else {
                        cores.onPrimaryContainer
                    }
                ) {
                    Text(
                        text = mensagem,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = ::enviarCadastro,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                enabled = !carregando,
                shape = formas.small
            ) {
                if (carregando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = cores.onSurfaceVariant,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                }

                Text(if (carregando) "Enviando cadastro…" else "Enviar cadastro")
            }

            OutlinedButton(
                onClick = aoVoltar,
                modifier = Modifier.fillMaxWidth(),
                enabled = !carregando,
                shape = formas.small
            ) {
                Text("Voltar")
            }
        }
    }
}

@Composable
private fun SecaoCadastroPaciente(
    titulo: String,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    val cores = MaterialTheme.colorScheme

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = cores.surface,
            contentColor = cores.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = titulo,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            conteudo()
        }
    }
}

@Composable
private fun CampoCadastroPaciente(
    valor: String,
    aoAlterar: (String) -> Unit,
    rotulo: String,
    habilitado: Boolean,
    teclado: KeyboardType = KeyboardType.Text,
    exemplo: String? = null,
    ajuda: String? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoAlterar,
        label = { Text(rotulo) },
        placeholder = exemplo?.let { texto -> { Text(texto) } },
        supportingText = ajuda?.let { texto -> { Text(texto) } },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        visualTransformation = if (teclado == KeyboardType.Password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        singleLine = true,
        enabled = habilitado
    )
}