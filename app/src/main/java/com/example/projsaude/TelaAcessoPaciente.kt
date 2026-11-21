package com.example.projsaude

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TelaAcessoPaciente(
    aoEntrar: () -> Unit,
    aoCadastrar: () -> Unit,
    aoVoltar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text("Área do paciente", style = MaterialTheme.typography.headlineSmall)

        Button(onClick = aoEntrar, modifier = Modifier.fillMaxWidth()) {
            Text("Entrar com e-mail e senha")
        }

        OutlinedButton(onClick = aoCadastrar, modifier = Modifier.fillMaxWidth()) {
            Text("Criar cadastro")
        }

        TextButton(onClick = aoVoltar) {
            Text("Voltar")
        }
    }
}