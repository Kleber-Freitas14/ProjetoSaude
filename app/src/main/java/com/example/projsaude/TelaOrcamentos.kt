
package com.example.projsaude

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


private data class Orcamento(
    val paciente: String,
    val medico: String,
    val procedimento: String,
    val descricao: String,
    val dataPrevista: String,
    val hospedagem: String,
    val necessitaUti: String,
    val valor: String
)

@Composable
fun TelaOrcamentos(aoVoltar: () -> Unit) {

    var paciente by remember { mutableStateOf("") }
    var medico by remember { mutableStateOf("") }
    var procedimento by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var dataPrevista by remember { mutableStateOf("") }
    var hospedagem by remember { mutableStateOf("Enfermaria") }
    var necessitaUti by remember { mutableStateOf("Não") }
    var valor by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    val orcamentos = remember {
        mutableStateListOf<Orcamento>()
    }

    var orcamentoSelecionado by remember {
        mutableStateOf<Orcamento?>(null)
    }

    if (orcamentoSelecionado != null) {
        val selecionado = orcamentoSelecionado!!

        TelaComposicaoOrcamento(
            paciente = selecionado.paciente,
            procedimento = selecionado.procedimento,
            aoVoltar = {
                orcamentoSelecionado = null
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Orçamentos Hospitalares",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = paciente,
                    onValueChange = { paciente = it },
                    label = { Text("Nome do paciente *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = medico,
                    onValueChange = { medico = it },
                    label = { Text("Médico solicitante *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = procedimento,
                    onValueChange = { procedimento = it },
                    label = { Text("Procedimento *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = dataPrevista,
                    onValueChange = { dataPrevista = it },
                    label = { Text("Data prevista do procedimento *") },
                    placeholder = { Text("DD/MM/AAAA") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Data para planejamento da reserva do Centro Cirúrgico.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item {
                Text(
                    text = "Tipo de hospedagem *",
                    style = MaterialTheme.typography.titleMedium
                )

                listOf(
                    "Enfermaria",
                    "Apartamento Individual",
                    "Apartamento Duplo"
                ).forEach { opcao ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = hospedagem == opcao,
                            onClick = { hospedagem = opcao }
                        )
                        Text(opcao)
                    }
                }
            }

            item {
                Text(
                    text = "Necessita de UTI? *",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = necessitaUti == "Sim",
                        onClick = { necessitaUti = "Sim" }
                    )
                    Text("Sim")

                    Spacer(modifier = Modifier.width(24.dp))

                    RadioButton(
                        selected = necessitaUti == "Não",
                        onClick = { necessitaUti = "Não" }
                    )
                    Text("Não")
                }
            }

            item {
                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it },
                    label = { Text("Valor total (R$) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Button(
                    onClick = {
                        if (
                            paciente.isNotBlank() &&
                            medico.isNotBlank() &&
                            procedimento.isNotBlank() &&
                            dataPrevista.isNotBlank() &&
                            valor.isNotBlank()
                        ) {
                            orcamentos.add(
                                Orcamento(
                                    paciente = paciente.trim(),
                                    medico = medico.trim(),
                                    procedimento = procedimento.trim(),
                                    descricao = descricao.trim(),
                                    dataPrevista = dataPrevista.trim(),
                                    hospedagem = hospedagem,
                                    necessitaUti = necessitaUti,
                                    valor = valor.trim()
                                )
                            )

                            paciente = ""
                            medico = ""
                            procedimento = ""
                            descricao = ""
                            dataPrevista = ""
                            hospedagem = "Enfermaria"
                            necessitaUti = "Não"
                            valor = ""

                            mensagem = "Orçamento cadastrado com sucesso!"
                        } else {
                            mensagem = "Preencha todos os campos obrigatórios (*)."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cadastrar orçamento")
                }
            }

            item {
                if (mensagem.isNotEmpty()) {
                    Text(
                        text = mensagem,
                        color = if (mensagem.startsWith("Orçamento"))
                            androidx.compose.ui.graphics.Color(0xFF16803D)
                        else
                            MaterialTheme.colorScheme.error
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text(
                    text = "Orçamentos cadastrados: ${orcamentos.size}",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(orcamentos) { orcamento ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            orcamentoSelecionado = orcamento
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Paciente: ${orcamento.paciente}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("Médico: ${orcamento.medico}")
                        Text("Procedimento: ${orcamento.procedimento}")
                        Text("Descrição: ${orcamento.descricao}")
                        Text("Data prevista: ${orcamento.dataPrevista}")
                        Text("Hospedagem: ${orcamento.hospedagem}")
                        Text("Necessita de UTI: ${orcamento.necessitaUti}")
                        Text("Valor total: R$ ${orcamento.valor}")

                        Text(
                            text = "Toque para abrir a composição do orçamento",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = aoVoltar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar")
        }
    }
}
