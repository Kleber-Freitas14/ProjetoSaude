package com.example.projsaude

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

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
            value = estado.email,
            onValueChange = modelo::alterarEmail,
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            enabled = !bloqueado
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = estado.senha,
            onValueChange = modelo::alterarSenha,
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !bloqueado
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (estado.mensagem.isNotBlank()) {
            Text(
                text = estado.mensagem,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = { modelo.entrar(autenticar) },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(30.dp),
            enabled = !bloqueado,
            colors = ButtonDefaults.buttonColors(containerColor = azul)
        ) {
            Text(
                text = if (bloqueado) "Entrando..." else "Entrar",
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = aoVoltar, enabled = !bloqueado) {
            Text("Voltar")
        }
    }
}
