# I3 — Reconstruir una app después de que su proceso dejó de existir

La pregunta central de este corte es: ¿qué información alcanza para reconstruir la pantalla sin Internet ni memoria anterior? Abrí [ADR-008](../docs/adr/008-persistence.md) y comparalo con [I2](09-I2-network.md).

## 1. Nuevo recorrido del dato

Antes: HTTP → mapper → mapa en memoria → StateFlow → pantalla.

Ahora: HTTP → mapper → transacción Room → consulta Flow → ViewModel → pantalla. El resultado de refresh informa éxito/falla, pero no trae una segunda copia observable del dato. Si falla la escritura, la pantalla conserva la versión comprometida anteriormente.

Archivos para recorrer:

- [Entidades](../app/src/main/kotlin/dev/bruze/forekast/data/local/Entities.kt): representación del disco y claves foráneas.
- [DAO y base](../app/src/main/kotlin/dev/bruze/forekast/data/local/ForeKastDatabase.kt): consultas, transacciones y versión del esquema.
- [Mapeos locales](../app/src/main/kotlin/dev/bruze/forekast/data/local/LocalMappings.kt): Instant/LocalDate/ZoneId hacia primitivas almacenables.
- [Repositorio persistente](../app/src/main/kotlin/dev/bruze/forekast/data/local/PersistentWeatherRepository.kt): frescura, descarga, validación y commit.
- [Preferencias](../app/src/main/kotlin/dev/bruze/forekast/data/local/DataStorePreferences.kt): Flow y edit para unidad/tema.
- [ViewModel](../app/src/main/kotlin/dev/bruze/forekast/feature/weather/WeatherViewModel.kt): restauración y acciones.
- [Política de caché](../app/src/main/kotlin/dev/bruze/forekast/core/model/CachePolicy.kt): decisiones temporales sin Android.

## 2. Puentes con iOS

| Concepto | Puente útil | Pregunta para evitar una equivalencia falsa |
| --- | --- | --- |
| Room sobre SQLite | Core Data/SwiftData o una capa SQLite | ¿Qué garantiza la transacción y qué parte es solo observación? |
| DAO + Flow | Consulta observable / resultados obtenidos | ¿La UI lee una versión completa o puede mezclar hijos nuevos y viejos? |
| DataStore | Preferencias persistentes, cercanas en propósito a UserDefaults | ¿Cuándo se confirma la escritura asíncrona y cómo se comunica una falla? |
| SavedStateHandle | Restauración de estado de interfaz | ¿Puede reconstruir una ciudad si sus coordenadas solo estaban en memoria? |
| ViewModel retenido | Modelo que sobrevive reconstrucción de la vista | ¿Quién lo vuelve a crear tras morir el proceso? |
| KSP y esquema exportado | Generación de código y versionado del modelo | ¿Qué se genera y qué archivo forma parte del contrato durable? |

No hay un equivalente exacto para todos los scopes. Room crea implementaciones de DAO en build; no consulta la base durante compilación. El JSON exportado sirve para revisar y probar cambios de estructura, no es una copia de los datos del usuario.

## 3. Transacciones: una regla de producto

Seleccionar otra ciudad implica guardar sus datos, apuntar la selección a ella y retirar ciudades que ya no se retienen. Si esos pasos fueran independientes, un cierre podría dejar una referencia rota. La transacción los convierte en una sola modificación visible.

El mismo argumento aplica al pronóstico: header, horas y días representan una versión. Leé replace en ForeKastDao y la prueba failedChildInsertRollsBackHeaderAndAllRows. Se fuerza un conflicto de clave primaria después de cambiar la cabecera; al consultar, el snapshot original debe seguir completo. Esa prueba defiende una garantía y no se limita a comprobar que se llamó a un método.

Ejercicio: predecí qué rompería eliminar @Transaction. Hacé el experimento en una rama y explicá el resultado. Después restaurá la implementación correcta.

## 4. Una sola fuente observable

La descarga no modifica UiState directamente. El Flow de Room entrega el nuevo snapshot después del commit. Updated significa persistido. Cambiar favorita o preferencia sigue la misma idea: esperar el dato confirmado. El usuario puede observar una demora pequeña de escritura, pero no un éxito falso si el disco falla.

Ejercicio: seguí una selección con debugger. Identificá el punto donde cambia SQLite, el punto donde emite Flow y el punto donde loadSelection cambia la suscripción meteorológica. Explicá qué cancela la solicitud anterior y qué evita que reaparezca una ciudad purgada.

## 5. Cuatro tiempos distintos

- validAt: instante al que corresponde la condición actual.
- fetchedAt: instante de descarga validada y guardada.
- attemptedAt: intento, aunque haya fallado.
- cooldown until: momento antes del cual el proveedor no admite otra consulta.

Una descarga reciente no implica que el modelo meteorológico se haya actualizado. Un intento fallido no rejuvenece fetchedAt. Y quitar la app de memoria no elimina la espera del proveedor.

Revisá las fronteras 29:59/30:00, 5:59:59/6:00:00, 6 días 23:59:59/7 días y 59/60 segundos en CachePolicyTest. Si el reloj retrocede, ¿qué regla evita llamar fresca a una descarga cuya edad ya no es confiable?

## 6. Tres experimentos de restauración

1. Recrear Activity: prueba ciclo de configuración; el proceso sigue vivo.
2. Cerrar y volver a abrir Room/DataStore: prueba archivos y serialización; no certifica por sí sola el proceso completo.
3. force-stop y relanzar sin red: prueba que la pantalla pueda reconstruirse con un proceso nuevo y almacenamiento durable.

En el tercer experimento se conserva la app instalada y no se usa pm clear. Desinstalar o borrar datos es otra operación: elimina deliberadamente la base. Ver las instrucciones de [TOOLCHAIN](../docs/TOOLCHAIN.md).

Antes de cerrar: elegir una ciudad fuera de la demo, guardarla, cambiar a Fahrenheit y tema oscuro, y esperar el pronóstico. Después de reabrir offline: verificar la misma ciudad/favorita/unidad/tema y el mismo fetchedAt. Si la caché es reciente, no debe intentarse actualización automática; si envejeció, debe mostrarse mientras el intento falla.

## 7. Migraciones y evolución

I3 introduce el esquema v1. No hay una base de I2 que transformar. Cuando agregues una columna, no borres la base para hacer desaparecer el problema: incrementá versión, escribí una migración compatible, exportá el nuevo esquema y probá con datos de v1.

Ejercicio futuro: agregar unidad de viento en DataStore. Después comparar con agregar una columna persistida a Room. ¿Cuál necesita migración estructural? ¿Qué valores por defecto preservan el significado de datos existentes?

## 8. Checklist de comprensión

Podés explicar por qué selección está en Room y tema en DataStore; por qué conservar cinco favoritas no obliga a que la ciudad activa sea favorita; por qué un JSON cacheado en preferencias sería una decisión distinta; por qué cambiar unidades no hace HTTP; y qué ocurre si falla un commit.

Siguiente corte recomendado: I4, accesibilidad, pruebas en API mínima, errores de almacenamiento y pulido. Hilt puede estudiarse como refactor controlado del grafo existente. El contrato Python continúa independiente de persistencia meteorológica.
