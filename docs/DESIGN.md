# Diseño de ForeKast

Dirección inicial: una herramienta tranquila, legible y compacta. El pronóstico es el protagonista. I4 implementa bienvenida vacía, navegación adaptable y semántica; la revisión manual con TalkBack sigue pendiente. El documento conserva propuestas visuales además de hechos implementados; el [wireframe SVG](../assets/forekast-wireframe.svg) ilustra jerarquía con datos sintéticos, no una pantalla Android ejecutable.

## 1. Composición

Pantalla principal: barra con ciudad y acciones; temperatura grande y condición; sensación/mínima/máxima; estado de datos; métricas de humedad y viento; fila horizontal de horas; lista vertical de días. Evitar scroll horizontal de toda la pantalla. Las tarjetas horarias tienen hora, icono, temperatura y probabilidad, con texto accesible combinado.

Buscar y Ajustes son destinos separados. En I4 la bienvenida muestra la acción antes del texto largo y la búsqueda oculta el teclado cuando llegan resultados; el contenido permanece desplazable. Usar Material 3 para campos, botones, menús y contenedores. Componentes iniciales: `CurrentWeatherHeader`, `WeatherMetric`, `HourlyForecastItem`, `DailyForecastRow`, `FreshnessNotice`, `LocationResultRow`, `EmptyState` y `RetryState`.

Cada componente recibe datos y callbacks; no obtiene ViewModels ni resuelve DI internamente. La `Route` conecta dependencias, navegación y estado; la `Screen` permite previews con datos sintéticos.

## 2. Tokens propuestos

| Token | Valor de referencia | Uso |
| --- | --- | --- |
| Primary | `#215E73` | Acciones y acentos en tema claro |
| Background | `#F5F8FA` | Fondo claro |
| Surface | `#FFFFFF` | Tarjetas |
| OnSurface | `#172B36` | Texto principal |
| Muted | `#506571` | Texto secundario |
| Accent | `#F5B544` | Ilustración de sol; no texto pequeño sobre blanco |
| Spacing | 4, 8, 12, 16, 24, 32 dp | Ritmo consistente |
| Corner | 16–24 dp | Contenedores; seguir roles Material |
| Typography | Sistema; escala Material | Evitar distribuir una fuente adicional |

Construir esquemas claro/oscuro completos en I1. Los colores anteriores expresan intención visual y requieren contraste medido en cada combinación real. Dynamic color es mejora opcional posterior; no debe cambiar semántica de avisos.

## 3. Accesibilidad y adaptación

- Objetivos táctiles de al menos 48×48 dp aunque el icono sea pequeño.
- Contraste objetivo: 4,5:1 para texto normal, 3:1 para texto grande y controles relevantes. Verificar claro, oscuro y estados deshabilitados.
- Escala de fuente 200 %: permitir altura variable y saltos. No truncar temperatura, ciudad activa ni aviso de datos antiguos.
- TalkBack: “14 horas, parcialmente nublado, 19 grados Celsius, probabilidad de precipitación 20 por ciento”. Omitir descripción en iconos decorativos si el grupo ya tiene la información.
- Orden de foco coherente; favorito anuncia si está activado y su acción siguiente. Error no depende de rojo.
- Indicador de carga accesible; no anunciar cada recomposición o cada minuto como alerta.
- Usar insets y edge-to-edge de la configuración Android seleccionada. Validar back del sistema.
- En ventana ancha puede distribuirse actual/métricas junto a la lista; no bloquear orientación.

La implementación I4 usa encabezados semánticos, roles, descripciones de horas/días y anuncios moderados. Las pruebas automáticas de accesibilidad se ejecutaron en Android 8; la pasada manual con TalkBack está pendiente. Consultar los controles de [accesibilidad Compose](https://developer.android.com/develop/ui/compose/accessibility). La revisión visual no reemplaza recorrer la app con lector de pantalla.

## 4. Contenido y estados

Textos iniciales: “Buscá una ciudad”, “No encontramos resultados”, “No pudimos actualizar”, “Mostrando datos guardados”, “Último dato disponible”, “No hay pronóstico vigente guardado”. Definir tono español consistente y recursos localizables; el voseo es una propuesta editorial revisable.

Los ejemplos usan Montevideo como ciudad ficticia de demostración; no es una inferencia sobre la ubicación del usuario. Todos los números del wireframe son sintéticos. El porcentaje siempre dice probabilidad, no cantidad de lluvia. “Actualizado” se acompaña de hora de descarga; el dato actual tiene su propio horario.

## 5. Assets y sustitución

Hay iconos vectoriales originales provisionales, una marca ilustrativa y un wireframe. El inventario está en [assets/README](../assets/README.md). Importar/adaptar SVG a VectorDrawable o ImageVector en I1; comprobar strokes, bounds y tinte. Los SVG no se copian como XML Android sin conversión.

Los iconos de acciones podrán usar Material Symbols oficiales al implementar, con su licencia incluida. No hace falta que el usuario consiga assets para iniciar. Para una dirección visual posterior bastará reemplazar la marca y el mapping de condición, preservando los contratos y etiquetas accesibles.

El icono launcher final necesita capas foreground/background y recurso monocromo apropiado; la marca SVG actual solo sirve de referencia. No bloquear I1 con ilustraciones de producción.
