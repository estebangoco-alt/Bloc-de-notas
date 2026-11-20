# Guía para contribuir

¡Gracias por querer ayudar! Esta guía explica cómo hacerlo sin sorpresas.

## Antes de empezar

- Busca primero en las [incidencias](../../issues) por si ya existe lo que quieres proponer.
- Para cambios grandes (una pantalla nueva, un cambio de datos), abre una incidencia y habla antes de programar.
- Sé amable y paciente. Todas las personas son bienvenidas.

## Reglas del proyecto

Estas reglas existen para poder publicar en F-Droid y no se negocian:

1. **Solo software libre.** Ninguna dependencia ni herramienta de compilación propietaria.
2. **Sin Google Play Services, Firebase, analítica ni anuncios.**
3. **Sin permiso de internet** salvo en una función opcional y explícita (por ejemplo, sincronización) que el usuario active.
4. **Sin descargar código ejecutable** en tiempo de ejecución.
5. **Los datos son del usuario:** todo local, exportable y sin cuentas obligatorias.
6. Los recursos (iconos, fuentes, imágenes) deben tener una licencia que permita redistribuirlos. Indica su origen.

## Contacto

Lo mejor es usar las [incidencias](../../issues) de GitHub: así todo queda público y otras personas pueden ayudar. Si no tienes cuenta de GitHub o prefieres algo privado, escribe a **unedited@tutamail.com**.

## Cómo informar de un error

Abre una incidencia con:

- Qué esperabas y qué pasó.
- Pasos para reproducirlo.
- Versión de la app, versión de Android y modelo del móvil.
- Una captura, si ayuda. No incluyas datos personales.

## Cómo proponer una función

Explica el problema que resuelve, no solo la solución. Si encaja con la [hoja de ruta](docs/ROADMAP.md), indica en qué versión. Mantén el espíritu de la app: apuntar rápido, sin fricción.

## Preparar el entorno

```bash
git clone https://github.com/estebangoco-alt/Bloc-de-notas.git
cd Bloc-de-notas
./gradlew assembleDebug
./gradlew test
```

Necesitas JDK 17 y el Android SDK (o Android Studio).

## Flujo de trabajo

1. Haz un *fork* y crea una rama desde `main`: `feature/nombre-corto` o `fix/nombre-corto`.
2. Haz cambios pequeños y con un solo objetivo.
3. Añade o actualiza pruebas cuando cambies lógica.
4. Comprueba que compila y que las pruebas pasan.
5. Abre un *pull request* explicando qué cambia y por qué. Enlaza la incidencia.

## Estilo de código

- Kotlin con la [guía de estilo oficial](https://kotlinlang.org/docs/coding-conventions.html).
- Interfaz con Jetpack Compose; los textos van en recursos (`strings.xml`), nunca escritos en el código.
- Nombres de variables y funciones en inglés; comentarios y documentación pueden ir en español o inglés.

## Mensajes de commit

Formato recomendado, en presente y en una línea:

```text
tipo: resumen corto

feat: añade la bandeja de entrada
fix: corrige la hora del recordatorio
docs: actualiza el README
```

Tipos habituales: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.

## Accesibilidad y diseño

- Zonas táctiles de al menos 48 dp.
- Contraste de texto suficiente en tema claro y oscuro.
- Todo icono sin texto lleva descripción para lectores de pantalla.

## Traducciones

Los textos están en `app/src/main/res/values/strings.xml`. Para añadir un idioma, crea `values-xx/strings.xml` y traduce todas las cadenas. Los textos de la ficha de F-Droid van en `fastlane/metadata/android/xx-XX/`.

## Licencia de las contribuciones

Al enviar una contribución aceptas que se publique bajo la licencia del proyecto (GPL-3.0 o posterior).
