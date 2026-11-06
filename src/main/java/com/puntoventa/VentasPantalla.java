package com.puntoventa;

import java.util.ArrayList;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import com.puntoventa.dao.CajaDAO;
import com.puntoventa.dao.ClienteDAO;
import com.puntoventa.dao.ConfiguracionDAO;
import com.puntoventa.dao.ProductoDAO;
import com.puntoventa.dao.VentaDAO;
import com.puntoventa.service.CalculoFactura;
import modelo.CajaInfo;
import modelo.Cliente;
import modelo.LineaVenta;
import modelo.Producto;

/**
 * Pantalla de ventas: catálogo con búsqueda en vivo, carrito, descuento, ITBIS, método de pago,
 * efectivo recibido y cambio. Los totales se recalculan al instante. Al cobrar, la venta se
 * guarda en una sola transacción (factura, detalle, stock, caja y auditoría).
 */
public class VentasPantalla {

    private static final String[] METODOS = {"Efectivo", "Tarjeta", "Transferencia"};

    public static Node crear() {
        int idUsuario = Sesion.getActual().getId();
        CajaInfo caja = new CajaDAO().obtenerAbierta(idUsuario);

        if (caja == null) {
            Label aviso = new Label("Para vender necesitas una caja abierta. Ve al módulo Caja y ábrela con el monto inicial.");
            aviso.getStyleClass().add("alerta-advertencia");
            aviso.setWrapText(true);
            VBox vacio = new VBox(aviso);
            vacio.setPadding(new Insets(20));
            return vacio;
        }
        return construir(idUsuario, caja);
    }

    private static Node construir(int idUsuario, CajaInfo caja) {
        double itbisPct = ConfiguracionDAO.obtenerNumero("itbis_porcentaje", 18);

        // =================== CATÁLOGO (izquierda) ===================
        ObservableList<Producto> todos = FXCollections.observableArrayList(new ProductoDAO().listar());
        FilteredList<Producto> filtrados = new FilteredList<>(todos, p -> p.getStock() > 0);

        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar producto por código o nombre...");
        txtBuscar.textProperty().addListener((obs, anterior, texto) -> {
            String f = texto == null ? "" : texto.trim().toLowerCase();
            filtrados.setPredicate(p -> p.getStock() > 0 && (f.isEmpty()
                    || p.getCodigo().toLowerCase().contains(f)
                    || p.getNombre().toLowerCase().contains(f)));
        });

        TableView<Producto> tablaCatalogo = new TableView<>();
        tablaCatalogo.setPlaceholder(new Label("No hay productos con stock que coincidan."));
        tablaCatalogo.getStyleClass().add("table-view");
        tablaCatalogo.getColumns().add(VentasPantalla.<Producto>columnaTexto("Código", p -> p.getCodigo()));
        tablaCatalogo.getColumns().add(VentasPantalla.<Producto>columnaTexto("Producto", p -> p.getNombre()));
        tablaCatalogo.getColumns().add(VentasPantalla.<Producto>columnaTexto("Precio", p -> String.format("$%,.2f", p.getPrecio())));
        tablaCatalogo.getColumns().add(VentasPantalla.<Producto>columnaTexto("Stock", p -> String.valueOf(p.getStock())));
        tablaCatalogo.setItems(filtrados);
        VBox.setVgrow(tablaCatalogo, Priority.ALWAYS);

        TextField txtCantidad = new TextField("1");
        txtCantidad.setPrefWidth(70);
        Button btnAgregar = new Button("Agregar al carrito");
        btnAgregar.getStyleClass().add("button-primario");
        HBox filaAgregar = new HBox(8, new Label("Cantidad:"), txtCantidad, btnAgregar);
        filaAgregar.setAlignment(Pos.CENTER_LEFT);

        Label lblCatalogo = new Label("Catálogo");
        lblCatalogo.getStyleClass().add("label-titulo");
        VBox izquierda = new VBox(10, lblCatalogo, txtBuscar, tablaCatalogo, filaAgregar);
        izquierda.setPadding(new Insets(12));

        // =================== CARRITO Y PAGO (derecha) ===================
        ObservableList<LineaVenta> carrito = FXCollections.observableArrayList();

        TableView<LineaVenta> tablaCarrito = new TableView<>();
        tablaCarrito.setPlaceholder(new Label("El carrito está vacío. Agrega productos del catálogo."));
        tablaCarrito.getStyleClass().add("table-view");
        tablaCarrito.getColumns().add(VentasPantalla.<LineaVenta>columnaTexto("Código", l -> l.getCodigo()));
        tablaCarrito.getColumns().add(VentasPantalla.<LineaVenta>columnaTexto("Producto", l -> l.getNombre()));
        tablaCarrito.getColumns().add(VentasPantalla.<LineaVenta>columnaTexto("Cant.", l -> String.valueOf(l.getCantidad())));
        tablaCarrito.getColumns().add(VentasPantalla.<LineaVenta>columnaTexto("Precio", l -> String.format("$%,.2f", l.getPrecioUnitario())));
        tablaCarrito.getColumns().add(VentasPantalla.<LineaVenta>columnaTexto("Subtotal", l -> String.format("$%,.2f", l.getSubtotal())));
        tablaCarrito.setItems(carrito);
        tablaCarrito.setPrefHeight(220);
        VBox.setVgrow(tablaCarrito, Priority.ALWAYS);

        Button btnQuitar = new Button("Quitar seleccionado");
        Button btnVaciar = new Button("Vaciar carrito");
        HBox filaCarrito = new HBox(8, btnQuitar, btnVaciar);

        ComboBox<Cliente> comboCliente = new ComboBox<>(FXCollections.observableArrayList(new ClienteDAO().listar()));
        comboCliente.setConverter(new StringConverter<>() {
            @Override
            public String toString(Cliente c) {
                return c == null ? "" : c.getNombre() + " (" + c.getCedula() + ")";
            }

            @Override
            public Cliente fromString(String s) {
                return null;
            }
        });
        comboCliente.getItems().stream().filter(c -> VentaDAO.CLIENTE_GENERICO.equals(c.getCedula()))
                .findFirst().ifPresent(comboCliente::setValue);
        comboCliente.setMaxWidth(Double.MAX_VALUE);

        TextField txtDescuento = new TextField("0");
        txtDescuento.setPrefWidth(80);
        ComboBox<String> comboMetodo = new ComboBox<>(FXCollections.observableArrayList(METODOS));
        comboMetodo.setValue("Efectivo");
        TextField txtEfectivo = new TextField();
        txtEfectivo.setPromptText("Efectivo recibido");

        Label lblSubtotal = new Label("$0.00");
        Label lblDescuento = new Label("$0.00");
        Label lblItbis = new Label("$0.00");
        Label lblCambio = new Label("$0.00");
        Label lblTotal = new Label("$0.00");
        lblTotal.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
        Label lblEstado = new Label();
        lblEstado.getStyleClass().add("alerta-error");
        lblEstado.setWrapText(true);

        GridPane pago = new GridPane();
        pago.setHgap(12);
        pago.setVgap(8);
        int f = 0;
        pago.addRow(f++, new Label("Cliente:"), comboCliente);
        pago.addRow(f++, new Label("Descuento (%):"), txtDescuento);
        pago.addRow(f++, new Label("Método de pago:"), comboMetodo);
        pago.addRow(f++, new Label("Efectivo recibido:"), txtEfectivo);
        pago.addRow(f++, new Label("Subtotal:"), lblSubtotal);
        pago.addRow(f++, new Label("Descuento:"), lblDescuento);
        pago.addRow(f++, new Label("ITBIS (" + formatoPorcentaje(itbisPct) + "%):"), lblItbis);
        pago.addRow(f++, new Label("Cambio:"), lblCambio);
        pago.addRow(f, new Label("TOTAL:"), lblTotal);
        GridPane.setHgrow(comboCliente, Priority.ALWAYS);

        Button btnCobrar = new Button("Cobrar");
        btnCobrar.getStyleClass().add("button-acento");
        btnCobrar.setMaxWidth(Double.MAX_VALUE);
        btnCobrar.setStyle("-fx-font-size: 16px;");
        btnCobrar.setDisable(true);

        Label lblCarrito = new Label("Carrito");
        lblCarrito.getStyleClass().add("label-titulo");
        Label lblCaja = new Label("Caja #" + caja.getIdCaja() + " abierta");
        VBox derecha = new VBox(10, lblCarrito, lblCaja, tablaCarrito, filaCarrito, pago, lblEstado, btnCobrar);
        derecha.setPadding(new Insets(12));

        // =================== LÓGICA ===================

        // Recalcula todos los totales en vivo y decide si se puede cobrar.
        Runnable recalcular = () -> {
            lblEstado.setText("");
            btnCobrar.setDisable(true);
            boolean efectivo = "Efectivo".equals(comboMetodo.getValue());
            txtEfectivo.setDisable(!efectivo);

            if (carrito.isEmpty()) {
                lblSubtotal.setText("$0.00");
                lblDescuento.setText("$0.00");
                lblItbis.setText("$0.00");
                lblTotal.setText("$0.00");
                lblCambio.setText("$0.00");
                return;
            }
            try {
                double descuento = txtDescuento.getText().isBlank() ? 0 : parseNumero(txtDescuento.getText());
                CalculoFactura.Resultado base = CalculoFactura.calcular(new ArrayList<>(carrito), descuento,
                        itbisPct, false, null);
                lblSubtotal.setText(dinero(base.subtotal));
                lblDescuento.setText(dinero(base.descuento));
                lblItbis.setText(dinero(base.itbis));
                lblTotal.setText(dinero(base.total));
                lblCambio.setText("$0.00");

                if (efectivo) {
                    if (txtEfectivo.getText().isBlank()) {
                        lblEstado.setText("Indica el efectivo recibido.");
                        return;
                    }
                    CalculoFactura.Resultado completo = CalculoFactura.calcular(new ArrayList<>(carrito), descuento,
                            itbisPct, true, parseNumero(txtEfectivo.getText()));
                    lblCambio.setText(dinero(completo.cambio));
                }
                btnCobrar.setDisable(false);
            } catch (NumberFormatException ex) {
                lblEstado.setText("El descuento y el efectivo deben ser números.");
            } catch (IllegalArgumentException ex) {
                lblEstado.setText(ex.getMessage());
            }
        };

        carrito.addListener((javafx.collections.ListChangeListener<LineaVenta>) c -> recalcular.run());
        txtDescuento.textProperty().addListener((obs, a, b) -> recalcular.run());
        txtEfectivo.textProperty().addListener((obs, a, b) -> recalcular.run());
        comboMetodo.valueProperty().addListener((obs, a, b) -> recalcular.run());

        Runnable agregar = () -> {
            Producto p = tablaCatalogo.getSelectionModel().getSelectedItem();
            if (p == null) {
                lblEstado.setText("Selecciona un producto del catálogo.");
                return;
            }
            int cantidad;
            try {
                cantidad = Integer.parseInt(txtCantidad.getText().trim());
            } catch (NumberFormatException ex) {
                lblEstado.setText("La cantidad debe ser un número entero.");
                return;
            }
            if (cantidad <= 0) {
                lblEstado.setText("La cantidad debe ser mayor a 0.");
                return;
            }

            int indice = -1;
            int yaEnCarrito = 0;
            for (int i = 0; i < carrito.size(); i++) {
                if (carrito.get(i).getCodigo().equals(p.getCodigo())) {
                    indice = i;
                    yaEnCarrito = carrito.get(i).getCantidad();
                }
            }
            if (yaEnCarrito + cantidad > p.getStock()) {
                lblEstado.setText("Stock insuficiente de \"" + p.getNombre() + "\": disponible " + p.getStock()
                        + ", en el carrito " + yaEnCarrito + ".");
                return;
            }

            LineaVenta nueva = new LineaVenta(p.getCodigo(), p.getNombre(), yaEnCarrito + cantidad, p.getPrecio());
            if (indice >= 0) {
                carrito.set(indice, nueva);
            } else {
                carrito.add(nueva);
            }
            txtCantidad.setText("1");
            recalcular.run();
        };

        btnAgregar.setOnAction(e -> agregar.run());
        tablaCatalogo.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                agregar.run();
            }
        });

        btnQuitar.setOnAction(e -> {
            LineaVenta seleccionada = tablaCarrito.getSelectionModel().getSelectedItem();
            if (seleccionada != null) {
                carrito.remove(seleccionada);
            }
        });
        btnVaciar.setOnAction(e -> carrito.clear());

        btnCobrar.setOnAction(e -> {
            Alert confirmacion = new Alert(AlertType.CONFIRMATION, "¿Registrar la venta por " + lblTotal.getText() + "?");
            confirmacion.setTitle("Confirmar venta");
            confirmacion.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
                btnCobrar.setDisable(true); // evita enviar la misma venta dos veces
                try {
                    boolean efectivo = "Efectivo".equals(comboMetodo.getValue());
                    Cliente cliente = comboCliente.getValue();
                    double descuento = txtDescuento.getText().isBlank() ? 0 : parseNumero(txtDescuento.getText());

                    VentaDAO.ResultadoVenta resultado = new VentaDAO().registrar(
                            idUsuario, caja.getIdCaja(), cliente == null ? null : cliente.getCedula(),
                            new ArrayList<>(carrito), descuento, comboMetodo.getValue(),
                            efectivo ? parseNumero(txtEfectivo.getText()) : null);

                    String mensaje = "Factura " + resultado.numeroFactura + " registrada.\n"
                            + "Total: " + dinero(resultado.totales.total);
                    if (efectivo) {
                        mensaje += "\nCambio: " + dinero(resultado.totales.cambio);
                    }
                    ButtonType btnVer = new ButtonType("Ver / imprimir factura");
                    Alert ok = new Alert(AlertType.INFORMATION, mensaje, btnVer, ButtonType.CLOSE);
                    ok.setTitle("Venta registrada");
                    ok.showAndWait().filter(b -> b == btnVer).ifPresent(b -> FacturaVista.mostrar(resultado.numeroFactura));

                    carrito.clear();
                    txtEfectivo.clear();
                    txtDescuento.setText("0");
                } catch (Exception ex) {
                    new Alert(AlertType.ERROR, "No se pudo registrar la venta: " + ex.getMessage()).showAndWait();
                } finally {
                    // Se recarga el catálogo para ver el stock real después de la venta (o del error).
                    todos.setAll(new ProductoDAO().listar());
                    recalcular.run();
                }
            });
        });

        SplitPane dividido = new SplitPane(izquierda, derecha);
        dividido.setDividerPositions(0.46);
        return dividido;
    }

    // =================== UTILIDADES ===================

    private static <T> TableColumn<T, String> columnaTexto(String titulo, java.util.function.Function<T, String> valor) {
        TableColumn<T, String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(c -> new SimpleStringProperty(valor.apply(c.getValue())));
        return col;
    }

    private static String dinero(java.math.BigDecimal valor) {
        return String.format("$%,.2f", valor);
    }

    private static String formatoPorcentaje(double valor) {
        return valor == Math.floor(valor) ? String.valueOf((long) valor) : String.valueOf(valor);
    }

    /** Acepta "12,500.50" o "12500.50" (quita las comas de miles). */
    private static double parseNumero(String texto) {
        return Double.parseDouble(texto.trim().replace(",", "").replace(" ", ""));
    }
}
