#!/usr/bin/env bash
# Cambia el identificador de la app (applicationId y paquete Kotlin).
# Uso: scripts/rename-package.sh com.tunombre.blocdenotas
set -euo pipefail

OLD="io.github.tuusuario.blocdenotas"
NEW="${1:-}"

if [[ ! "$NEW" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]]; then
  echo "Uso: $0 nuevo.paquete.valido" >&2
  exit 1
fi

cd "$(dirname "$0")/.."
OLD_PATH="${OLD//.//}"
NEW_PATH="${NEW//.//}"

# 1) Sustituir el texto en fuentes y configuración.
grep -rlF "$OLD" --include='*.kt' --include='*.kts' --include='*.xml' --include='*.md' \
  --include='*.pro' --include='*.properties' --include='*.yml' . \
  | xargs -r sed -i "s/${OLD//./\\.}/${NEW}/g"

# 2) Mover los directorios de código.
for set in main test androidTest; do
  src="app/src/$set/java/$OLD_PATH"
  dst="app/src/$set/java/$NEW_PATH"
  if [[ -d "$src" ]]; then
    mkdir -p "$dst"
    cp -a "$src/." "$dst/"
    rm -rf "$src"
    find "app/src/$set/java" -type d -empty -delete
  fi
done

echo "Hecho. Nuevo paquete: $NEW"
echo "Sincroniza Gradle y revisa 'git status'."
