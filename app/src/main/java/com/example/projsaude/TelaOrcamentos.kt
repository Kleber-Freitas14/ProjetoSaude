package com.example.projsaude

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import java.text.Normalizer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private enum class PaginaOrcamentos(val titulo: String) {
    MENU("Orçamentos"),
    PEDIDOS("Pedidos recebidos"),
    NOVO("Novo orçamento"),
    RASCUNHOS("Retomar composição"),
    ANALISE("Orçamentos em análise"),
    APROVADOS("Orçamentos aprovados")
}

@Serializable
private data class PacienteDoOrcamento(
    val id: String,
    val nome_completo: String,
    val cpf: String? = null
) {
    fun rotulo(): String {
        val finalCpf = cpf.orEmpty().filter { it in '0'..'9' }.takeLast(4)
        return if (finalCpf.isEmpty()) nome_completo
        else "$nome_completo · CPF final $finalCpf"
    }
}

@Serializable
private data class MedicoDoOrcamento(
    val id: String,
    val nome: String,
    val crm: String? = null,
    val uf_crm: String? = null,
    val atuacao: String? = null
) {
    fun rotulo(): String {
        val registro = listOfNotNull(crm, uf_crm)
            .filter { it.isNotBlank() }
            .joinToString("/")
        return if (registro.isEmpty()) nome else "$nome · CRM $registro"
    }

    fun atuacaoNormalizada(): String =
        Normalizer.normalize(atuacao.orEmpty().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
}

@Serializable
private data class ProcedimentoDoOrcamento(val id: String, val nome: String)

@Serializable
private data class PedidoRecebido(
    val id: String,
    val paciente_id: String,
    val nome_paciente: String,
    val cpf: String? = null,
    val foto_path: String,
    val criado_em: String
) {
    fun paciente() = PacienteDoOrcamento(paciente_id, nome_paciente, cpf)
}

@Serializable
private data class OrcamentoDaEquipe(
    val id: String,
    val status: String? = null,
    val data_prevista: String? = null,
    val paciente: PacienteDoOrcamento? = null,
    val procedimento: ProcedimentoDoOrcamento? = null,
    val composicao: JsonElement? = null
) {
    fun composicaoFoiSalva(): Boolean {
        val registro = when (val conteudo = composicao) {
            is JsonObject -> conteudo
            is JsonArray -> conteudo.singleOrNull() as? JsonObject
            else -> null
        }
        val data = registro?.get("salvo_em") as? JsonPrimitive
        return data?.isString == true && data.content.isNotBlank()
    }
}

@Serializable
private data class NovoOrcamentoEquipe(
    val paciente_id: String,
    val medico_cirurgiao_id: String,
    val medico_anestesista_id: String,
    val procedimento_id: String,
    val data_prevista: String,
    val descricao: String,
    val hospedagem: String,
    val necessita_uti: Boolean,
    val criado_por: String,
    val status: String,
    val pedido_medico_id: String? = null
)

@Serializable
private data class IdOrcamentoCriado(val id: String)

private data class OpcaoOrcamentoEquipe(
    val id: String,
    val nome: String,
    val rotulo: String = nome
)

private data class OrcamentoParaAbrir(
    val id: String,
    val paciente: String,
    val procedimento: String
)

private val dataBrOrcamentoEquipe = DateTimeFormatter.ofPattern("dd/MM/uuuu")
    .withResolverStyle(ResolverStyle.STRICT)

private val dataHoraPedido = DateTimeFormatter.ofPattern("dd/MM/uuuu 'às' HH:mm")

private fun exibirDataOrcamentoEquipe(data: String): String =
    runCatching { LocalDate.parse(data).format(dataBrOrcamentoEquipe) }
        .getOrDefault(data)

private fun exibirDataPedido(data: String): String =
    runCatching {
        OffsetDateTime.parse(data)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(dataHoraPedido)
    }.getOrDefault(data)

private fun formatarCpfOrcamento(cpf: String): String =
    "${cpf.substring(0, 3)}.${cpf.substring(3, 6)}.${cpf.substring(6, 9)}-${cpf.substring(9)}"

@Composable
fun TelaOrcamentos(aoVoltar: () -> Unit) {
    var pagina by remember { mutableStateOf(PaginaOrcamentos.MENU) }
    var aberto by remember { mutableStateOf<OrcamentoParaAbrir?>(null) }
    var pedidoSelecionado by remember { mutableStateOf<PedidoRecebido?>(null) }

    val orcamento = aberto
    if (orcamento != null) {
        TelaComposicaoOrcamento(
            paciente = orcamento.paciente,
            procedimento = orcamento.procedimento,
            orcamentoId = orcamento.id,
            aoMenuPrincipal = aoVoltar,
            aoVoltar = {
                aberto = null
                if (pagina == PaginaOrcamentos.NOVO) {
                    pedidoSelecionado = null
                    pagina = PaginaOrcamentos.ANALISE
                }
            }
        )
        return
    }

    when (pagina) {
        PaginaOrcamentos.MENU -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CabecalhoOrcamentosEquipe("Orçamentos") }

            val destinos = listOf(
                PaginaOrcamentos.PEDIDOS to "Abra o pedido médico e inicie um orçamento.",
                PaginaOrcamentos.NOVO to "Crie um orçamento ou retome uma composição.",
                PaginaOrcamentos.ANALISE to "Consulte os orçamentos que aguardam aceite.",
                PaginaOrcamentos.APROVADOS to "Consulte os orçamentos com aceite registrado."
            )

            items(destinos, key = { it.first.name }) { (destino, descricao) ->
                OutlinedCard(
                    onClick = {
                        pedidoSelecionado = null
                        pagina = destino
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = destino.titulo,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = descricao,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                OutlinedButton(onClick = aoVoltar, modifier = Modifier.fillMaxWidth()) {
                    Text("Voltar")
                }
            }
        }

        PaginaOrcamentos.PEDIDOS -> TelaPedidosRecebidos(
            aoVoltar = { pagina = PaginaOrcamentos.MENU },
            aoIniciar = {
                pedidoSelecionado = it
                pagina = PaginaOrcamentos.NOVO
            }
        )

        PaginaOrcamentos.NOVO -> FormularioNovoOrcamentoEquipe(
            pedido = pedidoSelecionado,
            aoVoltar = {
                pagina = if (pedidoSelecionado == null) {
                    PaginaOrcamentos.MENU
                } else {
                    PaginaOrcamentos.PEDIDOS
                }
                pedidoSelecionado = null
            },
            aoRetomar = { pagina = PaginaOrcamentos.RASCUNHOS },
            aoAbrir = { aberto = it }
        )

        else -> ListaOrcamentosEquipe(
            pagina = pagina,
            aoVoltar = {
                pagina = if (pagina == PaginaOrcamentos.RASCUNHOS) {
                    PaginaOrcamentos.NOVO
                } else {
                    PaginaOrcamentos.MENU
                }
            },
            aoAbrir = { aberto = it }
        )
    }
}

@Composable
private fun TelaPedidosRecebidos(
    aoVoltar: () -> Unit,
    aoIniciar: (PedidoRecebido) -> Unit
) {
    val cliente = SupabaseConfig.client
    var pagina by remember { mutableIntStateOf(0) }
    var recarga by remember { mutableIntStateOf(0) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf("") }
    var pedidos by remember { mutableStateOf(emptyList<PedidoRecebido>()) }
    var temProximaPagina by remember { mutableStateOf(false) }
    var pedidoAberto by remember { mutableStateOf<PedidoRecebido?>(null) }

    LaunchedEffect(pagina, recarga) {
        carregando = true
        erro = ""
        pedidos = emptyList()
        temProximaPagina = false

        try {
            val resultado = cliente.postgrest.rpc(
                "listar_pedidos_recebidos",
                buildJsonObject { put("p_pagina", pagina) }
            ).decodeList<PedidoRecebido>()

            pedidos = resultado.take(25)
            temProximaPagina = resultado.size > 25
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val detalhe = e.message.orEmpty()
            erro = when {
                "PGRST202" in detalhe ->
                    "A função de pedidos recebidos ainda não está disponível no Supabase."
                "42501" in detalhe ->
                    "Sua conta não tem permissão para consultar os pedidos recebidos."
                else -> "Não foi possível carregar os pedidos recebidos."
            }
        } finally {
            carregando = false
        }
    }

    val pedido = pedidoAberto
    val voltar: () -> Unit = {
        if (pedidoAberto != null) pedidoAberto = null else aoVoltar()
    }
    BackHandler { voltar() }

    if (pedido != null) {
        FotoPedidoRecebido(
            pedido = pedido,
            aoVoltar = voltar,
            aoIniciar = { aoIniciar(pedido) }
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CabecalhoOrcamentosEquipe("Pedidos recebidos")

            TextButton(
                onClick = {
                    pagina = 0
                    recarga++
                },
                enabled = !carregando
            ) { Text("Atualizar") }

            when {
                carregando -> CircularProgressIndicator()
                erro.isNotBlank() -> AvisoOrcamentosEquipe(erro, erro = true)
                pedidos.isEmpty() && pagina == 0 ->
                    AvisoOrcamentosEquipe("Nenhum pedido aguardando orçamento.")
                pedidos.isEmpty() -> AvisoOrcamentosEquipe("Nenhum pedido nesta página.")
            }
        }

        items(pedidos, key = { it.id }) { registro ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = registro.paciente().rotulo(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Recebido em ${exibirDataPedido(registro.criado_em)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { pedidoAberto = registro }) {
                        Text("Abrir pedido")
                    }
                }
            }
        }

        item {
            if (pagina > 0 || temProximaPagina) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { pagina-- },
                        enabled = pagina > 0 && !carregando
                    ) { Text("Anterior") }

                    TextButton(
                        onClick = { pagina++ },
                        enabled = temProximaPagina && !carregando
                    ) { Text("Próxima") }
                }
            }

            OutlinedButton(onClick = voltar, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar")
            }
        }
    }
}

@Composable
private fun FotoPedidoRecebido(
    pedido: PedidoRecebido,
    aoVoltar: () -> Unit,
    aoIniciar: () -> Unit
) {
    val cliente = SupabaseConfig.client
    var imagem by remember(pedido.id) { mutableStateOf<ImageBitmap?>(null) }
    var recarga by remember(pedido.id) { mutableIntStateOf(0) }
    var carregando by remember(pedido.id) { mutableStateOf(true) }
    var erro by remember(pedido.id) { mutableStateOf("") }

    LaunchedEffect(pedido.id, recarga) {
        carregando = true
        erro = ""
        imagem = null

        try {
            val caminho = pedido.foto_path.removePrefix("pedidos-medicos/")
            val bytes = cliente.storage.from("pedidos-medicos")
                .downloadAuthenticated(caminho)

            imagem = withContext(Dispatchers.Default) {
                val opcoes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opcoes)

                require(opcoes.outWidth > 0 && opcoes.outHeight > 0)
                var escala = 1
                while (maxOf(opcoes.outWidth, opcoes.outHeight) / escala > 2400) {
                    escala *= 2
                }

                opcoes.inJustDecodeBounds = false
                opcoes.inSampleSize = escala
                checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opcoes))
                    .asImageBitmap()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            erro = "Não foi possível abrir a foto do pedido médico."
        } finally {
            carregando = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { CabecalhoOrcamentosEquipe("Pedido médico") }

        item {
            Text(pedido.paciente().rotulo(), style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Recebido em ${exibirDataPedido(pedido.criado_em)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            val foto = imagem
            when {
                carregando -> CircularProgressIndicator()
                foto != null -> OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Image(
                        bitmap = foto,
                        contentDescription = "Foto do pedido médico",
                        modifier = Modifier.fillMaxWidth()
                            .aspectRatio(foto.width.toFloat() / foto.height)
                    )
                }
                erro.isNotBlank() -> {
                    AvisoOrcamentosEquipe(erro, erro = true)
                    TextButton(onClick = { recarga++ }) { Text("Tentar novamente") }
                }
            }
        }

        item {
            Button(
                onClick = aoIniciar,
                enabled = imagem != null && !carregando,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) { Text("Iniciar orçamento") }

            OutlinedButton(onClick = aoVoltar, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar aos pedidos")
            }
        }
    }
}

@Composable
private fun FormularioNovoOrcamentoEquipe(
    pedido: PedidoRecebido?,
    aoVoltar: () -> Unit,
    aoRetomar: () -> Unit,
    aoAbrir: (OrcamentoParaAbrir) -> Unit
) {
    val cliente = SupabaseConfig.client
    val escopo = rememberCoroutineScope()

    var paciente by remember(pedido?.id) {
        mutableStateOf(
            pedido?.paciente()?.let { OpcaoOrcamentoEquipe(it.id, it.nome_completo, it.rotulo()) }
        )
    }
    var cirurgiao by remember { mutableStateOf<OpcaoOrcamentoEquipe?>(null) }
    var anestesista by remember { mutableStateOf<OpcaoOrcamentoEquipe?>(null) }
    var procedimento by remember { mutableStateOf<OpcaoOrcamentoEquipe?>(null) }
    var dataDigitada by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var hospedagem by remember { mutableStateOf("Enfermaria") }
    var necessitaUti by remember { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("") }

    BackHandler { if (!salvando) aoVoltar() }

    suspend fun buscarMedico(termo: String, atuacao: String): List<OpcaoOrcamentoEquipe> =
        cliente.from("medicos")
            .select(Columns.raw("id,nome,crm,uf_crm,atuacao")) {
                filter {
                    eq("ativo", true)
                    ilike("nome", "%$termo%")
                }
            }
            .decodeList<MedicoDoOrcamento>()
            .filter { it.atuacaoNormalizada() == atuacao }
            .sortedBy { it.nome.lowercase() }
            .take(8)
            .map { OpcaoOrcamentoEquipe(it.id, it.nome, it.rotulo()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CabecalhoOrcamentosEquipe("Novo orçamento")

            if (pedido == null) {
                TextButton(onClick = aoRetomar, enabled = !salvando) {
                    Text("Retomar composição pelo CPF")
                }
            } else {
                Spacer(Modifier.height(12.dp))
                AvisoOrcamentosEquipe("Paciente do pedido: ${pedido.paciente().rotulo()}")
            }
        }

        if (pedido == null) {
            item {
                CampoBuscaEquipe(
                    "Nome ou CPF do paciente",
                    paciente,
                    { paciente = it },
                    !salvando
                ) { termo ->
                    val digitos = termo.filter { it in '0'..'9' }
                    val buscaPorCpf = termo.all { it in '0'..'9' || it in ".- " }

                    if (buscaPorCpf && digitos.length != 11) {
                        throw IllegalArgumentException("Informe os 11 dígitos do CPF.")
                    }

                    cliente.from("pacientes")
                        .select(Columns.raw("id,nome_completo,cpf")) {
                            filter {
                                eq("ativo", true)
                                if (buscaPorCpf) {
                                    or {
                                        eq("cpf", digitos)
                                        eq("cpf", formatarCpfOrcamento(digitos))
                                    }
                                } else {
                                    ilike("nome_completo", "%$termo%")
                                }
                            }
                            limit(8)
                        }
                        .decodeList<PacienteDoOrcamento>()
                        .map { OpcaoOrcamentoEquipe(it.id, it.nome_completo, it.rotulo()) }
                }
            }
        }

        item {
            CampoBuscaEquipe("Cirurgião", cirurgiao, { cirurgiao = it }, !salvando) { termo ->
                buscarMedico(termo, "cirurgiao")
            }
        }

        item {
            CampoBuscaEquipe("Anestesista", anestesista, { anestesista = it }, !salvando) { termo ->
                buscarMedico(termo, "anestesista")
            }
        }

        item {
            CampoBuscaEquipe("Procedimento", procedimento, { procedimento = it }, !salvando) { termo ->
                cliente.from("procedimentos")
                    .select(Columns.raw("id,nome")) {
                        filter {
                            eq("ativo", true)
                            ilike("nome", "%$termo%")
                        }
                        limit(8)
                    }
                    .decodeList<ProcedimentoDoOrcamento>()
                    .map { OpcaoOrcamentoEquipe(it.id, it.nome) }
            }
        }

        item {
            OutlinedTextField(
                value = dataDigitada,
                onValueChange = { texto ->
                    val digitos = texto.filter { it in '0'..'9' }.take(8)
                    dataDigitada = listOf(
                        digitos.take(2),
                        digitos.drop(2).take(2),
                        digitos.drop(4)
                    ).filter { it.isNotEmpty() }.joinToString("/")
                },
                label = { Text("Data prevista") },
                placeholder = { Text("DD/MM/AAAA") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !salvando,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it },
                label = { Text("Descrição / observações") },
                enabled = !salvando,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Tipo de hospedagem",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    listOf("Enfermaria", "Apartamento Individual", "Apartamento Duplo").forEach { opcao ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = hospedagem == opcao,
                                onClick = { hospedagem = opcao },
                                enabled = !salvando
                            )
                            Text(opcao)
                        }
                    }

                    HorizontalDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = necessitaUti,
                            onCheckedChange = { necessitaUti = it },
                            enabled = !salvando
                        )
                        Text("Necessita de UTI")
                    }
                }
            }
        }

        item {
            if (mensagem.isNotBlank()) {
                AvisoOrcamentosEquipe(mensagem, erro = true)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                enabled = !salvando,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                onClick = {
                    val pessoa = paciente
                    val cirurgiaoId = cirurgiao?.id
                    val anestesistaId = anestesista?.id
                    val cirurgia = procedimento
                    val usuarioId = cliente.auth.currentUserOrNull()?.id

                    if (pessoa == null || cirurgiaoId == null || anestesistaId == null ||
                        cirurgia == null
                    ) {
                        mensagem = "Selecione o paciente, os médicos e o procedimento."
                        return@Button
                    }

                    val dataPrevista = try {
                        LocalDate.parse(dataDigitada, dataBrOrcamentoEquipe)
                    } catch (_: DateTimeParseException) {
                        mensagem = "Digite uma data prevista válida no formato DD/MM/AAAA."
                        return@Button
                    }

                    if (usuarioId == null) {
                        mensagem = "Entre novamente para cadastrar o orçamento."
                        return@Button
                    }

                    val novo = NovoOrcamentoEquipe(
                        paciente_id = pessoa.id,
                        medico_cirurgiao_id = cirurgiaoId,
                        medico_anestesista_id = anestesistaId,
                        procedimento_id = cirurgia.id,
                        data_prevista = dataPrevista.toString(),
                        descricao = descricao.trim(),
                        hospedagem = hospedagem,
                        necessita_uti = necessitaUti,
                        criado_por = usuarioId,
                        status = "Pendente",
                        pedido_medico_id = pedido?.id
                    )

                    salvando = true
                    mensagem = ""

                    escopo.launch {
                        try {
                            val criado = cliente.from("orcamentos")
                                .insert(novo) { select(Columns.raw("id")) }
                                .decodeSingle<IdOrcamentoCriado>()

                            aoAbrir(OrcamentoParaAbrir(criado.id, pessoa.nome, cirurgia.nome))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            val detalhe = e.message.orEmpty()
                            mensagem = if (
                                pedido != null && "orcamentos_pedido_medico_ux" in detalhe
                            ) {
                                "Este pedido já está vinculado a um orçamento. Consulte Retomar composição ou Orçamentos em análise."
                            } else {
                                "A gravação não foi confirmada. Confira em Retomar composição antes de tentar novamente."
                            }
                        } finally {
                            salvando = false
                        }
                    }
                }
            ) {
                Text(if (salvando) "Criando orçamento…" else "Criar e abrir composição")
            }

            OutlinedButton(
                onClick = aoVoltar,
                enabled = !salvando,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Voltar") }
        }
    }
}

@Composable
private fun CampoBuscaEquipe(
    titulo: String,
    selecionado: OpcaoOrcamentoEquipe?,
    aoSelecionar: (OpcaoOrcamentoEquipe?) -> Unit,
    habilitado: Boolean,
    buscar: suspend (String) -> List<OpcaoOrcamentoEquipe>
) {
    var texto by remember { mutableStateOf("") }
    var sugestoes by remember { mutableStateOf(emptyList<OpcaoOrcamentoEquipe>()) }
    var mensagem by remember { mutableStateOf("") }

    LaunchedEffect(texto, selecionado) {
        sugestoes = emptyList()
        mensagem = ""
        val termo = texto.trim().replace(Regex("[%*_\\\\]"), "")
        if (selecionado != null || termo.length < 2) return@LaunchedEffect

        delay(300)

        try {
            sugestoes = buscar(termo)
            if (sugestoes.isEmpty()) mensagem = "Nenhum registro ativo encontrado."
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            mensagem = e.message ?: "Confira o valor digitado."
        } catch (_: Exception) {
            mensagem = "Não foi possível consultar o banco."
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = selecionado?.rotulo ?: texto,
            onValueChange = {
                texto = it
                sugestoes = emptyList()
                aoSelecionar(null)
            },
            label = { Text(titulo) },
            modifier = Modifier.fillMaxWidth(),
            enabled = habilitado,
            singleLine = true
        )

        if (mensagem.isNotBlank()) {
            Text(
                text = mensagem,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        sugestoes.forEach { opcao ->
            OutlinedButton(
                enabled = habilitado,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    texto = opcao.nome
                    sugestoes = emptyList()
                    aoSelecionar(opcao)
                }
            ) { Text(opcao.rotulo) }
        }
    }
}

@Composable
private fun ListaOrcamentosEquipe(
    pagina: PaginaOrcamentos,
    aoVoltar: () -> Unit,
    aoAbrir: (OrcamentoParaAbrir) -> Unit
) {
    val cliente = SupabaseConfig.client
    val escopo = rememberCoroutineScope()
    var cpf by remember { mutableStateOf("") }
    var cpfConsultado by remember { mutableStateOf<String?>(null) }
    var busca by remember { mutableIntStateOf(0) }
    var carregando by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("") }
    var registros by remember { mutableStateOf(emptyList<OrcamentoDaEquipe>()) }
    var registroParaExcluir by remember { mutableStateOf<OrcamentoDaEquipe?>(null) }
    var excluindo by remember { mutableStateOf(false) }
    var mensagemExclusao by remember { mutableStateOf("") }

    val voltar: () -> Unit = {
        when {
            excluindo -> Unit
            registroParaExcluir != null -> registroParaExcluir = null
            else -> aoVoltar()
        }
    }
    BackHandler { voltar() }

    fun excluirOrcamento(registro: OrcamentoDaEquipe) {
        if (excluindo || carregando) return
        excluindo = true
        mensagemExclusao = ""

        escopo.launch {
            try {
                cliente.postgrest.rpc(
                    "gerenciar_orcamento",
                    buildJsonObject {
                        put("p_orcamento_id", registro.id)
                        put("p_acao", "excluir")
                    }
                )

                val aindaExiste = cliente.from("orcamentos")
                    .select(Columns.raw("id")) {
                        filter { eq("id", registro.id) }
                    }
                    .decodeList<IdOrcamentoCriado>()
                    .isNotEmpty()

                if (aindaExiste) {
                    mensagemExclusao = "O Supabase ainda retorna este orçamento. Confira o status."
                    return@launch
                }

                registros = registros.filterNot { it.id == registro.id }
                if (registros.isEmpty()) {
                    mensagem = "Nenhum orçamento nesta situação para esse CPF."
                }
                mensagemExclusao = "Orçamento excluído com sucesso."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val detalhe = e.message.orEmpty()
                mensagemExclusao = when {
                    "PGRST202" in detalhe ->
                        "A função de exclusão não foi encontrada no Supabase (PGRST202)."
                    "42501" in detalhe ->
                        "Sua conta não tem permissão para excluir este orçamento (42501)."
                    else ->
                        "Não foi possível confirmar a exclusão. Busque novamente para conferir o status."
                }
            } finally {
                registroParaExcluir = null
                excluindo = false
            }
        }
    }

    LaunchedEffect(pagina, busca) {
        val digitos = cpfConsultado ?: return@LaunchedEffect
        carregando = true
        registros = emptyList()
        mensagem = ""

        try {
            val pacientes = cliente.from("pacientes")
                .select(Columns.raw("id,nome_completo")) {
                    filter {
                        or {
                            eq("cpf", digitos)
                            eq("cpf", formatarCpfOrcamento(digitos))
                        }
                    }
                }
                .decodeList<PacienteDoOrcamento>()

            if (pacientes.isEmpty()) {
                mensagem = "Nenhum paciente encontrado com esse CPF."
                return@LaunchedEffect
            }
            if (pacientes.size > 1) {
                mensagem = "Há mais de um cadastro com esse CPF. Confira o cadastro do paciente."
                return@LaunchedEffect
            }

            val encontrados = cliente.from("orcamentos")
                .select(
                    Columns.raw(
                        "id,status,data_prevista," +
                                "paciente:pacientes(id,nome_completo)," +
                                "procedimento:procedimentos(id,nome)," +
                                "composicao:composicoes_orcamento(salvo_em)"
                    )
                ) {
                    filter {
                        eq("paciente_id", pacientes.single().id)
                        if (pagina == PaginaOrcamentos.APROVADOS) {
                            eq("status", "Aceito")
                        } else {
                            or {
                                eq("status", "Pendente")
                                eq("status", "Em espera")
                            }
                        }
                    }
                }
                .decodeList<OrcamentoDaEquipe>()

            registros = encontrados.filter { registro ->
                when (pagina) {
                    PaginaOrcamentos.APROVADOS -> true
                    PaginaOrcamentos.RASCUNHOS -> !registro.composicaoFoiSalva()
                    else -> registro.composicaoFoiSalva()
                }
            }.sortedByDescending { it.data_prevista }

            if (registros.isEmpty()) mensagem = "Nenhum orçamento nesta situação para esse CPF."
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            mensagem = "Não foi possível buscar os orçamentos no banco."
        } finally {
            carregando = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CabecalhoOrcamentosEquipe(pagina.titulo)
        }

        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = cpf,
                        enabled = !excluindo,
                        onValueChange = {
                            cpf = it
                            cpfConsultado = null
                            registroParaExcluir = null
                            busca++
                            registros = emptyList()
                            mensagem = ""
                            mensagemExclusao = ""
                        },
                        label = { Text("CPF do paciente") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        enabled = !carregando && !excluindo,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            registroParaExcluir = null
                            mensagemExclusao = ""
                            val digitos = cpf.filter { it in '0'..'9' }
                            if (digitos.length != 11) {
                                mensagem = "Informe os 11 dígitos do CPF."
                            } else {
                                cpfConsultado = digitos
                                busca++
                            }
                        }
                    ) { Text("Buscar orçamentos") }

                    if (carregando) CircularProgressIndicator()
                    if (mensagem.isNotBlank()) AvisoOrcamentosEquipe(mensagem)
                    if (mensagemExclusao.isNotBlank()) AvisoOrcamentosEquipe(mensagemExclusao)
                }
            }
        }

        items(registros, key = { it.id }) { registro ->
            val nomePaciente = registro.paciente?.nome_completo ?: "Paciente indisponível"
            val nomeProcedimento = registro.procedimento?.nome ?: "Procedimento indisponível"

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = nomePaciente,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(nomeProcedimento)
                    registro.status?.let { EtiquetaSituacaoOrcamento(it) }
                    registro.data_prevista?.let {
                        Text(
                            text = "Data prevista: ${exibirDataOrcamentoEquipe(it)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        enabled = !excluindo,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            aoAbrir(OrcamentoParaAbrir(registro.id, nomePaciente, nomeProcedimento))
                        }
                    ) {
                        Text(
                            if (pagina == PaginaOrcamentos.RASCUNHOS) "Continuar composição"
                            else "Ver orçamento"
                        )
                    }

                    if (pagina == PaginaOrcamentos.ANALISE &&
                        (registro.status == "Pendente" || registro.status == "Em espera")
                    ) {
                        if (registroParaExcluir?.id == registro.id) {
                            AvisoOrcamentosEquipe(
                                "Excluir definitivamente este orçamento e sua composição?",
                                erro = true
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { excluirOrcamento(registro) },
                                    enabled = !carregando && !excluindo,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(if (excluindo) "Excluindo…" else "Confirmar exclusão")
                                }
                                TextButton(
                                    onClick = { registroParaExcluir = null },
                                    enabled = !excluindo
                                ) { Text("Cancelar") }
                            }
                        } else {
                            TextButton(
                                onClick = { registroParaExcluir = registro },
                                enabled = !carregando && !excluindo,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Excluir orçamento")
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = voltar,
                enabled = !excluindo,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Voltar") }
        }
    }
}

@Composable
private fun CabecalhoOrcamentosEquipe(titulo: String) {
    Text(
        text = titulo,
        modifier = Modifier.fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF10345E), Color(0xFF064FB5))
                ),
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}

@Composable
private fun AvisoOrcamentosEquipe(texto: String, erro: Boolean = false) {
    val cores = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.small,
        color = if (erro) cores.errorContainer else cores.primaryContainer,
        contentColor = if (erro) cores.onErrorContainer else cores.onPrimaryContainer
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EtiquetaSituacaoOrcamento(situacao: String) {
    val cores = MaterialTheme.colorScheme
    val aceito = situacao == "Aceito"

    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (aceito) cores.tertiaryContainer else cores.secondaryContainer,
        contentColor = if (aceito) cores.onTertiaryContainer else cores.onSecondaryContainer
    ) {
        Text(
            text = situacao,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}