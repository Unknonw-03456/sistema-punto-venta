# Matriz de regresión — POS

Línea base: versión 3.0.0 (commit de la entrega del 5 nov 2026). El estado se verifica, no se supone.
IDs RG = automáticas; M = manuales. Columna "Cómo se prueba": JUnit = automática (`RegresionPOSTest`), Manual = en la app.

| ID | Función existente | Cómo se prueba | Resultado esperado | Estado (llenar con evidencia real) |
|---|---|---|---|---|
| RG-01 | ITBIS y totales | JUnit | 1 x 500 = 590 | |
| RG-02 | Descuento + ITBIS | JUnit | 2 x 500, 10 % = 1062 | |
| RG-03 | Efectivo y cambio | JUnit | cambio 10; error si no alcanza | |
| RG-04 | Permisos de anulación | JUnit | solo Admin/Supervisor | |
| RG-05 | Reportes por rol | JUnit | cada rol ve solo los suyos | |
| RG-06 | Subtotal de compras | JUnit | cantidad x costo | |
| RG-07 | Descuento máximo por rol (mejora nueva) | JUnit | Cajero máx. 10 % | |
| RG-08 | La mejora no altera el cálculo de factura | JUnit | total 1062 con 10 % | |
| M-01 | Inicio de sesión por rol | Manual | ingresa y ve su menú | |
| M-02 | Registrar venta | Manual | venta y detalle persistidos (tablas `ventas`, `detalle_venta`) | |
| M-03 | Inventario | Manual | stock baja al vender y sube al comprar/anular | |
| M-04 | Anulación | Manual | estado ANULADA, stock devuelto, reintegro en caja | |
| M-05 | Arqueo de caja | Manual | cierre sin diferencia | |
| M-06 | Factura PDF | Manual | factura clara con ITBIS y total | |
