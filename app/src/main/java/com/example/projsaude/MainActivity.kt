
package com.example.projsaude

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.projsaude.ui.theme.ProjSaudeTheme
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firebaseAuth = FirebaseAuth.getInstance()
        enableEdgeToEdge()


        setContent {
            ProjSaudeTheme {

                var telaAtual by remember {
                    mutableStateOf("inicio")
                }

                var ehAdministrador by remember {
                    mutableStateOf(false)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    when (telaAtual) {

                        "inicio" -> TelaInicial(
                            aoAcessarColaborador = {
                                telaAtual = "login"
                            },
                            aoAcessarPaciente = {
                                telaAtual = "cadastroPaciente"
                            }
                        )

                        "login" -> TelaLogin(
                            aoVoltar = {
                                telaAtual = "inicio"
                            },
                            aoEntrar = { administrador ->
                                ehAdministrador = administrador
                                telaAtual = "principal"
                            }
                        )

                        "principal" -> TelaPrincipal(
                            aoSair = {
                                ehAdministrador = false
                                telaAtual = "inicio"
                            },
                            aoAbrirPacientes = {
                                telaAtual = "pacientes"
                            },
                            aoAbrirConsultas = {
                                telaAtual = "consultas"
                            },
                            aoAbrirProcedimentos = {
                                telaAtual = "procedimentos"
                            },
                            aoAbrirOrcamentos = {
                                telaAtual = "orcamentos"
                            },
                            aoAbrirMedicos = {
                                telaAtual = "medicos"
                            },
                            aoAbrirOpme = {
                                telaAtual = "opme"
                            },
                            aoAbrirCadastroValores = {
                                if (ehAdministrador) {
                                    telaAtual = "cadastroValores"
                                }
                            },
                            ehAdministrador = ehAdministrador
                        )

                        "cadastroValores" -> {
                            if (ehAdministrador) {
                                TelaCadastroValores(
                                    aoVoltar = {
                                        telaAtual = "principal"
                                    }
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("Acesso restrito ao administrador.")

                                    Button(
                                        onClick = {
                                            telaAtual = "principal"
                                        }
                                    ) {
                                        Text("Voltar")
                                    }
                                }
                            }
                        }

                        "cadastroPaciente" -> TelaCadastroPaciente(
                            aoVoltar = {
                                telaAtual = "inicio"
                            },
                            aoEnviar = {
                                telaAtual = "acompanhamentoPaciente"
                            }
                        )

                        "acompanhamentoPaciente" -> TelaCadastroPaciente(
                            aoVoltar = {
                                telaAtual = "inicio"
                            },
                            aoEnviar = {
                                telaAtual = "inicio"
                            }
                        )

                        "acompanhemntoPaciente" ->
                            TelaAcompanhamentoPaciente(
                                aoVoltar = {
                                    telaAtual = "inicio"
                                }
                            )

                        "consultas" -> TelaConsultas(
                            aoVoltar = {
                                telaAtual = "principal"
                            }
                        )

                        "pacientes" -> TelaPacientes(
                            aoVoltar = {
                                telaAtual = "principal"
                            }
                        )

                        "procedimentos" -> TelaProcedimentos(
                            aoVoltar = {
                                telaAtual = "principal"
                            }
                        )

                        "orcamentos" -> TelaOrcamentos(
                            aoVoltar = {
                                telaAtual = "principal"
                            }
                        )

                        "medicos" -> TelaMedicos(
                            aoVoltar = {
                                telaAtual = "principal"
                            }
                        )

                        "opme" -> TelaOpme(
                            aoVoltar = {
                                telaAtual = "principal"
                            }
                        )
                    }
                }
            }
        }
    }
}
