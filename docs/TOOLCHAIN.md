# Toolchain de I0 + I1

Baseline fijada el 2026-09-24. Las versiones viven en [libs.versions.toml](../gradle/libs.versions.toml), el wrapper y la configuración del módulo. No se usan versiones dinámicas.

| Componente | Versión/configuración |
| --- | --- |
| Gradle wrapper | 9.5.0 |
| Android Gradle Plugin | 9.3.1, Kotlin integrado |
| Plugins Compose / serialization | 2.2.21 |
| Compose BOM | 2026.09.00 |
| Activity Compose | 1.13.0 |
| Lifecycle | 2.11.0 |
| Navigation 3 | 1.2.0 |
| Coroutines | 1.10.2 |
| kotlinx.serialization | 1.9.0 |
| AndroidX Test / Espresso | Runner 1.7.0, JUnit extension 1.3.0, Espresso 3.7.0 explícito |
| compileSdk / targetSdk | 37 |
| SDK platform instalada | android-37.0 |
| minSdk | 26 |
| Build tools | 36.0.0 |
| Java/Kotlin bytecode target | 17 |
| JDK usado localmente | Android Studio JBR 25.0.2 |
| Android Studio local | 2026.1.3, build 261.26222.65.2613.15948027 |

El JDK que ejecuta Gradle y el target de bytecode cumplen funciones distintas. El proyecto no añade el plugin Kotlin Android tradicional porque AGP ya integra Kotlin. El Java generado por BuildConfig no es código de aplicación escrito a mano.

Espresso se declara explícitamente: la dependencia transitiva anterior intentaba usar `InputManager.getInstance` por reflexión, incompatible con el emulador Android 17. La versión 3.7.0 contiene la corrección documentada en [AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test). Se usa el helper Compose test `junit4.v2`.

## Abrir y ejecutar

Abrir la raíz `ForeKast` en Android Studio y sincronizar Gradle. Seleccionar el JDK de Android Studio y disponer de SDK platform 37.0 + build tools 36.0.0. Android Studio puede crear `local.properties` con la ruta local del SDK; ese archivo está ignorado por Git.

En PowerShell, el helper detecta el JDK de una instalación estándar de Android Studio:

```powershell
.\scripts\gradle.ps1 :app:assembleDebug
.\scripts\gradle.ps1 :app:testDebugUnitTest :app:lintDebug
.\scripts\gradle.ps1 :app:connectedDebugAndroidTest
```

El último requiere un emulador/dispositivo conectado. Si JAVA_HOME ya está configurado, se puede usar directamente `gradlew.bat`. En Linux/macOS usar `bash ./gradlew` y configurar SDK/JDK propios.

APK debug: `app/build/outputs/apk/debug/app-debug.apk`. Reportes: `app/build/reports/`. Todos son generados e ignorados por Git.

## Certificados locales

En este equipo, Avast intercepta TLS y su raíz ya está confiada por Windows. Para que Gradle descargara dependencias se creó una copia local del truststore del JDK con esa misma raíz pública, en `work/build-truststore`. El helper la usa solo durante su ejecución y restaura variables al salir. No se desactivó validación HTTPS ni se modificó el JDK global.

La copia local y los logs están ignorados. Otro equipo normalmente no necesita esa configuración. Si aparece un problema de certificados, revisar la cadena de confianza propia; no copiar certificados ajenos ni deshabilitar validación TLS. Android Studio, si requiere nuevas descargas en este equipo, necesitará la configuración de confianza equivalente o que se ejecuten mediante el helper.

## CI

El workflow [android.yml](../.github/workflows/android.yml) prepara JDK/SDK y ejecuta build, unit tests y lint en GitHub. Queda preparado para el primer push; todavía no se ejecutó en GitHub. Pruebas instrumentadas se corren localmente por ahora. Las versiones mínimas y compatibilidad de AGP se consultaron en [release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes).

## I2: red real y demo reproducible

Ktor 3.3.3 (core, OkHttp, content negotiation, kotlinx JSON y MockEngine), kotlinx.serialization JSON 1.9.0. App 0.2.0. La compilación normal accede a Open-Meteo por HTTPS sin clave ni permiso de ubicación.

```powershell
.\scripts\gradle.ps1 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\scripts\gradle.ps1 -PforekastDemo=true :app:connectedDebugAndroidTest
.\scripts\gradle.ps1 :app:assembleDebug
```

El último comando vuelve a generar el APK live; ambas configuraciones comparten applicationId y ruta de salida. Para estudiar demo sin tests: agregar `-PforekastDemo=true` a assembleDebug. No guardar esta propiedad globalmente si se desea ejecutar live desde Android Studio. Los tests de UI sintéticos se omiten cuando DEMO=false; las pruebas unitarias prueban ambas fronteras mediante inyección.

### Prueba live opcional y confianza local

Las pruebas LiveWeatherSmokeTest y LiveWeatherUiTest requieren el argumento liveWeather=true. No se ejecutan en la suite habitual: consumen requests del proveedor y dependen de Internet. La segunda recorre Lisboa, fuera del catálogo demo.

En este equipo se comprobó SSLHandshakeException en el emulador por inspección HTTPS de Avast. Para verificar sin desactivar TLS se añadió una opción explícita, solo debug:

```powershell
.\scripts\gradle.ps1 :app:connectedDebugAndroidTest '-PforekastLocalCa=work/local-root.cer' '-Pandroid.testInstrumentationRunnerArguments.class=dev.bruze.forekast.LiveWeatherSmokeTest,dev.bruze.forekast.LiveWeatherUiTest' '-Pandroid.testInstrumentationRunnerArguments.liveWeather=true'
```

La raíz pública de confianza ya existente en Windows se usa únicamente en recursos generados de esta compilación debug. El archivo work/local-root.cer está ignorado, no se comparte y no se modifica la confianza del emulador ni de Windows. En otra red normalmente se omite forekastLocalCa. Nunca usar un certificado cuyo origen no se conoce.

Para ejecutar manualmente en este emulador: assembleDebug con esa misma propiedad y luego instalar el APK. Android Studio puede recibirla en la configuración Gradle local del desarrollador. **El APK entregable se recompila sin esa propiedad y se comprueba que no incluya local_debug_ca.** Release no añade esta fuente debug. La app siempre conserva la verificación de hostname/cadena y prohíbe HTTP sin cifrar.

## I3: base durable y pruebas de proceso

App 0.3.0; Room 2.8.5, KSP 2.3.12, Preferences DataStore 1.2.1. El plugin Room exporta el esquema v1 en app/schemas; ese directorio sí se versiona. Los archivos de base y preferencias se crean en el almacenamiento privado de la app, no en el repositorio. No hay migración desde I2 porque solo usaba memoria. No se habilita migración destructiva.

Suite habitual, sin solicitudes públicas:

```powershell
.\scripts\gradle.ps1 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\scripts\gradle.ps1 -PforekastDemo=true :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=dev.bruze.forekast.PersistenceTest,dev.bruze.forekast.ForeKastFlowTest'
```

PersistenceTest usa bases temporales separadas, DataStore temporal y MockEngine; limpia únicamente sus archivos de prueba. Incluye rollback, reapertura, frescura, intentos/cooldown duraderos y concurrencia entre ciudades. Los tests unitarios cubren CachePolicy y confirmación de preferencias sin HTTP. El workflow existente ejecuta build/unit/lint; estos tests de dispositivo se ejecutaron localmente, no se afirma ejecución de GitHub Actions.

### Ensayo opt-in con cierre de proceso

Usar un emulador de desarrollo dedicado. Las etapas seedOnline/verifyOffline cambian ciudad, favorita, unidades y tema; seedOnline consume consultas públicas. Se ejecutan separadamente mediante adb para que Gradle no desinstale la app entre etapas. Ambas se omiten en la suite normal.

1. Compilar assembleDebug y assembleDebugAndroidTest en modo live; agregar forekastLocalCa solamente si este entorno lo requiere.
2. Instalar ambos APK con adb install -r. Mantener la app instalada entre etapas; no usar pm clear.
3. Ejecutar seedOnline. Espera datos reales de Lisboa, favorita confirmada, Fahrenheit y oscuro. Guarda ID, fetchedAt y PID en un archivo de evidencia dentro del almacenamiento de prueba de la app.
4. Ejecutar am force-stop sobre dev.bruze.forekast. Desactivar Wi-Fi y datos del emulador temporalmente.
5. Ejecutar verifyOffline. Compara el PID nuevo, la ciudad, favorita, unidades, tema y fetchedAt sin cambios; guarda captura.
6. Restaurar Wi-Fi/datos en un finally, incluso si la prueba falla.

Comandos de instrumentación (adb debe estar en PATH o usar su ruta del SDK):

```powershell
adb shell am instrument -w -e class 'dev.bruze.forekast.ProcessPersistenceTest#seedOnline' -e processPersistence true dev.bruze.forekast.test/androidx.test.runner.AndroidJUnitRunner
adb shell am force-stop dev.bruze.forekast
# Desactivar red en el emulador y restaurarla después de verifyOffline.
adb shell am instrument -w -e class 'dev.bruze.forekast.ProcessPersistenceTest#verifyOffline' -e processPersistence true dev.bruze.forekast.test/androidx.test.runner.AndroidJUnitRunner
```

La etapa seed supone una instalación de desarrollo sin preferencias anteriores. Para un ensayo manual sobre datos propios, seguir el recorrido de pantalla y no ejecutar scripts que cambien esos datos. El APK entregable se vuelve a compilar con DEMO=false y sin certificado local.

## I4: emulador mínimo y calidad visible

Versión 0.4.0, `minSdk=26` sin cambio de esquema Room. La primera instalación live abre la bienvenida y no consulta pronóstico hasta seleccionar una ciudad. En Ajustes, el diálogo **Borrar ciudades y pronósticos** elimina selección, favoritas y caché en una transacción, y conserva unidad, tema y cooldown por host. [ADR-009](adr/009-mvp-quality.md) detalla el alcance.

La prueba en Android 8/API 26 usa `ForeKast_API26` (imagen oficial `system-images;android-26;default;x86_64`). `connectedDebugAndroidTest` live ejecuta persistencia y primera apertura; la variante `-PforekastDemo=true` ejecuta además navegación, homónimos, recreación en carga y checks automáticos de accesibilidad. Las pruebas opt-in de red y muerte de proceso se invocan aparte. API 26 es el mínimo de la app, no una recomendación general de Android.

```powershell
.\scripts\gradle.ps1 :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
.\scripts\gradle.ps1 :app:connectedDebugAndroidTest
.\scripts\gradle.ps1 :app:connectedDebugAndroidTest '-PforekastDemo=true'
```

La captura [bienvenida a 200 % en 320 dp](screenshots/i4-api26-welcome.png) muestra el botón de búsqueda en la primera vista; el texto explicativo continúa desplazándose. La búsqueda oculta el teclado al recibir resultados para que puedan revisarse en ventanas pequeñas. Ver [QUALITY](QUALITY.md) para los criterios aún pendientes de revisión manual con TalkBack.
