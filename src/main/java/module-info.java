module com.puntoventa {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires com.github.librepdf.openpdf;
    requires java.desktop;
    requires com.zaxxer.hikari;

    requires mysql.connector.j;
    exports com.puntoventa;
    opens com.puntoventa to javafx.fxml;

    opens modelo to javafx.base;
}