# I6a — Construir un motor de insights sin IA

## Qué es el motor

Es una función de negocio con una entrada estructurada, reglas visibles y una salida verificable. No aprende, no consulta Internet y no genera predicciones meteorológicas nuevas. Toma horas ya descargadas por Open-Meteo y produce una descripción o un ranking. `suspend` aparece en el puerto para admitir un proveedor HTTP futuro; la implementación local no suspende ni lanza coroutines. La predicción meteorológica proviene del proveedor, no del motor.

Recorrido real: [`WeatherSnapshot`](../app/src/main/kotlin/dev/bruze/forekast/core/model/Weather.kt) → [`InsightRequestFactory`](../app/src/main/kotlin/dev/bruze/forekast/core/insights/InsightRequestFactory.kt) → [`WeatherInsightProvider`](../app/src/main/kotlin/dev/bruze/forekast/core/insights/WeatherInsightProvider.kt) → [`RuleBasedWeatherInsightProvider`](../app/src/main/kotlin/dev/bruze/forekast/core/insights/RuleBasedWeatherInsightProvider.kt) → `Ready` o `NoInsight`. Hoy ninguna pantalla invoca ese recorrido. La separación permite verificar el cálculo sin cambiar la revisión manual de I4.

## Cómo leer las reglas

1. Empezá por `generate`: valida el contrato, consulta el reloj inyectado y se abstiene si el snapshot tiene seis horas o más o una fecha futura.
2. `summary` filtra horas con ambos valores. Busca temperaturas mínima/máxima y máxima probabilidad de precipitación. Sus evidencias enlazan `timestamp` y `field` originales.
3. `walk` forma ventanas contiguas sobre la línea de tiempo UTC. Compara `2 × lluvia máxima + 5 × distancia de temperatura media a 20 °C`; un empate favorece el inicio más temprano. No significa que 20 °C sea universalmente cómodo ni que una probabilidad baja garantice tiempo seco.
4. La salida declara omisiones y resolución horaria. `expiresAt` limita cuánto tiempo se puede reutilizar un resultado positivo.

En iOS, el equivalente podría ser un protocolo de dominio y un tipo concreto con `Clock` inyectado. Aquí la interfaz Kotlin hace de puerto; `data class` y `sealed interface` representan valores y resultados cerrados. Ni ViewModel ni Compose son dueños de la regla. No se requiere un módulo Gradle separado mientras esta frontera pueda probarse sin dependencias Android; separar módulos tendría costo de build y mantenimiento.

## Ejercicio guiado

Antes de abrir las pruebas, predecí qué ventana gana con estas horas consecutivas: 16 °C/30 %, 22 °C/10 %, 19 °C/5 %. Para 120 minutos, las ventanas son las horas 1–2 y 2–3. Sus puntajes son `2×30 + 5×|19−20| = 65` y `2×10 + 5×|20,5−20| = 22,5`. Gana la segunda. Luego comprobalo en [`RuleBasedWeatherInsightProviderTest`](../app/src/test/kotlin/dev/bruze/forekast/RuleBasedWeatherInsightProviderTest.kt).

Modificá el peso de lluvia de 2 a 0,1 y escribí primero un test con datos en los que cambie el ganador. Explicá por qué es una preferencia de producto y no una verdad del proveedor. Después probá una hora faltante, una repetición de hora local durante DST y un snapshot exactamente al límite de seis horas. El motor nunca debe inventar el valor ausente ni confundir dos `02:00` con offsets distintos.

## Decisiones para discutir

- ¿Conviene pedir dos horas completas para el resumen, o permitir una con un texto más limitado? ¿Cómo afectaría la utilidad y el riesgo de exagerar?
- ¿Una ventana de 30 minutos puede evaluarse con una muestra por hora? Ahora es una aproximación explícita. ¿Qué dato haría falta para afirmar más?
- ¿Dónde vivirían preferencias de temperatura/riesgo de lluvia si cada usuario las ajusta? Evitá meterlas en el contrato remoto antes de precisar privacidad y semántica.
- ¿Qué cambia si el segundo proveedor es Python? La implementación varía; el contrato, fixtures y pruebas de evidencia deberían conservarse.

Para ejecutar: `.\scripts\gradle.ps1 :app:testDebugUnitTest`. El diseño completo y sus límites están en [ADR-010](../docs/adr/010-local-insights.md), y la extensión Python propuesta en [PYTHON_AI](../docs/PYTHON_AI.md).
