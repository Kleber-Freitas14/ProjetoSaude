
package com.example.projsaude

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TelaComposicaoOrcamento(
    paciente: String,
    procedimento: String,
    aoVoltar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Composição do Orçamento",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Paciente: $paciente")
        Text("Procedimento: $procedimento")

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Text(
            text = "Itens do orçamento cirúrgico",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Nesta área serão adicionadas as taxas, os equipamentos, " +
                    "os serviços hospitalares e os demais itens que compõem o orçamento."
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Total dos itens: A definir")

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = aoVoltar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar ao orçamento")
        }
    }
}
