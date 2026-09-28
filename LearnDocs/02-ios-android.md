# Puentes desde iOS hacia Android

Las comparaciones son orientativas. La transferencia más útil es el razonamiento sobre propiedad, estado y dependencias; los ciclos de vida concretos hay que aprenderlos en Android.

| Conocimiento previo | Punto de entrada Android | Diferencia que conviene investigar |
| --- | --- | --- |
| Swift structs y enums con associated values | data classes, sealed interfaces/classes | Data class es un objeto con semántica de referencia; `copy` es superficial |
| Swift Optional | Tipos `T?`, safe call y Elvis | Interoperabilidad puede producir platform types; evitar `!!` como escape habitual |
| Protocols | Interfaces y DI por constructor | Kotlin extension no equivale a dispatch virtual por interface |
| SwiftUI View y state | Composable, remember y state hoisting | Vida de composición y efectos no se trasladan literalmente |
| ObservableObject / Observation | ViewModel + StateFlow | ViewModel tiene scopes de lifecycle Android; StateFlow no es actor ni storage |
| async/await | Funciones suspend | Suspender no implica ejecutar en background ni lanzar una tarea |
| Task y structured concurrency | CoroutineScope, Job y builders | Reglas de parent/child, dispatchers y cancelación deben comprobarse |
| Combine / AsyncSequence | Flow / StateFlow / SharedFlow | Distinguir stream frío, estado compartido y eventos; backpressure no es idéntico |
| URLSession y Codable | Ktor/OkHttp y kotlinx.serialization | Cliente, engine y serializer son decisiones separadas |
| SwiftData/Core Data | Room sobre SQLite | Room es más explícito en tablas/SQL; no es un object graph equivalente |
| UserDefaults | DataStore Preferences | Observación y escritura asincrónica; no guarda relaciones |
| NavigationStack / Coordinator | Navigation 3 y estado de back stack | Integrar back del sistema y reconstrucción de destinos |
| XCTest y UI testing | Pruebas JVM + instrumentadas + Compose | Separar ejecución en host de ejecución en dispositivo |
| BackgroundTasks | WorkManager para ciertos trabajos persistentes | El SO decide oportunidad; no representa un timer exacto |

Referencias para contrastar comportamiento: [ViewModel Android](https://developer.android.com/topic/libraries/architecture/viewmodel), [estado Compose](https://developer.android.com/develop/ui/compose/state-saving), [Swift Task](https://developer.apple.com/documentation/swift/task). La tabla es una guía conceptual propia, no una afirmación de equivalencia API por API.

## Caso 1 — rotación y propiedad

En ForeKast la Activity puede recrearse y el ViewModel asociado conservarse. La Screen vuelve a describirse con el estado disponible. Si el proceso muere, habrá un ViewModel nuevo: la app observa Room y recupera selección. Dibujar estos dos recorridos por separado.

Pregunta de diseño: si tu servicio conserva una referencia a Activity para mostrar un error, ¿quién retiene a quién y cuánto debería vivir? Mover presentación a la UI evita acoplar el servicio al objeto anterior.

## Caso 2 — UDF y experiencia con TCA/Redux

Si conocés reducers, reconocerás la idea de acción → transición → estado. En esta app algunas transiciones simples pueden ser métodos del ViewModel. La propiedad importante es que pueda explicarse quién modifica cada estado y cómo se evita que una respuesta tardía gane.

Experimentá con un reducer puro para SearchUiState y comparalo con métodos explícitos. Medí legibilidad, cantidad de estados inválidos representables y facilidad de prueba. No elijas por cantidad de archivos ni por similitud con un framework conocido.

## Caso 3 — tiempo y calendarios

`Instant` representa un punto temporal; `LocalDate` una fecha sin hora/zona; `ZoneId` permite traducir. La hora del pronóstico pertenece a la ciudad. Un offset actual no describe todas las reglas futuras de esa zona.

Ejercicio: buscar una ciudad en una zona distinta, cambiar la zona del emulador y cruzar medianoche. Explicar por qué el encabezado Hoy permanece ligado a la ciudad. Repetir con una transición de horario estacional y conservar la identidad de horas repetidas.

## Caso 4 — Android moderno no exige eliminar interoperabilidad

No escribir archivos Java es compatible con usar `java.time`, JDK de Gradle o bibliotecas JVM. La pregunta útil es si la API preserva cancelación, tipos y mantenibilidad en Kotlin. Retrofit con suspend también puede cumplir esos criterios; Ktor fue elegido aquí por objetivos de aprendizaje, no por una oposición entre lenguajes.
