package com.example.projsaude

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

suspend fun autenticarUsuario(
    email: String,
    senha: String,
    acessoPaciente: Boolean = false
): Boolean {
    val cliente = SupabaseConfig.client

    cliente.auth.signInWith(Email) {
        this.email = email.trim()
        this.password = senha
    }

    try {
        val usuarioId = cliente.auth.currentUserOrNull()?.id
            ?: error("Não foi possível identificar o usuário.")

        val perfil = cliente
            .from("perfis_usuarios")
            .select {
                filter {
                    eq("id", usuarioId)
                }
            }
            .decodeSingle<JsonObject>()

        val tipoUsuario = perfil["tipo_usuario"]?.jsonPrimitive?.contentOrNull
        val ativo = perfil["ativo"]?.jsonPrimitive?.booleanOrNull == true

        check(ativo) { "Usuário inativo. Procure o administrador." }
        check(!tipoUsuario.isNullOrBlank()) { "Perfil de usuário não identificado." }

        if (acessoPaciente) {
            check(tipoUsuario == "paciente") { "Esta área é exclusiva para pacientes." }
        } else {
            check(tipoUsuario != "paciente") { "Use a opção Acesso paciente." }
        }

        return tipoUsuario == "administrador"
    } catch (erro: Exception) {
        runCatching { cliente.auth.signOut() }
        throw erro
    }
}