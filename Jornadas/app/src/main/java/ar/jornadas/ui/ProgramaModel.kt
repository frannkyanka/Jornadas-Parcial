package ar.jornadas.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import ar.jornadas.Servicios
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

class ProgramaModel(app: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(app) {
    private val servicios = Servicios(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    private val prefs = app.getSharedPreferences("estado_app", Context.MODE_PRIVATE)

    val charlas = repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val dia = savedStateHandle.getStateFlow("filtro_dia", 0)
    val tema = savedStateHandle.getStateFlow("filtro_tema", "Todos")

    val aviso = MutableStateFlow("")
    val cargando = MutableStateFlow(false)
    private var peticionEnCurso = false

    init {
        recargar()
    }

    fun setDia(d: Int) { savedStateHandle["filtro_dia"] = d }
    fun setTema(t: String) { savedStateHandle["filtro_tema"] = t }

    fun tieneDatosGuardados(): Boolean =
        charlas.value.isNotEmpty() || prefs.getBoolean("descarga_valida", false)

    fun recargar() {
        if (peticionEnCurso) return
        viewModelScope.launch {
            peticionEnCurso = true
            cargando.value = true
            aviso.value = ""
            try {
                repo.sincronizar()
                prefs.edit().putBoolean("descarga_valida", true).apply()
            } catch (e: IOException) {
                aviso.value = if (tieneDatosGuardados()) {
                    "Se están usando datos guardados (sin señal)"
                } else {
                    "No se pudo consultar el programa"
                }
            } finally {
                cargando.value = false
                peticionEnCurso = false
            }
        }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }
}