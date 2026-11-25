package com.example.projsaude

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val AzulMarinhoLogin = Color(0xFF10345E)
private val AzulAcaoLogin = Color(0xFF064FB5)
private val BordaLogin = Color(0xFFA9C6E9)
private val TextoSecundarioLogin = Color(0xFF35516F)

@Composable
fun TelaLogin(
    aoVoltar: () -> Unit,
    aoEntrar: (Boolean) -> Unit,
    titulo: String = "Login",
    autenticar: suspend (email: String, senha: String) -> Boolean = { email, senha ->
        autenticarUsuario(email = email, senha = senha)
    }
) {
    val modelo: LoginViewModel = viewModel()
    val estado by modelo.estado.collectAsStateWithLifecycle()
    val bloqueado = estado.carregando || estado.administrador != null

    LaunchedEffect(estado.administrador) {
        estado.administrador?.let(aoEntrar)
    }

    BackHandler(enabled = bloqueado) {}

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFFB8D6F6),
                    0.55f to Color(0xFFD7E8FC),
                    1f to Color(0xFFF0F6FE)
                )
            )
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        val alturaDisponivel = maxHeight

        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = alturaDisponivel)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFFF5F5F5),
                border = BorderStroke(1.dp, BordaLogin)
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_projeto_saude),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(6.dp)
                )
            }

            Text(
                text = "Projeto Saúde",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = AzulMarinhoLogin
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                contentColor = AzulMarinhoLogin,
                border = BorderStroke(1.dp, BordaLogin)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = titulo,
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    val coresCampos = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulAcaoLogin,
                        unfocusedBorderColor = BordaLogin,
                        focusedLabelColor = AzulAcaoLogin,
                        unfocusedLabelColor = TextoSecundarioLogin,
                        focusedTextColor = AzulMarinhoLogin,
                        unfocusedTextColor = AzulMarinhoLogin,
                        cursorColor = AzulAcaoLogin
                    )

                    OutlinedTextField(
                        value = estado.email,
                        onValueChange = modelo::alterarEmail,
                        label = { Text("E-mail") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = coresCampos,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        singleLine = true,
                        enabled = !bloqueado
                    )

                    OutlinedTextField(
                        value = estado.senha,
                        onValueChange = modelo::alterarSenha,
                        label = { Text("Senha") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = coresCampos,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !bloqueado
                    )

                    if (estado.mensagem.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ) {
                            Text(
                                text = estado.mensagem,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Button(
                        onClick = { modelo.entrar(autenticar) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !bloqueado,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AzulAcaoLogin,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFD7E8FC),
                            disabledContentColor = AzulMarinhoLogin
                        )
                    ) {
                        if (estado.carregando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AzulAcaoLogin,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                        }

                        Text(
                            text = if (bloqueado) "Entrando…" else "Entrar",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            TextButton(
                onClick = aoVoltar,
                enabled = !bloqueado,
                colors = ButtonDefaults.textButtonColors(contentColor = AzulMarinhoLogin)
            ) {
                Text("Voltar")
            }
        }
    }
}