# ForeKast — especificación de producto v0.1

Estado: I0–I4 implementados; aceptación final según la evidencia y pendientes de [QUALITY](QUALITY.md). Alcance de esta versión: I0–I4 del [roadmap](ROADMAP.md). Las extensiones I5+ no son requisitos del MVP.

## 1. Propósito y experiencia

Responder tres preguntas en pocos segundos: qué tiempo hace en una ciudad, cómo evolucionará durante las próximas 24 horas y qué se espera durante los próximos siete días. La interfaz explicará la antigüedad de los datos y seguirá siendo útil con un pronóstico previamente guardado.

Objetivo paralelo: una base suficientemente clara para que su autor pueda rastrear una interacción desde Compose hasta la red, cambiar una pieza y explicar las consecuencias. La organización deberá permitir comparar decisiones con iOS sin asumir equivalencias exactas.

## 2. Supuestos de partida

- Proyecto personal y educativo, sin anuncios, suscripciones ni cuenta de usuario.
- Android nativo, Kotlin para todo el código escrito por nosotros. Gradle Kotlin DSL.
- `minSdk = 26` como decisión de alcance; compileSdk, targetSdk y toolchain se fijan en I0 según versiones estables compatibles. API 26 no es una exigencia universal de Compose.
- Español inicial; cadenas en recursos desde el primer día. Unidades predeterminadas: °C, km/h y mm. Tema del sistema.
- Teléfono primero, con uso correcto en landscape y ventanas redimensionadas; optimización de tablet después.
- Búsqueda manual de ciudades. Ubicación del dispositivo queda para I5 para aislar el aprendizaje de permisos.
- Horizonte de 24 horas futuras y siete fechas diarias, contando el día local actual, siempre que el proveedor tenga datos.

## 3. Alcance funcional

| ID | Requisito MVP | Criterio observable |
| --- | --- | --- |
| F01 | Buscar ciudad por nombre | Con tres caracteres y 350 ms sin cambios, buscar; mostrar nombre, región y país |
| F02 | Elegir ciudad | Abrir su pronóstico sin confundir ciudades homónimas |
| F03 | Condiciones actuales | Temperatura, sensación, condición, humedad, viento y hora de los datos |
| F04 | Próximas horas | Hasta 24 muestras futuras con hora local, temperatura, probabilidad y condición |
| F05 | Siete días | Fecha, condición, mínima, máxima y probabilidad máxima diaria |
| F06 | Favoritos | Guardar/quitar hasta cinco ciudades; conservarlas al cerrar la app |
| F07 | Recuperación | Volver a la última ciudad elegida; si no existe, mostrar favoritos o búsqueda |
| F08 | Actualización | Botón accesible y gesto de refresco; mantener contenido mientras se actualiza |
| F09 | Lectura sin conexión | Mostrar caché con antigüedad; sin caché, ofrecer reintento y cambiar ciudad |
| F10 | Preferencias | °C/°F y sistema/claro/oscuro; el cambio de temperatura no genera una request |
| F11 | Transparencia | Atribución accesible, origen y distinción entre descarga y hora meteorológica |
| F12 | Estado recuperable | Rotación no duplica requests; restauración tras muerte del proceso reconstruye desde persistencia |

Fuera del MVP: ubicación GPS, mapas/radar, alertas oficiales, notificaciones, widgets, cuentas, sincronización entre dispositivos, monetización, chat y modelos de IA. Un código de tormenta no se presentará como una alerta emitida por una autoridad.

## 4. Pantallas y navegación

**Inicio sin selección.** Título ForeKast, texto breve, botón Buscar ciudad. Si hay favoritos, se muestran antes del buscador. Nunca se pide permiso de ubicación al abrir.

**Buscar.** Campo con foco al entrar, lista de hasta diez resultados. Menos de tres caracteres muestra una ayuda. Cambiar la consulta invalida resultados anteriores. Al elegir se persiste la selección y se reemplaza la pantalla de búsqueda por Pronóstico. Una ciudad no queda favorita automáticamente.

**Pronóstico.** Encabezado con ciudad y acción de favorito; bloque actual; estado de actualización; horas; días; atribución. Barra superior con Ciudades y Ajustes. Cero datos implica un estado de carga o error específico; los ceros numéricos reales se muestran como ceros.

**Ciudades.** Favoritos con nombre, región y país, indicador de ciudad activa y acciones de elegir/quitar. En MVP no se consulta automáticamente el tiempo de las cinco ciudades. Al quitar la ciudad activa de favoritos sigue seleccionada: favorito y selección son conceptos distintos. El sexto favorito muestra un mensaje con opción de administrar.

**Ajustes y Acerca de.** Unidades, tema, fuentes y licencias, borrar datos locales. Borrar datos requiere confirmación contextual, limpia selección, favoritos y caché, y vuelve al inicio. Conserva unidades, tema y el cooldown anónimo de proveedor; el diálogo explica qué se borra. El sistema de backup deberá excluir caché y datos de ubicación/selección para que esta política sea coherente.

Back desde Ajustes/Ciudades devuelve a la pantalla anterior. Back desde Pronóstico raíz sale de la app. El estado de navegación guarda claves pequeñas, nunca un objeto de pronóstico completo.

## 5. Modelo del estado visible

| Estado | Qué ve la persona | Acción |
| --- | --- | --- |
| Sin ciudad | Explicación breve | Buscar |
| Primera carga | Indicador y ciudad elegida | Volver a ciudades |
| Datos recientes | Pronóstico y hora | Actualizar/cambiar ciudad |
| Actualizando con datos | Contenido existente y progreso discreto | Navegar; no duplicar refresh |
| Falló actualización con caché | Datos y aviso persistente | Reintentar cuando corresponda |
| Falló sin caché | Error comprensible | Reintentar o cambiar ciudad |
| Respuesta parcial | Campos ausentes como “—” | Usar lo disponible |
| Límite del proveedor | Datos previos o error, próxima oportunidad | Esperar el cooldown |

Los estados de contenido y actualización son ortogonales: recibir un error no borra un éxito anterior. Un error HTTP no prueba que el dispositivo esté sin Internet; el texto general será “No pudimos actualizar”. Usar “Sin conexión” solo cuando hay evidencia adicional de conectividad.

## 6. Frescura y comportamiento temporal

Reglas propuestas, medibles con un reloj inyectable:

- Menos de 30 minutos desde `fetchedAt`: reutilizar sin refresco automático.
- Desde 30 minutos hasta menos de seis horas: mostrar caché y refrescar al entrar o volver al foreground.
- Desde seis horas: mostrar aviso destacado “Pronóstico guardado”, con fecha y hora completas. El bloque actual se rotula “Último dato disponible”.
- Sin muestras futuras: mostrar “No hay pronóstico vigente guardado”; no colocar horas del pasado bajo “Próximas horas”.
- Retener como máximo siete días la última instantánea por ciudad. Al superar esa edad, tratar como caché no utilizable y purgar al iniciar/consultar.
- Actualización manual permitida incluso con caché reciente, limitada a una por 60 segundos por ciudad. El 429 puede imponer una espera mayor.
- En MVP no hay polling ni sincronización en background. Mientras la pantalla está visible, recalcular antigüedad y ventanas al cambiar el minuto; eso no dispara HTTP.
- Si el reloj retrocede y produce una edad negativa, marcar frescura como desconocida y solicitar refresh al siguiente disparador permitido; aplicar cooldown para evitar un bucle.

Mostrar horarios en la zona de la ciudad, no en la del teléfono. `fetchedAt` indica descarga exitosa; `current.time` indica el tiempo al que corresponde el dato. No se afirmará que descargar de nuevo equivale a un modelo meteorológico nuevo. En fechas diarias, “Hoy” se calcula en la zona seleccionada.

## 7. Requisitos no funcionales

- Accesibilidad: TalkBack, texto al 200 %, acciones de al menos 48 dp, significado independiente del color; véase [diseño](DESIGN.md).
- Uso de red acotado: una actualización por ciudad en vuelo; consulta cancelable; sin cascadas de reintentos entre capas.
- Privacidad: guardar solo ciudades elegidas y preferencias. No incorporar analytics ni enviar datos a Python durante el MVP.
- Resiliencia: errores tipados, datos parciales y códigos desconocidos contemplados; sin crash por una respuesta inesperada.
- Testabilidad: tiempo, red y repositorios sustituibles; escenarios deterministas sin depender de meteorología real.
- Legibilidad: comentarios para decisiones o invariantes; nombres que explican responsabilidad. No se exige un use case o interfaz para cada clase.

Metas iniciales de experiencia, a medir en I4: primera visualización de caché dentro de un segundo en el dispositivo de referencia acordado; feedback de refresh inmediato; deadline total de actualización de 20 segundos. Son objetivos, todavía no mediciones.

## 8. Aceptación del MVP

Una persona puede instalar el build, buscar una ciudad, consultar horas/días, guardarla, cerrar, abrir sin red y reconocer la antigüedad del pronóstico. Puede cambiar a °F sin conexión ni descarga, distinguir dos ciudades homónimas y rotar sin perder el contenido. Los escenarios [Q01–Q18](QUALITY.md) forman la aceptación. La implementación I4 no se declara certificada hasta cerrar sus verificaciones manuales pendientes.

## 9. Evolución controlada

Cambios de alcance actualizan este archivo, el roadmap y los criterios de aceptación. Los ADR son propuestas para esta versión; su estado pasa a aceptado al incorporarse a la implementación. La disponibilidad de un framework no constituye por sí sola una razón para usarlo.
