# Cambios y Justificaciones - Jornadas del Sur

## 41 · Programa sin señal (Comportamiento offline)
* **Problema:** Un fallo de red (`IOException`) pisaba la pantalla con un mensaje de error bloqueante aunque ya existieran charlas guardadas en Room. Además, al recibir una edición vacía (v4) y perder señal, no se recordaba que ya había habido una descarga válida.
* **Solución:** En `ProgramaModel`, si ocurre un fallo de red pero existen datos guardados (verificados mediante la lista de Room o una bandera en `SharedPreferences`), se muestra un aviso no bloqueante (*"Se están usando datos guardados (sin señal)"*) y `Catalogo.kt` sigue dibujando la grilla sin mostrar el botón de reintento.

## 42 · Agenda personal (Conservación de favoritas y sincronización)
* **Problema:** Al sincronizar una nueva edición del programa, `CarteleraDao.guardar()` usaba `@Insert(onConflict = OnConflictStrategy.REPLACE)` sobre fichas nuevas con `favorita = false`, borrando las marcas de favorito del usuario. Tampoco eliminaba charlas retiradas en ediciones posteriores (v3/v4).
* **Solución:** En `AgendaRepository.sincronizar()` se lee primero el estado local (`dao.observar().first()`) para rescatar los IDs marcados como favoritos y transferirlos a las fichas entrantes. En `CarteleraDao` se agregó una operación `@Transaction` (`reemplazarTodo`) que limpia y reinserta la instantánea completa de forma atómica.

## 43 · Volver al punto de lectura (Supervivencia a rotación y Process Death)
* **Problema:** El filtro `tema` en `Catalogo.kt` usaba `remember` simple y se perdía al navegar al detalle, rotar la pantalla o al matar el proceso en segundo plano; lo mismo ocurría con el filtro `dia` en el ViewModel.
* **Solución:** Ambos filtros (`filtro_dia` y `filtro_tema`) se movieron a `SavedStateHandle` dentro de `ProgramaModel`, mientras que `Portada.kt` conserva `seccion` y `detalle` mediante `rememberSaveable`.

## 44 · Control de descargas (Concurrencia y ciclo de vida)
* **Problema:** `Catalogo.kt` ejecutaba `LaunchedEffect(Unit) { model.recargar() }` y `MainActivity.onStart()` también llamaba a `recargar()`, provocando descargas duplicadas al rotar o navegar. Además, múltiples toques en *Actualizar* lanzaban peticiones concurrentes.
* **Solución:** Se eliminaron las llamadas desde `Catalogo` y `MainActivity.onStart()`, moviendo la carga inicial única al bloque `init` de `ProgramaModel` y agregando una guarda `peticionEnCurso` en `recargar()` para ignorar toques repetidos mientras hay una descarga activa.