# Publicar en F-Droid

F-Droid **no tiene cuentas de desarrollador** como Google Play. No te registras ni subes un APK. Lo que haces es proponer la receta de compilación de tu app en un repositorio público (`fdroiddata`), alojado en **GitLab**. F-Droid compila la app a partir de tu código fuente y la firma con su propia clave.

Esta guía resume la [guía oficial de F-Droid](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/). Si algo no coincide, manda la oficial.

## 0. Qué necesitas

- Una cuenta en **GitHub** (donde vive el código) y otra en **GitLab** (solo para enviar la receta).
- Una versión publicada de la app, con su etiqueta de Git.
- Git instalado. Opcional pero útil: `fdroidserver` para comprobar la receta en tu equipo.

## 1. Crear la cuenta de GitLab

1. Entra en <https://gitlab.com> y pulsa **Register**.
2. Crea la cuenta con tu correo o con tu cuenta de GitHub.
3. Confirma el correo.

Con esa cuenta podrás hacer un *fork* de `fdroiddata` y abrir un *merge request* (el equivalente a un pull request).

## 2. Comprobar que la app cumple los requisitos

- Repositorio público con un archivo de licencia libre (GPL-3.0 o posterior) y el código real y actualizado.
- Solo dependencias y herramientas de compilación libres. Sin Firebase ni Google Play Services.
- Si no eres la persona autora, pide permiso al autor.
- Más detalle en la [política de inclusión](https://f-droid.org/docs/Inclusion_Policy).

## 3. Preparar el repositorio de la app

### 3.1 Etiquetar cada versión

Cada versión oficial debe tener una etiqueta de Git que coincida con su `versionName`. Por ejemplo, para la versión `1.0.0`:

```bash
git tag v1.0.0
git push origin v1.0.0
```

### 3.2 Añadir los metadatos de la ficha (estilo Fastlane)

Dentro del repositorio de la app, con esta estructura (no hace falta instalar Fastlane):

```text
fastlane/metadata/android/
├── es-ES/
│   ├── short_description.txt     (menos de 80 caracteres, sin punto final)
│   ├── full_description.txt
│   ├── changelogs/1.txt          (el número es el versionCode, máx. 500 caracteres)
│   └── images/
│       ├── icon.png
│       └── phoneScreenshots/1.png, 2.png ...
└── en-US/
    └── (lo mismo, en inglés)
```

En este repositorio ya tienes ejemplos de `short_description.txt` y `full_description.txt` en español e inglés.

## 4. Enviar la receta a F-Droid

1. En GitLab, abre <https://gitlab.com/fdroid/fdroiddata> y pulsa **Fork**.
2. Clona tu fork y crea una rama desde `master` con el ID de la app:

   ```bash
   git clone https://gitlab.com/TU_USUARIO_GITLAB/fdroiddata.git
   cd fdroiddata
   git checkout -b io.github.estebangocoalt.blocdenotas
   ```

3. Crea el archivo `metadata/io.github.estebangocoalt.blocdenotas.yml`. **El nombre debe ser igual al ID de aplicación.** Puedes partir de una plantilla de la carpeta `templates` o de la receta de una app parecida.

### Ejemplo de receta (adáptala y valídala)

```yaml
Categories:
  - Writing
  - Time
License: GPL-3.0-or-later
AuthorName: Esteban
SourceCode: https://github.com/nicarao-dev/Bloc-de-notas
IssueTracker: https://github.com/nicarao-dev/Bloc-de-notas/issues
Changelog: https://github.com/nicarao-dev/Bloc-de-notas/blob/main/CHANGELOG.md

AutoName: Bloc de notas

RepoType: git
Repo: https://github.com/nicarao-dev/Bloc-de-notas.git

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
CurrentVersion: 1.0.0
CurrentVersionCode: 1
```

Los nombres de campos y categorías pueden cambiar; consulta la [referencia de metadatos](https://f-droid.org/docs/Build_Metadata_Reference) y ejecuta `fdroid lint` antes de enviar.

## 5. Probar la receta

Con `fdroidserver` instalado:

```bash
fdroid readmeta
fdroid rewritemeta io.github.estebangocoalt.blocdenotas
fdroid checkupdates --allow-dirty io.github.estebangocoalt.blocdenotas
fdroid lint io.github.estebangocoalt.blocdenotas
fdroid build io.github.estebangocoalt.blocdenotas
```

Si no quieres instalar nada, sube la rama a tu fork y deja que la integración continua de GitLab la pruebe. Si falla, lee el registro y corrige.

## 6. Abrir el merge request

```bash
git add metadata/io.github.estebangocoalt.blocdenotas.yml
git commit -m "New App: io.github.estebangocoalt.blocdenotas"
git push origin io.github.estebangocoalt.blocdenotas
```

En GitLab, abre un *merge request* de tu rama hacia `fdroiddata`. Responde pronto a las preguntas de los revisores.

## 7. Revisión y publicación

- Los revisores comprueban licencias, sistema de compilación, dependencias no libres, características no deseadas (*anti-features*), rastreo y método de actualización. Pueden retocar la receta o proponer cambios.
- Cuando se fusiona, el servidor de compilación de F-Droid construye la app en su siguiente tanda. Suele aparecer en el repositorio principal entre 24 y 48 horas después.
- A partir de ahí, las actualizaciones se detectan solas si configuraste `AutoUpdateMode` y etiquetas de versión.

## Lista de comprobación final

- [ ] Licencia libre en el repositorio (`LICENSE`)
- [ ] Sin Firebase, Google Play Services, anuncios ni analítica
- [ ] ID de aplicación propio y definitivo
- [ ] Etiqueta de Git para la versión (`v1.0.0`)
- [ ] `versionName` y `versionCode` en `app/build.gradle.kts`
- [ ] Carpeta `fastlane/metadata/android/` con textos, icono y capturas
- [ ] Gradle Wrapper incluido y la app compila con `./gradlew assembleRelease`
- [ ] Receta `.yml` validada con `fdroid lint`
- [ ] Merge request abierto en `fdroiddata`

## Si prefieres que lo haga otra persona

Puedes abrir una petición de empaquetado (*Request For Packaging*) en el gestor de incidencias de `fdroiddata`, o preguntar en el foro de F-Droid. Es más lento que enviar tú mismo el merge request.
