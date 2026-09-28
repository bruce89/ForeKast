# ADR-002 — Open-Meteo y cliente Kotlin

Estado: propuesto · 2026-09-24.

## Contexto y decisión

Una app educativa necesita datos reales sin sumar gestión de credenciales y facturación desde la primera pantalla. Elegimos Open-Meteo para uso no comercial, con adapter detrás de WeatherRepository. Usamos Ktor Client + engine OkHttp + kotlinx.serialization para operaciones suspend y configuración explícita.

La comparación y condiciones están centralizadas en [WEATHER_API](../WEATHER_API.md); no se duplican cuotas aquí. DTOs del proveedor no llegan a UI, ni el resto de la app conoce nombres de query parameters.

## Alternativas

- Retrofit + OkHttp + kotlinx.serialization también es moderno y soporta suspend. No se descarta por “ser Java”: sería una buena opción si se prioriza familiaridad del ecosistema Android y servicios declarativos por annotations.
- Ktor favorece practicar APIs Kotlin y deja abierta una futura comparación KMP. Su configuración requiere aprender plugins, engine y errores; la menor cantidad de annotations no equivale a menor complejidad.
- SDK específico del proveedor simplifica un comienzo, pero suma acoplamiento y no es necesario para estos GET.
- Proxy propio desde el comienzo permitiría control central de cuotas, pero añade operación de backend a un objetivo inicialmente móvil.

## Consecuencias

El adapter absorbe arrays paralelos, unidades, tiempo y códigos. Se conserva la procedencia de los datos. Habrá fake para UI y fixtures para la integración. No se añade fallback silencioso: necesitaríamos demostrar equivalencia del significado antes.

Validar respuestas reales y edge cases en I2. Reabrir al monetizar, necesitar alertas oficiales, incorporar cobertura especializada o detectar problemas medidos del cliente elegido.
