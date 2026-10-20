
package com.example.projsaude

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TelaAcompanhamentoPaciente(
    aoVoltar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Acompanhamento da Solicitação",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Status da solicitação",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Solicitação recebida")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Seu pedido médico será analisado pela equipe do hospital."
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Orçamento",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Quando o orçamento estiver disponível, ele poderá ser consultado nesta área."
        )

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = aoVoltar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar")
        }
    }
}
