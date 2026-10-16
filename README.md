# Sistema Punto de Venta - Tienda de Refrigeración

Proyecto de la asignatura **Programación II** (UNEV) — Tarea No. 4 (Release 2.0).

## Descripción

Sistema de escritorio en Java + JavaFX para gestionar ventas e inventario de una tienda de refrigeración (equipos, compresores, repuestos y accesorios). Permite registrar productos, clientes y empleados, generar ventas con detalle de productos actualizando el stock automáticamente, y administrar usuarios por rol con autenticación de dos factores.

El proyecto evolucionará durante el cuatrimestre: escritorio (JavaFX) → web → móvil.

## Tecnologías

- Java 21+ / JDK 25
- JavaFX 21
- Maven
- MySQL 8 (hospedado en la nube vía MyASP.NET)
- HikariCP (pool de conexiones)
- jBCrypt (hash de contraseñas)
- OpenPDF (generación de reportes PDF)
- TOTP/2FA implementado desde cero (javax.crypto)
- Git / GitHub

## Funcionalidades del Release 2.0

1. **Análisis previo del sistema**
2. **Paleta de colores en CSS** (WCAG-AA)
3. **Modales para CRUD** de Productos, Clientes y Categorías
4. **Roles de usuario** (Administrador, Supervisor, Operador) con contraseñas hasheadas en BCrypt y reportes PDF exportables según el rol
5. **Login con 2FA** (TOTP vía QR), bloqueo temporal tras 3 intentos fallidos y bitácora de accesos
6. **Dashboard con KPIs y gráficos** (productos, clientes, stock bajo, valor de inventario)
7. **Diseño responsive** (escritorio / tableta / teléfono)
8. **Conexión mediante patrón DAO**, credenciales externalizadas en `config.properties`, pool de conexiones con HikariCP
9. **Base de datos migrada a MySQL en la nube** (MyASP.NET / site4now.net)

## Configuración de la base de datos

Copia `config.properties.example` a `src/main/resources/config.properties` y completa tus credenciales:

```
db.url=jdbc:mysql://mysql5050.site4now.net:3306/db_acfbe1_ventas
db.usuario=acfbe1_ventas
db.password=David.291998
db.pool.maximo=10
```

El script de creación de la base de datos está en `database/ScriptSQL.sql`.

## Credenciales de prueba

| Rol           | Usuario     | Contraseña |
|---------------|-------------|------------|
| Administrador | admin       | admin123 |
| Supervisor    | supervisor  | super123 |
| Operador      | operador    | oper123 |

> Nota: al ingresar por primera vez con cada usuario, el sistema solicitará configurar la autenticación de dos factores (2FA) escaneando un código QR con una app autenticadora (Google Authenticator, Authy, etc.).

## Cómo ejecutar el proyecto

```
mvn clean javafx:run
```

## Ejecutable

Se incluye una versión empaquetada con `jpackage` (ver carpeta de entrega / release) que no requiere tener Java ni Maven instalados.

## Estructura de clases principales

- `Producto`, `Cliente`, `Empleado`, `Usuario`, `Rol`, `Categoria`, `Proveedor`, `Venta`, `DetalleVenta`, `Compra`, `Pago`, `Inventario`

## Uso de IA en el desarrollo

Este proyecto se desarrolló con asistencia de IA (Claude) como apoyo en distintas fases:

- **Fase 1-2 (análisis y CSS):** apoyo en la definición de la paleta de colores accesible y estructura del documento de análisis.
- **Fase 3 (modales CRUD):** generación de la estructura base de los modales de Producto y Cliente.
- **Fase 4 (roles/BCrypt):** diseño del esquema de permisos por rol, integración de jBCrypt y generación del reporte PDF con OpenPDF.
- **Fase 5 (2FA):** implementación de TOTP desde cero (sin librerías externas), generación de QR y lógica de bloqueo por intentos fallidos.
- **Fase 6 (dashboard):** cálculo de KPIs y construcción de gráficos con JavaFX Charts.
- **Fase 7 (responsive):** lógica de detección de ancho de ventana y clases CSS por modo.
- **Fase 8 (DAO/HikariCP):** refactorización a patrón DAO, externalización de credenciales y configuración del pool de conexiones.
- **Fase 9 (migración a la nube):** guía paso a paso para la configuración del hosting MyASP.NET, resolución de errores de importación SQL y generación del ejecutable con jpackage.

Todo el código generado con IA fue revisado, probado y ajustado por el autor antes de su integración.

## Autor
David Duarte 2018-3200509
Proyecto individual — Programación II, UNEV.
