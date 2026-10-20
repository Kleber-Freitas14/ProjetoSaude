
package com.example.projsaude

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TelaProcedimentos(aoVoltar: () -> Unit) {

    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var valor by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    val procedimentos = remember {
        mutableStateListOf<String>()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Procedimentos",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome do procedimento") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = valor,
            onValueChange = { valor = it },
            label = { Text("Valor (R$)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (
                    nome.isNotBlank() &&
                    descricao.isNotBlank() &&
                    valor.isNotBlank()
                ) {
                    procedimentos.add(
                        "$nome | $descricao | R$ $valor"
                    )
                    nome = ""
                    descricao = ""
                    valor = ""
                    mensagem = "Procedimento cadastrado!"
                } else {
                    mensagem = "Preencha todos os campos."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cadastrar procedimento")
        }

        if (mensagem.isNotEmpty()) {
            Text(
                text = mensagem,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Text(
            text = "Procedimentos cadastrados: ${procedimentos.size}",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(procedimentos) { procedimento ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = procedimento,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        OutlinedButton(
            onClick = aoVoltar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar")
        }
    }
}
