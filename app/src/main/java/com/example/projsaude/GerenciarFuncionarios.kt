package com.example.projsaude

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Button(
        onClick = { aberto = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(55.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF085DD3)
        )
    ) {
        Text("Gerenciar funcionários", fontSize = 18.sp)
    }

    if (aberto) {
        TelaGerenciarFuncionarios(aoFechar = { aberto = false })
    }
}

@Composable
private fun TelaGerenciarFuncionarios(aoFechar: () -> Unit) {
    val scope = rememberCoroutineScope()
    val postgrest = SupabaseConfig.client.postgrest

    var funcionarios by remember {
        mutableStateOf<List<FuncionarioListado>>(emptyList())
    }
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

    Dialog(
        onDismissRequest = {
            if (!desativando) aoFechar()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = !desativando
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Gerenciar funcionários",
                    style = MaterialTheme.typography.headlineSmall
                )

                aviso?.let { Text(it) }
                erroAcao?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        carregando -> CircularProgressIndicator()

                        erroLista -> Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Não foi possível carregar os funcionários.")
                            OutlinedButton(onClick = { tentativa++ }) {
                                Text("Tentar novamente")
                            }
                        }

                        funcionarios.isEmpty() ->
                            Text("Nenhum funcionário encontrado.")

                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(funcionarios, key = { it.id }) { funcionario ->
                                val perfil = if (
                                    funcionario.tipo_usuario == "administrador"
                                ) {
                                    "Administrador"
                                } else {
                                    "Colaborador"
                                }

                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            funcionario.nome ?: "Funcionário",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        funcionario.email?.let { Text(it) }
                                        Text(
                                            "$perfil · ${
                                                if (funcionario.ativo == true) "Ativo"
                                                else "Inativo"
                                            }"
                                        )

                                        if (funcionario.propria_conta) {
                                            Text("Sua conta")
                                        } else if (funcionario.ativo == true) {
                                            TextButton(
                                                enabled = !desativando,
                                                onClick = {
                                                    aviso = null
                                                    erroAcao = null
                                                    selecionado = funcionario
                                                }
                                            ) {
                                                Text("Desativar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = aoFechar,
                    enabled = !desativando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Voltar")
                }
            }
        }
    }

    selecionado?.let { funcionario ->
        val identificacao = funcionario.nome ?: funcionario.email ?: "este funcionário"

        AlertDialog(
            onDismissRequest = {
                if (!desativando) selecionado = null
            },
            title = { Text("Desativar funcionário?") },
            text = {
                Text(
                    "O acesso de $identificacao será bloqueado. " +
                            "O cadastro e o histórico serão mantidos."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !desativando,
                    onClick = {
                        desativando = true
                        scope.launch {
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
                                erroAcao =
                                    "Não foi possível confirmar a desativação. " +
                                            "Confira o status da conta na lista."
                                tentativa++
                            } finally {
                                desativando = false
                            }
                        }
                    }
                ) {
                    Text(if (desativando) "Desativando…" else "Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !desativando,
                    onClick = { selecionado = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}