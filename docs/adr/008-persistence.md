# ADR-008 — Room como fuente de datos y DataStore para preferencias

Estado: aceptado en I3, 2026-09-25. Sustituye la memoria temporal de [ADR-007](007-real-network.md) en la compilación live; conserva la demo en memoria. Concreta la propuesta de [ADR-003](003-data-boundaries.md).

## Contexto

Una selección restaurada necesita coordenadas, zona e identidad. Guardar solo el ID en SavedStateHandle no permitía reabrir offline. Además, un pronóstico recibido por HTTP no puede considerarse guardado si falla su escritura. Favoritas y selección tienen invariantes conjuntas; las preferencias visuales no dependen de sus claves foráneas.

## Decisión

Room 2.8.5, KSP 2.3.12 y Preferences DataStore 1.2.1, con grafo manual en AppContainer y scope de aplicación. La base usa el contexto de aplicación. La extensión preferencesDataStore crea una única instancia por archivo. Ninguno se crea en un Composable. El dominio conserva puertos Kotlin sin anotaciones Room ni imports Android.

Room contiene locations, app_selection, weather_snapshots, hourly_weather, daily_weather, refresh_policy y host_cooldowns. app_selection tiene una fila singleton y referencia a locations. Cambiar selección inserta/actualiza la ciudad, cambia esa referencia y purga ciudades no favoritas que dejaron de estar activas dentro de una transacción. La limitación a cinco favoritas y su orden también se resuelven transaccionalmente. Se retienen como máximo cinco favoritas más una activa no favorita.

La consulta observable de biblioteca usa un único SELECT con JOIN para entregar ciudades y selección coherentes. El ViewModel aplica esa lectura, no un resultado optimista paralelo. Los resultados de búsqueda solo viven en memoria hasta seleccionar una ciudad. Retirarla de favoritas no borra la ciudad activa; cambiar la selección sí permite purgar una ciudad que ya no esté retenida. Las claves foráneas eliminan sus snapshots e intentos en cascada.

Cada ciudad tiene una cabecera de snapshot; hijos horarios y diarios usan claves compuestas locationId + instante/fecha. Esto simplifica el snapshotId propuesto originalmente porque I3 retiene una sola versión por ciudad. Reemplazar cabecera e hijos ocurre en una transacción, y la lectura agregada usa @Transaction. Un error o cancelación no deja media actualización. Si en el futuro se necesita historial, se agregará una identidad de corrida mediante migración.

PersistentWeatherRepository publica únicamente el Flow de Room. refresh transforma y valida la respuesta remota, la guarda y devuelve Updated solo después del commit. Una falla de transporte, mapeo o disco conserva la descarga anterior. RemoteWeatherRepository se conserva como ejemplo del corte I2 y para sus pruebas aisladas; AppContainer live construye el repositorio persistente.

DataStore conserva temperatura y tema. No contiene referencias a ciudades: evitamos necesitar una transacción entre dos sistemas. Los cambios se muestran al observar la escritura confirmada. SavedStateHandle mantiene su uso en la demo; la compilación live reconstruye selección, favoritas y preferencias desde almacenamiento.

## Frescura y límites de consumo

CachePolicy es Kotlin puro con Instant inyectado: menos de 30 minutos reutiliza; desde 30 minutos refresca al entrar/volver al foreground; desde seis horas avisa antigüedad; desde siete días la instantánea no se utiliza y se purga al actualizar/consultar. El ticker de UI también retira contenido vencido, sin hacer HTTP. Las fechas diarias pasadas se excluyen de Próximos días.

Cada ciudad registra su intento antes de consultar red, incluso si falla. Un nuevo intento requiere 60 segundos. Los 429 se guardan por host y se verifican también después de crear un cliente nuevo. Un gesto manual no evita esos controles. Las preferencias y favoritas no disparan HTTP. No hay polling ni sincronización background.

Si el reloj retrocede, la caché se considera de antigüedad desconocida y se permite un intento correctivo, cuyo timestamp inicia de nuevo la separación de 60 segundos. Un cooldown del proveedor conserva su fecha límite. Esto no promete inmunidad a manipulación del reloj; evita bucles normales tras una corrección horaria.

## Errores y ciclo de vida

Las lecturas y escrituras son suspend/Flow. Room y DataStore ejecutan el acceso en sus contextos adecuados; no se usa allowMainThreadQueries. El ViewModel cancela trabajos al cambiar ciudad o desaparecer. Las escrituras de acciones se serializan para respetar el orden de interacción. La UI comunica fallas de almacenamiento sin borrar datos automáticamente ni presentar un cambio como guardado antes de confirmarse.

La restauración inicial espera biblioteca y preferencias antes de mostrar contenido. Si no existe almacenamiento previo se crea Montevideo como selección. I2 no tenía una base durable que migrar. Una lectura inicial fallida muestra error; se puede reintentar al abrir nuevamente, sin reemplazar silenciosamente archivos corruptos.

## Alternativas y consecuencias

- Serializar todo en preferencias habría ocultado invariantes relacionales y exigido reescribir grandes bloques.
- Guardar selección en DataStore habría separado su referencia de la transacción de ciudades.
- Publicar red primero y guardar después habría hecho indistinguibles descarga y persistencia exitosa.
- Incorporar Hilt simultáneamente añade otra frontera de aprendizaje. Se conserva composición explícita; introducirlo queda como ejercicio posterior.

Se exporta esquema v1 bajo app/schemas y se incluye en control de versiones. No se usa fallbackToDestructiveMigration. Una versión futura requiere migración y tests con el esquema anterior. No se afirma haber probado una migración v1→v2 inexistente. La app continúa con backup del sistema desactivado; borrar datos o desinstalar elimina el almacenamiento local.

## Validación y cuándo revisar

Pruebas de fronteras temporales, reapertura de Room/DataStore, rollback ante hijos duplicados, límite/purga de favoritas, cooldown e intentos después de recrear repositorios y conservación offline. El ensayo de proceso usa force-stop y una reapertura sin red; ver [VALIDATION](../../VALIDATION.md) para resultados y límites concretos.

Reabrir al añadir historial, varios procesos, sincronización background, ubicación, más ciudades o intercambio de datos entre dispositivos. Hilt y modularización deben resolver una necesidad demostrable, no cambiar las reglas de persistencia.
