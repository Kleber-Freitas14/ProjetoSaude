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

data class ValorHospitalar(
    val nome: String,
    val valor: String
)

private data class SecaoValoresHospitalares(
    val titulo: String,
    val itens: List<String>
)

private val secoesValoresHospitalares = listOf(
    SecaoValoresHospitalares(
        titulo = "Internação e portes cirúrgicos",
        itens = listOf(
            "Taxa de internação",
            "Porte cirúrgico I",
            "Porte cirúrgico II",
            "Porte cirúrgico III",
            "Porte cirúrgico IV",
            "Porte cirúrgico V",
            "Porte cirúrgico VI",
            "Porte cirúrgico VII"
        )
    ),
    SecaoValoresHospitalares(
        titulo = "Diárias e acomodações",
        itens = listOf(
            "Diária de UTI",
            "Taxa de UTI",
            "Diária de Enfermaria",
            "Diária de Apartamento Individual",
            "Diária de Apartamento Duplo"
        )
    ),
    SecaoValoresHospitalares(
        titulo = "Suporte e serviços",
        itens = listOf(
            "Respirador a volume",
            "Oxigênio por diária",
            "Alimentação de acompanhante",
            "Procedimento extra",
            "Hemoterapia",
            "Fisioterapia"
        )
    )
)

object ValoresHospitalares {
    val valores = mutableStateMapOf<String, String>()
    val itens = secoesValoresHospitalares.flatMap { it.itens }
}

private fun valorHospitalarInvalido(texto: String): Boolean {
    val entrada = texto.trim()
    if (entrada.isEmpty()) return false

    val valor = entrada.replace(",", ".").toDoubleOrNull()
    return valor == null || !valor.isFinite() || valor < 0
}

@Composable
fun TelaCadastroValores(aoVoltar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val formas = MaterialTheme.shapes

    var itensInvalidos by remember { mutableStateOf(emptySet<String>()) }
    var mensagem by remember { mutableStateOf("") }

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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Valores hospitalares",
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

                    Text(
                        text = "Os valores preenchidos ficam disponíveis durante esta execução do app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cores.onSurfaceVariant
                    )
                }
            }

            items(secoesValoresHospitalares, key = { it.titulo }) { secao ->
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
                            text = secao.titulo,
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        secao.itens.forEach { item ->
                            val invalido = item in itensInvalidos

                            OutlinedTextField(
                                value = ValoresHospitalares.valores[item].orEmpty(),
                                onValueChange = { novoValor ->
                                    ValoresHospitalares.valores[item] = novoValor
                                    if (item in itensInvalidos && !valorHospitalarInvalido(novoValor)) {
                                        itensInvalidos = itensInvalidos - item
                                    }
                                    mensagem = ""
                                },
                                label = { Text(item) },
                                prefix = { Text("R\$") },
                                placeholder = { Text("0,00") },
                                supportingText = if (invalido) {
                                    { Text("Informe um número maior ou igual a zero.") }
                                } else {
                                    null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = formas.small,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                isError = invalido,
                                singleLine = true
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (mensagem.isNotBlank()) {
                        val erro = itensInvalidos.isNotEmpty()

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            shape = formas.small,
                            color = if (erro) cores.errorContainer else cores.secondaryContainer,
                            contentColor = if (erro) cores.onErrorContainer else cores.onSecondaryContainer
                        ) {
                            Text(
                                text = mensagem,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Button(
                        onClick = {
                            itensInvalidos = ValoresHospitalares.itens.filter { item ->
                                valorHospitalarInvalido(ValoresHospitalares.valores[item].orEmpty())
                            }.toSet()

                            mensagem = if (itensInvalidos.isEmpty()) {
                                "Valores validados para esta execução do app."
                            } else {
                                "Revise os valores destacados."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = formas.small
                    ) {
                        Text("Validar valores")
                    }

                    OutlinedButton(
                        onClick = aoVoltar,
                        modifier = Modifier.fillMaxWidth(),
                        shape = formas.small
                    ) {
                        Text("Voltar")
                    }
                }
            }
        }
    }
}