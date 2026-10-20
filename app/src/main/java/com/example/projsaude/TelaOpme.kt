
package com.example.projsaude

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Opme(
    val nome: String,
    val descricao: String,
    val fabricante: String,
    val codigo: String
)

@Composable
fun TelaOpme(aoVoltar: () -> Unit) {
    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var fabricante by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    val materiais = remember { mutableStateListOf<Opme>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Cadastro de OPME",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome do material") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição / tipo de OPME") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = fabricante,
            onValueChange = { fabricante = it },
            label = { Text("Fabricante") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            label = { Text("Código ou referência") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (nome.isBlank() || descricao.isBlank()) {
                    mensagem = "Informe o nome e a descrição do material."
                } else {
                    materiais.add(
                        Opme(
                            nome = nome.trim(),
                            descricao = descricao.trim(),
                            fabricante = fabricante.trim(),
                            codigo = codigo.trim()
                        )
                    )

                    nome = ""
                    descricao = ""
                    fabricante = ""
                    codigo = ""
                    mensagem = "Material cadastrado com sucesso!"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cadastrar material")
        }

        if (mensagem.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(mensagem)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Materiais cadastrados: ${materiais.size}",
            style = MaterialTheme.typography.titleLarge
        )

        materiais.forEach { material ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Material: ${material.nome}")
                    Text("Descrição: ${material.descricao}")
                    Text("Fabricante: ${material.fabricante.ifBlank { "Não informado" }}")
                    Text("Código: ${material.codigo.ifBlank { "Não informado" }}")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = aoVoltar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar")
        }
    }
}
