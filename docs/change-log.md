# Bitácora de cambios y trazabilidad

| ID | Fecha | Rama / commit | Módulo | Cambio o incidente | Evidencia | Estado |
|---|---|---|---|---|---|---|
| CH-001 | 2026-11-05 | master | Compras / Anulación / Reportes | Entrega 3.0.0 del plan de mejoras | Documento de entrega y capturas | Cerrado |
| CH-002 | 2026-11-11 | — | Empaquetado | El MSI 3.0.0 instalaba la versión vieja: `instalador\app` conservaba el JAR anterior (91 KB, 30/10). Causa: la copia desde `instalador\input` no actualizó el JAR. Corrección: copiar el JAR de `target` a `input` y `app` y regenerar el MSI | `dir` de los JAR y tamaño distinto del MSI (49,020,928 bytes) | Cerrado |
| CH-003 | 2026-11-17 | feature/descuento-maximo / 3253698 | Ventas / Regresión | Mejora: descuento máximo del 10 % para el Cajero (`PoliticaDescuento` + validación en `VentaDAO`) y 8 pruebas de regresión RG-01 a RG-08 | `mvn clean test` BUILD SUCCESS; Actions en verde (Control de calidad #1) | Cerrado |
| CH-004 | 2026-11-17 | feature/descuento-maximo / 04bca19 | Regresión | Incidente provocado a propósito: se alteró el valor esperado de RG-01 (590.00 a 591.00). Primer intento sin efecto: se cambió el nombre de la prueba y no el `assertEquals`, y Actions salió verde. Segundo intento: Actions en rojo, el flujo bloqueó el cambio | Capturas de Actions en rojo y del detalle del error | Cerrado |
| CH-005 | 2026-11-17 | feature/descuento-maximo | Regresión | RG-01 restaurado al valor correcto (590.00); Actions vuelve a verde | Captura de Actions en verde | Cerrado |
| CH-006 | 2026-11-17 | master | Integración | Pull Request de feature/descuento-maximo a master, con chequeo verde y merge | Captura del Pull Request | Cerrado |
| CH-007 | 2026-11-17 | — | Entorno | Git falló con "No space left on device" por disco C: lleno. Se liberó espacio y se repitió el commit | Mensaje de error y commit posterior | Cerrado |
| CH-008 | 2026-11-18 | — | Despliegue | FTPS a MyASP.NET (WIN8120.SITE4NOW.NET:21): certificado `*.site4now.net` verificado, pero el servidor respondió `530 User cannot log in.` (log de WinSCP). Causa: contraseña de la cuenta FTP raíz. Corrección: restablecida desde Sitios web, site1, FTP. Vista previa y publicación en `/site1/staging` con WinSCP (`deploy-preview.txt` y `deploy-staging.txt`, sin `-delete`) con `version.txt` y README | Log del error 530, capturas de la vista previa, de la publicación y del listado de staging | Cerrado |
| CH-009 | 2026-11-19 | prueba/rollback | Despliegue / Rollback | Prueba de rollback con `git revert -m 1` del merge de la mejora, en una rama aparte, sin modificar master | evidencia Capturas de git log, dir y mvn clean test |Cerrado,  2026-11-20. |

Formato de incidente: ID, fecha, commit, módulo, síntomas, pasos de reproducción, evidencia, causa probable, corrección, pruebas posteriores, estado final.
