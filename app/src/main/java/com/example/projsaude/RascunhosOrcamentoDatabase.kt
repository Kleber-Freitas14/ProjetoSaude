package com.example.projsaude

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Entity(
    tableName = "rascunhos_composicao",
    primaryKeys = ["usuarioId", "orcamentoId"]
)
internal data class RascunhoComposicao(
    val usuarioId: String,
    val orcamentoId: String,
    val etapa: Int,
    val porte: String?,
    val acomodacao: String,
    val entradaUti: String,
    val saidaUti: String,
    val entradaAcomodacao: String,
    val saidaAcomodacao: String,
    val extras: String,
    val hemoterapia: String,
    val fisioterapia: String
)

@Dao
internal interface RascunhosComposicaoDao {
    @Query("SELECT * FROM rascunhos_composicao WHERE usuarioId = :usuarioId AND orcamentoId = :orcamentoId")
    suspend fun buscar(usuarioId: String, orcamentoId: String): RascunhoComposicao?

    @Upsert
    suspend fun salvar(rascunho: RascunhoComposicao)

    @Query("DELETE FROM rascunhos_composicao WHERE usuarioId = :usuarioId AND orcamentoId = :orcamentoId")
    suspend fun remover(usuarioId: String, orcamentoId: String)
}

@Database(entities = [RascunhoComposicao::class], version = 1, exportSchema = false)
internal abstract class RascunhosOrcamentoDatabase : RoomDatabase() {
    abstract fun rascunhos(): RascunhosComposicaoDao

    companion object {
        @Volatile
        private var instancia: RascunhosOrcamentoDatabase? = null

        fun obter(contexto: Context): RascunhosOrcamentoDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    RascunhosOrcamentoDatabase::class.java,
                    "rascunhos_orcamento.db"
                ).build().also { instancia = it }
            }
    }
}
