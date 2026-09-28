# Proveedor y contrato meteorológico

Verificación documental: 2026-09-24. La elección aplica al prototipo educativo. Antes de publicar o monetizar se revisan nuevamente términos, consumo y atribución.

## Estado implementado I2

La app usa Ktor/OkHttp y dos requests por actualización: actual/horaria UNIX y diaria ISO. Usa la zona explícita del geocoding, no `auto`. No solicita `is_day` horario; sí actual. El detalle y la justificación sustituyen el request único propuesto debajo: [ADR-007](adr/007-real-network.md). Desde I3, cooldown, intentos por ciudad y snapshots viven en Room. Un error de escritura no publica la descarga; ver [ADR-008](adr/008-persistence.md). El detalle siguiente conserva la propuesta original para comparar su evolución.

## 1. Comparación acotada

| Opción | Plan gratuito consultado | Ajuste a ForeKast |
| --- | --- | --- |
| Open-Meteo | Sin clave para acceso gratuito no comercial; pronóstico y geocoding | Elegido por integración inicial sencilla y horizonte requerido |
| WeatherAPI | 100K llamadas mensuales y tres días de pronóstico; requiere integración con clave | Alternativa si se reduce horizonte o se contrata otro plan |
| OpenWeather | Free: condiciones actuales y pronóstico a tres horas por cinco días; One Call es otro producto | Requiere revisar producto y facturación, no asumir que todos sus endpoints comparten plan |

Fuentes: [Open-Meteo](https://open-meteo.com/), [WeatherAPI pricing](https://www.weatherapi.com/pricing.aspx), [OpenWeather pricing](https://openweathermap.org/price). No se propone fallback automático entre proveedores: mezclar sus modelos y semánticas exige evaluación explícita.

Los términos de Open-Meteo limitan el acceso gratuito no comercial a menos de 10.000 llamadas/día, 5.000/hora y 600/minuto; publican además una referencia de 300.000/mes. No asumir un presupuesto independiente por instalación. Los requests amplios pueden computar como varias llamadas. No hay garantía de disponibilidad en el free tier. [Términos](https://open-meteo.com/en/terms), [cómputo y planes](https://open-meteo.com/en/pricing).

**Distinción clave:** la licencia CC BY 4.0 de los datos y el permiso de acceso al servicio gratuito son cosas diferentes. La redistribución de datos con atribución no implica permiso para usar el endpoint gratuito en una app comercial. Acerca de enlazará proveedor y licencia; el detalle meteorológico mostrará atribución breve. Indicar conversiones, redondeos y resúmenes propios. Ver [licencia y planes](https://open-meteo.com/en/pricing).

## 2. Requests propuestos

Geocoding:

```http
GET https://geocoding-api.open-meteo.com/v1/search?name=Montevideo&count=10&language=es&format=json
```

Usar el constructor de parámetros del cliente para escapar texto. La política ForeKast exige tres caracteres, 350 ms de debounce y máximo diez resultados. Campos faltantes de región o país no deben romper una fila; respuesta sin `results` se interpreta como lista vacía. No es reverse geocoding. La semántica del buscador y campos está en la [documentación de geocoding](https://open-meteo.com/en/docs/geocoding-api).

Pronóstico, valores de ejemplo para una ciudad pública:

```text
GET https://api.open-meteo.com/v1/forecast
latitude=-34.90
longitude=-56.16
current=temperature_2m,apparent_temperature,relative_humidity_2m,is_day,weather_code,wind_speed_10m
hourly=temperature_2m,precipitation_probability,weather_code,is_day
daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max
temperature_unit=celsius
wind_speed_unit=kmh
precipitation_unit=mm
timeformat=unixtime
timezone=auto
forecast_days=7
```

Las líneas representan query parameters de una única request. No construir siete llamadas por día ni una por variable. Solicitar siempre unidades canónicas. Los parámetros, series, valores actuales y códigos se contrastan con la [referencia Forecast](https://open-meteo.com/en/docs).

## 3. Normalización y fechas

Política del adapter de ForeKast:

1. Validar zona IANA y unidades. Valores actuales/horarios UNIX se interpretan como segundos desde epoch, no milisegundos. El modelo usa `Instant`; UI aplica `ZoneId` de ciudad.
2. Las fechas de agregados diarios se obtienen según la semántica diaria del proveedor. Con UNIX, comprobar su fecha local frente al equivalente ISO del endpoint en I2, incluyendo cambio horario. No sumar manualmente offset y luego aplicar ZoneId: duplicaría el desplazamiento.
3. No usar `utc_offset_seconds` como regla fija para una semana que cruza DST. La zona IANA conserva reglas de cada fecha. Si la prueba de contrato no permite resolver inequívocamente los días, usar un request diario ISO separado antes de cerrar I2 y registrar el cambio.
4. Emparejar arrays por índice solo tras validar que sus longitudes coinciden con `time`. No hacer `zip` que trunque silenciosamente. Array presente con longitud distinta invalida esa sección; sección ausente produce datos parciales.
5. Una sección estructuralmente inválida no reemplaza la instantánea guardada: resultado `InvalidResponse`. Si la estructura es válida pero hay mediciones nulas, conservarlas como null. Snapshot completamente vacío también es inválido.
6. Validar rangos semánticos: probabilidades y humedad entre 0 y 100, números finitos, fechas ordenadas, tiempos no duplicados. No “corregir” temperaturas extremas con valores inventados.
7. `current` ausente permite mostrar pronóstico sin bloque actual. Campo desconocido se ignora; condición desconocida se conserva con código y etiqueta genérica.
8. Mostrar las primeras 24 muestras con `instant >= now` ordenadas. Al cruzar fecha o zona no asumir que todo día dura 24 horas. En una hora local repetida, distinguir abreviatura/offset y conservar keys basadas en Instant.

El formato y la particularidad de fechas diarias UNIX están documentados por [Open-Meteo](https://open-meteo.com/en/docs). Las comprobaciones son decisiones propias para evitar errores silenciosos. `java.time` es una biblioteca interoperable usada desde Kotlin, no implica escribir Java.

## 4. Condiciones e iconos

El adapter agrupa códigos WMO en Clear, PartlyCloudy, Cloudy, Fog, Drizzle, Rain, Snow, Thunderstorm y Unknown. No interpretar el número como intensidad lineal. Mantener el código original para poder afinar el mapping.

Los vectores iniciales incluyen sol, luna, nube, lluvia, nieve, tormenta, niebla y desconocido. Drizzle puede reutilizar lluvia con etiqueta correcta; PartlyCloudy puede reutilizar nube en el primer build. Día/noche del dato actual usa `is_day`; para filas diarias se usa un icono neutral. El mapping completo se implementa y prueba contra la tabla oficial, no contra el aspecto del icono.

## 5. Transporte, errores y presupuesto

Ktor Client con engine OkHttp, `ContentNegotiation` y kotlinx.serialization. `ignoreUnknownKeys = true`; mantener validación de campos requeridos y tipos. Una instancia compartida del cliente, sin crear una por Composable. Referencias: [requests](https://ktor.io/docs/client-requests.html) y [serialización](https://ktor.io/docs/client-serialization.html).

Políticas propuestas:

| Situación | Conducta |
| --- | --- |
| Conexión | Timeout de conexión 5 s; request individual 8 s |
| Actualización completa | Deadline 20 s incluyendo espera y reintento |
| Timeout/transporte/502/503/504 | A lo sumo un reintento GET con demora de 500–1.000 ms, dentro del deadline |
| Otros 4xx o JSON inválido | No reintentar automáticamente |
| 429 | No reintentar en la operación; guardar cooldown compartido por host |
| Retry-After | Aceptar segundos o fecha HTTP; nunca reintentar antes; si falta/es inválido, 15 minutos |
| Cancelación | Propagar; no convertir en error visible ni reintentar |
| Error de disco | No marcar descarga como aplicada; mantener última instantánea |

Guardar último intento por ciudad y cooldown por host en `refresh_policy`, para no reiniciarlos al recrear la Activity. Un manual refresh no evade un 429. No sumar reintentos de Ktor y del repositorio: la implementación tendrá una sola autoridad. [Soporte de retry de Ktor](https://ktor.io/docs/client-request-retry.html).

## 6. Prueba de integración antes de cerrar I2

Ejecutar una búsqueda y una consulta por HTTPS; guardar fixture con fecha/procedencia. Confirmar campos/unidades, truncar de manera explícita para fixtures pequeños y probar el mapper contra versión completa. Agregar fixture sintético de valores nulos, arrays desalineados y DST. La prueba automatizada cotidiana usa fixtures, no el endpoint público.

Las llamadas reales realizadas durante la preparación del paquete, si las hay, se registran en `VALIDATION.md`; una consulta exitosa no certifica disponibilidad futura ni todas las zonas horarias.
