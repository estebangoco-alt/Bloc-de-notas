# Política de seguridad

## Cómo reportar una vulnerabilidad

Si encuentras un fallo de seguridad o de privacidad, **no abras una incidencia pública**. Avisa de forma privada:

1. En GitHub, ve a la pestaña **Security** del repositorio y pulsa **Report a vulnerability** (aviso privado).
2. Cuenta qué pasa, los pasos para reproducirlo, la versión de la app y la de Android.

Responderé lo antes posible, normalmente en una semana. Cuando haya arreglo, publicaré una versión nueva y te mencionaré en el CHANGELOG si quieres.

## Versiones con soporte

Solo la última versión publicada recibe correcciones.

## Qué hace la app por tu seguridad y privacidad

- Todo se guarda en tu dispositivo. Sin cuentas, anuncios ni analítica.
- `allowBackup="false"`: Android no copia tus notas a la nube de Google.
- Permisos mínimos: `INTERNET` solo para la sincronización opcional con tu servidor (solo `https`), micrófono solo al grabar, avisos y alarmas para los recordatorios. La cámara no necesita permiso.
- Las imágenes y notas de voz están en el almacenamiento privado de la app.
- Solo están expuestos al exterior la pantalla principal, «Compartir» (solo texto), los widgets y el arranque para reprogramar los avisos. El texto compartido se limita en tamaño.
- Las copias de seguridad validan los nombres de archivo para que no puedan escribir fuera de su carpeta.

## Limitaciones conocidas

- La copia de seguridad en ZIP **no está cifrada**: guárdala en un lugar de confianza.
- La contraseña de WebDAV se guarda en las preferencias privadas de la app (sin cifrar).
