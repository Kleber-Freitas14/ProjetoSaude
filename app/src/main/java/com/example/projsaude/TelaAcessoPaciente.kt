package com.example.projsaude

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

@Composable
fun TelaAcessoPaciente(
    aoEntrar: () -> Unit,
    aoCadastrar: () -> Unit,
    aoVoltar: () -> Unit
) {
    val cores = MaterialTheme.colorScheme
    val formas = MaterialTheme.shapes

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(cores.primaryContainer, cores.background)
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
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF10345E), Color(0xFF064FB5))
                        ),
                        shape = formas.large
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(90.dp),
                    shape = formas.medium,
                    color = Color(0xFFF5F5F5)
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_projeto_saude),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(6.dp)
                    )
                }

                Text(
                    text = "Área do paciente",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = formas.large,
                color = cores.surface,
                contentColor = cores.onSurface,
                border = BorderStroke(1.dp, cores.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = aoEntrar,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = formas.small
                    ) {
                        Text(
                            text = "Entrar com e-mail e senha",
                            textAlign = TextAlign.Center
                        )
                    }

                    OutlinedButton(
                        onClick = aoCadastrar,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = formas.small,
                        border = BorderStroke(1.dp, cores.primary)
                    ) {
                        Text("Criar cadastro")
                    }
                }
            }

            TextButton(onClick = aoVoltar) {
                Text("Voltar")
            }
        }
    }
}