
package com.example.projsaude

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TelaInicial(
    aoAcessarColaborador: () -> Unit,
    aoAcessarPaciente: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sistema de Saúde",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Selecione como deseja acessar",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = aoAcessarColaborador,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Acesso do Colaborador")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = aoAcessarPaciente,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Acesso do Paciente")
        }
    }
}
