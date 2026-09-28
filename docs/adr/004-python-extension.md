# ADR-004 — Python como extensión opcional

Estado: propuesto · 2026-09-24.

## Contexto y decisión

Se quiere explorar Python y agentes sin volver frágil la primera app. Definimos `WeatherInsightProvider` como puerto suspend. El MVP no lo invoca ni muestra UI de insights. I6 añade un adapter determinista; I7 puede llamar a un servicio Python/FastAPI con contrato versionado.

El servicio recibe un resumen mínimo del pronóstico normalizado y devuelve insights estructurados con referencias a evidencia. No reemplaza el proveedor meteorológico. La UI básica y su base de datos siguen operativas si el servicio está caído.

## Alternativas

- Python embebido en Android: permite ejecución local, pero suma runtime, tamaño, ABI, librerías nativas y distribución. No justificado para transformar un pronóstico.
- SDK de agentes dentro del cliente: dificulta proteger claves y controlar gasto. Mantener orquestación y credenciales del modelo en backend.
- Todo en Kotlin: suficiente para reglas meteorológicas simples y será baseline. Python se justifica si el experimento aporta valor medible.
- Microservicios/múltiples agentes: más piezas y fallos antes de tener un caso de uso evaluable. Empezar por una función determinista y, luego, un flujo acotado con herramientas.

## Consecuencias

HTTP exige versionado, autenticación al exponerlo y pruebas de timeout; agrega latencia y costo si hay LLM. Datos enviados y habilitación son explícitos. Una respuesta generada nunca modifica cifras ni presenta una alerta oficial.

Validación: misma UI con fake local y HTTP; servicio caído no afecta pronóstico; toda afirmación cuantitativa puede verificarse contra una evidencia. Reabrir solo con una necesidad probada de ejecución offline, modelo on-device o capacidades Python no trasladables.
