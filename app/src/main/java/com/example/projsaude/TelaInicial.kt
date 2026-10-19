
package com.example.projsaude

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TelaInicial(aoAcessar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Projeto Saúde",
            color = Color(0xFF085DD3),
            fontSize = 30.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = aoAcessar,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF085DD3)
            )
        ) {
            Text("Acessar")
        }
    }
}
