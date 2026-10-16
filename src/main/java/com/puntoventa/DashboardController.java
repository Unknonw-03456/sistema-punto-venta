package com.puntoventa;

import java.io.File;
import java.time.LocalDate;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.puntoventa.dao.CategoriaDAO;
import com.puntoventa.dao.ClienteDAO;
import com.puntoventa.dao.KpiDAO;
import com.puntoventa.dao.ProductoDAO;
import com.puntoventa.dao.RolDAO;
import com.puntoventa.dao.UsuarioDAO;
import modelo.Categoria;
import modelo.Cliente;
import modelo.Producto;
import modelo.Rol;
import modelo.UsuarioAdmin;

public class DashboardController {

    // Rol de la persona que inició sesión; se usa para mostrar/ocultar y habilitar/deshabilitar acciones.
    private final String rolActual = Sesion.getRolActual();

    public Scene crearEscena() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                crearTabInicio(),
                crearTabProductos(),
                crearTabClientes(),
                crearTabCategorias(),
                crearTabReportes()
        );
        if ("Administrador".equals(rolActual)) {
            tabPane.getTabs().add(crearTabUsuarios());
        }
        Scene escena = new Scene(tabPane, 900, 600);
        escena.getStylesheets().add(getClass().getResource("/com/puntoventa/css/styles.css").toExternalForm());

        // Diseño responsive: según el ancho de la ventana se aplica una clase CSS
        // distinta (escritorio / tableta / teléfono), que ajusta tamaños de letra,
        // tarjetas y botones. Los FlowPane de la pestaña Inicio ya reacomodan sus
        // tarjetas y gráficos solos cuando el ancho disponible cambia.
        escena.widthProperty().addListener((obs, anchoAnterior, anchoNuevo) ->
                actualizarModoResponsive(tabPane, anchoNuevo.doubleValue()));
        actualizarModoResponsive(tabPane, escena.getWidth());

        return escena;
    }

    private void actualizarModoResponsive(javafx.scene.Parent raiz, double ancho) {
        raiz.getStyleClass().removeAll("modo-escritorio", "modo-tableta", "modo-telefono");
        if (ancho < 500) {
            raiz.getStyleClass().add("modo-telefono");
        } else if (ancho < 1000) {
            raiz.getStyleClass().add("modo-tableta");
        } else {
            raiz.getStyleClass().add("modo-escritorio");
        }
    }

    // ================= INICIO (KPIs y gráficos) =================

    private static class KpiDatos {
        int totalProductos;
        int totalClientes;
        int categoriasRegistradas;
        int stockBajo;
        double valorInventario;
        Map<String, Integer> productosPorCategoria;
        Map<String, Integer> stockPorCategoria;
    }

    private Tab crearTabInicio() {
        VBox contenedor = new VBox(16);
        contenedor.setPadding(new Insets(16));

        Label lblTitulo = new Label("Panel general");
        lblTitulo.getStyleClass().add("label-titulo");

        Label lblCargando = new Label("Cargando indicadores...");

        // FlowPane en vez de HBox: las tarjetas y los gráficos pasan a la siguiente
        // fila solos cuando la ventana se angosta (tableta/teléfono), sin código extra.
        FlowPane filaKpis = new FlowPane(14, 14);
        FlowPane filaGraficos = new FlowPane(14, 14);

        contenedor.getChildren().addAll(lblTitulo, lblCargando);

        // Las consultas corren en un hilo aparte (Task) para no congelar la interfaz.
        Task<KpiDatos> tarea = new Task<>() {
            @Override
            protected KpiDatos call() {
                KpiDAO dao = new KpiDAO();
                KpiDatos datos = new KpiDatos();
                datos.totalProductos = dao.contarProductos();
                datos.totalClientes = dao.contarClientes();
                datos.categoriasRegistradas = dao.contarCategorias();
                datos.stockBajo = dao.contarStockBajo();
                datos.valorInventario = dao.calcularValorInventario();
                datos.productosPorCategoria = dao.productosPorCategoria();
                datos.stockPorCategoria = dao.stockPorCategoria();
                return datos;
            }
        };

        tarea.setOnSucceeded(e -> {
            KpiDatos datos = tarea.getValue();
            contenedor.getChildren().remove(lblCargando);

            filaKpis.getChildren().addAll(
                    crearTarjetaKpi("Productos", String.valueOf(datos.totalProductos)),
                    crearTarjetaKpi("Clientes", String.valueOf(datos.totalClientes)),
                    crearTarjetaKpi("Stock bajo (<10)", String.valueOf(datos.stockBajo))
            );
            // El valor monetario del inventario no se muestra a Operador; en su lugar ve categorías.
            if ("Operador".equals(rolActual)) {
                filaKpis.getChildren().add(crearTarjetaKpi("Categorías", String.valueOf(datos.categoriasRegistradas)));
            } else {
                filaKpis.getChildren().add(crearTarjetaKpi("Valor inventario", String.format("$%.2f", datos.valorInventario)));
            }

            BarChart<String, Number> graficoBarras = crearGraficoBarras(datos.productosPorCategoria);
            PieChart graficoCircular = crearGraficoCircular(datos.stockPorCategoria);
            graficoBarras.setPrefSize(400, 280);
            graficoCircular.setPrefSize(380, 280);
            filaGraficos.getChildren().addAll(graficoBarras, graficoCircular);

            contenedor.getChildren().addAll(filaKpis, filaGraficos);
        });

        tarea.setOnFailed(e -> lblCargando.setText("No se pudieron cargar los indicadores."));

        new Thread(tarea).start();

        Tab tab = new Tab("Inicio", contenedor);
        tab.setClosable(false);
        return tab;
    }

    private VBox crearTarjetaKpi(String descripcion, String valor) {
        Label lblValor = new Label(valor);
        lblValor.getStyleClass().add("label-valor");
        Label lblDescripcion = new Label(descripcion);
        lblDescripcion.getStyleClass().add("label-descripcion");

        VBox tarjeta = new VBox(4, lblValor, lblDescripcion);
        tarjeta.getStyleClass().add("tarjeta-kpi");
        tarjeta.setPrefWidth(170);
        return tarjeta;
    }

    private BarChart<String, Number> crearGraficoBarras(Map<String, Integer> datos) {
        CategoryAxis ejeX = new CategoryAxis();
        NumberAxis ejeY = new NumberAxis();
        ejeX.setLabel("Categoría");
        ejeY.setLabel("Cantidad de productos");

        BarChart<String, Number> grafico = new BarChart<>(ejeX, ejeY);
        grafico.setTitle("Productos por categoría");
        grafico.setLegendVisible(false);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (Map.Entry<String, Integer> entrada : datos.entrySet()) {
            serie.getData().add(new XYChart.Data<>(entrada.getKey(), entrada.getValue()));
        }
        grafico.getData().add(serie);
        return grafico;
    }

    private PieChart crearGraficoCircular(Map<String, Integer> datos) {
        PieChart grafico = new PieChart();
        grafico.setTitle("Stock por categoría");
        for (Map.Entry<String, Integer> entrada : datos.entrySet()) {
            grafico.getData().add(new PieChart.Data(entrada.getKey(), entrada.getValue()));
        }
        return grafico;
    }

    // ================= PRODUCTOS (con modales) =================
    // Permisos: Administrador todo. Supervisor ve/crea/edita, no elimina. Operador solo ve.

    private Tab crearTabProductos() {
        TableView<Producto> tabla = new TableView<>();
        tabla.setPlaceholder(new Label("No hay productos registrados."));
        tabla.getStyleClass().add("table-view");

        TableColumn<Producto, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));

        TableColumn<Producto, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));

        TableColumn<Producto, Double> colPrecio = new TableColumn<>("Precio");
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));

        TableColumn<Producto, Integer> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        tabla.getColumns().addAll(colCodigo, colNombre, colPrecio, colStock);
        tabla.setItems(cargarProductos());

        Button btnNuevo = new Button("Nuevo");
        btnNuevo.getStyleClass().add("button-primario");
        Button btnEditar = new Button("Editar");
        Button btnEliminar = new Button("Eliminar");
        btnEliminar.getStyleClass().add("button-peligro");

        boolean esOperador = "Operador".equals(rolActual);
        boolean esAdmin = "Administrador".equals(rolActual);
        ocultar(btnNuevo, esOperador);
        ocultar(btnEditar, esOperador);
        ocultar(btnEliminar, !esAdmin);

        btnNuevo.setOnAction(e -> {
            Stage owner = (Stage) tabla.getScene().getWindow();
            ProductoModal.mostrar(owner, null, () -> tabla.setItems(cargarProductos()));
        });

        btnEditar.setOnAction(e -> {
            Producto seleccionado = tabla.getSelectionModel().getSelectedItem();
            if (seleccionado == null) {
                new Alert(AlertType.WARNING, "Selecciona un producto para editar.").showAndWait();
                return;
            }
            Stage owner = (Stage) tabla.getScene().getWindow();
            ProductoModal.mostrar(owner, seleccionado, () -> tabla.setItems(cargarProductos()));
        });

        btnEliminar.setOnAction(e -> {
            Producto seleccionado = tabla.getSelectionModel().getSelectedItem();
            if (seleccionado == null) {
                new Alert(AlertType.WARNING, "Selecciona un producto para eliminar.").showAndWait();
                return;
            }
            Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                    "¿Eliminar el producto \"" + seleccionado.getNombre() + "\"? Esta acción no se puede deshacer.");
            confirmacion.setTitle("Confirmar eliminación");
            confirmacion.showAndWait().ifPresent(respuesta -> {
                if (respuesta == ButtonType.OK) {
                    try {
                        new ProductoDAO().eliminar(seleccionado.getCodigo());
                        tabla.setItems(cargarProductos());
                    } catch (Exception ex) {
                        new Alert(AlertType.ERROR, "No se pudo eliminar: " + ex.getMessage()).showAndWait();
                    }
                }
            });
        });

        HBox botones = new HBox(10, btnNuevo, btnEditar, btnEliminar);
        botones.setPadding(new Insets(10, 0, 0, 0));

        VBox contenido = new VBox(10, tabla, botones);
        contenido.setPadding(new Insets(10));

        Tab tab = new Tab("Productos", contenido);
        tab.setClosable(false);
        return tab;
    }

    // ================= CLIENTES (con modales) =================
    // Permisos: Administrador todo. Supervisor ve/crea/edita, no elimina. Operador ve/crea, no edita ni elimina.

    private Tab crearTabClientes() {
        TableView<Cliente> tabla = new TableView<>();
        tabla.setPlaceholder(new Label("No hay clientes registrados."));
        tabla.getStyleClass().add("table-view");

        TableColumn<Cliente, String> colCedula = new TableColumn<>("Cédula");
        colCedula.setCellValueFactory(new PropertyValueFactory<>("cedula"));

        TableColumn<Cliente, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));

        TableColumn<Cliente, String> colTelefono = new TableColumn<>("Teléfono");
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        TableColumn<Cliente, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        tabla.getColumns().addAll(colCedula, colNombre, colTelefono, colEmail);
        tabla.setItems(cargarClientes());

        Button btnNuevo = new Button("Nuevo");
        btnNuevo.getStyleClass().add("button-primario");
        Button btnEditar = new Button("Editar");
        Button btnEliminar = new Button("Eliminar");
        btnEliminar.getStyleClass().add("button-peligro");

        boolean esOperador = "Operador".equals(rolActual);
        boolean esAdmin = "Administrador".equals(rolActual);
        ocultar(btnEditar, esOperador);
        ocultar(btnEliminar, !esAdmin);

        btnNuevo.setOnAction(e -> {
            Stage owner = (Stage) tabla.getScene().getWindow();
            ClienteModal.mostrar(owner, null, () -> tabla.setItems(cargarClientes()));
        });

        btnEditar.setOnAction(e -> {
            Cliente seleccionado = tabla.getSelectionModel().getSelectedItem();
            if (seleccionado == null) {
                new Alert(AlertType.WARNING, "Selecciona un cliente para editar.").showAndWait();
                return;
            }
            Stage owner = (Stage) tabla.getScene().getWindow();
            ClienteModal.mostrar(owner, seleccionado, () -> tabla.setItems(cargarClientes()));
        });

        btnEliminar.setOnAction(e -> {
            Cliente seleccionado = tabla.getSelectionModel().getSelectedItem();
            if (seleccionado == null) {
                new Alert(AlertType.WARNING, "Selecciona un cliente para eliminar.").showAndWait();
                return;
            }
            Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                    "¿Eliminar al cliente \"" + seleccionado.getNombre() + "\"? Esta acción no se puede deshacer.");
            confirmacion.setTitle("Confirmar eliminación");
            confirmacion.showAndWait().ifPresent(respuesta -> {
                if (respuesta == ButtonType.OK) {
                    try {
                        new ClienteDAO().eliminar(seleccionado.getCedula());
                        tabla.setItems(cargarClientes());
                    } catch (Exception ex) {
                        new Alert(AlertType.ERROR, "No se pudo eliminar: " + ex.getMessage()).showAndWait();
                    }
                }
            });
        });

        HBox botones = new HBox(10, btnNuevo, btnEditar, btnEliminar);
        botones.setPadding(new Insets(10, 0, 0, 0));

        VBox contenido = new VBox(10, tabla, botones);
        contenido.setPadding(new Insets(10));

        Tab tab = new Tab("Clientes", contenido);
        tab.setClosable(false);
        return tab;
    }

    // ================= CATEGORIAS (formulario inline) =================
    // Permisos: Operador no puede agregar.

    private Tab crearTabCategorias() {
        TableView<Categoria> tabla = new TableView<>();
        tabla.setPlaceholder(new Label("No hay categorías registradas."));
        tabla.getStyleClass().add("table-view");

        TableColumn<Categoria, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("idCategoria"));

        TableColumn<Categoria, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));

        TableColumn<Categoria, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));

        tabla.getColumns().addAll(colId, colNombre, colDescripcion);
        tabla.setItems(cargarCategorias());

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre");
        TextField txtDescripcion = new TextField();
        txtDescripcion.setPromptText("Descripción");

        Label lblMensaje = new Label();

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.setPadding(new Insets(10));
        form.addRow(0, new Label("Nombre:"), txtNombre, new Label("Descripción:"), txtDescripcion);

        Button btnAgregar = new Button("Agregar categoría");
        btnAgregar.getStyleClass().add("button-primario");
        ocultar(btnAgregar, "Operador".equals(rolActual));

        btnAgregar.setOnAction(e -> {
            try {
                new CategoriaDAO().crear(txtNombre.getText(), txtDescripcion.getText());

                lblMensaje.setText("Categoría agregada correctamente.");
                txtNombre.clear();
                txtDescripcion.clear();
                tabla.setItems(cargarCategorias());
            } catch (Exception ex) {
                lblMensaje.setText("Error: " + ex.getMessage());
            }
        });

        VBox contenido = new VBox(10, tabla, form, btnAgregar, lblMensaje);
        contenido.setPadding(new Insets(10));

        Tab tab = new Tab("Categorías", contenido);
        tab.setClosable(false);
        return tab;
    }

    // ================= USUARIOS (solo Administrador) =================

    private Tab crearTabUsuarios() {
        TableView<UsuarioAdmin> tabla = new TableView<>();
        tabla.setPlaceholder(new Label("No hay usuarios registrados."));
        tabla.getStyleClass().add("table-view");

        TableColumn<UsuarioAdmin, String> colUsuario = new TableColumn<>("Usuario");
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        TableColumn<UsuarioAdmin, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        TableColumn<UsuarioAdmin, String> colCargo = new TableColumn<>("Cargo");
        colCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));
        TableColumn<UsuarioAdmin, String> colRol = new TableColumn<>("Rol");
        colRol.setCellValueFactory(new PropertyValueFactory<>("rolNombre"));
        TableColumn<UsuarioAdmin, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        tabla.getColumns().addAll(colUsuario, colNombre, colCargo, colRol, colEstado);
        tabla.setItems(cargarUsuarios());

        TextField txtUsuario = new TextField();
        txtUsuario.setPromptText("Usuario");
        PasswordField txtContrasena = new PasswordField();
        txtContrasena.setPromptText("Contraseña temporal");
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre completo");
        TextField txtCargo = new TextField();
        txtCargo.setPromptText("Cargo");
        ComboBox<Rol> comboRol = new ComboBox<>(FXCollections.observableArrayList(new RolDAO().listarRoles()));
        comboRol.setPromptText("Rol");

        Label lblMensaje = new Label();

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.setPadding(new Insets(10));
        form.addRow(0, new Label("Usuario:"), txtUsuario, new Label("Contraseña:"), txtContrasena);
        form.addRow(1, new Label("Nombre:"), txtNombre, new Label("Cargo:"), txtCargo);
        form.addRow(2, new Label("Rol:"), comboRol);

        Button btnCrear = new Button("Crear usuario");
        btnCrear.getStyleClass().add("button-primario");
        btnCrear.setOnAction(e -> {
            lblMensaje.setText("");
            if (txtUsuario.getText().isBlank() || txtContrasena.getText().isBlank()
                    || txtNombre.getText().isBlank() || comboRol.getValue() == null) {
                lblMensaje.setText("Usuario, contraseña, nombre y rol son obligatorios.");
                return;
            }
            try {
                new UsuarioDAO().crearUsuario(txtUsuario.getText(), txtContrasena.getText(),
                        txtNombre.getText(), txtCargo.getText(), comboRol.getValue().getIdRol());
                lblMensaje.setText("Usuario creado correctamente.");
                txtUsuario.clear();
                txtContrasena.clear();
                txtNombre.clear();
                txtCargo.clear();
                comboRol.setValue(null);
                tabla.setItems(cargarUsuarios());
            } catch (Exception ex) {
                lblMensaje.setText("Error: " + ex.getMessage());
            }
        });

        Button btnActivarDesactivar = new Button("Activar / Desactivar seleccionado");
        btnActivarDesactivar.setOnAction(e -> {
            UsuarioAdmin seleccionado = tabla.getSelectionModel().getSelectedItem();
            if (seleccionado == null) {
                new Alert(AlertType.WARNING, "Selecciona un usuario.").showAndWait();
                return;
            }
            boolean nuevoEstado = !"Activo".equals(seleccionado.getEstado());
            try {
                new UsuarioDAO().cambiarEstado(seleccionado.getIdUsuario(), nuevoEstado);
                tabla.setItems(cargarUsuarios());
            } catch (Exception ex) {
                new Alert(AlertType.ERROR, "No se pudo actualizar: " + ex.getMessage()).showAndWait();
            }
        });

        HBox acciones = new HBox(10, btnActivarDesactivar);
        acciones.setPadding(new Insets(10, 0, 0, 0));

        VBox contenido = new VBox(10, tabla, acciones, form, btnCrear, lblMensaje);
        contenido.setPadding(new Insets(10));

        Tab tab = new Tab("Usuarios", contenido);
        tab.setClosable(false);
        return tab;
    }

    private ObservableList<UsuarioAdmin> cargarUsuarios() {
        return FXCollections.observableArrayList(new UsuarioDAO().listarUsuariosAdmin());
    }

    // ================= REPORTES (PDF, contenido según rol) =================

    private Tab crearTabReportes() {
        Label lblTitulo = new Label("Generar informe de productos y clientes");
        lblTitulo.getStyleClass().add("label-titulo");

        Label lblExplicacion = new Label(
                "Administrador".equals(rolActual) ? "Tu informe incluye productos, clientes y el listado de usuarios."
                : "Supervisor".equals(rolActual) ? "Tu informe incluye productos y clientes completos."
                : "Tu informe incluye productos (sin precio) y clientes (sin datos de contacto).");
        lblExplicacion.setWrapText(true);

        DatePicker dpDesde = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker dpHasta = new DatePicker(LocalDate.now());

        Label lblMensaje = new Label();

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10, 0, 10, 0));
        form.addRow(0, new Label("Desde:"), dpDesde, new Label("Hasta:"), dpHasta);

        Button btnGenerar = new Button("Generar informe PDF");
        btnGenerar.getStyleClass().add("button-primario");
        btnGenerar.setOnAction(e -> {
            lblMensaje.setText("");
            LocalDate desde = dpDesde.getValue();
            LocalDate hasta = dpHasta.getValue();
            if (desde == null || hasta == null) {
                lblMensaje.setText("Selecciona ambas fechas.");
                return;
            }
            if (desde.isAfter(hasta)) {
                lblMensaje.setText("La fecha 'Desde' no puede ser posterior a 'Hasta'.");
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Guardar informe PDF");
            chooser.setInitialFileName("informe_" + LocalDate.now() + ".pdf");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo PDF", "*.pdf"));
            Stage owner = (Stage) btnGenerar.getScene().getWindow();
            File destino = chooser.showSaveDialog(owner);
            if (destino == null) {
                return;
            }

            try {
                ReportePdfGenerator.generar(destino, desde, hasta);
                lblMensaje.setText("Informe generado: " + destino.getName());
            } catch (Exception ex) {
                lblMensaje.setText("Error al generar el informe: " + ex.getMessage());
            }
        });

        VBox contenido = new VBox(14, lblTitulo, lblExplicacion, form, btnGenerar, lblMensaje);
        contenido.setPadding(new Insets(14));

        Tab tab = new Tab("Reportes", contenido);
        tab.setClosable(false);
        return tab;
    }

    // ================= UTILIDAD DE PERMISOS =================

    /** Oculta por completo un control (no solo lo deshabilita) cuando ocultarlo es true. */
    private void ocultar(javafx.scene.Node nodo, boolean ocultarlo) {
        nodo.setVisible(!ocultarlo);
        nodo.setManaged(!ocultarlo);
    }

    // ================= CARGA DE DATOS (delegada a los DAO) =================

    private ObservableList<Producto> cargarProductos() {
        return new ProductoDAO().listar();
    }

    private ObservableList<Cliente> cargarClientes() {
        return new ClienteDAO().listar();
    }

    private ObservableList<Categoria> cargarCategorias() {
        return new CategoriaDAO().listar();
    }
}
