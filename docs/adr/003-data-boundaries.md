# ADR-003 — Persistencia observable y complejidad gradual

Estado: propuesto · 2026-09-24.

## Contexto y decisión

El pronóstico debe seguir disponible tras cerrar la app sin red. Room será la fuente observable del pronóstico persistido y de ciudades; DataStore guardará unidades y tema. La red actualiza la base y la UI observa modelos derivados. Un módulo Gradle inicial, paquetes por funcionalidad y fronteras explícitas.

Los use cases aparecerán al combinar repositorios o concentrar reglas reutilizadas, por ejemplo elegir una ventana horaria. No se crea una clase por cada lectura simple del DAO.

## Alternativas

- Caché HTTP: reduce red, pero no expresa favoritos, ventanas vigentes ni mensajes de antigüedad por sí sola.
- JSON en archivo: opción razonable para una sola instantánea. Room enseña consultas, transacciones y migraciones y facilita varias ciudades; su costo es esquema, DAO y codegen.
- Guardar todo en DataStore: evita otra tecnología, pero obliga a leer/escribir un agregado grande y dificulta consultas/relaciones.
- Multimódulo completo desde el día uno: límites compilables y potencial de build paralelo; agrega configuración e indirection antes de necesitarlo.
- Clean Architecture con capas obligatorias: útil como disciplina de dependencias, pero puede producir clases delegadoras sin lógica. Adoptamos separación de políticas/adaptadores y evaluamos cada capa.

## Consecuencias

Hay que manejar fallo de escritura: un HTTP 200 no basta para declarar actualización aplicada. Favoritos y selección comparten transacciones; preferencias no contienen claves de Room. La limpieza de caché no borra favoritos. Inicialmente las fronteras se verifican en revisión; podrán convertirse en módulos más adelante.

Validación: prueba transaccional de reemplazo, datos disponibles al reabrir sin red y ausencia de datos de una ciudad en otra. Reabrir cuando dos features o plataformas compartan código real, o la compilación se convierta en un costo demostrado.
