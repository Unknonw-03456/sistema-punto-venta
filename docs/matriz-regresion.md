# Matriz de regresión — POS

Línea base: versión 3.0.0 (commit de la entrega del 5 nov 2026). El estado se verifica, no se supone.
IDs RG = automáticas; M = manuales. Columna "Cómo se prueba": JUnit = automática (`RegresionPOSTest`), Manual = en la app.

| ID | Función existente | Cómo se prueba | Resultado esperado | Estado (llenar con evidencia real) |
|---|---|---|---|---|
| RG-01 | ITBIS y totales | JUnit | 1 x 500 = 590 |	Aprobada — 17/11/2026. Actions en verde (commit 3253698). Se rompió a propósito el valor esperado y Actions salió en rojo; al restaurarlo volvió a verde. |
| RG-02 | Descuento + ITBIS | JUnit | 2 x 500, 10 % = 1062 |Aprobada — 17/11/2026, mvn BUILD SUCCESS y Actions en verde|
| RG-03 | Efectivo y cambio | JUnit | cambio 10; error si no alcanza |Aprobada — 17/11/2026, mvn BUILD SUCCESS y Actions en verde |
| RG-04 | Permisos de anulación | JUnit | solo Admin/Supervisor | Aprobada — 17/11/2026, mvn BUILD SUCCESS y Actions en verde |
| RG-05 | Reportes por rol | JUnit | cada rol ve solo los suyos | Aprobada — 17/11/2026, mvn BUILD SUCCESS y Actions en verde |
| RG-06 | Subtotal de compras | JUnit | cantidad x costo | 	Aprobada — 17/11/2026, mvn BUILD SUCCESS y Actions en verde |
| RG-07 | Descuento máximo por rol (mejora nueva) | JUnit | Cajero máx. 10 % | Aprobada — 17/11/2026, mvn BUILD SUCCESS y Actions en verde |
| RG-08 | La mejora no altera el cálculo de factura | JUnit | total 1062 con 10 % | Aprobada — 17/11/2026, mvn BUILD SUCCESS y Actions en verde |
| M-01 | Inicio de sesión por rol | Manual | ingresa y ve su menú |Aprobada — 19/11/2026, admin y operador ingresan y ven su menú |
| M-02 | Registrar venta | Manual | venta y detalle persistidos (tablas `ventas`, `detalle_venta`) | Aprobada — 19/11/2026, factura guardada y visible en la tabla ventas |
| M-03 | Inventario | Manual | stock baja al vender y sube al comprar/anular | Aprobada — 19/11/2026, el stock bajó tras la venta |
| M-04 | Anulación | Manual | estado ANULADA, stock devuelto, reintegro en caja | Aprobada — 19/11/2026, la venta queda ANULADA y el stock vuelve |
| M-05 | Arqueo de caja | Manual | cierre sin diferencia | Aprobada — 19/11/2026, el cierre calcula la diferencia |
| M-06 | Factura PDF | Manual | factura clara con ITBIS y total | Aprobada — 19/11/2026, PDF con ITBIS y total |
| M-07 | Rollback con git revert | Manual |Aprobada — 20/11/2026. git revert -m 1 feb1f30 creó b5e742a; PoliticaDescuento.java desapareció; mvn clean test BUILD SUCCESS (25 pruebas, sin RegresionPOSTest)|

