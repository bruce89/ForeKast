# Ingeniería inversa guiada

Usá este documento cada vez que recibas una iteración implementada. Los nombres son objetivos del diseño; al construir se completa una tabla de rutas reales. No hace falta leer todo el repo de principio a fin.

## Recorrido 1 — seguir una temperatura

Elegí una cifra visible. Localizá el Composable que la muestra y preguntá si recibe Double, Temperature o texto ya formateado. Seguí su origen hasta UiState, ViewModel, repositorio, mapper y DTO. Identificá cada transformación: unidad, redondeo, ausencia, zona y localización.

Poné breakpoints en los límites. Cambiá una temperatura del fixture y predecí todos los textos que deberían variar. Si una Screen necesita conocer `temperature_2m`, investigá por qué el contrato externo atravesó la frontera.

**Resultado:** diagrama con un ejemplo de valor antes/después de cada mapper, incluyendo su unidad. Explicación de dónde se pierde precisión y si esa pérdida es intencional.

## Recorrido 2 — seguir una acción

Pulsá actualizar. Encontrá callback, método del ViewModel, exclusión de solicitudes, llamada remota, validación y transacción. Observá cómo la emisión local vuelve al ViewModel. Identificá cuándo se activa/desactiva progreso y quién decide si hay cooldown.

Hacé doble tap; cambiá de ciudad mientras llega una respuesta; cortá red con caché existente. Antes de cada prueba escribí el resultado esperado. Si dos responsabilidades modifican el mismo loading flag, buscá una carrera.

**Resultado:** secuencia con éxito, cancelación y error. Propuesta de un test que detecte cada fallo.

## Recorrido 3 — observar vida útil

Recomponé mediante un cambio local. Rotá. Navegá a Ajustes y volvé. Mandá al background. Finalmente terminá el proceso y abrí otra vez. No llames a todos esos eventos “reinicio”.

Para cada uno, registrá identidad de Activity/ViewModel, contenido de Room, suscripciones y número de requests. No imprimir coordenadas en logs compartidos. Explicá qué objeto retiene el estado y qué dato permite reconstruirlo.

**Resultado:** matriz evento × dato que sobrevive × mecanismo. Comparala con [arquitectura](../docs/ARCHITECTURE.md), no con una intuición basada en otra plataforma.

## Recorrido 4 — sustituir una pieza

Cambiar WeatherRepository por un fake de “nieve, datos antiguos”. La Screen no debería cambiar. Después añadir un cliente meteorológico alternativo como experimento sin conectarlo a producción. Anotar qué semánticas no tienen equivalencia.

Una interface reemplazable no demuestra desacoplamiento completo si sus modelos copian literalmente al proveedor. Revisá qué costo tiene mapear otra fuente.

**Resultado:** lista de cambios necesarios y explicación de cuáles son inevitables diferencias del negocio.

## Recorrido 5 — leer el grafo de dependencias

Encontrá dónde se construyen cliente HTTP, base y repositorios. Dibujá scopes. Sustituí el Clock en un test. Identificá si alguna clase resuelve dependencias globalmente en vez de recibirlas.

**Resultado:** poder armar manualmente el mismo grafo reducido en un test. Entender qué genera Hilt sin leer todos los archivos generados.

## Plantilla de mapa por versión

| Pregunta | Ruta real a completar | Responsabilidad |
| --- | --- | --- |
| ¿Dónde entra la app? | [MainActivity](../app/src/main/kotlin/dev/bruze/forekast/MainActivity.kt) | Activity y composición |
| ¿Quién posee WeatherUiState? | [WeatherViewModel](../app/src/main/kotlin/dev/bruze/forekast/feature/weather/WeatherViewModel.kt) | ViewModel de sesión I1 |
| ¿Quién traduce Open-Meteo? | Pendiente I2 | Mapper |
| ¿Quién decide frescura? | Pendiente I3 | Política/repositorio |
| ¿Quién escribe snapshot? | Pendiente I3 | Adapter Room |
| ¿Quién llama Python? | Pendiente I7 | Adapter de insights |

## Desafíos sin solución paso a paso

Agregar una unidad; cambiar TTL por configuración; mostrar un dato faltante; incorporar nueva condición; añadir detalle de día; separar una regla reutilizada en use case. Para cada uno, predecir qué archivos deberían cambiar. Si cambian muchos más, buscar si hay acoplamiento útil o accidental antes de refactorizar.
