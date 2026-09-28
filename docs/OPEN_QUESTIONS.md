# Supuestos revisables y registro de cambios

## Defaults usados para avanzar

Español inicial, sistema métrico, tema del sistema, búsqueda manual, máximo cinco favoritos y app educativa no comercial. Nivel de aprendizaje provisional: experiencia en ingeniería/iOS, fundamentos Kotlin/Android por consolidar. LearnDocs incluye un diagnóstico para saltar lo ya conocido.

## Decisiones pendientes

| Pregunta | Default propuesto | Momento de resolver |
| --- | --- | --- |
| ¿Qué temas Kotlin/Compose ya dominás? | Recorrido con diagnóstico, sin repetir ingeniería básica | Antes de graduar ejercicios I1 |
| ¿Qué dispositivo vas a usar? | API 26 y 37 probadas en emulador; dispositivo físico aún por decidir | Antes de distribución |
| ¿Namespace definitivo para publicar? | `dev.bruze.forekast` usado en I0; revisar antes de publicar | Antes de distribución pública |
| ¿Cuánto detalle visual querés? | Material 3 + recursos provisionales originales | I1, luego de ver pantalla real |
| ¿Habrá distribución comercial? | No | Antes de publicar/monetizar revisar proveedor |
| ¿Cuáles son las regiones prioritarias? | Cobertura global; probar varias zonas | I2 para pruebas de datos |
| ¿Preferís aprender Hilt de inmediato? | AppContainer manual implementado; Hilt como rama educativa | Después de I4 |
| ¿Qué experimento Python entusiasma más? | Resumen determinista y luego explicación por LLM | I6 |
| ¿Servicio AI local o alojado? | Local para laboratorio, acceso remoto posterior | I7 |

No se necesita resolver todo para comenzar. Requieren evidencia técnica en I0/I2: compatibilidad del build, fidelidad de fechas diarias y política de consumo. Esas comprobaciones no se reemplazan por preferencias.

## Cambios

### I3 — 2026-09-25

Room/DataStore, esquema v1 exportado, favoritas/selección transaccionales, repositorio observable desde disco y políticas duraderas de actualización. Restauración probada con un proceso nuevo sin red; ver [ADR-008](adr/008-persistence.md), [guía I3](../LearnDocs/10-I3-persistence.md) y [validación](../VALIDATION.md). Hilt se mantiene como refactor educativo posterior.

### I2 — 2026-09-25

Open-Meteo real con Ktor/OkHttp, búsqueda remota con debounce, mapeo validado y memoria temporal. Dos requests separan instantes UNIX de fechas diarias ISO. Se mantiene demo al compilar, tests offline y smoke live opcional. [ADR-007](adr/007-real-network.md) documenta el costo de esta decisión; [LearnDocs I2](../LearnDocs/09-I2-network.md) guía el aprendizaje. Próximo corte: I3, persistencia.

### I0 + I1 — 2026-09-24

Proyecto Android implementado, toolchain fijada, interfaz con datos sintéticos y navegación, laboratorio debug, pruebas y recorrido de código real. ADR-006 documenta el corte y sus límites. La próxima implementación es I2, red real; ver [TOOLCHAIN](TOOLCHAIN.md) y [VALIDATION](../VALIDATION.md).

### 0.1 — 2026-09-24

Primera propuesta de producto, arquitectura, ADR, aprendizaje, assets, QA y contrato Python. No hay implementación Android. Los siguientes cambios deben registrar requisitos afectados y razones, no solo una lista de archivos modificados.
