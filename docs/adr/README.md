# Decisiones de arquitectura y diseño

ADR-001 a ADR-005 describen el objetivo propuesto del MVP. ADR-006 registra el primer corte **aceptado e implementado** de I0 + I1 y sus diferencias transitorias. ADR-007 registra I2 y ADR-008 registra I3, ambos aceptados el 2026-09-25. ADR-009 registra el inicio vacío y el alcance del borrado en I4. ADR-010 registra el motor local I6a. Ver [arquitectura](../ARCHITECTURE.md) para contratos objetivo y [fuentes](../SOURCES.md) para documentación primaria.

| ADR | Decisión | Reabrir cuando… |
| --- | --- | --- |
| [001](001-native-state.md) | Kotlin, Compose y UDF con ViewModel | Aparezca una necesidad concreta de UI compartida |
| [002](002-network-provider.md) | Open-Meteo, Ktor y modelos propios | Cambien uso comercial, cobertura o experiencia del equipo |
| [003](003-data-boundaries.md) | Room, DataStore y módulos graduales | Exista reutilización o costo medido de build |
| [004](004-python-extension.md) | Puerto de insights + Python remoto opcional | Se justifique ejecución local o un modelo especializado |
| [005](005-design-scope.md) | Búsqueda manual, vectores y lectura accesible | La app básica esté validada y se añadan permisos |
| [006](006-first-slice.md) | Primer corte con grafo manual, estado de sesión y fake | Al introducir red y persistencia |

| [007](007-real-network.md) | Red real, dos formatos temporales y memoria de sesión | Al introducir persistencia u optimizar requests |

| [008](008-persistence.md) | Room, DataStore y restauración offline | Historial, background o sincronización entre dispositivos |
| [009](009-mvp-quality.md) | Inicio vacío, borrado acotado y accesibilidad verificable | Cuentas, otra política de borrado o sincronización |
| [010](010-local-insights.md) | Reglas locales con evidencia y abstención | Existan preferencias explícitas o feedback de utilidad |

Para modificar una decisión aceptada, crear un nuevo ADR que la sustituya; conservar contexto histórico. Cada ADR nuevo incluye contexto, elección, alternativas, consecuencias, validación y condición para reabrir.
