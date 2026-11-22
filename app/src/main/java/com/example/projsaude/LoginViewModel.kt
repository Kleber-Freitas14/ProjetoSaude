package com.example.projsaude

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoLogin(
    val email: String = "",
    val senha: String = "",
    val mensagem: String = "",
    val carregando: Boolean = false,
    val administrador: Boolean? = null
)

class LoginViewModel : ViewModel() {
    private val estadoInterno = MutableStateFlow(EstadoLogin())
    val estado = estadoInterno.asStateFlow()

    fun alterarEmail(email: String) {
        estadoInterno.update { it.copy(email = email, mensagem = "") }
    }

    fun alterarSenha(senha: String) {
        estadoInterno.update { it.copy(senha = senha, mensagem = "") }
    }

    fun entrar(autenticar: suspend (email: String, senha: String) -> Boolean) {
        val atual = estadoInterno.value
        if (atual.carregando || atual.administrador != null) return

        val email = atual.email.trim()
        val mensagem = when {
            email.isBlank() -> "Informe o e-mail."
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Informe um e-mail válido."
            atual.senha.isBlank() -> "Informe a senha."
            else -> ""
        }
        if (mensagem.isNotEmpty()) {
            estadoInterno.update { it.copy(mensagem = mensagem) }
            return
        }

        estadoInterno.update { it.copy(carregando = true, mensagem = "") }
        viewModelScope.launch {
            try {
                val administrador = autenticar(email, atual.senha)
                estadoInterno.update { it.copy(senha = "", administrador = administrador) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val erro = if (
                    e.message?.contains("Invalid login credentials", ignoreCase = true) == true
                ) {
                    "E-mail ou senha incorretos."
                } else {
                    "Não foi possível entrar nesta área. Confira seus dados e tente novamente."
                }
                estadoInterno.update { it.copy(mensagem = erro) }
            } finally {
                estadoInterno.update { it.copy(carregando = false) }
            }
        }
    }
}
