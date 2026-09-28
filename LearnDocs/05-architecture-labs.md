# Laboratorios de arquitectura y pruebas

## Laboratorio A — repositorio y fuente canónica

**Pregunta:** ¿qué ocurre si UI escucha red y base por separado?

Crear en una rama experimental una respuesta nueva de red y retrasar su escritura. Hacer que la base emita el valor viejo después. Observar si la UI retrocede. Volver al diseño que publica contenido desde una sola fuente.

**Evidencia:** timeline de emisiones y test que falla con el diseño de dos fuentes. Explicar por qué una fuente canónica no significa que haya una sola representación del dato en todo el sistema.

## Laboratorio B — frontera temporal

Inyectar Clock. Probar exactos 30 minutos y seis horas. Cambiar el reloj hacia atrás y decidir política explícita: una edad negativa anómala debe invalidar confianza en frescura y pedir actualización cuando sea posible, sin un bucle de requests.

Probar cambio de día de la ciudad con el teléfono en otra zona. Para DST, usar timestamps de instantes verificables y fecha diaria esperada independiente; no calcular expected con la misma función que se prueba.

**Evidencia:** casos límite, comportamiento documentado y distinción entre “edad de descarga” y “validez del pronóstico”.

## Laboratorio C — DI manual antes de Hilt

Construir un fake datasource, repository y ViewModel mediante constructores. Reemplazar el Clock. Luego observar cómo Hilt resuelve el mismo grafo.

Introducir intencionalmente dos instancias de una dependencia que debería compartirse y comprobar diferencias de caché. Retirar el error. Evaluar qué scope corresponde a cada recurso según costo y vida útil, no según comodidad.

**Evidencia:** dibujo del grafo y un test sin framework de DI. Si no puede construirse manualmente, investigar dependencias ocultas.

## Laboratorio D — snapshot transaccional

Guardar una cabecera con 24 horas. Simular fallo mientras se reemplazan hijos. Un lector debe ver versión anterior completa o nueva completa. Validar también que la consulta agregada no mezcle tablas leídas en instantes diferentes.

**Evidencia:** prueba de base real de test, esquema y explicación de la transacción. Un mock de DAO no demuestra atomicidad de SQLite.

## Laboratorio E — el costo de una capa

Implementar `ObserveWeatherUseCase` como delegación directa; luego un caso real `RankWalkingWindows` con validación y reglas. Comparar lo que cada abstracción permite probar y reutilizar.

**Evidencia:** un ADR corto que conserve o retire la delegación según el proyecto. No hay premio por maximizar clases ni minimizar líneas.

## Laboratorio F — transporte sustituible

Mantener fixtures y contrato; construir un segundo adapter con Retrofit como experimento opcional. Comparar setup, manejo de errores, cancelación, serialización y legibilidad. No migrar automáticamente la app por terminar el ejercicio.

**Evidencia:** tabla de tradeoffs basada en código ejecutado, no opiniones sobre cuál es más “moderno”.

## Laboratorio G — tests que pueden refutar

Elegir una regla, introducir una mutación conceptual y comprobar que la prueba fallaría: quitar guard de respuesta vieja; cambiar `>=` por `>` en TTL; convertir null a cero; usar zona del teléfono; guardar cabecera antes de transacción.

No hace falta instalar mutation testing para empezar. La pregunta es si el test puede detectar un error relevante. Evitar assertions que vuelven a calcular lo mismo que la implementación.

## Registro final por laboratorio

Hipótesis, experimento, resultado observado, prueba reproducible, conclusión y límite. Vincular con ADR correspondiente. Una herramienta nueva debe resolver una dificultad que ahora podés describir.
