
package com.example.projsaude

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Consulta(
    val paciente: String,
    val data: String,
    val horario: String,
    val profissional: String,
    val observacoes: String
)

@Composable
fun TelaConsultas(aoVoltar: () -> Unit) {

    var paciente by remember { mutableStateOf("") }
    var data by remember { mutableStateOf("") }
    var horario by remember { mutableStateOf("") }
    var profissional by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    val consultas = remember { mutableStateListOf<Consulta>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Consultas",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = paciente,
            onValueChange = { paciente = it },
            label = { Text("Nome do paciente") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = data,
            onValueChange = { data = it },
            label = { Text("Data da consulta") },
            placeholder = { Text("DD/MM/AAAA") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = horario,
            onValueChange = { horario = it },
            label = { Text("Horário") },
            placeholder = { Text("HH:MM") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = profissional,
            onValueChange = { profissional = it },
            label = { Text("Nome do profissional") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = observacoes,
            onValueChange = { observacoes = it },
            label = { Text("Observações") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (
                    paciente.isNotBlank() &&
                    data.isNotBlank() &&
                    horario.isNotBlank() &&
                    profissional.isNotBlank()
                ) {
                    consultas.add(
                        Consulta(
                            paciente,
                            data,
                            horario,
                            profissional,
                            observacoes
                        )
                    )

                    paciente = ""
                    data = ""
                    horario = ""
                    profissional = ""
                    observacoes = ""
                    mensagem = "Consulta cadastrada com sucesso!"
                } else {
                    mensagem = "Preencha os campos obrigatórios."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cadastrar consulta")
        }

        if (mensagem.isNotEmpty()) {
            Text(
                text = mensagem,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Consultas cadastradas: ${consultas.size}",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(consultas) { consulta ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text("Paciente: ${consulta.paciente}")
                        Text("Data: ${consulta.data}")
                        Text("Horário: ${consulta.horario}")
                        Text("Profissional: ${consulta.profissional}")

                        if (consulta.observacoes.isNotBlank()) {
                            Text("Observações: ${consulta.observacoes}")
                        }
                    }
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
