package com.example.projsaude

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    val cores = MaterialTheme.colorScheme
    val formas = MaterialTheme.shapes

    var nome by remember { mutableStateOf("") }
    var crm by remember { mutableStateOf("") }
    var rqe by remember { mutableStateOf("") }
    var especialidade by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var atuacao by remember { mutableStateOf("Cirurgião") }
    var mensagem by remember { mutableStateOf("") }
    var cadastroConcluido by remember { mutableStateOf(false) }

    val medicos = remember { mutableStateListOf<Medico>() }

    fun cadastrarMedico() {
        cadastroConcluido = false
        val crmInformado = crm.trim()

        if (nome.isBlank() || crmInformado.isBlank() || especialidade.isBlank()) {
            mensagem = "Preencha nome, CRM e especialidade."
            return
        }

        if (medicos.any { it.crm.equals(crmInformado, ignoreCase = true) }) {
            mensagem = "Já existe um médico cadastrado com esse CRM."
            return
        }

        medicos.add(
            Medico(
                nome = nome.trim(),
                crm = crmInformado,
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
        cadastroConcluido = true
        mensagem = "Médico cadastrado com sucesso!"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cores.background)
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().fillMaxHeight(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Cadastro de médicos",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF10345E), Color(0xFF064FB5))
                            ),
                            shape = formas.large
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = formas.large,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = cores.surface,
                        contentColor = cores.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Dados do médico",
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = nome,
                            onValueChange = { nome = it },
                            label = { Text("Nome completo *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = crm,
                            onValueChange = { crm = it },
                            label = { Text("CRM *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = rqe,
                            onValueChange = { rqe = it },
                            label = { Text("RQE (se aplicável)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = especialidade,
                            onValueChange = { especialidade = it },
                            label = { Text("Especialidade médica *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = telefone,
                            onValueChange = { telefone = it },
                            label = { Text("Telefone") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = formas.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )

                        HorizontalDivider(color = cores.outlineVariant)

                        Text(
                            text = "Atuação na cirurgia",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Cirurgião", "Anestesista").forEach { tipo ->
                                FilterChip(
                                    selected = atuacao == tipo,
                                    onClick = { atuacao = tipo },
                                    modifier = Modifier.weight(1f),
                                    label = { Text(tipo) }
                                )
                            }
                        }

                        if (mensagem.isNotBlank()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                                shape = formas.small,
                                color = if (cadastroConcluido) {
                                    cores.tertiaryContainer
                                } else {
                                    cores.errorContainer
                                },
                                contentColor = if (cadastroConcluido) {
                                    cores.onTertiaryContainer
                                } else {
                                    cores.onErrorContainer
                                }
                            ) {
                                Text(
                                    text = mensagem,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Button(
                            onClick = ::cadastrarMedico,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = formas.small
                        ) {
                            Text("Cadastrar médico")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Médicos cadastrados: ${medicos.size}",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                if (medicos.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Nenhum médico cadastrado nesta tela.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cores.onSurfaceVariant
                    )
                }
            }

            items(medicos) { medico ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = formas.medium,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = cores.surface,
                        contentColor = cores.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = medico.nome,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Surface(
                            shape = formas.small,
                            color = cores.secondaryContainer,
                            contentColor = cores.onSecondaryContainer
                        ) {
                            Text(
                                text = medico.atuacao,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Text("CRM: ${medico.crm}")
                        Text("RQE: ${medico.rqe.ifBlank { "Não informado" }}")
                        Text("Especialidade: ${medico.especialidade}")
                        Text("Telefone: ${medico.telefone.ifBlank { "Não informado" }}")
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = aoVoltar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Voltar")
                }
            }
        }
    }
}