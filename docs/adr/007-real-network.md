# ADR-007 — Red real, fechas explícitas y memoria de sesión

Estado: aceptado e implementado en I2, 2026-09-25. Amplía [ADR-006](006-first-slice.md) y concreta la selección de [ADR-002](002-network-provider.md).

## Contexto

El primer corte ejercitaba UI con un catálogo fijo. Una ciudad remota necesita identidad, coordenadas y zona horaria; un JSON válido no garantiza datos meteorológicos coherentes. Además, una fecha diaria representa un agregado local y no un instante de observación.

## Decisión

Ktor Client 3.3.3, engine OkHttp y kotlinx.serialization. Una instancia por proceso en AppContainer, sin clientes creados en Composables. Esta versión se fija para mantener el toolchain Kotlin 2.2 existente; actualizarla es una tarea separada que exige compilar y verificar compatibilidad.

WeatherRepository conserva su puerto observable y publica una instantánea solo después de validar ambas respuestas. RemoteLocationDirectory implementa búsqueda y registra ciudades en memoria. El ViewModel se ocupa de debounce (350 ms), cancelación y estado de pantalla. Un contador de generación evita aplicar resultados viejos aunque una dependencia no coopere con cancelación.

Cada actualización pide dos respuestas: actual/horaria en epoch seconds y diaria en ISO local. Evitamos inferir fechas diarias aplicando un offset fijo a una semana que cruza DST. El costo son dos solicitudes y una publicación atómica: si una falla, se conserva la instantánea anterior completa. No afirmamos que los dos cuerpos sean una transacción del proveedor. La fecha de descarga se asigna al publicar; la hora de validez actual se conserva por separado.

La zona solicitada proviene del geocoding (Montevideo inicial tiene coordenadas públicas y zona explícitas). El mapper valida las zonas de ambas respuestas. Las horas conservan Instant, y la UI muestra offset para distinguir horas locales repetidas. Los días usan LocalDate. Secciones ausentes permiten datos parciales; arrays desalineados, duplicados, unidades incorrectas o rangos inválidos rechazan la actualización completa. Se preserva el código WMO original incluso cuando es desconocido.

El transporte es HTTPS exclusivamente, sin claves. Timeout de conexión 5 s, request/socket 8 s y deadline de operación 20 s. Reintento único de errores de transporte y 502/503/504 con 750 ms de espera. OkHttp no añade su propio reintento de conexión. Un 429 registra Retry-After (segundos o fecha HTTP; fallback 15 minutos) por host. La puerta por host impide que llamadas encoladas salteen un cooldown recibido. Cancelación se propaga. No hay retry automático de otros códigos ni de errores de serialización.

Se mantiene la demo con -PforekastDemo=true, seleccionada al compilar. La compilación normal usa red real. No hay fallback a datos inventados ante una falla real. El laboratorio de escenarios solo se muestra en debug demo; las pruebas de UI del primer corte deben compilarse con esa propiedad.

## Alternativas

- Un request con fechas diarias UNIX: ahorra una llamada; requiere probar la semántica de agregados del proveedor en transiciones DST. Se podrá adoptar con evidencia contractual suficiente.
- Retrofit: alternativa válida, pero Ktor permite estudiar engine intercambiable, plugins y MockEngine con el mismo modelo coroutine.
- Room y Hilt ahora: amplían la frontera de aprendizaje. Se posponen a I3, manteniendo inyección explícita por constructor.
- Mezclar demo y live mediante un switch de pantalla: exige aislar cachés, identidades y estado. Una selección de compilación hace la procedencia inequívoca.

## Consecuencias y límites

La caché y el cooldown viven en el proceso, sobreviven recreación de Activity y se pierden al finalizarlo. No se afirma persistencia de favoritas ni selección remota tras muerte de proceso; si su ID no está en el directorio, se vuelve a Montevideo y se descartan favoritas no resolubles. La durabilidad de datos y refresh_policy pertenece a I3. No se implementa trabajo en background, ubicación ni un servidor Python.

La búsqueda descarta entradas sin coordenadas válidas o zona IANA resoluble. La UI comunica errores generales de búsqueda y errores de pronóstico, incluyendo rate limit. Los datos meteorológicos usan atribución Open-Meteo/CC BY 4.0; geocoding acredita GeoNames. La licencia de datos no sustituye las condiciones del endpoint gratuito no comercial.

## Validación y revisión

Fixtures completos obtenidos por HTTPS, tests de mapper, transiciones horarias sintéticas, MockEngine para reintentos/cooldown/JSON/cancelación y conservación del último snapshot. Ver [VALIDATION](../../VALIDATION.md). Reabrir cuando se incorpore Room, se optimice consumo con mediciones o cambie el uso del servicio.
