
package com.example.projsaude

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

private val AzulPrincipal = Color(0xFF085DD3)


@Composable
fun TelaPrincipal(
    aoSair: () -> Unit,
    aoAbrirPacientes: () -> Unit,
    aoAbrirConsultas: () -> Unit,
    aoAbrirProcedimentos: () -> Unit,
    aoAbrirOrcamentos: () -> Unit,
    aoAbrirMedicos: () -> Unit,
    aoAbrirOpme: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8FC))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Projeto Saúde",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AzulPrincipal
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Menu Principal",
            fontSize = 20.sp,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(32.dp))

        BotaoMenu("Pacientes", aoAbrirPacientes)
        BotaoMenu("Consultas", aoAbrirConsultas)
        BotaoMenu("Procedimentos", aoAbrirProcedimentos)
        BotaoMenu("Orçamentos",  aoAbrirOrcamentos)
        BotaoMenu( "Médicos", aoAbrirMedicos)
        BotaoMenu("OPME", aoAbrirOpme)

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = aoSair,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sair")
        }
    }
}

@Composable
private fun BotaoMenu(
    titulo: String,
    aoClicar: () -> Unit
) {
    Button(
        onClick = aoClicar,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(55.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AzulPrincipal
        )
    ) {
        Text(text = titulo, fontSize = 18.sp)
    }
}
