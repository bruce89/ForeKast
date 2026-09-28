# Contratos de referencia

- [insights.openapi.json](insights.openapi.json): OpenAPI 3.1 para el futuro servicio Python.
- [WeatherInsightProvider.kt](../app/src/main/kotlin/dev/bruze/forekast/core/insights/WeatherInsightProvider.kt): puerto Kotlin compilado, independiente del transporte.
- [Solicitud](examples/insight-request.json), [respuesta](examples/insight-response.json), [abstención](examples/insight-no-result.json) y [error](examples/insight-error.json): ejemplos sintéticos.

No hay servidor ni SDK generado. I6a implementa el puerto en Kotlin con [reglas locales](../app/src/main/kotlin/dev/bruze/forekast/core/insights/RuleBasedWeatherInsightProvider.kt), todavía sin interfaz de usuario. Un adapter HTTP futuro convertirá a DTOs de serialización y códigos HTTP. Sus fechas son Instant; los JSON usan RFC 3339. El contrato HTTP especifica límites adicionales de longitud y estructura.

Validar también los invariantes de [PYTHON_AI](../docs/PYTHON_AI.md): tiempo, referencias de evidencia, correlación, orden y coherencia. JSON Schema no garantiza veracidad del texto. El ejemplo de abstención ilustra una salida alternativa y no afirma que el request de ejemplo obligue a abstenerse.

Todos los ejemplos fijan el reloj a 2026-09-24T14:10:00Z. No son lecturas del tiempo real. El endpoint local de OpenAPI es una referencia para laboratorio futuro; autenticación remota todavía debe implementarse.
