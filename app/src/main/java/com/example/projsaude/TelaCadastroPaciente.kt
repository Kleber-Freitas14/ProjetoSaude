
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
import androidx.compose.ui.unit.dp

@Composable
fun TelaCadastroPaciente(
    aoVoltar: () -> Unit,
    aoEnviar: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var nascimento by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
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
            text = "Pedido médico",
            style = MaterialTheme.typography.titleMedium
        )

        Button(
            onClick = {
                seletorImagem.launch("image/*")
            },
            modifier = Modifier.fillMaxWidth()
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
                if (
                    nome.isBlank() ||
                    cpf.isBlank() ||
                    nascimento.isBlank() ||
                    telefone.isBlank()
                ) {
                    mensagem = "Preencha todos os campos."
                } else {
                    mensagem = ""
                    aoEnviar()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enviar cadastro")
        }

        if (mensagem.isNotBlank()) {
            Text(
                text = mensagem,
                color = MaterialTheme.colorScheme.error
            )
        }

        OutlinedButton(
            onClick = aoVoltar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar")
        }
    }
}
