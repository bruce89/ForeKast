# Kotlin, coroutines y Flow en ForeKast

## 1. Modelar ausencia y estados

```kotlin
data class Temperature(val celsius: Double) {
    fun fahrenheit(): Double = celsius * 9.0 / 5.0 + 32.0
}

sealed interface LoadState {
    data object Loading : LoadState
    data class Ready(val temperature: Temperature?) : LoadState
    data class Failed(val messageKey: String) : LoadState
}
```

Ejemplo didáctico reducido. Null expresa ausencia, no cero. Una sealed hierarchy sirve cuando los estados son excluyentes. Para pronóstico con contenido y refresh simultáneos, conviene separar el contenido del estado de actualización, como plantea el SPEC.

Ejercicio: agregar un error de refresh a Ready sin perder datos. Comparar una jerarquía anidada con una data class que tiene contenido + refresh. Buscar combinaciones inválidas y decidir dónde imponer invariantes.

## 2. Suspensión, scope y dispatcher

Una función suspend puede suspenderse sin bloquear el hilo. Eso no significa que cree un hilo ni que todo su contenido sea no bloqueante. Un parseo pesado continúa ocupando CPU; una API bloqueante sigue bloqueando si nadie la mueve.

El scope expresa vida útil. La búsqueda pertenece a una pantalla; se cancela cuando deja de ser relevante. El cliente HTTP compartido puede vivir toda la aplicación, pero eso no implica que todas sus operaciones deban vivir tanto. Dispatcher selecciona contexto de ejecución, no políticas de negocio.

Base técnica: [coroutines en Android](https://developer.android.com/kotlin/coroutines/coroutines-best-practices). Ejercicio: reemplazar una demora suspend por bloqueo en un laboratorio y observar UI; retirar el bloqueo al terminar.

## 3. Propagar cancelación

```kotlin
try {
    remote.fetch(location)
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: IOException) {
    // Convertir a un error de transporte del dominio.
}
```

El snippet muestra el orden conceptual; imports y errores concretos dependen del cliente. Si atrapás `Exception` y devolvés Failed para todo, cancelar una búsqueda puede producir un error visible e incluso un retry. `runCatching` captura Throwable: no es una solución automática para errores suspend.

Ejercicio: un fake marca un flag en `finally`; cancelá la operación y comprobá que el cleanup ocurre y que la UI no entra en error. Después hacé fallar una IOException y comprobá el estado opuesto.

## 4. Stream frío y estado compartido

Flow puede representar una secuencia que comienza a producir al recolectar. Dos collectors sobre un flujo que hace HTTP podrían iniciar dos descargas. StateFlow representa un valor actual observable y conflado; no es una cola que garantice entregar todas las transiciones.

En ForeKast, `observe` lee cambios locales. `refresh` es una operación explícita. Ese diseño evita asociar “alguien está mirando” con “hay que llamar siempre a la API”. El ViewModel transforma el stream y la UI recoge con lifecycle.

Ejercicio: contar requests al agregar un segundo collector. Corregir el diseño sin un singleton global mutable que oculte el problema. Explicar en qué capa compartir y cuál es la fuente canónica.

## 5. Búsqueda cancelable

Pipeline conceptual: normalizar consulta → invalidar resultado previo → debounce → evitar repetidos → operación latest → publicar solo si sigue vigente. Un texto de dos caracteres emite estado de ayuda y cancela la búsqueda anterior, aunque no lance una nueva.

No implementar el umbral como un `filter` que simplemente descarta el texto corto y deja una request vieja activa. Ese bug aparece al escribir una consulta larga y borrarla antes de recibir respuesta.

Ejercicio con scheduler virtual:

1. Emitir “Monte”, avanzar menos de 350 ms: cero requests.
2. Completar 350 ms: una request.
3. Borrar hasta “Mo”: estado de ayuda inmediato y resultado anterior inválido.
4. Entregar respuesta vieja: nunca aparece en pantalla.

## 6. No todos los fallos se reintentan

Una desconexión puede ser transitoria; un JSON incompatible normalmente no se arregla repitiendo en un segundo. El 429 es señal de esperar. Un deadline acota tiempo total, no solo cada intento individual. Dos capas que reintentan multiplican tráfico y latencia.

Ejercicio: registrar secuencia de intentos y tiempo total ante 503, 429 y cancelación. Usar los límites del SPEC como política inyectada para evitar tests con esperas reales.

## 7. Autoevaluación

Explicá por qué son incorrectas estas frases: “suspend significa background”, “StateFlow entrega todos los eventos”, “ViewModel persiste para siempre”, “cancelar cliente siempre cancela servidor”, “una variable val es profundamente inmutable”. Corregilas con un ejemplo concreto de ForeKast.
