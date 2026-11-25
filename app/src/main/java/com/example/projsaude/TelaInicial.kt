package com.example.projsaude

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val AzulMarinhoInicial = Color(0xFF10345E)
private val AzulAcessoInicial = Color(0xFF064FB5)
private val AzulFundoSuperior = Color(0xFFB8D6F6)
private val AzulFundoCentral = Color(0xFFD7E8FC)
private val AzulFundoInferior = Color(0xFFF0F6FE)
private val AzulFundoPaciente = Color(0xFFE9F2FF)
private val TextoSecundarioInicial = Color(0xFF35516F)
private val BordaInicial = Color(0xFFA9C6E9)

@Composable
fun TelaInicial(
    aoAcessarColaborador: () -> Unit,
    aoAcessarPaciente: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to AzulFundoSuperior,
                    0.55f to AzulFundoCentral,
                    1f to AzulFundoInferior
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        val alturaDisponivel = maxHeight

        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = alturaDisponivel)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(144.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFFF5F5F5),
                border = BorderStroke(1.dp, BordaInicial)
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_projeto_saude),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Projeto Saúde",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = AzulMarinhoInicial
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Selecione como deseja acessar",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = TextoSecundarioInicial
            )

            Spacer(Modifier.height(28.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BordaInicial)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = aoAcessarColaborador,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AzulAcessoInicial,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Acesso do colaborador",
                            textAlign = TextAlign.Center
                        )
                    }

                    OutlinedButton(
                        onClick = aoAcessarPaciente,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AzulAcessoInicial),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = AzulFundoPaciente,
                            contentColor = AzulMarinhoInicial
                        )
                    ) {
                        Text(
                            text = "Acesso do paciente",
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}