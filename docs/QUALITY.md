# Calidad, aceptación y verificación

Estos son los escenarios de aceptación del MVP. Las pruebas y la revisión visual realizadas se registran en [VALIDATION](../VALIDATION.md); una prueba automatizada no equivale a una certificación de uso con TalkBack.

## 1. Escenarios de aceptación

| ID | Preparación y acción | Resultado requerido | Nivel de prueba |
| --- | --- | --- | --- |
| Q01 | Primera apertura, sin datos | Inicio permite buscar sin pedir ubicación | UI |
| Q02 | Buscar dos ciudades homónimas | Se distinguen región/país e identidad | Mapper + UI |
| Q03 | Escribir A, luego B; A responde última | Solo B puede publicar resultados | Unitario con tiempo virtual |
| Q04 | Menos de 3 caracteres o escritura continua | No hay requests prematuras | Unitario |
| Q05 | Datos completos; elegir ciudad | Actual, hasta 24 horas futuras y 7 días | Integración + UI |
| Q06 | Refresh simultáneo o doble gesto | Máximo una llamada en vuelo por ciudad | Repositorio |
| Q07 | Rotar con petición en vuelo | Conserva estado y no duplica request | Instrumentado |
| Q08 | Reabrir proceso sin red con caché | Contenido guardado, sin spinner infinito | Instrumentado |
| Q09 | Error sin caché / con caché | Reintento total / aviso con datos conservados | ViewModel + UI |
| Q10 | 29:59, 30:00, 5:59:59, 6:00:00 desde fetch | Frescura y refresh respetan fronteras | Unitario con Clock |
| Q11 | Teléfono en otra zona; ciudad cruza medianoche/DST | Fechas de ciudad, horas repetidas distinguibles | Mapper + unitario |
| Q12 | Campo null, código nuevo, sección ausente | Ausencia explícita, fallback y datos disponibles | Mapper |
| Q13 | Arrays distintos, JSON inválido o unidades inesperadas | No se sustituye caché válida | Integración |
| Q14 | Timeout, 503, 429 y cancelación | Presupuesto acotado, cooldown y cancelación correctos | Ktor mock + repositorio |
| Q15 | Cambiar °C/°F sin red y reiniciar | Conversión local y preferencia persistida | Unitario + almacenamiento |
| Q16 | Cinco favoritos, agregar sexto, quitar activo | Límite claro; quitar favorito no borra selección | Datos + UI |
| Q17 | Escritura interrumpida o migración | Sin mezcla de snapshots ni pérdida silenciosa de favoritos | Room |
| Q18 | TalkBack, fuente 200 %, oscuro, ventana pequeña | Lectura y acciones utilizables sin recortes críticos | Manual + UI |

I5 añade rechazo/revocación de permisos, ubicación aproximada y timeout. I6/I7 añaden el conjunto de evaluación de insights; no retrasan el MVP.

## 2. Estrategia

Unitarias JVM para conversiones, frescura, mapper, búsqueda y coordinación. Usar `kotlinx-coroutines-test`, reloj falso y repositorios falsos; el scheduler debe controlar debounce y retry. Si un StateFlow usa `WhileSubscribed`, la prueba necesita un collector activo.

Pruebas de cliente con Ktor MockEngine, fixtures válidos y corruptos. Room requiere verificar operaciones reales y transacciones con una base de prueba; no basta un mock del DAO para afirmar integridad. UI Compose comprueba decisiones visibles y semántica, sin probar cada modifier.

Los fixtures sintéticos y cuerpos HTTPS capturados viven en `app/src/test/resources/`; su [README](../app/src/test/resources/open-meteo/README.md) distingue procedencia y uso. Cada archivo indica si viene de proveedor o fue construido. Separar respuesta de red de tiempo del reloj: no hacer depender el test del día en que se ejecuta.

## 3. Muerte del proceso y lifecycle

Recomposición, recreación de Activity, muerte de proceso y force-stop son experimentos distintos. `ActivityScenario.recreate()` comprueba recreación, no muerte de proceso. Probar recuperación real enviando la app al background, terminando su proceso mediante herramientas de Android y abriendo de nuevo. Registrar dispositivo/API y procedimiento utilizado.

Verificar que SavedState restaure claves pequeñas si el sistema lo permite y que Room/DataStore basten para recuperar selección y datos duraderos. No prometer que un query transitorio sobrevive a un force-stop explícito.

## 4. Definition of Done por entrega implementada

Compila desde un checkout limpio con wrapper y catálogo fijados; lint y pruebas relevantes pasan; se puede ejecutar en el emulador mínimo soportado y uno actual. Las funcionalidades modificadas tienen criterio de aceptación comprobado. Al cambiar persistencia hay esquema y migración cuando corresponde. No se incluyen secretos ni logs con ubicaciones.

Cada corte incluye instrucciones reproducibles, screenshots de estados afectados y una nota de aprendizaje con el recorrido de un dato. La documentación refleja limitaciones reales. Los comandos de referencia están en [IMPLEMENTATION](IMPLEMENTATION.md).

## 5. Métricas y observabilidad

Medir por separado tiempo a caché, duración HTTP y aplicación de datos. Para debug registrar evento, duración, resultado y requestId aleatorio; omitir query, coordenadas y body. No añadir analytics de terceros en MVP. Evaluar recomposiciones/carga solo ante un problema observado; no optimizar todo preventivamente.

Calidad meteorológica y calidad de software son evaluaciones diferentes: tests que demuestran un mapper correcto no prueban que lloverá. Si se compara precisión posteriormente, guardar el pronóstico tal como fue emitido y contrastarlo con observaciones apropiadas para evitar evaluar con datos revisados.
## 6. Matriz de evidencia I4

Esta matriz distingue **automatizado**, **inspeccionado** y **pendiente**. Los tests corren con red simulada salvo los casos live opt-in de I2/I3. El detalle de ejecución, API y comandos está en [VALIDATION](../VALIDATION.md).

| Criterio | Evidencia | Estado |
| --- | --- | --- |
| Q01 | `LiveLocalFlowTest`, instalación limpia Android 8 y captura al 200 % | Automatizado + inspeccionado |
| Q02 | Mapper preserva ID/región/país; UI demo busca ambos Montevideo y selecciona el de Minnesota | Automatizado en Android 8 |
| Q03 | `WeatherViewModelTest` cubre cancelación al elegir ciudad y resultado tardío de búsqueda | Automatizado |
| Q04 | Test de debounce 350 ms y consultas cortas con tiempo virtual | Automatizado |
| Q05 | Fixture 168 horas/7 días y UI demo verifica 24 horas, 7 fechas, sección horaria, tarjeta diaria y probabilidad | Automatizado en Android 8 |
| Q06 | Dedupe en ViewModel y repositorio; concurrencia de ciudades distintas | Automatizado |
| Q07 | Test de recreación en carga demo conserva ViewModel y cuenta refresh | Automatizado |
| Q08 | `ProcessPersistenceTest` I4 reinicia proceso en Android 8 sin red y confirma PID nuevo y el mismo snapshot | Automatizado en API 26 |
| Q09 | Demo UI con y sin caché, reintento/cambio de ciudad; repositorio preserva último éxito | Automatizado en Android 8 |
| Q10 | `CachePolicyTest` cubre fronteras exactas y reloj inverso | Automatizado |
| Q11 | Mapper con fechas locales y hora DST repetida + offset mostrado | Automatizado |
| Q12 | Mapper con nulos, sección ausente y WMO desconocido | Automatizado |
| Q13 | Respuestas inválidas y transacción dejan el snapshot anterior | Automatizado |
| Q14 | MockEngine prueba deadline, 503, 429/Retry-After y cancelación | Automatizado |
| Q15 | Preferencias/DataStore + etapa offline I3; el cambio de unidad no llama a red | Automatizado |
| Q16 | Room limita cinco y conserva selección; UI muestra límite y deshabilita agregar sexto | Automatizado en Android 8 |
| Q17 | Rollback por inserción de hijo duplicado; esquema v1 exportado | Automatizado para rollback; migración no aplica hasta v2 |
| Q18 | Fuente 200 %, ventana 320 dp y dark; checks automáticos Compose | **Pendiente** recorrido manual TalkBack y revisión completa de pantallas |

**Puerta de aceptación:** I4 es un corte implementado y verificable, pero el MVP no se considera certificado hasta cerrar el recorrido manual Q18 y cualquier medición de experiencia que se declare obligatoria. No convertimos ausencia de evidencia en un “pasa”.
