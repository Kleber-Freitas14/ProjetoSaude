
package com.example.projsaude

import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.github.jan.supabase.auth.auth

suspend fun autenticarUsuario(
    email: String,
    senha: String
): Boolean {

    val cliente = SupabaseConfig.client

    // Autentica o usuário no Supabase
    cliente.auth.signInWith(Email) {
        this.email = email.trim()
        this.password = senha
    }

    // Obtém o ID do usuário autenticado
    val usuarioId = cliente.auth.currentUserOrNull()?.id
        ?: throw Exception("Não foi possível identificar o usuário.")

    // Consulta o perfil cadastrado
    val perfil = cliente
        .from("perfis_usuarios")
        .select {
            filter {
                eq("id", usuarioId)
            }
        }
        .decodeSingle<JsonObject>()

    val tipoUsuario = perfil["tipo_usuario"]
        ?.jsonPrimitive?.content

    val ativo = perfil["ativo"]
        ?.jsonPrimitive?.content?.toBoolean()

    if (ativo != true) {
        cliente.auth.signOut()
        throw Exception("Usuário inativo. Procure o administrador.")
    }

    if (tipoUsuario.isNullOrBlank()) {
        cliente.auth.signOut()
        throw Exception("Perfil de usuário não identificado.")
    }

    return tipoUsuario == "administrador"
}


