# Bitácora de cambios y trazabilidad

| ID | Fecha | Rama / commit | Módulo | Cambio o incidente | Evidencia | Estado |
|---|---|---|---|---|---|---|
| CH-001 | 2026-11-05 | master | Compras / Anulación / Reportes | Entrega 3.0.0 del plan de mejoras | Doc de entrega + capturas | Cerrado |
| CH-002 | 2026-11-11 | — | Empaquetado | MSI 3.0.0 instalaba la versión vieja: `instalador\app` conservaba el JAR anterior (91 KB, 30/10). Causa: la copia desde `instalador\input` no actualizó el JAR. Corrección: copiar el JAR de `target` a `input` y `app`, regenerar el MSI. | `dir` de los JAR y tamaño distinto del MSI (49,020,928 bytes) | Cerrado |
| CH-003 | 2026-11-15 | feature/... | Regresión | (completar) Mejora pequeña + pruebas RG-01..RG-06 | `mvn clean test`, Actions | Abierto |

Formato de incidente: ID, fecha, commit, módulo, síntomas, pasos de reproducción, evidencia, causa probable, corrección, pruebas posteriores, estado final.
