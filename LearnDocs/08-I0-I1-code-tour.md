Este documento describe el corte histórico I0 + I1. Para ejecutar sus escenarios usar `-PforekastDemo=true`; la compilación normal ya usa red. Continuación: [I2](09-I2-network.md).

# I0 + I1 — recorrido por el código real

Esta es la primera versión ejecutable. Todavía usa datos sintéticos y memoria, no HTTP ni Room. Su objetivo es permitirte recorrer una interacción completa y cambiarla sin que una API externa agregue ruido.

## Antes de empezar

Seguí [TOOLCHAIN](../docs/TOOLCHAIN.md), ejecutá la app y abrí Ajustes → Laboratorio de estados. Probá completo, parcial, error, guardado y carga sostenida. El último se mantiene hasta que cambies escenario: sirve para estudiar loading y cancelación. Reintentar mantiene el escenario seleccionado.

## Mapa de responsabilidades

| Archivo | Qué mirar |
| --- | --- |
| [MainActivity](../app/src/main/kotlin/dev/bruze/forekast/MainActivity.kt) | Construcción del ViewModel y recolección con lifecycle |
| [AppContainer](../app/src/main/kotlin/dev/bruze/forekast/di/AppContainer.kt) | Grafo manual y lifetime de repositorio/reloj |
| [WeatherRepository](../app/src/main/kotlin/dev/bruze/forekast/core/ports/WeatherRepository.kt) | Puerto de observación y operación de refresco |
| [FakeWeatherRepository](../app/src/main/kotlin/dev/bruze/forekast/data/demo/FakeWeatherRepository.kt) | Suspensión, publicación y control de escenarios |
| [WeatherFixtures](../app/src/main/kotlin/dev/bruze/forekast/data/demo/WeatherFixtures.kt) | Datos sintéticos, Clock e instantes |
| [WeatherViewModel](../app/src/main/kotlin/dev/bruze/forekast/feature/weather/WeatherViewModel.kt) | Propiedad de UiState, cancelación y generación de solicitudes |
| [WeatherScreen](../app/src/main/kotlin/dev/bruze/forekast/feature/weather/WeatherScreen.kt) | Render de estado, callbacks y previews |
| [ForeKastApp](../app/src/main/kotlin/dev/bruze/forekast/navigation/ForeKastApp.kt) | Keys serializables y back stack de Navigation 3 |
| [WeatherFormatting](../app/src/main/kotlin/dev/bruze/forekast/designsystem/WeatherFormatting.kt) | Conversión y representación accesible |
| [WeatherViewModelTest](../app/src/test/kotlin/dev/bruze/forekast/WeatherViewModelTest.kt) | Tiempo virtual y pruebas de carreras |
| [ForeKastFlowTest](../app/src/androidTest/kotlin/dev/bruze/forekast/ForeKastFlowTest.kt) | Recorridos reales con Compose |

## Recorrido: actualizar

El botón llama a `onRefresh`; MainActivity conecta ese callback con `WeatherViewModel.refresh`. Si hay un Job activo, no se inicia otro. La coroutine activa progreso y llama al puerto. El fake espera con `delay`, crea una instantánea y la publica en su StateFlow interno. `observe` selecciona los datos de esa ciudad; el collector del ViewModel actualiza contenido. Por separado, el resultado de refresh termina progreso o activa error.

Ese orden separa contenido de operación. En el escenario guardado, la instantánea ya existe antes del refresh y el resultado falla. La pantalla conserva el contenido. Anotá dónde cambiaría el comportamiento si Failed borrara snapshot.

## Recorrido: cambiar ciudad

Search usa un catálogo local y filtra nombres/regiones/países; no simula la búsqueda HTTP ni debounce de I2. Elegir una ciudad actualiza su clave, cancela observación/refresh previos, prepara el fixture y cambia la suscripción. El contador `generation` protege contra resultados de un trabajo que dejó de ser relevante.

Ejercicio: cambiá la latencia del fake y alterná ciudades. Después leé la prueba que cambia a Madrid a mitad de la espera. Explicá por qué no aparece Montevideo bajo el nuevo encabezado.

## Estado y persistencia

I1 mantiene el ViewModel a nivel Activity como dueño del recorrido de pronóstico y sus preferencias de demostración. Es un corte pedagógico explícito, documentado en [ADR-006](../docs/adr/006-first-slice.md). No es una recomendación de mantener todas las features futuras en una clase global.

SavedStateHandle guarda claves pequeñas: ciudad, tema, unidad, escenario y favoritas. Puede reconstruirlas en restauraciones gestionadas por Android; no equivale a persistencia duradera ni garantiza sobrevivir a force-stop. Tras crear otro proceso, el fake genera datos nuevos. Room/DataStore entran en I3.

## Ejercicios verificables

1. Cambiá °C/°F y poné breakpoint en `refresh`: el cambio de unidad no debe llamar al repositorio.
2. Encontrá un dato null en Partial. Cambialo por cero y explicá por qué la pantalla debe distinguirlos.
3. Añadí un tercer tipo de condición al fixture sin cambiar el contrato del repositorio.
4. Leé las cinco previews y explicá qué dependencias no necesitan.
5. Rotá o usá `ActivityScenario.recreate()` y compará con un proceso nuevo.
6. Quitá la guardia de refresh en una rama experimental y comprobá que falla el test de deduplicación. Restaurala después.
7. Identificá los puntos concretos donde se conectarán Ktor y Room. La Screen no debería saber cuál se usa.

## Diferencias deliberadas respecto del MVP

Arranque directo en una ciudad de muestra; seis ciudades de catálogo; controles de laboratorio solo en debug; favoritos y ajustes en memoria/SavedState; sin permisos de red ni ubicación. Las políticas HTTP, TTL completo, cooldown, cache persistida y Python siguen pendientes. El modelo de errores reducido de este corte se ampliará cuando haya errores reales que distinguir.

El siguiente corte es I2: cliente HTTP y mapper con fixtures externos, búsqueda cancelable y validación de fechas. No cambiar simultáneamente a Room/Hilt mientras se estudia esa frontera.
