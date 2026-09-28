# Bitácora, preguntas y glosario

## Plantilla de sesión

```text
Fecha / corte / commit:
Pregunta concreta:
Predicción antes de ejecutar:
Recorrido de datos implicado:
Experimento:
Resultado observado:
Prueba o pasos para repetir:
Qué entendí:
Qué sigo sin poder explicar:
Relación con iOS o arquitectura general:
Documento/ADR a actualizar:
Siguiente experimento pequeño:
```

## Revisión al cerrar un corte

- Puedo ejecutar desde cero y explicar la configuración agregada.
- Puedo seguir un dato de UI al origen y de regreso.
- Sé quién posee estado, scope, cancelación y persistencia.
- Sé simular éxito, error, nulls y respuestas tardías.
- Distingo una decisión del proyecto de una restricción de Android.
- Puedo justificar una alternativa descartada y cuándo reabrirla.

## Glosario de trabajo

| Término | Significado en ForeKast |
| --- | --- |
| UDF | Flujo unidireccional: acciones hacia el dueño del estado, estado hacia UI |
| State hoisting | Llevar estado al nivel que debe poseerlo y pasar valores/callbacks |
| Recomposición | Reevaluación de UI declarativa afectada por cambios observables |
| Scope | Vida útil y contexto de trabajo o dependencia; precisar cuál se discute |
| Suspend | Operación que puede suspenderse; no garantiza background |
| Flow | Secuencia asincrónica observable |
| StateFlow | Estado actual observable y compartido, con conflación |
| DTO | Forma del dato de transporte, ligada al proveedor/contrato |
| Mapper | Conversión explícita de representación y semántica |
| Repository | Frontera de datos que coordina fuentes y ofrece modelos de app |
| Fuente canónica | Origen que define el contenido observado por capas superiores |
| TTL | Política de tiempo de reutilización antes de pedir refresh |
| Instant | Punto temporal independiente de zona |
| LocalDate | Fecha de calendario sin hora ni zona incorporada |
| Snapshot | Conjunto coherente de datos aplicado como unidad |
| ADR | Registro de una decisión, su contexto, alternativas y consecuencias |
| Baseline | Solución de referencia contra la que medir una mejora |
| Grounding | Vincular resultados a evidencia disponible |
| Tool calling | Solicitud estructurada para ejecutar una herramienta permitida |
| Abstención | Respuesta explícita cuando faltan datos para concluir |

## Preguntas de arquitectura para volver a usar

¿Qué cambia junto? ¿Qué invariante protege esta frontera? ¿Cuál es el costo de reemplazar el proveedor? ¿Qué estado inválido permite el modelo? ¿Qué falla si el proceso desaparece ahora? ¿Dónde se paga la latencia? ¿Qué evidencia justificaría agregar una capa? ¿Qué simplificación mantiene la capacidad de aprender y modificar?
