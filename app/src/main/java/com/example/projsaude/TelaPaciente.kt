
package com.example.projsaude

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AzulPacientes = Color(0xFF085DD3)

private data class Paciente(
    val nome: String,
    val cpf: String,
    val nascimento: String,
    val telefone: String
)

@Composable
fun TelaPacientes(
    aoVoltar: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var nascimento by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    val pacientes = remember { mutableStateListOf<Paciente>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8FC))
            .padding(20.dp)
    ) {
        TextButton(onClick = aoVoltar) {
            Text("← Voltar ao menu")
        }

        Text(
            text = "Cadastro de Pacientes",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = AzulPacientes
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome completo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cpf,
            onValueChange = { cpf = it },
            label = { Text("CPF") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = nascimento,
            onValueChange = { nascimento = it },
            label = { Text("Data de nascimento (DD/MM/AAAA)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = telefone,
            onValueChange = { telefone = it },
            label = { Text("Telefone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                    pacientes.add(
                        Paciente(
                            nome = nome.trim(),
                            cpf = cpf.trim(),
                            nascimento = nascimento.trim(),
                            telefone = telefone.trim()
                        )
                    )

                    nome = ""
                    cpf = ""
                    nascimento = ""
                    telefone = ""
                    mensagem = "Paciente cadastrado nesta sessão."
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AzulPacientes
            )
        ) {
            Text("Salvar paciente", fontSize = 17.sp)
        }

        if (mensagem.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mensagem,
                color = if (mensagem.startsWith("Paciente"))
                    Color(0xFF16803D)
                else
                    Color.Red
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Pacientes cadastrados: ${pacientes.size}",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))


        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pacientes) { paciente ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = paciente.nome,
                            fontWeight = FontWeight.Bold
                        )

                        Text("CPF: ${paciente.cpf}")
                        Text("Nascimento: ${paciente.nascimento}")
                        Text("Telefone: ${paciente.telefone}")

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                pacientes.remove(paciente)
                                mensagem = "Paciente excluído da lista."
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFC62828)
                            )
                        ) {
                            Text("Excluir paciente")
                        }
                    }
                }
            }
        }
    }
}

