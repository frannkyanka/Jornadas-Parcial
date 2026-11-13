package ar.jornadas.data

import kotlinx.coroutines.flow.first

class AgendaRepository(private val api: ProgramaApi, private val dao: CarteleraDao) {
    val programa = dao.observar()

    suspend fun sincronizar() {
        val fichasRemotas = api.consultar().map {
            Charla(it.id, it.title, it.track, it.day, it.time, it.room, it.speaker, it.summary)
        }

        val estadoLocal = dao.observar().first()
        val idsFavoritasPrevias = estadoLocal.filter { it.favorita }.map { it.id }.toSet()

        val fichasListasParaGuardar = fichasRemotas.map {
            if (idsFavoritasPrevias.contains(it.id)) it.copy(favorita = true) else it
        }

        dao.reemplazarTodo(fichasListasParaGuardar)
    }

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)
}