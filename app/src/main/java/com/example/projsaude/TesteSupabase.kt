package com.example.projsaude

import android.util.Log
import io.github.jan.supabase.postgrest.from

suspend fun testarConexaoSupabase() {
    Log.d("SUPABASE_TESTE", "Entrou na função de teste")

    try {
        val resultado = SupabaseConfig.client
            .from("valores_hospitalares")
            .select()

        Log.d(
            "SUPABASE_TESTE",
            "Consulta realizada com sucesso: $resultado"
        )

    } catch (e: Exception) {
        Log.e(
            "SUPABASE_TESTE",
            "ERRO: ${e.message}",
            e
        )
    }
}