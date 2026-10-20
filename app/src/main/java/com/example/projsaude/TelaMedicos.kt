
package com.example.projsaude

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Medico(
    val nome: String,
    val crm: String,
    val rqe: String,
    val especialidade: String,
    val telefone: String,
    val atuacao: String
)

@Composable
fun TelaMedicos(aoVoltar: () -> Unit) {

    var nome by remember { mutableStateOf("") }
    var crm by remember { mutableStateOf("") }
    var rqe by remember { mutableStateOf("") }
    var especialidade by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var atuacao by remember { mutableStateOf("Cirurgião") }
    var mensagem by remember { mutableStateOf("") }

    val medicos = remember {
        mutableStateListOf<Medico>()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Cadastro de Médicos",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome completo *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = crm,
            onValueChange = { crm = it },
            label = { Text("CRM *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = rqe,
            onValueChange = { rqe = it },
            label = { Text("RQE (se aplicável)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = especialidade,
            onValueChange = { especialidade = it },
            label = { Text("Especialidade médica *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = telefone,
            onValueChange = { telefone = it },
            label = { Text("Telefone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Atuação na cirurgia",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Cirurgião", "Anestesista").forEach { tipo ->
                FilterChip(
                    selected = atuacao == tipo,
                    onClick = { atuacao = tipo },
                    label = { Text(tipo) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                when {
                    nome.isBlank() ||
                            crm.isBlank() ||
                            especialidade.isBlank() -> {
                        mensagem =
                            "Preencha nome, CRM e especialidade."
                    }

                    medicos.any {
                        it.crm.equals(
                            crm.trim(),
                            ignoreCase = true
                        )
                    } -> {
                        mensagem =
                            "Já existe um médico cadastrado com esse CRM."
                    }

                    else -> {
                        medicos.add(
                            Medico(
                                nome = nome.trim(),
                                crm = crm.trim(),
                                rqe = rqe.trim(),
                                especialidade = especialidade.trim(),
                                telefone = telefone.trim(),
                                atuacao = atuacao
                            )
                        )

                        nome = ""
                        crm = ""
                        rqe = ""
                        especialidade = ""
                        telefone = ""
                        atuacao = "Cirurgião"

                        mensagem = "Médico cadastrado com sucesso!"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cadastrar médico")
        }

        if (mensagem.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mensagem,
                color = if (mensagem.startsWith("Médico cadastrado")) {
                    androidx.compose.ui.graphics.Color(0xFF16803D)
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Médicos cadastrados: ${medicos.size}",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        medicos.forEach { medico ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Nome: ${medico.nome}")
                    Text("CRM: ${medico.crm}")
                    Text(
                        "RQE: ${medico.rqe.ifBlank { "Não informado" }}"
                    )
                    Text("Especialidade: ${medico.especialidade}")
                    Text(
                        "Telefone: ${
                            medico.telefone.ifBlank {
                                "Não informado"
                            }
                        }"
                    )
                    Text("Atuação: ${medico.atuacao}")
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
