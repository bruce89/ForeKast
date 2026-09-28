# ADR-001 — Kotlin, Compose y estado unidireccional

Estado: propuesto · 2026-09-24.

## Contexto

El proyecto busca aprender Android actual, apoyándose en experiencia de arquitectura iOS. Necesitamos una pantalla comprensible bajo recomposición, rotación, navegación y errores asincrónicos.

## Decisión

Kotlin, Compose y Material 3; una Activity; Navigation 3; ViewModel por pantalla con estado inmutable expuesto por StateFlow. Acciones suben y estado baja. Los componentes reciben datos y callbacks. Hilt compone el grafo, con un laboratorio previo de DI manual.

Usaremos “MVVM con UDF” como descripción de responsabilidades. No introduciremos una biblioteca MVI ni una maquinaria de intents/reducers/effects obligatoria. Si un reducer puro simplifica una pantalla, se añade por esa razón.

## Alternativas y tradeoffs

- Views/XML: válido para mantenimiento e interoperabilidad, pero no aporta al objetivo inicial de una app nueva declarativa.
- Kotlin Multiplatform/Compose Multiplatform: permitiría compartir código; añade targets, toolchains y decisiones antes de entender Android. Mantener dominio simple facilita evaluarlo después.
- Reducer global estilo Redux/TCA: trazabilidad atractiva para estados complejos; costo de abstracción elevado para cuatro pantallas pequeñas. UDF conserva la dirección de flujo sin imponer toda esa estructura.
- Estado local de Compose para todo: rápido para una maqueta; mezcla peticiones y vida útil de pantalla. Se conserva para detalles efímeros de componentes.
- Koin o DI manual: viables. Hilt permite estudiar scopes Android y grafo generado; agrega KSP/build y annotations. La implementación debe justificar cada scope.

## Consecuencias y validación

Más tipos de estado al principio; menos banderas ambiguas después. Se necesitan pruebas de refresh, cambio de ciudad y restauración. ViewModel no se equipara a un singleton ni a almacenamiento permanente. La elección es consistente con [Android architecture](https://developer.android.com/topic/architecture/recommendations).

Reabrir si un segundo cliente necesita compartir UI o si la cantidad de transiciones hace difícil razonar sin reducer explícito.
