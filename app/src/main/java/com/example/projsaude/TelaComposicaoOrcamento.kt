
package com.example.projsaude

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoMoeda = NumberFormat.getCurrencyInstance(
    Locale("pt", "BR")
)

private fun converterValor(nome: String): Double {
    return ValoresHospitalares.valores[nome]
        ?.trim()
        ?.replace(",", ".")
        ?.toDoubleOrNull()
        ?: 0.0
}

private fun calcularDias(entrada: String, saida: String): Int {
    if (entrada.isBlank() || saida.isBlank()) return 0

    return try {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        formato.isLenient = false

        val dataInicial: Date = formato.parse(entrada) ?: return 0
        val dataFinal: Date = formato.parse(saida) ?: return 0

        val diferenca = dataFinal.time - dataInicial.time

        if (diferenca < 0) {
            0
        } else {
            (diferenca / 86_400_000L).toInt() + 1
        }
    } catch (_: Exception) {
        0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaComposicaoOrcamento(
    paciente: String,
    procedimento: String,
    aoVoltar: () -> Unit
) {
    var etapaAtual by remember { mutableIntStateOf(1) }

    var porteCirurgico by remember { mutableStateOf("") }

    var dataEntradaUTI by remember { mutableStateOf("") }
    var dataSaidaUTI by remember { mutableStateOf("") }

    var dataEntradaApartamento by remember { mutableStateOf("") }
    var dataSaidaApartamento by remember { mutableStateOf("") }

    var quantidadeExtras by remember { mutableStateOf("0") }
    var quantidadeHemoterapia by remember { mutableStateOf("0") }
    var quantidadeFisioterapia by remember { mutableStateOf("0") }

    var apartamento by remember { mutableStateOf("Nenhum") }
    var menuPorteAberto by remember { mutableStateOf(false) }

    val portes = listOf(
        "Porte I (Até 2h)",
        "Porte II (2h à 4h)",
        "Porte III (4h à 6h)",
        "Porte IV (> 6h)",
        "Porte V (Grande Procedimento)",
        "Porte VI (Cirurgia Especial)",
        "Porte VII (Cirurgia Especial Cardíaca)"
    )

    val diasUTI = calcularDias(dataEntradaUTI, dataSaidaUTI)

    val diasApartamento = if (apartamento != "Nenhum") {
        calcularDias(dataEntradaApartamento, dataSaidaApartamento)
    } else {
        0
    }

    val quantidadeProcedimentos = quantidadeExtras.toIntOrNull() ?: 0
    val quantidadeHemoterapiaInt = quantidadeHemoterapia.toIntOrNull() ?: 0
    val quantidadeFisioterapiaInt = quantidadeFisioterapia.toIntOrNull() ?: 0

    val chavePorte = porteCirurgico
        .substringBefore(" (")
        .replace("Porte ", "Porte cirúrgico ")

    val chaveApartamento = when (apartamento) {
        "Apartamento Individual" -> "Diária de Apartamento Individual"
        "Apartamento Duplo" -> "Diária de Apartamento Duplo"
        else -> ""
    }

    val taxaInternacao = converterValor("Taxa de internação")

    val valorPorte = if (porteCirurgico.isNotBlank()) {
        converterValor(chavePorte)
    } else {
        0.0
    }

    val valorDiariaUTI = converterValor("Diária de UTI")
    val valorTaxaUTI = converterValor("Taxa de UTI")
    val valorRespirador = converterValor("Respirador a volume")
    val valorOxigenio = converterValor("Oxigênio por diária")

    val valorDiariaApartamento = if (chaveApartamento.isNotBlank()) {
        converterValor(chaveApartamento)
    } else {
        0.0
    }

    val valorAlimentacao = converterValor("Alimentação de acompanhante")
    val valorExtra = converterValor("Procedimento extra")
    val valorHemoterapia = converterValor("Hemoterapia")
    val valorFisioterapia = converterValor("Fisioterapia")

    val total = taxaInternacao +
            valorPorte +
            (valorDiariaUTI * diasUTI) +
            (valorTaxaUTI * if (diasUTI > 0) 1 else 0) +
            (valorRespirador * diasUTI) +
            (valorOxigenio * diasUTI) +
            (valorDiariaApartamento * diasApartamento) +
            (valorAlimentacao * diasApartamento) +
            (valorExtra * quantidadeProcedimentos) +
            (valorHemoterapia * quantidadeHemoterapiaInt) +
            (valorFisioterapia * quantidadeFisioterapiaInt)

    val valoresPendentes = listOf(
        "Taxa de internação",
        chavePorte.takeIf { porteCirurgico.isNotBlank() },
        "Diária de UTI",
        "Taxa de UTI",
        "Respirador a volume",
        "Oxigênio por diária",
        chaveApartamento.takeIf { apartamento != "Nenhum" },
        "Alimentação de acompanhante",
        "Procedimento extra".takeIf { quantidadeProcedimentos > 0 },
        "Hemoterapia".takeIf { quantidadeHemoterapiaInt > 0 },
        "Fisioterapia".takeIf { quantidadeFisioterapiaInt > 0 }
    ).filterNotNull().filter { nome ->
        ValoresHospitalares.valores[nome]
            ?.trim()
            ?.replace(",", ".")
            ?.toDoubleOrNull() == null
    }

    val titulosEtapas = listOf(
        "Taxas e porte cirúrgico",
        "Internação e acomodação",
        "Procedimentos e serviços",
        "Materiais, medicamentos e fechamento"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Composição do Orçamento",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Paciente: $paciente")
        Text("Procedimento: $procedimento")

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Etapa $etapaAtual de 4",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { etapaAtual / 4f },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = titulosEtapas[etapaAtual - 1],
            style = MaterialTheme.typography.titleLarge
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            when (etapaAtual) {

                // ETAPA 1
                1 -> {

                    ItemOrcamento(
                        "Taxa de internação",
                        taxaInternacao
                    )

                    Text("Porte cirúrgico")

                    ExposedDropdownMenuBox(
                        expanded = menuPorteAberto,
                        onExpandedChange = {
                            menuPorteAberto = !menuPorteAberto
                        }
                    ) {
                        OutlinedTextField(
                            value = porteCirurgico,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Selecione o porte") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = menuPorteAberto
                                )
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = menuPorteAberto,
                            onDismissRequest = {
                                menuPorteAberto = false
                            }
                        ) {
                            portes.forEach { porte ->
                                DropdownMenuItem(
                                    text = { Text(porte) },
                                    onClick = {
                                        porteCirurgico = porte
                                        menuPorteAberto = false
                                    }
                                )
                            }
                        }
                    }

                    if (porteCirurgico.isNotBlank()) {
                        ItemOrcamento(
                            "Valor do $chavePorte",
                            valorPorte
                        )
                    }
                }

                // ETAPA 2
                2 -> {

                    CampoDataSimples(
                        "Entrada na UTI",
                        dataEntradaUTI
                    ) {
                        dataEntradaUTI = it
                    }

                    CampoDataSimples(
                        "Saída da UTI",
                        dataSaidaUTI
                    ) {
                        dataSaidaUTI = it
                    }

                    if (diasUTI > 0) {
                        Text("Dias de UTI: $diasUTI")
                    }

                    ItemOrcamento(
                        "Diária de UTI ($diasUTI dias)",
                        valorDiariaUTI * diasUTI
                    )

                    ItemOrcamento(
                        "Taxa de UTI",
                        valorTaxaUTI * if (diasUTI > 0) 1 else 0
                    )

                    ItemOrcamento(
                        "Respirador a volume ($diasUTI dias)",
                        valorRespirador * diasUTI
                    )

                    ItemOrcamento(
                        "Oxigênio ($diasUTI dias)",
                        valorOxigenio * diasUTI
                    )

                    HorizontalDivider()

                    Text(
                        text = "Acomodação",
                        style = MaterialTheme.typography.titleMedium
                    )

                    listOf(
                        "Nenhum",
                        "Apartamento Individual",
                        "Apartamento Duplo"
                    ).forEach { opcao ->

                        Row(
                            verticalAlignment =
                                androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = apartamento == opcao,
                                onClick = {
                                    apartamento = opcao
                                }
                            )

                            Text(opcao)
                        }
                    }

                    if (apartamento != "Nenhum") {

                        CampoDataSimples(
                            "Entrada no apartamento",
                            dataEntradaApartamento
                        ) {
                            dataEntradaApartamento = it
                        }

                        CampoDataSimples(
                            "Saída do apartamento",
                            dataSaidaApartamento
                        ) {
                            dataSaidaApartamento = it
                        }

                        if (diasApartamento > 0) {
                            Text("Dias de acomodação: $diasApartamento")
                        }

                        ItemOrcamento(
                            "Diária de $apartamento ($diasApartamento dias)",
                            valorDiariaApartamento * diasApartamento
                        )

                        ItemOrcamento(
                            "Alimentação para acompanhante ($diasApartamento dias)",
                            valorAlimentacao * diasApartamento
                        )
                    }
                }

                // ETAPA 3
                3 -> {

                    CampoQuantidade(
                        "Procedimentos Extras",
                        quantidadeExtras
                    ) {
                        quantidadeExtras = it
                    }

                    ItemOrcamento(
                        "Total de procedimentos extras",
                        valorExtra * quantidadeProcedimentos
                    )

                    CampoQuantidade(
                        "Componentes de Hemoterapia",
                        quantidadeHemoterapia
                    ) {
                        quantidadeHemoterapia = it
                    }

                    ItemOrcamento(
                        "Total de hemoterapia",
                        valorHemoterapia * quantidadeHemoterapiaInt
                    )

                    CampoQuantidade(
                        "Procedimentos de Fisioterapia",
                        quantidadeFisioterapia
                    ) {
                        quantidadeFisioterapia = it
                    }

                    ItemOrcamento(
                        "Total de fisioterapia",
                        valorFisioterapia * quantidadeFisioterapiaInt
                    )
                }

                // ETAPA 4
                4 -> {

                    Text(
                        text = "Materiais e medicamentos",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "Os materiais e medicamentos ainda não estão incluídos " +
                                "no cálculo automático."
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (valoresPendentes.isNotEmpty()) {

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {

                                Text(
                                    text = "Atenção: existem valores não cadastrados.",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )

                                valoresPendentes.forEach { nome ->
                                    Text(
                                        text = "• $nome",
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }

                                Text(
                                    text = "O total pode estar incompleto.",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider()

                    Text(
                        text = "Total do orçamento: ${formatoMoeda.format(total)}",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "Os itens sem preço cadastrado são considerados " +
                                "R$ 0,00. Confira os valores antes de utilizar o orçamento.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            OutlinedButton(
                onClick = {
                    if (etapaAtual == 1) {
                        aoVoltar()
                    } else {
                        etapaAtual--
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (etapaAtual == 1) {
                        "Voltar ao orçamento"
                    } else {
                        "Voltar"
                    }
                )
            }

            if (etapaAtual < 4) {

                Button(
                    onClick = {
                        etapaAtual++
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Próximo")
                }

            } else {

                Button(
                    onClick = aoVoltar,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Salvar")
                }
            }
        }
    }
}

@Composable
private fun ItemOrcamento(
    nome: String,
    valor: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = nome,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = formatoMoeda.format(valor),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CampoDataSimples(
    titulo: String,
    valor: String,
    aoAlterar: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoAlterar,
        label = { Text(titulo) },
        placeholder = { Text("DD/MM/AAAA") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true
    )
}

@Composable
private fun CampoQuantidade(
    titulo: String,
    valor: String,
    aoAlterar: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { novoValor ->
            if (novoValor.isEmpty() || novoValor.all { it.isDigit() }) {
                aoAlterar(novoValor)
            }
        },
        label = { Text(titulo) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true
    )
}
