# REGLAS DEL PROYECTO — Sistema Punto de Venta (Refrigeración Dominicana)

Objetivo: mejorar el sistema sin destruir funciones anteriores.
Stack: Java 21+/JavaFX 21, Maven, MySQL 8, HikariCP, OpenPDF, JUnit 5.
Capas: `com.puntoventa` (pantallas) → `service` (reglas) → `dao` (SQL/transacciones) → `modelo`.

## Reglas
1. Inspeccionar arquitectura y dependencias antes de editar.
2. Trabajar en una rama Git (`feature/...`); no modificar `master` directamente.
3. Preservar contratos públicos: firmas de los DAO, esquema de la BD y comportamiento existente.
4. Proponer el cambio mínimo que satisfaga el requisito.
5. Crear o actualizar pruebas; ejecutar la regresión (`mvn clean test`) antes de cada commit.
6. Registrar archivos afectados, causas y evidencias en `docs/change-log.md`.
7. Nunca exponer claves, tokens ni datos sensibles. `config.properties` NO se sube a Git.
8. No usar datos reales para pruebas (clientes, cédulas, contraseñas).
9. No publicar con pruebas críticas fallidas.
10. Pedir aprobación humana para producción y migraciones destructivas (DROP, DELETE, ALTER sobre datos).
11. Preparar respaldo, verificación de salud y rollback antes de publicar (ver `docs/rollback.md`).
12. Distinguir pruebas aprobadas, fallidas y NO ejecutadas; no declarar exitosa una prueba no ejecutada.

## Módulos protegidos (no alterar sin matriz de impacto)
- Facturación: `CalculoFactura`, `VentaDAO` (ITBIS 18 %, descuento, cambio, numeración FAC-000001).
- Caja: `CajaDAO` (apertura, cierre, arqueo; solo efectivo entra al arqueo).
- Inventario: movimientos de stock en `VentaDAO`, `CompraDAO`, `AnulacionDAO`.
- Seguridad: login BCrypt + 2FA, `Roles`, permisos validados en DAO.
- Auditoría: `AuditoriaDAO` (se escribe dentro de la misma transacción).

## Agentes disponibles (carpeta `agents/`)
Orquestador, Protector de módulos, Regresión y QA, Trazabilidad, Integrador, Deploy, Seguridad.
La aprobación de producción es siempre humana (el programador).
