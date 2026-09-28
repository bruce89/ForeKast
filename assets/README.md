# Assets provisionales

Los SVG de esta carpeta son originales creados para el proyecto; no se descargaron imágenes de terceros. Pueden reemplazarse sin cambiar modelos meteorológicos. El inventario está en [manifest.json](manifest.json).

En I1, los iconos meteorológicos se dibujan como vectores nativos con [WeatherGlyph](../app/src/main/kotlin/dev/bruze/forekast/designsystem/WeatherGlyph.kt); los SVG conservan su función de referencia. La marca se adaptó a un icono launcher con capas Android.

- [Wireframe](forekast-wireframe.svg): tres estados para discutir jerarquía; datos ficticios, sin interacción.
- [Vista PNG](forekast-wireframe.png): versión renderizada del mismo wireframe para revisión rápida.
- [Marca provisional](forekast-mark.svg): referencia para identidad, no launcher final.
- `weather/`: sol, luna, nube, lluvia, nieve, tormenta, niebla y desconocido.

Para Android convertir iconos SVG a VectorDrawable o ImageVector y revisar visualmente importación, stroke y tint. Usar condición normalizada para seleccionar recurso. Mantener texto accesible separado del dibujo. La app no debe necesitar red para cargar estos iconos.

La atribución de datos a Open-Meteo sigue siendo necesaria aunque los dibujos sean propios. Si se reemplazan por un pack externo, registrar autor, URL, licencia, archivos y modificaciones en el inventario. No se asigna desde este paquete una licencia global al futuro proyecto: se decidirá antes de distribuirlo.
