# Fixtures Open-Meteo

Obtenidos por HTTPS el 2026-09-25. Cuerpos completos sin truncar. Datos Open-Meteo, CC BY 4.0; resultados de geocoding basados en GeoNames. Uso educativo no comercial. Fuentes: https://open-meteo.com/en/docs y https://open-meteo.com/en/docs/geocoding-api.

- forecast.json: /v1/forecast, latitude=-34.90328, longitude=-56.18816, timezone=America/Montevideo, forecast_days=7, temperature_unit=celsius, wind_speed_unit=kmh, timeformat=unixtime, current=temperature_2m,apparent_temperature,relative_humidity_2m,is_day,weather_code,wind_speed_10m, hourly=temperature_2m,precipitation_probability,weather_code.
- daily.json: mismas coordenadas/zona/unidades/horizonte, timeformat=iso8601, daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max.
- search.json: https://geocoding-api.open-meteo.com/v1/search?name=Montevideo&count=10&language=es&format=json.

Los tests de DST y respuestas inválidas construyen variantes sintéticas; no representan observaciones reales. Los tests cotidianos no hacen llamadas públicas. No regenerar automáticamente fixtures para hacer pasar una prueba.
