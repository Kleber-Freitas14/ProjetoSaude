
package com.example.projsaude

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.projsaude.ui.theme.ProjSaudeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ProjSaudeTheme {

                var telaAtual by remember {
                    mutableStateOf("inicio")
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
                            aoEntrar = {
                                telaAtual = "principal"
                            }
                        )

                        "principal" -> TelaPrincipal(
                            aoSair = {
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
                            }
                        )

                        "cadastroPaciente" -> TelaCadastroPaciente(
                            aoVoltar = {
                                telaAtual = "inicio"
                            },
                            aoEnviar = {
                                telaAtual = "acompanhamentoPaciente"
                            }
                        )

                        "acompanhamentoPaciente" -> {
                            TelaCadastroPaciente(
                                aoVoltar = {
                                    telaAtual = "inicio"
                                },
                                aoEnviar = {
                                    telaAtual = "inicio"
                                }
                            )
                        }

                        "acompanhemntoPaciente" -> TelaAcompanhamentoPaciente (
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
