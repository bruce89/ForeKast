**Corte histórico I2:** la compilación actual ya guarda datos con Room/DataStore. Para entender ese cambio, continuar con [I3](10-I3-persistence.md).

# I2 — Seguir una búsqueda hasta un pronóstico real

Este recorrido usa la app 0.2.0. Primero ejecutar la compilación normal con Internet; después repetir la experiencia con la demo para aislar la red. Ver [toolchain](../docs/TOOLCHAIN.md) y [decisión de red](../docs/adr/007-real-network.md).

## 1. Dos recorridos que se encuentran

Búsqueda: SearchScreen → WeatherViewModel.search → LocationDirectory → RemoteLocationDirectory → OpenMeteoClient → GeocodingDto → Location.

Pronóstico: selección de Location.id → WeatherViewModel.loadSelection → WeatherRepository.refresh → OpenMeteoClient.forecast → ForecastDto + WeatherMapper → WeatherSnapshot → StateFlow → WeatherScreen.

Los archivos están en [feature/search](../app/src/main/kotlin/dev/bruze/forekast/feature/search/SearchScreen.kt), [ViewModel](../app/src/main/kotlin/dev/bruze/forekast/feature/weather/WeatherViewModel.kt), [cliente](../app/src/main/kotlin/dev/bruze/forekast/data/remote/OpenMeteoClient.kt), [mapper](../app/src/main/kotlin/dev/bruze/forekast/data/remote/WeatherMapper.kt) y [repositorio](../app/src/main/kotlin/dev/bruze/forekast/data/remote/RemoteWeatherRepository.kt).

Antes de leerlos, predecí: ¿qué pasa si escribís Madrid, borrás antes de 350 ms y escribís Tokio? ¿Quién cancela? ¿Qué mantiene el contenido anterior cuando falla una actualización? ¿Cambiar a Fahrenheit hace HTTP?

## 2. Puente con iOS

| Android/Kotlin | Analogía útil | Diferencia que conviene estudiar |
| --- | --- | --- |
| suspend + viewModelScope.launch | async/await + Task propiedad del modelo | suspend no cambia de hilo por sí solo; el engine resuelve I/O |
| Job.cancel / CancellationException | Task.cancel / CancellationError | La cancelación es cooperativa: no capturarla como error de producto |
| StateFlow + collectAsStateWithLifecycle | Estado observable consumido por SwiftUI | La colección sigue el lifecycle; ViewModel y proceso tienen vidas distintas |
| @Serializable DTO | Codable DTO | La deserialización tipada no sustituye validación semántica |
| Ktor engine MockEngine | URLProtocol/stub transport | Permite probar requests y respuestas sin tocar el endpoint público |
| Constructor injection + AppContainer | Composition root / factories | Cambiar una dependencia no requiere un framework de DI |
| Instant / LocalDate / ZoneId | Date / componentes y Calendar con TimeZone | Fecha civil y punto en la línea temporal son conceptos diferentes |

## 3. Deserializar no es validar

El servidor puede devolver JSON perfectamente válido con tres tiempos y dos temperaturas. Un zip perdería información silenciosamente. WeatherMapper rechaza longitudes diferentes antes de construir el dominio. Null significa ausencia de medición, no cero. La estructura puede ser parcial: no inventamos un bloque actual si solo llegó pronóstico.

Ejercicio: modificá una copia del fixture para poner humedad 120, una unidad °F o dos tiempos iguales. Predecí qué falla y comprobalo en [OpenMeteoTest](../app/src/test/kotlin/dev/bruze/forekast/OpenMeteoTest.kt). Después agregá un código WMO desconocido: el resultado debe seguir siendo representable como Unknown y preservar el número original.

## 4. Tiempo, una frontera de dominio

Epoch seconds se transforma en Instant. La UI interpreta ese instante con ZoneId de ciudad, nunca con la zona del dispositivo. En Madrid, el cambio de otoño produce dos horas 02:00 con offsets distintos. En primavera no existe una de las horas locales. Las keys de filas son instantes; las etiquetas incluyen offset.

Un agregado diario llega como fecha ISO y se convierte directamente a LocalDate. No se le agrega un offset. El request diario separado hace explícita esta semántica. Buscá las pruebas fallBack y springForward: explicá por qué siete días no implica siempre 168 horas reales en cualquier origen de datos.

## 5. Cancelación, reloj virtual y errores

El ViewModel es dueño del Job de búsqueda y del Job de actualización. Una ciudad nueva cancela la carga anterior. La generación protege la aplicación del resultado; la cancelación evita trabajo innecesario. Son garantías distintas.

El transporte tiene un único lugar para reintentar. Si cada capa reintentara, dos intentos por capa podrían multiplicarse. Con 429 se detiene la operación y se consulta el cooldown antes de nuevos requests; el botón no evade esa política.

En tests, MockEngine usa el mismo TestCoroutineScheduler que runTest. Si su dispatcher usara tiempo real mientras el deadline usa tiempo virtual, el test podría adelantar veinte segundos antes de que responda el mock. Esto fue observado al implementar I2 y corregido inyectando el dispatcher del scheduler de la prueba. No se aumentó el timeout para ocultarlo.

Ejercicios: cambiar 503 por 400 y comprobar que no se reintenta; devolver Retry-After en fecha HTTP; cancelar mientras hay espera de backoff; devolver un JSON roto después de una descarga exitosa y verificar que el snapshot anterior no cambie.

## 6. Fuente de verdad y memoria

El repositorio publica el snapshot mediante Flow. El resultado de refresh informa si se aplicó o falló, sin entregar otra copia del dato a la UI. La sesión usa un directorio de IDs y coordenadas. Sobrevive una recreación de Activity gracias al contenedor de aplicación, pero no un proceso nuevo.

No confundas SavedStateHandle con base de datos: conserva claves pequeñas en circunstancias de restauración del sistema, pero un ID remoto aislado no reconstruye sus coordenadas. I2 vuelve a la ciudad inicial si no puede resolverlo. I3 deberá guardar Location, preferencias, favoritas y snapshot con una política definida y probar cierre/reapertura offline.

## 7. Cómo estudiar este corte

1. Ejecutar y buscar una ciudad fuera del catálogo de demo.
2. Poner breakpoints en search, request, map y snapshots.update.
3. Observar una actualización con éxito; después quitar red y volver a actualizar.
4. Cambiar unidades: confirmar que el conteo de requests no cambia.
5. Ejecutar los tests sin acceso a Internet: los fixtures y MockEngine deben bastar (con dependencias Gradle ya descargadas).
6. Anotar en la bitácora una decisión que tomarías distinto y qué evidencia pedirías antes de cambiarla.

El siguiente corte es I3, no sumar más pantallas todavía: persistencia, restauración y caché offline. Python mantiene su puerto opcional y no participa en el camino crítico meteorológico.
