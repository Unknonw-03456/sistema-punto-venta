# Despliegue controlado por SFTP (WinSCP)

1. En WinSCP crear una sesión guardada llamada `SFTP-PRUEBAS` (host, puerto, usuario de despliegue,
   ruta remota `/app-staging`) y **aceptar/verificar la huella del servidor** la primera vez.
   La contraseña o la clave privada se guardan en WinSCP, NUNCA en Git.
2. Vista previa (no publica):
   `winscp.com /script="deploy\deploy-preview.txt" /log="deploy\deploy.log"`
3. Revisar el listado y el log. Con autorización, publicar en staging:
   `winscp.com /script="deploy\deploy-staging.txt" /log="deploy\deploy-staging.log"`
4. Smoke tests: abrir la app instalada, login, una venta de prueba y un reporte.
5. Ante fallo grave: ver `docs/rollback.md`.

Reglas: no usar `-delete`, no sincronizar `config.properties` ni datos, no publicar en producción sin aprobación.
