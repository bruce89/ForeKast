# I4 — Calidad, estado vacío y accesibilidad

Este corte se estudia después de [I3](10-I3-persistence.md). La pregunta central es cómo convertir un SPEC en evidencia reproducible. Compará cada escenario [Q01–Q18](../docs/QUALITY.md) con una prueba o una revisión manual; que compile no implica que una persona pueda usarlo con TalkBack.

## Recorrido: primera apertura

1. Partí de una instalación sin datos. `RoomLibraryRepository.library` recibe una lista vacía del DAO y emite `Library(selected = null, favorites = [])`. Seguí [RoomLibraryRepository.kt](../app/src/main/kotlin/dev/bruze/forekast/data/local/RoomLibraryRepository.kt) y [ForeKastDao](../app/src/main/kotlin/dev/bruze/forekast/data/local/ForeKastDatabase.kt).
2. `WeatherViewModel.restorePersistentState` lee Room y DataStore, pone `initialized = true`, y `loadSelection` no llama al repositorio meteorológico cuando no existe selección. La ubicación inicial del catálogo sirve como valor interno de `WeatherUiState.location`; `hasSelection` decide si puede mostrarse un pronóstico. Pregunta: ¿qué cambiaría si `location` fuese nullable en toda la UI? ¿Cuántos sitios requieren manejo explícito?
3. [ForeKastApp.kt](../app/src/main/kotlin/dev/bruze/forekast/navigation/ForeKastApp.kt) muestra bienvenida. Al elegir un resultado remoto, `select` persiste la ciudad y emite un nuevo estado. El resultado de geocoding contiene identidad, coordenadas y zona IANA, sin permiso GPS.
4. Cerrá y reabrí: ahora el primer Flow de Room contiene la selección. No se reintroduce el onboarding porque no hay un flag separado.

Experimento: instrumentá una instalación limpia y contá las llamadas al endpoint de pronóstico antes de elegir ciudad. Deberían ser cero. Repetí tras seleccionar y cerrar el proceso. Separá el tráfico de geocoding del tráfico de pronóstico.

## Recorrido: borrar datos

Desde Ajustes, la acción abre un diálogo. Cancelar no cambia Room. Confirmar invoca `WeatherViewModel.clearCities`; se cancelan observación y refresh, cambia la generación, y una transacción elimina selección y ciudades. Room aplica `CASCADE` a snapshots, filas horarias/diarias y marcas de intento. Un resultado de red que llegue tarde no puede resucitar una ciudad ausente: `ForeKastDao.replace` comprueba su existencia dentro de la transacción.

DataStore sigue teniendo unidades y tema. `host_cooldowns` también persiste: es un límite del servidor, no una preferencia asociada a una ciudad. Contrastá esto con `Keychain`/`UserDefaults` y Core Data/SwiftData en iOS: un borrado de entidad no debería mutar preferencias por accidente; la transacción protege la consistencia del conjunto Room, no crea una transacción distribuida entre stores.

Ejercicios:

- Modificá temporalmente el DAO para fallar después de `deleteSelection()`. Demostrá que la transacción revierte también ese delete.
- Simulá una descarga lenta, confirmá borrado y comprobá que no reaparece la ciudad.
- Decidí cómo cambiaría el contrato si «restablecer app» también debiera borrar DataStore y los cooldowns. Documentá orden, cancelación y fallos parciales antes de codificar.

## Accesibilidad como contrato observable

Compose ofrece `semantics` para headings, roles, selección, descripciones y anuncios. Las pruebas automáticas detectan etiquetas ausentes, contraste o zonas táctiles pequeñas; no juzgan por completo el orden hablado ni si una acción es comprensible. Android recomienda complementarlas con uso manual de TalkBack o Switch Access: [guía oficial](https://developer.android.com/develop/ui/compose/accessibility/testing).

En I4, buscá un elemento del pronóstico en el árbol semántico y leé su descripción; comprobá unidad, fecha, ciudad y probabilidad. Después repetí con TalkBack: recorré bienvenida → búsqueda → resultado → pronóstico → favoritas → ajustes → diálogo de borrado. Escuchá cada foco y activá cada acción. Anotá dónde aparece una descripción redundante o falta contexto.

Para fuente al 200 %, probá **Configuración Android → Pantalla → Tamaño de fuente**, también con tema oscuro y orientación horizontal. La cabecera crece, la búsqueda se desplaza como una sola lista, humedad/viento se apilan y las tarjetas diarias usan varias líneas. Elegí una ciudad con nombre largo y probabilidad de tres dígitos. Capturá la pantalla y registrá dispositivo/API, tamaño de ventana y escala. Un test que solo usa `assertExists()` no demuestra que el elemento sea visible o alcanzable.

## Mapa de verificación

- JVM: [WeatherViewModelTest.kt](../app/src/test/kotlin/dev/bruze/forekast/WeatherViewModelTest.kt) y [PersistentStateTest.kt](../app/src/test/kotlin/dev/bruze/forekast/PersistentStateTest.kt) para cancelación, ausencia de selección y resultado tardío.
- Android con Room real: [PersistenceTest.kt](../app/src/androidTest/kotlin/dev/bruze/forekast/PersistenceTest.kt) para borrado, reapertura y snapshot anterior ante respuesta inválida.
- UI: [ForeKastFlowTest.kt](../app/src/androidTest/kotlin/dev/bruze/forekast/ForeKastFlowTest.kt) para navegación y recreación; [ProcessPersistenceTest.kt](../app/src/androidTest/kotlin/dev/bruze/forekast/ProcessPersistenceTest.kt) distingue muerte de proceso de recreación de Activity.
- Registro de ejecución y pendientes: [VALIDATION.md](../VALIDATION.md).

Una prueba de migración solo es significativa al existir dos versiones de esquema. Room sigue en v1; [el esquema exportado](../app/schemas/dev.bruze.forekast.data.local.ForeKastDatabase/1.json) sirve de punto de partida para I5.

## Decisión que podés discutir

[ADR-009](../docs/adr/009-mvp-quality.md) conserva el cooldown HTTP tras borrar ciudades. Formulá una alternativa que respete `Retry-After` sin guardar ese cooldown localmente. Evaluá qué ocurre cuando el proceso se reinicia durante la espera. La respuesta necesita distinguir una preferencia de usuario de una restricción del proveedor.
