# ADR-006 — primer corte ejecutable con grafo manual

Estado: aceptado para I0 + I1 · 2026-09-24.

## Contexto

Necesitamos una app ejecutable para estudiar Compose y el recorrido de estado antes de añadir red, persistencia y generación de dependencias. Los ADR anteriores describen la arquitectura objetivo del MVP.

## Decisión

Un módulo `app`, grafo manual en AppContainer y un WeatherViewModel a nivel Activity que conserva la sesión de demostración entre destinos. Pantallas sin dependencias de repositorio, navegación Navigation 3 con claves serializables y fake suspend observable. Ajustes contiene un laboratorio debug que manipula el fake mediante un contrato separado del puerto meteorológico.

La primera versión abre una ciudad de ejemplo y usa catálogo local. Preferencias/favoritos se guardan como claves pequeñas de SavedStateHandle, con alcance de restauración de sesión. No afirmamos persistencia offline entre sesiones independientes.

## Alternativas y consecuencias

Hilt/Room desde este corte permitirían llegar antes al grafo final, pero dificultarían atribuir un comportamiento a Compose, DI o disco. Se posponen a I3. Un ViewModel por destino sería más cercano al diseño final, pero requeriría un repositorio adicional de selección/preferencias compartidas antes de estudiarlo.

El ViewModel de sesión tiene acoplamiento temporal al catálogo/fake: es explícito y limitado a esta demo. Al introducir repositorios de ubicaciones/ajustes se extraerán esas responsabilidades; no se agregará allí lógica de red o agentes. `DemoController` mantiene los escenarios fuera de WeatherRepository.

## Validación

Build debug, tests de cancelación/deduplicación, datos guardados y escenarios, y recorridos instrumentados de navegación/recreación. Resultados efectivos en [VALIDATION](../../VALIDATION.md). Reabrir esta decisión al comenzar I2/I3, manteniendo Screen y modelos como fronteras estables.
