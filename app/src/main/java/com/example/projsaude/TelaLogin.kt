package com.example.projsaude

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun TelaLogin(
    aoVoltar: () -> Unit,
    aoEntrar: (Boolean) -> Unit,
    titulo: String = "Login",
    autenticar: suspend (email: String, senha: String) -> Boolean = { email, senha ->
        autenticarUsuario(email = email, senha = senha)
    }
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }

    val escopo = rememberCoroutineScope()
    val azul = Color(0xFF085DD3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Projeto Saúde",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = azul
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = titulo,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                mensagem = ""
            },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            enabled = !carregando
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = senha,
            onValueChange = {
                senha = it
                mensagem = ""
            },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !carregando
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (mensagem.isNotBlank()) {
            Text(
                text = mensagem,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = {
                val emailInformado = email.trim()

                mensagem = when {
                    emailInformado.isBlank() -> "Informe o e-mail."
                    !Patterns.EMAIL_ADDRESS.matcher(emailInformado).matches() ->
                        "Informe um e-mail válido."
                    senha.isBlank() -> "Informe a senha."
                    else -> ""
                }

                if (mensagem.isEmpty()) {
                    val senhaInformada = senha
                    carregando = true

                    escopo.launch {
                        try {
                            val ehAdministrador = autenticar(emailInformado, senhaInformada)
                            aoEntrar(ehAdministrador)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            mensagem = if (
                                e.message?.contains(
                                    "Invalid login credentials",
                                    ignoreCase = true
                                ) == true
                            ) {
                                "E-mail ou senha incorretos."
                            } else {
                                "Não foi possível entrar nesta área. Confira seus dados e tente novamente."
                            }
                        } finally {
                            carregando = false
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(30.dp),
            enabled = !carregando,
            colors = ButtonDefaults.buttonColors(containerColor = azul)
        ) {
            Text(
                text = if (carregando) "Entrando..." else "Entrar",
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = aoVoltar, enabled = !carregando) {
            Text("Voltar")
        }
    }
}