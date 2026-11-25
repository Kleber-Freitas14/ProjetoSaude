package com.example.projsaude

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class FuncionarioListado(
    val id: String,
    val nome: String?,
    val email: String?,
    val tipo_usuario: String,
    val ativo: Boolean?,
    val propria_conta: Boolean
)

@Composable
fun BotaoGerenciarFuncionarios() {
    var aberto by remember { mutableStateOf(false) }

    OutlinedCard(
        onClick = { aberto = true },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = "Gerenciar funcionários",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Consulte perfis e desative acessos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (aberto) {
        TelaGerenciarFuncionarios(aoFechar = { aberto = false })
    }
}

@Composable
private fun TelaGerenciarFuncionarios(aoFechar: () -> Unit) {
    val escopo = rememberCoroutineScope()
    val postgrest = SupabaseConfig.client.postgrest
    val cores = MaterialTheme.colorScheme
    val formas = MaterialTheme.shapes

    var funcionarios by remember { mutableStateOf(emptyList<FuncionarioListado>()) }
    var selecionado by remember { mutableStateOf<FuncionarioListado?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var desativando by remember { mutableStateOf(false) }
    var erroLista by remember { mutableStateOf(false) }
    var erroAcao by remember { mutableStateOf<String?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var tentativa by remember { mutableIntStateOf(0) }

    LaunchedEffect(tentativa) {
        carregando = true
        erroLista = false

        try {
            funcionarios = postgrest
                .rpc("listar_funcionarios")
                .decodeList<FuncionarioListado>()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            erroLista = true
        } finally {
            carregando = false
        }
    }

    fun voltar() {
        if (desativando) return
        if (selecionado != null) {
            selecionado = null
        } else {
            aoFechar()
        }
    }

    fun desativarFuncionario(funcionario: FuncionarioListado) {
        if (desativando || funcionario.propria_conta || funcionario.ativo != true) return
        desativando = true

        escopo.launch {
            try {
                postgrest.rpc(
                    "desativar_funcionario",
                    buildJsonObject {
                        put("p_funcionario_id", funcionario.id)
                    }
                )

                selecionado = null
                aviso = "Funcionário desativado."
                tentativa++
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                selecionado = null
                erroAcao = "Não foi possível confirmar a desativação. Confira a situação da conta na lista."
                tentativa++
            } finally {
                desativando = false
            }
        }
    }

    Dialog(
        onDismissRequest = ::voltar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = !desativando
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = cores.background,
            contentColor = cores.onBackground
        ) {
            Box(
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Gerenciar funcionários",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFF10345E), Color(0xFF064FB5))
                                ),
                                shape = formas.large
                            )
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                            .semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    aviso?.let { AvisoGerenciarFuncionarios(it) }
                    erroAcao?.let { AvisoGerenciarFuncionarios(it, erro = true) }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            carregando -> Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                                Text("Carregando funcionários…")
                            }

                            erroLista -> Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AvisoGerenciarFuncionarios(
                                    "Não foi possível carregar os funcionários.",
                                    erro = true
                                )

                                OutlinedButton(onClick = { tentativa++ }) {
                                    Text("Tentar novamente")
                                }
                            }

                            funcionarios.isEmpty() ->
                                AvisoGerenciarFuncionarios("Nenhum funcionário encontrado.")

                            else -> LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(funcionarios, key = { it.id }) { funcionario ->
                                    CartaoGerenciarFuncionario(
                                        funcionario = funcionario,
                                        confirmando = selecionado?.id == funcionario.id,
                                        desativando = desativando,
                                        aoSolicitarDesativacao = {
                                            aviso = null
                                            erroAcao = null
                                            selecionado = funcionario
                                        },
                                        aoConfirmar = { desativarFuncionario(funcionario) },
                                        aoCancelar = { selecionado = null }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = ::voltar,
                        enabled = !desativando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selecionado != null) "Cancelar seleção" else "Voltar")
                    }
                }
            }
        }
    }
}

@Composable
private fun CartaoGerenciarFuncionario(
    funcionario: FuncionarioListado,
    confirmando: Boolean,
    desativando: Boolean,
    aoSolicitarDesativacao: () -> Unit,
    aoConfirmar: () -> Unit,
    aoCancelar: () -> Unit
) {
    val cores = MaterialTheme.colorScheme
    val formas = MaterialTheme.shapes
    val ativo = funcionario.ativo == true
    val perfil = if (funcionario.tipo_usuario == "administrador") {
        "Administrador"
    } else {
        "Colaborador"
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = formas.medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = cores.surface,
            contentColor = cores.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = funcionario.nome ?: "Funcionário",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            funcionario.email?.let {
                Text(it, color = cores.onSurfaceVariant)
            }

            Text(
                text = perfil,
                style = MaterialTheme.typography.bodyMedium,
                color = cores.onSurfaceVariant
            )

            Surface(
                shape = formas.small,
                color = if (ativo) cores.tertiaryContainer else cores.surfaceVariant,
                contentColor = if (ativo) cores.onTertiaryContainer else cores.onSurfaceVariant
            ) {
                Text(
                    text = if (ativo) "Ativo" else "Inativo",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            when {
                funcionario.propria_conta -> Text(
                    text = "Sua conta",
                    style = MaterialTheme.typography.labelLarge,
                    color = cores.onSurfaceVariant
                )

                confirmando && ativo -> {
                    val identificacao = funcionario.nome ?: funcionario.email ?: "este funcionário"

                    AvisoGerenciarFuncionarios(
                        "O acesso de $identificacao será bloqueado. O cadastro e o histórico serão mantidos.",
                        erro = true
                    )

                    Button(
                        onClick = aoConfirmar,
                        enabled = !desativando,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cores.error,
                            contentColor = cores.onError
                        )
                    ) {
                        Text(if (desativando) "Desativando…" else "Confirmar desativação")
                    }

                    TextButton(onClick = aoCancelar, enabled = !desativando) {
                        Text("Cancelar")
                    }
                }

                ativo -> TextButton(
                    onClick = aoSolicitarDesativacao,
                    enabled = !desativando,
                    colors = ButtonDefaults.textButtonColors(contentColor = cores.error)
                ) {
                    Text("Desativar")
                }
            }
        }
    }
}

@Composable
private fun AvisoGerenciarFuncionarios(texto: String, erro: Boolean = false) {
    val cores = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.small,
        color = if (erro) cores.errorContainer else cores.secondaryContainer,
        contentColor = if (erro) cores.onErrorContainer else cores.onSecondaryContainer
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}