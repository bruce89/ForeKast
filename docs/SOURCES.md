# Fuentes y vigencia

Consultadas el **24 de septiembre de 2026**. Son fuentes primarias. El SPEC contiene decisiones propias; las fuentes respaldan capacidades y comportamientos de plataformas/proveedores, no los valores elegidos de TTL, límites del producto o prioridad de mejoras.

| Tema | Fuente | Qué revisar al implementar |
| --- | --- | --- |
| Arquitectura Android | [Recommendations](https://developer.android.com/topic/architecture/recommendations) | Capas, estado y uso de coroutines |
| UI | [UI layer](https://developer.android.com/topic/architecture/ui-layer) | Estado de pantalla y acciones |
| Caché observable | [Offline-first](https://developer.android.com/topic/architecture/data-layer/offline-first) | Lectura local y actualización |
| ViewModel | [Overview](https://developer.android.com/topic/libraries/architecture/viewmodel) | Scope y vida útil |
| Estado recuperable | [Save UI state](https://developer.android.com/develop/ui/compose/state-saving) | SavedState y estado persistente |
| Coroutines | [Best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices) | Cancelación, scopes y pruebas |
| Kotlin integrado | [AGP migration](https://developer.android.com/build/migrate-to-built-in-kotlin) | Plugins y compatibilidad del build |
| Navegación | [Navigation 3 releases](https://developer.android.com/jetpack/androidx/releases/navigation3) | Canal estable y APIs |
| Room | [Room overview](https://developer.android.com/training/data-storage/room) | Familia de artifacts, KSP, esquemas |
| Preferencias | [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) | Setup e instancias |
| DI | [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) | Scopes y generación |
| Accesibilidad | [Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility) | Semántica y componentes |
| Ktor | [Requests](https://ktor.io/docs/client-requests.html) | Cliente, engine y operaciones |
| JSON | [Content negotiation](https://ktor.io/docs/client-serialization.html) | Serialización y plugins |
| Retry | [Ktor retries](https://ktor.io/docs/client-request-retry.html) | Configuración y orden de plugins |
| Forecast | [Open-Meteo docs](https://open-meteo.com/en/docs) | Campos, tiempo y códigos |
| Ciudades | [Geocoding](https://open-meteo.com/en/docs/geocoding-api) | Campos y semántica de búsqueda |
| Cuotas/licencia | [Open-Meteo pricing](https://open-meteo.com/en/pricing) y [Terms](https://open-meteo.com/en/terms) | Elegibilidad y consumo |
| Alternativas | [WeatherAPI](https://www.weatherapi.com/pricing.aspx), [OpenWeather](https://openweathermap.org/price) | Productos, horizontes y facturación |
| Servicio Python | [FastAPI features](https://fastapi.tiangolo.com/features/) | Validación y OpenAPI |
| Comparación iOS | [Swift Task](https://developer.apple.com/documentation/swift/task) | Semántica real de tareas |

No fijar versiones por memoria o por el primer snippet de una página: algunos ejemplos muestran alpha aunque haya una versión estable. Las dependencias exactas se verifican juntas en I0 y se registran en la futura matriz de toolchain.

## Revisión I2 — 2026-09-25

Se consultaron nuevamente [Forecast](https://open-meteo.com/en/docs), [Geocoding](https://open-meteo.com/en/docs/geocoding-api) y [Ktor Client](https://ktor.io/docs/client-create-new-application.html). Se fijó Ktor 3.3.3 y se verificó compilando con el toolchain existente. No se declara que sea la versión más nueva. Los fixtures y pruebas ejecutadas están en [VALIDATION](../VALIDATION.md).

## Revisión I3 — 2026-09-25

Se consultaron las notas oficiales de [Room](https://developer.android.com/jetpack/androidx/releases/room), [DataStore](https://developer.android.com/jetpack/androidx/releases/datastore) y [KSP](https://github.com/google/ksp/releases). Se fijaron Room 2.8.5, DataStore 1.2.1 y KSP 2.3.12 y se verificaron compilando con el toolchain existente. Las decisiones propias de retención/frescura están en [ADR-008](adr/008-persistence.md).
