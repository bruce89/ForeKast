# ADR-005 — Lectura clara, búsqueda manual y assets reemplazables

Estado: propuesto · 2026-09-24.

## Contexto y decisión

La primera interfaz tiene que enseñar composición y estado mientras muestra información real. Buscar ciudad manualmente evita permisos antes de poder aportar valor. La pantalla prioriza ciudad, temperatura, condición, próximas horas y días; la frescura siempre está disponible.

Usaremos tipografía de sistema, Material 3 y vectores simples originales incluidos en `assets/`. Son recursos provisionales reemplazables; sin fotografías, servicios de imágenes o iconos cargados por red.

## Alternativas y consecuencias

- Fondos fotográficos o animaciones de lluvia: pueden dar identidad, pero complican contraste, rendimiento y pruebas. Explorarlos después de validar jerarquía.
- Ubicación automática obligatoria: reduce un paso cuando funciona; produce un callejón sin salida al rechazar permisos. En I5 será una acción opcional.
- Colores solamente para estados: insuficiente para accesibilidad. Siempre combinar texto, forma y semántica.
- Copiar exactamente la navegación iOS: familiar para el autor, pero no enseña convenciones Android. Usar back del sistema, edge-to-edge e insets correctamente.

Validación: TalkBack, fuente al 200 %, ancho pequeño, tema oscuro y error con caché. Reabrir al incorporar ubicación o trabajar identidad visual final. Los assets no constituyen validación de marca ni un icono de publicación terminado.
