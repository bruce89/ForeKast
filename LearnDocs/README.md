# LearnDocs — aprender reconstruyendo ForeKast

Esta carpeta explica conceptos y propone experimentos; `docs/` define el producto. El punto de partida es experiencia en ingeniería de software e iOS, con profundidad Kotlin/Android por ajustar. El recorrido permite saltar sintaxis conocida sin saltear diferencias de plataforma.

## Método de trabajo

Para cada corte: ejecutar, observar, predecir, modificar y explicar. Primero describir qué debería pasar; después usar debugger/logs/tests; finalmente registrar por qué la hipótesis se sostuvo o falló. Poder repetir un tutorial no es el criterio de dominio: poder cambiar una regla sin romper otra sí lo es.

| Orden | Material | Entrega que acompaña |
| --- | --- | --- |
| 0 | [Diagnóstico y hoja de ruta](01-learning-roadmap.md) | I0–I8 |
| 1 | [Puentes y diferencias con iOS](02-ios-android.md) | I0–I4 |
| 2 | [Kotlin, coroutines y Flow](03-kotlin-concurrency.md) | I1–I3 |
| 3 | [Ingeniería inversa guiada](04-reverse-engineering.md) | Cada corte |
| 4 | [Laboratorios de arquitectura y testing](05-architecture-labs.md) | I2–I4 |
| 5 | [Python y agentes](06-python-agents.md) | I6–I8 |
| 6 | [Bitácora y glosario](07-workbook.md) | Durante todo el proyecto |
| 7 | [Recorrido del código I0 + I1](08-I0-I1-code-tour.md) | App ya implementada |
| 8 | [Red real, mapeo y pruebas I2](09-I2-network.md) | Ktor, errores y tiempo |
| 9 | [Persistencia y restauración I3](10-I3-persistence.md) | Room, DataStore y transacciones |
| 10 | [Calidad, estado vacío y accesibilidad I4](11-I4-quality.md) | Pruebas, Android 8 y TalkBack |
| 11 | [Motor local de insights I6a](12-I6-local-insights.md) | Reglas puras, evidencia y abstención |

Las analogías son puntos de entrada, no equivalencias de runtime. Los snippets explican una idea y no forman por sí solos una app compilable. Cuando se implemente, cada corte debe registrar rutas reales para seguir datos sin adivinar nombres.

## Criterio común de progreso

Podés explicar quién posee cada dato, cuánto vive, qué pasa al cancelar y dónde se convierte un error. Podés mostrar una prueba que fallaría si tu explicación fuera falsa. Para aprender, conviene conservar fakes y fixtures aunque ya exista una API real.
