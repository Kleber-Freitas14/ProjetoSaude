package com.example.projsaude


import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseConfig {

    val client = createSupabaseClient(
        supabaseUrl = "https://trxdswjajfhrubnedevl.supabase.co",
        supabaseKey = "sb_publishable_PkmXQJrq4PTPuEQR1H0l2A_lJhRGoXG"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}