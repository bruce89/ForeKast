# Hoja de aprendizaje

## Diagnóstico inicial

Sin mirar una solución, explicar: diferencia entre nullable y no nullable; qué hace `copy` en una data class; cuándo usar suspend o Flow; qué sobrevive a una rotación; qué provoca una recomposición; por qué un ViewModel no debe recibir una Activity; qué capa conoce un nombre de campo del JSON.

Si algo ya está claro, hacer el ejercicio de comprobación y avanzar. No hace falta estudiar todo Kotlin antes de tocar una pantalla.

## Etapa A — herramientas y lenguaje

**Conceptos:** módulo Gradle, source sets main/test/androidTest, manifiesto, recursos, emulator vs JVM; val/var, nulabilidad, data class, sealed interface, lambdas y extensiones.

**Ejercicio:** modelar una medición que puede faltar. Convertir °C a °F preservando null; redondear solo al mostrar. Agregar un código meteorológico desconocido sin lanzar una excepción.

**Dominio:** explicar por qué `val` no vuelve inmutable un objeto mutable; demostrar que `copy()` no hace una copia profunda de sus listas. Relacionar ese resultado con publicar UiState consistente.

## Etapa B — Compose y estado

**Conceptos:** composición, recomposición, state hoisting, identidad de elementos, Screen/Route, previews, efectos.

**Ejercicio:** implementar componente de temperatura con parámetros y preview. Agregar cambio de unidades; producir el nuevo texto desde estado. Provocar recomposición repetida y comprobar que no inicia requests.

**Dominio:** señalar qué estado pertenece al componente, a la pantalla y a persistencia. Usar una clave estable de hora y demostrar qué problema causa usar posición cuando cambia la lista.

## Etapa C — asincronía

**Conceptos:** suspensión vs bloqueo, scope, dispatcher, Job, cancelación, Flow frío, StateFlow, debounce y latest.

**Ejercicio:** fake de búsqueda con demoras controladas; enviar A y luego B, hacer responder A última. Corregir para que solo B llegue a la pantalla. Escribir prueba con tiempo virtual.

**Dominio:** explicar por qué lanzar una coroutine desde cada recomposición falla, y por qué capturar una cancelación puede volver visible un error falso.

## Etapa D — datos reales

**Conceptos:** HTTP, DTO, serialización, mapper, unidades, Instant/LocalDate/ZoneId, errores tipados.

**Ejercicio:** seguir una hora desde JSON hasta texto visible. Cambiar zona del teléfono y demostrar que no altera la hora de la ciudad. Alimentar nulls y longitudes inconsistentes.

**Dominio:** poder sustituir Ktor por un fake sin cambiar la Screen. Explicar la diferencia entre hora de descarga, validez meteorológica y tiempo de generación del insight.

## Etapa E — persistencia y arquitectura

**Conceptos:** SQLite/Room, transacción, fuente canónica, DataStore, DI por constructor, scopes de Hilt, restauración.

**Ejercicio:** cargar, cerrar y abrir sin red. Interrumpir una escritura de snapshot y comprobar que no quedan horas nuevas bajo cabecera vieja. Implementar un Clock falso para la política de frescura.

**Dominio:** reconstruir el estado desde disco tras morir el proceso. Explicar por qué leer de Room y HTTP independientemente puede producir pantallas contradictorias.

## Etapa F — calidad Android

**Conceptos:** unitario vs instrumentado, semántica Compose, TalkBack, accesibilidad, lifecycle y limitaciones de background.

**Ejercicio:** probar rotación y muerte real de proceso como experimentos separados. Hacer el flujo completo con TalkBack y fuente grande. Seleccionar solo tests que demuestran riesgos relevantes.

**Dominio:** dar evidencia reproducible de Q01–Q18; poder diferenciar un bug de UI, uno de mapper y una respuesta meteorológica simplemente inesperada.

## Etapa G — Python y agentes

**Conceptos:** tipos Pydantic, función pura, HTTP, contrato, idempotencia, tool calling, grounding y evaluación.

**Ejercicio:** implementar el mismo resumen con reglas Kotlin y Python. Comparar con un LLM usando fixtures idénticos y una rúbrica previa.

**Dominio:** justificar el costo y utilidad de la IA con evidencia. Explicar qué parte es determinista, cuál probabilística y dónde se detectan afirmaciones no respaldadas.

## Ritmo sugerido

Trabajar por entregables, no por cantidad de videos. Una sesión puede terminar con una hipótesis fallida bien documentada. Reservar una sesión por corte para volver al código sin guía y dibujar el recorrido completo. Si no podés ubicar una responsabilidad, registrar esa fricción como dato de diseño.
