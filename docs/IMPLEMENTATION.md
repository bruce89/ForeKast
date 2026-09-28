**Actualización I4 (2026-09-28):** Compose, Ktor/Open-Meteo, Room y DataStore están implementados. La app fue probada en API 26. El grafo sigue siendo manual; Hilt es un ejercicio de refactor futuro. Ver [TOOLCHAIN](TOOLCHAIN.md), [ADR-009](adr/009-mvp-quality.md) y [VALIDATION](../VALIDATION.md).

# Preparación de la implementación

Esta página conserva las decisiones iniciales y su secuencia como guía de aprendizaje. La configuración efectiva está en [TOOLCHAIN](TOOLCHAIN.md); I0–I4 ya están implementados. No copiar versiones de ejemplos sobre el catálogo fijado sin comprobar compatibilidad.

## 1. I0: baseline reproducible

Crear proyecto Android Studio estable con Empty Activity/Compose y Kotlin DSL. Elegir namespace propio antes de publicar; `dev.example.forekast` sirve únicamente de ejemplo. `minSdk = 26` es la configuración implementada. Registrar compileSdk, targetSdk, versión de Android Studio, AGP, Gradle y JDK, y comprobar compatibilidad conjuntamente.

AGP 9+ incorpora Kotlin integrado; no copiar automáticamente el plugin `org.jetbrains.kotlin.android` de tutoriales de AGP anteriores. El JDK de Gradle es una herramienta de build y no obliga a escribir Java. Seguir la [guía de Kotlin integrado](https://developer.android.com/build/migrate-to-built-in-kotlin).

## 2. Dependencias objetivo

| Área | Elección | Control de compatibilidad |
| --- | --- | --- |
| UI | Compose + Material 3 + Compose BOM | BOM alinea bibliotecas Compose, no toda la toolchain |
| Estado | Lifecycle/ViewModel + runtime-compose | Verificar APIs de recolección y scope |
| Navegación | Navigation 3, keys tipadas | 1.2.0 figura estable en consulta 2026-09-24; no copiar alpha del ejemplo |
| Asincronía | kotlinx.coroutines + coroutines-test | Misma línea compatible de runtime/tests |
| HTTP | Ktor Client core, OkHttp engine, content negotiation | Todos los artifacts Ktor alineados |
| JSON | kotlinx.serialization JSON + plugin | Plugin de compilación compatible con Kotlin |
| Datos | Room + compiler KSP | Elegir familia coherente; no mezclar imports Room 2 y Room 3 |
| Preferencias | DataStore Preferences | Instancia única por archivo |
| DI | Grafo manual/AppContainer; Hilt como refactor | Mantener scopes explícitos y cambiar solo con un caso concreto |
| Tests | JUnit compatible, Compose test, Ktor mock | No introducir JUnit5/plugins extra sin necesidad |

La documentación consultada de [Room](https://developer.android.com/training/data-storage/room) usa artifacts `androidx.room3` y KSP; tutoriales viejos pueden usar otra familia. [Navigation 3 releases](https://developer.android.com/jetpack/androidx/releases/navigation3) publica canal estable y pre-release por separado. La combinación exacta ya está fijada y compilada; ver [TOOLCHAIN](TOOLCHAIN.md).

Guardar versiones exactas en `gradle/libs.versions.toml`, incluir wrapper y evitar `+`/rangos dinámicos. No actualizar todo durante un laboratorio no relacionado. La matriz efectiva está en [TOOLCHAIN](TOOLCHAIN.md).

## 3. Secuencia técnica

1. Build Compose mínimo + tema + una preview.
2. Contratos de datos + fixtures normalizados + FakeWeatherRepository.
3. Screen/Route/ViewModel y estados; navegación entre pantallas.
4. Cliente y mapper Open-Meteo con fixtures y una prueba real acotada.
5. Room, DataStore y política de caché. Reemplazar adapter en memoria manteniendo la UI.
6. AppContainer conecta implementaciones; revisar scopes. Hilt puede estudiarse como refactor aislado.
7. Accesibilidad, restauración y pruebas del recorrido completo.

Permiso INTERNET al incorporar red. No son necesarios permisos de ubicación para ciudades buscadas. No habilitar cleartext global; un backend local de aprendizaje, si lo necesita, tendrá una configuración limitada a debug.

## 4. Comandos de referencia en Windows

Desde la raíz de este repo:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

El último requiere dispositivo/emulador disponible y tareas correspondientes a la configuración final. Registrar resultados reales antes de marcar una iteración terminada.

## 5. Qué mirar durante ingeniería inversa

Abrir una Screen, localizar su UiState, encontrar quién lo produce, seguir una acción hasta el repositorio y observar la emisión de regreso. Cambiar primero el fake para entender la UI; luego el mapper para entender el contrato externo; finalmente el DAO para entender persistencia. La guía detallada está en [LearnDocs](../LearnDocs/README.md).
