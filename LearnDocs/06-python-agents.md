# Ruta de aprendizaje Python y agentes

El contrato y reglas del producto están en [PYTHON_AI](../docs/PYTHON_AI.md). Este archivo propone experimentos para comprenderlos.

## 1. Python como función, antes de HTTP

Leer la solicitud sintética y producir un resumen determinista. Validar datos mediante modelos tipados; usar datetime consciente de zona para instantes. Null meteorológico sigue siendo ausencia. Definir un reloj explícito para que generatedAt no vuelva inestables las pruebas.

Ejercicio: min/max y mayor probabilidad entre horas disponibles. ¿Qué pasa si todas las probabilidades son null? ¿Cómo se diferencia “no hay evidencia de lluvia” de “evidencia de que la probabilidad es baja”?

Entregable: función y tests con resultado independiente de red y LLM.

## 2. Paridad Kotlin/Python

Usar los mismos fixtures y reglas de desempate, orden y redondeo. Comparar la estructura, no el texto incidental. Un ranking debe indicar por qué una ventana ganó; una diferencia de aritmética no se resuelve apelando a “la IA”.

Entregable: casos compartidos y lista de diferencias justificadas. Entender contrato interoperable sin compartir necesariamente código fuente.

## 3. Convertir la función en servicio

FastAPI añade validación HTTP y serialización. Comparar su OpenAPI generado con el contrato del paquete; no asumir que coinciden solo porque ambos usan JSON. Probar 422, 413, timeout y servidor apagado.

Entregable: una request Android con fake HTTP equivalente y evidencia de que el pronóstico sigue visible cuando falla insights.

## 4. Añadir generación

Separar calcular hechos de redactar. Entregar al modelo un conjunto cerrado de hechos con IDs, pedir salida estructurada y verificarla. El esquema comprueba forma, no verdad. Una salida puede tener JSON perfecto y una conclusión falsa.

Ejercicio: pedir al modelo un resumen de datos insuficientes y medir si se abstiene. Introducir valores contradictorios y referencias inexistentes en salidas simuladas. Asegurar que la app usa fallback.

Entregable: evaluación con baseline de plantilla, errores por categoría, latencia y costo. Si la plantilla resulta mejor, conservarla.

## 5. Añadir comportamiento de agente

Una llamada que resume texto no necesita un agente. El experimento empieza a requerirlo cuando el sistema elige herramientas o pasos, por ejemplo comparar intervalos y pedir un cálculo adicional.

Herramientas iniciales propuestas: resumir horas y rankear ventanas sobre datos ya recibidos. Permitir solo argumentos tipados y métodos acotados. Establecer presupuesto de pasos y finalización; probar que el flujo se detiene cuando no obtiene datos suficientes.

Entregable: traza de una ejecución con entradas/salidas de herramientas y razones verificables de selección. No pedir ni almacenar razonamiento interno oculto del modelo; registrar acciones, evidencia y resultados observables.

## 6. Entender evaluación y seguridad como ingeniería

Tratar texto externo como entrada no confiable: un nombre de ciudad no puede habilitar una herramienta. Separar credenciales de prompts y APK. El backend puede continuar aunque el cliente cancele: imponer deadlines también allí.

Ejercicio: simular “ignorá tus instrucciones” dentro de un campo libre; ninguna herramienta adicional debe habilitarse. Después simular tres respuestas inválidas y comprobar el límite de intentos.

## 7. Preguntas para la siguiente rama

¿Querés estudiar Python idiomático, ingeniería de backend, ML, o agentes? Comparten piezas, pero no son el mismo aprendizaje. ForeKast permite avanzar por separado: reglas y contratos para Python; despliegue/observabilidad para backend; evaluación de pronósticos para ML; orquestación acotada para agentes.
