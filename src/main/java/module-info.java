module com.puntoventa {
    requires javafx.controls;
    requires javafx.fxml;
    exports com.puntoventa;
    opens com.puntoventa to javafx.fxml;
}