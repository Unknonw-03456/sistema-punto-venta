# Rollback — cómo volver a una versión estable

## Qué se conserva antes de publicar
- Artefacto anterior: `SistemaPuntoVenta-<versión>.msi` guardado en `/app-staging/anteriores/` (y copia local).
- Etiqueta Git de la versión estable (ej. `git tag v3.0.0` y `git push --tags`).
- Respaldo de la BD: exportación de phpMyAdmin (estructura y datos) con fecha antes de cualquier migración.

## Procedimiento
1. Detectar el fallo con los smoke tests (login, venta, reporte) o los logs.
2. Desinstalar la versión nueva (Configuración → Aplicaciones).
3. Reinstalar el MSI anterior guardado.
4. Si hubo cambios de esquema en la BD: restaurar el respaldo (Importar en phpMyAdmin) SOLO si fueron probados; algunas migraciones no son reversibles automáticamente.
5. Repetir los smoke tests y registrar el resultado en `docs/change-log.md`.

## Código
`git revert <commit>` (conserva el historial) o volver a la etiqueta estable en una rama nueva. No reescribir el historial de `master`.
