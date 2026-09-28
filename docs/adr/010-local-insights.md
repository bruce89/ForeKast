# ADR-010 — Motor local determinista para insights

Estado: aceptado para I6a, 2026-09-28.

## Contexto

El contrato de insights existía como boceto fuera del módulo Android. Queremos estudiar decisiones de dominio y crear una referencia medible antes de implementar Python o un LLM. I4 todavía espera revisión manual de la interfaz; añadir tarjetas ahora mezclaría dos superficies de validación.

## Decisión

El puerto y sus modelos viven en `app/src/main/.../core/insights`. `RuleBasedWeatherInsightProvider` implementa el puerto con `Clock` inyectado y sin Android, red ni almacenamiento. `InsightRequestFactory` convierte `WeatherSnapshot` a horas en °C y porcentaje, con máximo 24 muestras futuras ordenadas. `snapshotId` deriva del ID de ciudad y el instante de descarga; el llamador aporta `requestId`. La factoría no se conecta todavía al ViewModel.

`day_summary` necesita al menos dos horas con temperatura y precipitación; informa mínimo, máximo y máxima probabilidad disponible, con evidencias por campo y hora. `walk_window` compara ventanas horarias completas y contiguas de 30, 60 o 120 minutos. Una muestra cubre 30/60 minutos de forma aproximada; dos muestras, 120. El puntaje experimental es `2 × máxima probabilidad de precipitación + 5 × |temperatura media − 20 °C|`. Gana el puntaje menor y, en empate, la ventana más temprana. Es una preferencia de ejemplo, no una recomendación de seguridad. El texto siempre declara la resolución horaria.

El motor se abstiene con `NoInsight` si la descarga tiene seis horas o más, su fecha está en el futuro, faltan muestras suficientes o no hay ventana contigua. Las entradas estructuralmente inválidas se rechazan con `IllegalArgumentException`; un adaptador HTTP futuro mapeará ese error al contrato wire. La salida `Ready` vence antes de una hora o al llegar al límite de frescura, lo que ocurra primero. El reloj inyectado vuelve repetibles los tests. Se formatea una cifra decimal y la hora local incluye offset para distinguir horas repetidas por DST.

## Alternativas y consecuencias

- Un LLM local o remoto: no aporta al cálculo verificable inicial, complica evidencia y costo.
- Reglas dentro de Compose/ViewModel: acoplan la política a la pantalla y dificultan su réplica en Python.
- Tratar datos nulos como cero: inventaría observaciones. Se omiten muestras incompletas y se informa la pérdida.
- Llamar “mejor momento” a la puntuación: ocultaría que el punto de confort de 20 °C y los pesos son arbitrarios. La UI futura debe exponerlos como preferencia o reemplazarlos por configuración del usuario.

I6a agrega código compilado al APK pero no una función visible ni tráfico nuevo. El contrato HTTP de `contracts/` sigue siendo propuesta para I7. Antes de mostrar insights, decidir lenguaje de UI, controles de duración/preferencias y prueba de accesibilidad. Reabrir la fórmula cuando haya feedback de utilidad, métricas o preferencias explícitas. Si Python reproduce este baseline, comparar reglas con los mismos fixtures antes de experimentar con agentes.
