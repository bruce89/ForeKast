# Versionar ForeKast en GitHub

El proyecto está organizado en la raíz del repositorio. `docs/` contiene especificación y decisiones; `LearnDocs/` contiene aprendizaje; `assets/` y `contracts/` contienen recursos de referencia. Los enlaces son relativos para navegar también desde GitHub.

## Primer commit

Desde PowerShell en la raíz del proyecto:

```powershell
git status
git add .
git diff --cached --stat
git commit -m "feat: add ForeKast Compose demo and learning roadmap"
```

Antes de confirmar, revisar los archivos preparados. El `.gitignore` excluye configuración local de Android, builds, archivos de firma y entornos Python. No se configura identidad Git ni se agregan credenciales desde este paquete.

## Remoto de este proyecto

El remoto `origin` de este proyecto es [bruce89/ForeKast](https://github.com/bruce89/ForeKast). Para una copia local nueva:

```powershell
git remote add origin https://github.com/bruce89/ForeKast.git
git push -u origin main
```

La autenticación se realiza mediante la configuración Git/GitHub del usuario. Si el remoto ya contiene commits, revisar su historia antes de combinarla; no usar force push para resolverlo automáticamente.

No se incluyó una licencia global: elegirla antes de distribuir públicamente código o recursos. Las condiciones de los datos meteorológicos están documentadas en [WEATHER_API](WEATHER_API.md).

El workflow Android ejecuta build, unit tests y lint al hacer push. `work/`, `local.properties`, certificados locales, APKs y reportes de build están ignorados.
