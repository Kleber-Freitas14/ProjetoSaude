
package com.example.projsaude

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ValorHospitalar(
    val nome: String,
    val valor: String
)

object ValoresHospitalares {
    val valores = mutableStateMapOf<String, String>()

    val itens = listOf(
        "Taxa de internação",
        "Porte cirúrgico I",
        "Porte cirúrgico II",
        "Porte cirúrgico III",
        "Porte cirúrgico IV",
        "Porte cirúrgico V",
        "Porte cirúrgico VI",
        "Porte cirúrgico VII",
        "Diária de UTI",
        "Taxa de UTI",
        "Diária de Enfermaria",
        "Diária de Apartamento Individual",
        "Diária de Apartamento Duplo",
        "Respirador a volume",
        "Oxigênio por diária",
        "Alimentação de acompanhante",
        "Procedimento extra",
        "Hemoterapia",
        "Fisioterapia"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCadastroValores(
    aoVoltar: () -> Unit
) {
    var mensagem by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Cadastro de Valores Hospitalares",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Informe os valores que serão utilizados na composição dos orçamentos."
        )

        ValoresHospitalares.itens.forEach { item ->
            OutlinedTextField(
                value = ValoresHospitalares.valores[item] ?: "",
                onValueChange = { novoValor ->
                    ValoresHospitalares.valores[item] = novoValor
                    mensagem = ""
                },
                label = { Text(item) },
                placeholder = { Text("0,00") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        if (mensagem.isNotBlank()) {
            Text(mensagem)
        }

        Button(
            onClick = {

                val valoresInvalidos =
                    ValoresHospitalares.itens.any { item ->
                        val texto = ValoresHospitalares.valores[item]?.trim()

                        !texto.isNullOrEmpty() && run {
                            val valor = texto
                                .replace(",", ".")
                                .toDoubleOrNull()

                            valor == null || !valor.isFinite() || valor < 0
                        }
                    }


                mensagem = if (valoresInvalidos) {
                    "Preencha todos os campos com valores numéricos válidos."
                } else {
                    "Valores registrados para uso durante esta execução do aplicativo."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Validar e salvar")
        }

        OutlinedButton(
            onClick = aoVoltar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar")
        }
    }
}
