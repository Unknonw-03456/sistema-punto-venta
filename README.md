# Sistema Punto de Venta - Tienda de Refrigeración

Proyecto de la asignatura **Programación II** (UNEV) — Tarea No. 1.

## Descripción

Sistema de escritorio en Java + JavaFX para gestionar ventas e inventario de una tienda de refrigeración (equipos, compresores, repuestos y accesorios). Permite registrar productos, clientes y empleados, y generar ventas con detalle de productos, actualizando el stock automáticamente.

El proyecto evolucionará durante el cuatrimestre: escritorio (JavaFX) → web → móvil.

## Tecnologías

- Java 21+
- JavaFX 21
- Maven
- Scene Builder (diseño de las pantallas FXML)
- Git / GitHub

## Estructura de clases

- `Producto` — productos del inventario (código, nombre, precio, stock).
- `Cliente` — clientes de la tienda.
- `Empleado` — empleados, usados también para el login del sistema.
- `Venta` — una venta, asociada a un Cliente y un Empleado.
- `DetalleVenta` — cada producto vendido dentro de una Venta.

## Funcionalidades implementadas

- **Login**: validación de usuario y contraseña contra la clase `Empleado`.
- **Registro**: formulario para crear nuevos usuarios, con validaciones de campos y confirmación de contraseña.

## Cómo ejecutar el proyecto

```
mvn clean javafx:run
```

## Autor

Proyecto individual — Programación II, UNEV.
