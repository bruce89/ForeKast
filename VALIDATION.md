# Verificación de ForeKast

## I6a — motor local, 2026-09-28

El puerto de insights ahora compila dentro de la app y tiene una implementación de reglas sin red ni IA. La factoría transforma un `WeatherSnapshot` a la entrada mínima. No hay integración de UI ni servicio Python; la revisión manual de I4 conserva la misma superficie visible. [ADR-010](docs/adr/010-local-insights.md) documenta fórmula y abstenciones.

- `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`: **BUILD SUCCESSFUL**. **45 pruebas JVM, 0 fallos**; nueve casos nuevos cubren resumen/evidencias, frescura, faltantes, ranking, huecos, resolución de 30 minutos, DST, entrada inválida y adaptación del snapshot. Lint: **0 errores, 14 avisos** previos.
- Se inyecta un reloj fijo; no se requiere API meteorológica ni servicio remoto para reproducir estas pruebas.
- No se ejecutaron pruebas instrumentadas para I6a porque no hay cambio de UI ni Android API en el motor. Permanecen pendientes la revisión manual TalkBack de I4 y la medición reproducible de aparición desde caché.

## I4 — 2026-09-28

App 0.4.0. Primera apertura sin ciudad y sin HTTP meteorológico, borrado transaccional de ciudades/pronósticos, preferencias conservadas, búsqueda usable con texto grande y semántica accesible. El esquema Room continúa en v1. [ADR-009](docs/adr/009-mvp-quality.md) explica las decisiones.

- **36 pruebas JVM, 0 fallos.** Se añadieron el inicio vacío/borrado y la respuesta de búsqueda tardía que ignora una consulta obsoleta.
- **Android 8/API 26 live: 11 pruebas ejecutadas, 0 fallos, 9 opt-in/demo omitidas.** Incluye primera apertura y cancelación/confirmación del borrado, reapertura de Room sin selección, cooldown intacto, respuesta inválida y los casos de persistencia previos.
- **Android 8/API 26 demo con fuente 200 % y ventana de 320 dp: 19 pruebas ejecutadas, 0 fallos, 5 live/opt-in omitidas.** Incluye dos Montevideo con identidad distinta, horas/días, error sin caché, límite de favoritas, recreación durante carga sin llamada duplicada y checks automáticos de accesibilidad sobre pronóstico y navegación.
- [Captura de bienvenida en Android 8 con fuente 200 % y 320 dp](docs/screenshots/i4-api26-welcome.png), revisada visualmente. El botón principal aparece sin desplazarse; la explicación continúa debajo en una lista desplazable. Se corrigieron dos problemas encontrados en la prueba: botón oculto bajo el texto largo y resultados de búsqueda detrás del teclado.
- **Dos etapas de proceso en Android 8/API 26, 0 fallos**: seedOnline buscó Lisboa y descargó datos reales; verifyOffline corrió con Wi-Fi/datos desactivados y confirmó PID distinto, misma selección, favorita, Fahrenheit, oscuro y fetchedAt. [Captura de Lisboa reabierta offline](docs/screenshots/i4-api26-offline.png), inspeccionada visualmente. Para la etapa online se usó la opción debug de CA local descrita en TOOLCHAIN; la etapa offline no requirió red.
- **Build y lint final: BUILD SUCCESSFUL**, 0 errores y 14 avisos (13 de versiones disponibles y uno por el directorio local `mipmap-anydpi-v26` redundante para minSdk 26). La auditoría local comprobó 39 Markdown y 185 enlaces locales válidos.

- **APK entregable 0.4.0:** `DEMO=false`, sin `local_debug_ca`; SHA-256 `5F6FE3F4C52CF9B8F4F6C8FD7D2DDE57F4731533EF4218DC874F83246BA6AB72`, igual en build y copia de entrega. El recurso `network_security_config.xml` estándar queda incluido.

El primer intento de repetir la prueba de proceso en Android 17 quedó invalidado: el emulador se cerró; en el segundo inicio el lowmemorykiller del sistema terminó la instrumentación durante la carga. No se atribuye ese fallo a un resultado de la app ni se cuenta como prueba aprobada. Android 8 completó ambas etapas.

La [matriz Q01–Q18](docs/QUALITY.md) indica qué criterios tienen evidencia y cuáles siguen parciales. **Q18 requiere un recorrido manual con TalkBack y revisión de todas las pantallas**: los checks automáticos y capturas no certifican orden hablado, foco o uso real. La meta de tiempo a caché de un segundo tampoco tiene una medición instrumentada confiable; se mantiene como objetivo.

### Reproducción mínima

```powershell
.\scripts\gradle.ps1 :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
.\scripts\gradle.ps1 :app:connectedDebugAndroidTest
.\scripts\gradle.ps1 :app:connectedDebugAndroidTest '-PforekastDemo=true'
```

Ejecutar las dos variantes instrumentadas por separado: `BuildConfig.DEMO` cambia el grafo y Gradle reinstala el APK. Los tests de Room y Ktor usan bases temporales/MockEngine, sin consultas al proveedor. La compilación demo se usa para escenarios sintéticos; el APK entregable es live.

## I3 — 2026-09-25

App 0.3.0. Room 2.8.5, DataStore 1.2.1 y KSP 2.3.12; esquema v1 exportado en app/schemas.

Compilación final: **BUILD SUCCESSFUL**. Lint: **0 errores, 14 avisos** (13 de versiones disponibles y uno por una carpeta local vacía con calificador redundante). No se ocultaron checks. El SHA-256 de la copia entregable coincide con el APK generado.

- **34 pruebas unitarias, 0 fallos**: incluye los 28 casos de I2, cuatro fronteras de CachePolicy y dos casos de restauración/confirmación de preferencias. Se corrigió el wrapper contador de tests para que también observe refreshIfNeeded, añadido en I3.
- **8 pruebas de persistencia en Android 17/API 37, 0 fallos**: reapertura de Room, rollback completo por conflicto en hijos, límite/purga de favoritas, reapertura DataStore, frescura e intentos duraderos, cooldown duradero, conservación offline/vencimiento y solicitudes concurrentes de ciudades distintas.
- **3 recorridos UI demo, 0 fallos**: siguen operando con la compilación demo sin depender de la API pública.
- **2 etapas de proceso, 0 fallos**: seedOnline descarga Lisboa y confirma favorita, Fahrenheit y oscuro; después de force-stop, con Wi-Fi y datos del emulador desactivados, verifyOffline confirma un PID distinto y recupera la misma selección, preferencias, favorita y fetchedAt. No se desinstaló ni borró datos entre etapas. La conectividad se restauró en finally.
- [Captura de Lisboa restaurada offline](docs/screenshots/i3-offline.png), inspeccionada visualmente. La etapa online usó la opción de confianza TLS local de debug descrita en TOOLCHAIN; la etapa offline no tuvo acceso a la API.
- APK 0.3.0 generado con DEMO=false y sin el recurso local_debug_ca. La entrega usa confianza estándar de Android.

La prueba de concurrencia se agregó al detectar que una exclusión global podía descartar una actualización de otra ciudad. El repositorio ahora deduplica por ID y retira ese ID al finalizar o cancelar. La serialización por host del cliente sigue protegiendo el cooldown del proveedor.

**Límites:** no se probó un dispositivo físico/API 26 ni se ejecutó GitHub Actions. No se simuló físicamente falta de espacio ni se certificó accesibilidad con TalkBack; sí se probaron rollback y error de preferencias. No existe todavía una migración v1→v2 que probar, ni se usa fallback destructivo. La retención temporal se comprueba con reloj inyectado; no se esperaron siete días reales. El ensayo de proceso verifica reapertura offline con caché reciente; el caso de caché vieja y fallo de red se prueba en el repositorio con MockEngine.

Los comandos están en [TOOLCHAIN](docs/TOOLCHAIN.md) y el razonamiento en [ADR-008](docs/adr/008-persistence.md). I4 conserva los criterios de MVP y accesibilidad aún pendientes.


## I2 — 2026-09-25

App 0.2.0 con geocoding y pronóstico Open-Meteo.

- `:app:assembleDebug`, `:app:testDebugUnitTest` y `:app:lintDebug`: **BUILD SUCCESSFUL** con configuración live estándar. Lint: **0 errores, 14 avisos** (13 de versiones disponibles y uno por la carpeta local vacía de recursos con calificador redundante). No se ocultaron checks.
- APK verificado: `DEMO=false`, sin recurso `local_debug_ca`; checksum de la copia entregable coincide con el APK generado.
- [Captura real de I2](docs/screenshots/i2-live.png) revisada visualmente: pronóstico de Montevideo, hora de ciudad y descarga, métricas y horas con offset.
- **28 pruebas unitarias, 0 fallos:** fixtures, unidades y fechas, WMO, arrays y rangos inválidos, nulos, DST primavera/otoño, búsqueda y encoding, debounce/cancelación, deadline, reintento acotado, HTTP 400/503/429, Retry-After, JSON inválido y preservación de snapshot.
- **3 pruebas UI demo, 0 fallos**, compilando con `-PforekastDemo=true`: búsqueda, unidades/tema/recreación y error con datos.
- **2 pruebas live opt-in, 0 fallos**, en Android 17/API 37: búsqueda + mapeo HTTPS y recorrido de interfaz para seleccionar Lisboa y ver pronóstico real. No forman parte de la suite cotidiana.
- Requests HTTPS de Montevideo guardadas completas en [fixtures](app/src/test/resources/open-meteo/README.md): 168 horas y 7 fechas diarias. El mapper consume esos cuerpos sin depender del servicio público en tests.
- La primera prueba live falló por `SSLHandshakeException`: el emulador no confiaba en la raíz de inspección HTTPS de Avast. Se verificó usando la opción debug local documentada en [TOOLCHAIN](docs/TOOLCHAIN.md), sin desactivar TLS, modificar certificados del sistema ni distribuir esa raíz.
- La compilación entregable usa confianza estándar Android. La configuración local debug es opcional, queda fuera del APK entregable y nunca se añade al source set release.

Límites: caché/cooldowns en memoria; sin Room/DataStore, restauración duradera, API 26 físico, auditoría TalkBack ni validación remota de GitHub Actions. DST se prueba con datos sintéticos y semánticas explícitas (epoch horario, ISO diario); no se afirma haber consultado el endpoint durante un cambio horario real. El proveedor puede devolver resultados distintos o fallar en otra ocasión.

## Registro histórico I0 + I1

Fecha: 2026-09-24. App 0.1.0-demo, I0 + I1. El servicio Python todavía no está implementado.

## Aplicación Android

- `:app:assembleDebug`: APK generado; se comprobó también una compilación limpia al reorganizar recursos.
- `:app:testDebugUnitTest`: **9 pruebas, 0 fallos**. Conversión/unidades, ciudad y zona temporal, catálogo, deduplicación, cancelación, conservación de datos, recuperación de escenarios, límite de favoritas y reconstrucción de claves.
- `:app:connectedDebugAndroidTest`: **3 pruebas, 0 fallos**, en Medium_Phone, Android 17/API 37. Cubren búsqueda y selección, unidades/tema con recreación de Activity y error con datos guardados.
- `:app:lintDebug`: **0 errores**. Permanecen avisos de versiones más nuevas y una carpeta local de recursos vacía con calificador redundante; no se deshabilitaron checks ni se añadió baseline para ocultar errores.
- APK instalado y abierto en emulador; revisión visual del pronóstico en [tema claro](docs/screenshots/i1-light.png) y [tema oscuro](docs/screenshots/i1-dark.png), incluidas las barras del sistema. Las pruebas instrumentadas no sustituyen una auditoría TalkBack ni toda la matriz de accesibilidad.
- Código y documentación preparados para Git; `local.properties`, confianza TLS local, logs, APKs y reportes están ignorados.

Se fijó Espresso 3.7.0 para corregir la incompatibilidad de la dependencia transitiva anterior con InputManager en Android 17. Los comandos y configuración efectiva están en [TOOLCHAIN](docs/TOOLCHAIN.md). Los reportes se regeneran bajo `app/build/reports/`.

**Límites de I1:** sin red meteorológica, Room, DataStore, Hilt ni Python en runtime. Estado de sesión/SavedState no es persistencia duradera. Las pruebas de recreación no equivalen a muerte real de proceso. No se ejecutó todavía en API 26, en un dispositivo físico ni en GitHub Actions. No se declaran aprobados Q01–Q18 del MVP completo.

## Comprobaciones realizadas

Requests reales por HTTPS a los endpoints propuestos: búsqueda Montevideo devolvió diez resultados; pronóstico devolvió 168 muestras horarias y siete días, zona America/Montevideo y unidades esperadas. Los arrays recibidos tienen longitudes consistentes. La primera fecha diaria interpretada en esa zona fue 2026-09-24.

La preparación original del SPEC revisó 28 documentos y sus enlaces; el repositorio con I1 contiene 32 documentos Markdown cuyos enlaces locales se revisaron nuevamente. Se compilaron seis esquemas JSON mediante Ajv con soporte Draft 2020-12 y formatos; cuatro ejemplos válidos pasaron y cuatro casos negativos fueron rechazados: porcentaje fuera de rango, ventana sin duración, éxito sin insights y abstención sin limitaciones. También se comprobó correlación de IDs, evidencia existente y vigencia temporal del ejemplo de éxito.

Los diez SVG se parsearon y renderizaron con Resvg. El wireframe se exportó a PNG y se inspeccionó visualmente; se ajustó el texto de los estados para que represente contenido de producto. La verificación del contrato cubre esquemas y ejemplos, no una certificación integral de OpenAPI ni una implementación servidor/cliente.

Los escenarios Android de QUALITY.md son criterios futuros y no se reportan como aprobados aquí.

## Límites

Una ciudad y una consulta no validan cambios de horario estacional, todas las regiones, tasas máximas, precisión meteorológica ni disponibilidad futura. I2 conserva una prueba específica de fechas diarias y DST. La baseline compilada de I1 incluía AGP/Kotlin/Compose/Navigation; Room y Hilt se verificarán al incorporarlos. No se desplegó un backend.

Los JSON de insights y las pantallas son sintéticos. Las fuentes externas están fechadas en [SOURCES](docs/SOURCES.md); deben revisarse al fijar dependencias y antes de publicar.
