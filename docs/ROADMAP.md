# Roadmap de producto y aprendizaje

Las iteraciones son cortes verificables. No representan semanas ni fechas comprometidas. Cada una deja una versión estudiable; conviene etiquetarla en Git cuando exista el repo y registrar qué diferencia produjo.

**Estado actual:** I0–I4 implementados; I6a incorpora el motor local de insights y sus pruebas, todavía sin UI. La aceptación final de accesibilidad de I4 sigue sujeta a la pasada manual registrada en [QUALITY](QUALITY.md). La compilación normal usa Open-Meteo, Room y DataStore, con restauración offline. Python sigue pendiente. Ver [VALIDATION](../VALIDATION.md), [ADR-008](adr/008-persistence.md) y [ADR-010](adr/010-local-insights.md).

| Iteración | Entrega | Aprendizaje | Criterio para avanzar |
| --- | --- | --- | --- |
| I0 — Base | Proyecto Compose compilable y toolchain fijada | Gradle, source sets, lifecycle, recursos | Build limpio reproducible, una preview y una pantalla |
| I1 — Vertical falsa | Pronóstico y navegación con fake, claro/oscuro | State hoisting, UDF, ViewModel, recomposición | Mostrar carga, éxito, parcial, error y caché simulada |
| I2 — Red real | Buscar ciudad + Open-Meteo + mapper; memoria temporal | suspend, Flow, cancelación, serialización | Requests verificadas y contratos/tiempo/errores probados |
| I3 — Persistencia | Room, favoritos, DataStore, caché y restauración | Transacciones, fuente canónica, DI y scopes | Reabrir offline y cambiar unidades sin HTTP |
| I4 — MVP fiable | Accesibilidad, pruebas, pulido y docs reales | Testing, proceso, observabilidad | Q01–Q18 y build instalable documentados |
| I5 — Android ampliado | Ubicación opcional y luego widget | Permisos, aproximación, Glance/WorkManager si aplica | App útil con permiso rechazado; trabajo acotado |
| I6 — Insights locales | I6a: puerto Kotlin y reglas explicables implementados; integración UI pendiente | Dominio puro, baseline y evaluación | Resultados trazables, sin LLM ni backend requerido |
| I7 — Python | FastAPI, mismo contrato y reglas | HTTP, Pydantic, contract tests, timeouts | Paridad con baseline y fallo remoto aislado |
| I8 — Agentes acotados | Resumen o recomendación verificable | Herramientas, grounding, evaluación y costo | Supera criterios definidos frente al baseline |

## Próximo paso recomendado

Estudiar el [motor local I6a](../LearnDocs/12-I6-local-insights.md) mientras se completa la pasada manual de TalkBack de I4. Queda medir de forma reproducible el tiempo de presentación desde caché. Después decidir si conectar insights a la UI, avanzar con I7/Python, añadir detalle diario o iniciar I5. Ubicación opcional añade permisos y widget añade otra superficie Android. Hilt puede estudiarse después como refactor del grafo manual.

## Mejoras candidatas

| Idea | Valor y aprendizaje | Dependencias y costo relativo | Prioridad propuesta |
| --- | --- | --- | --- |
| Ubicación aproximada a pedido | Menos escritura; permisos y lifecycle | I4; medio | Alta después del MVP |
| Widget con último pronóstico | Utilidad diaria; superficie Android adicional | Caché estable; refresh acotado; medio | Alta |
| Detalle de día | Profundizar lectura horaria y navegación | Datos existentes; bajo | Alta |
| Gráfico temperatura/lluvia accesible | Visualización y semántica | Resumen textual equivalente; medio | Media |
| Español/inglés y unidades de viento | Recursos y formateo internacional | Strings separadas; bajo/medio | Media |
| Tablet/foldable | Layout adaptable y estado de navegación | Pantallas estables; medio | Media |
| Notificación de lluvia | Scheduling y preferencias | Permisos; frecuencia no exacta; medio/alto | Posterior |
| UV/calidad del aire | Información adicional y nuevos contratos | Revisar disponibilidad y fuentes; medio | Posterior |
| Comparar modelos | Incertidumbre y evaluación de pronósticos | Almacenamiento de corridas; alto | Rama de datos |
| Comparación Ktor/Retrofit | Aprender tradeoffs con contrato idéntico | I4; experimento aislado; medio | Rama educativa |
| Compartir dominio con iOS vía KMP | Contrastar reutilización y límites | Segundo cliente concreto; alto | Rama educativa |
| Resumen natural del día | Python y generación grounded | I6–I8; costo de modelo variable | Primera rama AI |
| Ventana para caminar/correr | Reglas, preferencias y ranking | I6, cobertura horaria suficiente; medio | Buen baseline AI |
| Agente que explica cambios entre pronósticos | Memoria de snapshots y herramientas | Historial propio, evaluación; alto | Rama AI posterior |

## Ideas de experimentos AI

**Resumen diario:** calcular hechos primero, redactar después. Comparar texto de plantilla con texto de LLM. Medir errores numéricos, cobertura de evidencia, claridad, latencia y costo.

**Selector de ventana:** el usuario define duración y tolerancias. Kotlin/Python calculan candidatos sobre las horas disponibles. Un LLM puede explicar la elección; no decide rangos sin consultar los datos. Faltantes producen abstención.

**Explicación de cambios:** guardar dos pronósticos tal como se descargaron y calcular diferencias del mismo intervalo válido. No confundir variación del modelo con cambio del tiempo observado. Es una buena introducción a herramientas y memoria verificable.

## Criterios para elegir la siguiente rama

Una mejora debe responder una necesidad de uso o una pregunta de aprendizaje concreta, tener una demo pequeña y un costo explícito de implementación/mantenimiento. Si dos mejoras compiten, priorizar la que reutiliza la base y agrega una sola frontera nueva. Una integración con agentes no obliga a convertir ForeKast en un chat.
