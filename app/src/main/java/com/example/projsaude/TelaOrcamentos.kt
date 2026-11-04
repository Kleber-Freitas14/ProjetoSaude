
package com.example.projsaude

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private data class Orcamento(
    val paciente: String,
    val medico: String,
    val procedimento: String,
    val dataPrevista: String,
    val descricao: String,
    val hospedagem: String,
    val necessitaUti: String
)

private fun formatarData(millis: Long): String {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    formato.timeZone = TimeZone.getTimeZone("UTC")
    return formato.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaOrcamentos(aoVoltar: () -> Unit) {

    var paciente by remember { mutableStateOf("") }
    var medico by remember { mutableStateOf("") }
    var procedimento by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var hospedagem by remember { mutableStateOf("Enfermaria") }
    var necessitaUti by remember { mutableStateOf("Não") }
    var dataPrevistaMillis by remember { mutableStateOf<Long?>(null) }
    var mostrarCalendario by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("") }

    val orcamentos = remember {
        mutableStateListOf<Orcamento>()
    }

    var orcamentoSelecionado by remember {
        mutableStateOf<Orcamento?>(null)
    }

    val estadoCalendario = rememberDatePickerState(
        initialSelectedDateMillis = dataPrevistaMillis
    )

    if (mostrarCalendario) {
        DatePickerDialog(
            onDismissRequest = {
                mostrarCalendario = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dataPrevistaMillis =
                            estadoCalendario.selectedDateMillis
                        mostrarCalendario = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarCalendario = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(
                state = estadoCalendario,
                title = {
                    Text("Data prevista do procedimento")
                },
                showModeToggle = false
            )
        }
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
                    onValueChange = {
                        paciente = it
                    },
                    label = {
                        Text("Nome do paciente *")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = medico,
                    onValueChange = {
                        medico = it
                    },
                    label = {
                        Text("Médico solicitante *")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = procedimento,
                    onValueChange = {
                        procedimento = it
                    },
                    label = {
                        Text("Procedimento *")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = dataPrevistaMillis?.let {
                        formatarData(it)
                    } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text("Data prevista do procedimento *")
                    },
                    placeholder = {
                        Text("Selecione a data")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        mostrarCalendario = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Selecionar data")
                }
            }

            item {
                OutlinedTextField(
                    value = descricao,
                    onValueChange = {
                        descricao = it
                    },
                    label = {
                        Text("Descrição / observações")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
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
                            onClick = {
                                hospedagem = opcao
                            }
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
                        onClick = {
                            necessitaUti = "Sim"
                        }
                    )
                    Text("Sim")

                    Spacer(modifier = Modifier.width(24.dp))

                    RadioButton(
                        selected = necessitaUti == "Não",
                        onClick = {
                            necessitaUti = "Não"
                        }
                    )
                    Text("Não")
                }
            }

            item {
                Button(
                    onClick = {
                        if (
                            paciente.isNotBlank() &&
                            medico.isNotBlank() &&
                            procedimento.isNotBlank() &&
                            dataPrevistaMillis != null
                        ) {
                            orcamentos.add(
                                Orcamento(
                                    paciente = paciente.trim(),
                                    medico = medico.trim(),
                                    procedimento = procedimento.trim(),
                                    dataPrevista = formatarData(
                                        dataPrevistaMillis!!
                                    ),
                                    descricao = descricao.trim(),
                                    hospedagem = hospedagem,
                                    necessitaUti = necessitaUti
                                )
                            )

                            paciente = ""
                            medico = ""
                            procedimento = ""
                            descricao = ""
                            hospedagem = "Enfermaria"
                            necessitaUti = "Não"
                            dataPrevistaMillis = null
                            mensagem = "Orçamento cadastrado com sucesso!"
                        } else {
                            mensagem =
                                "Preencha os campos obrigatórios (*)."
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
                        color = if (
                            mensagem.startsWith("Orçamento")
                        ) {
                            Color(0xFF16803D)
                        } else {
                            MaterialTheme.colorScheme.error
                        }
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
                        Text("Data prevista: ${orcamento.dataPrevista}")
                        Text("Descrição: ${orcamento.descricao}")
                        Text("Hospedagem: ${orcamento.hospedagem}")
                        Text("Necessita de UTI: ${orcamento.necessitaUti}")

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
