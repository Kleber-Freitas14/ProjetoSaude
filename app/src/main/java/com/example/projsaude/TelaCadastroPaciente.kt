
package com.example.projsaude

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun TelaCadastroPaciente(
    aoVoltar: () -> Unit,
    aoEnviar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nome by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var nascimento by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }

    var mensagem by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }
    var fotoPedido by remember { mutableStateOf<Uri?>(null) }

    val seletorImagem = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        fotoPedido = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Cadastro do Paciente",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome completo *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = cpf,
            onValueChange = { cpf = it },
            label = { Text("CPF *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = nascimento,
            onValueChange = { nascimento = it },
            label = { Text("Data de nascimento (DD/MM/AAAA) *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = telefone,
            onValueChange = { telefone = it },
            label = { Text("Telefone *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        HorizontalDivider()

        Text(
            text = "Dados de acesso",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = confirmarSenha,
            onValueChange = { confirmarSenha = it },
            label = { Text("Confirmar senha *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        HorizontalDivider()

        Text(
            text = "Pedido médico",
            style = MaterialTheme.typography.titleMedium
        )

        Button(
            onClick = { seletorImagem.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !carregando
        ) {
            Text("Selecionar foto do pedido médico")
        }

        Text(
            text = if (fotoPedido != null) {
                "Foto do pedido médico selecionada."
            } else {
                "Nenhuma foto selecionada."
            },
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = {
                mensagem = ""

                when {
                    nome.isBlank() ||
                            cpf.isBlank() ||
                            nascimento.isBlank() ||
                            telefone.isBlank() ||
                            email.isBlank() ||
                            senha.isBlank() ||
                            confirmarSenha.isBlank() -> {
                        mensagem = "Preencha todos os campos obrigatórios."
                    }

                    senha != confirmarSenha -> {
                        mensagem = "As senhas não coincidem."
                    }

                    senha.length < 6 -> {
                        mensagem = "A senha deve possuir pelo menos 6 caracteres."
                    }

                    !android.util.Patterns.EMAIL_ADDRESS
                        .matcher(email.trim()).matches() -> {
                        mensagem = "Informe um e-mail válido."
                    }

                    else -> {
                        carregando = true

                        scope.launch {
                            try {
                                when (
                                    val resultado =
                                        CadastroPacienteRepository.cadastrarPaciente(
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
                            } catch (e: Exception) {
                                mensagem = e.message
                                    ?: "Não foi possível realizar o cadastro."
                            } finally {
                                carregando = false
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !carregando
        ) {
            if (carregando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Enviar cadastro")
            }
        }

        if (mensagem.isNotBlank()) {
            Text(
                text = mensagem,
                color = MaterialTheme.colorScheme.error
            )
        }

        OutlinedButton(
            onClick = aoVoltar,
            modifier = Modifier.fillMaxWidth(),
            enabled = !carregando
        ) {
            Text("Voltar")
        }
    }
}
