# ADR-009 — Inicio vacío, borrado acotado y evidencia de calidad

Estado: aceptado para la implementación I4, 2026-09-26. La aceptación completa del MVP depende de la matriz [QUALITY](../QUALITY.md).

## Contexto

I3 recuperaba la ciudad desde Room, pero sembraba Montevideo cuando no encontraba selección. Eso confundía «no elegí ciudad» con una preferencia real y disparaba HTTP al instalar. Además, el SPEC pedía borrar ciudades y caché desde Ajustes. El diseño inicial tenía filas de altura implícita y contenido horizontal que podía recortarse con texto grande.

## Decisión

`Library.selected` es nullable. Una biblioteca vacía es un resultado válido del almacenamiento; no se filtra del Flow ni se transforma en carga infinita. La UI deriva `hasSelection` de ese resultado y muestra la bienvenida. No hay un flag de onboarding separado ni una ciudad escrita por defecto. El catálogo demo conserva su inicio sintético para estudiar estados.

El borrado se llama **Borrar ciudades y pronósticos**. Un diálogo explica el alcance y permite cancelar. Una transacción elimina primero la selección y después las ciudades; las foreign keys eliminan snapshots, horas, días y marcas de refresh. Conserva preferencias de DataStore y cooldowns anónimos por host. Estos últimos no contienen ciudades y evitan que borrar datos permita ignorar un `Retry-After`. Desinstalar o borrar almacenamiento desde Android sigue eliminando todo.

No cambia el esquema Room v1: una tabla de selección sin filas ya es válida. No agregamos una migración vacía ni fallback destructivo. La UI espera el commit; ante fallo mantiene datos anteriores y expone el error de almacenamiento. Cancela observación/refresco al borrar y cambia la generación para que resultados tardíos no publiquen contenido anterior. El repositorio comprueba que la ciudad exista antes de guardar un snapshot.

Los encabezados de navegación pueden crecer, la búsqueda completa se desplaza y las tarjetas diarias usan varias líneas. Con fuente grande o ancho reducido, humedad/viento se apilan y se omite el glifo decorativo junto a la temperatura. El texto respeta el font scale; se cambia la composición, no la preferencia del usuario. Encabezados, selección de ciudad, avisos y temperaturas tienen semántica explícita.

## Alternativas

- Un booleano `onboardingCompleted` en DataStore: duplicaría la verdad de Room y exigiría coordinar dos commits.
- Sembrar la ubicación de ejemplo: cómodo para una demo, pero no expresa una elección del usuario.
- Vaciar toda la base o destruirla: borra también el cooldown, complica conexiones activas y pierde el alcance específico de la acción.
- Achicar globalmente el texto: contradice la preferencia de accesibilidad.
- Declarar accesibilidad a partir de tests verdes: no demuestra lectura real, orden de foco ni usabilidad con TalkBack.

## Consecuencias y verificación

Las llamadas a `Library.selected` deben contemplar ausencia. ViewModel y Room tienen pruebas de biblioteca vacía y borrado; las pruebas de interfaz comprueban confirmación/cancelación y navegación. Las verificaciones automáticas de accesibilidad complementan una pasada manual de TalkBack. [VALIDATION](../../VALIDATION.md) registra qué se ejecutó, en qué API y qué queda pendiente.

Reabrir si se permite quitar selección sin borrar favoritas, se agregan cuentas/sincronización, o existe una política explícita para restablecer también preferencias. Una migración futura requiere su propio caso de prueba sobre el esquema exportado anterior.
