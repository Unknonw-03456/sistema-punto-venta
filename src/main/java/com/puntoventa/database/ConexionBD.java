package com.puntoventa.database;

import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Punto único de acceso a la base de datos. Las credenciales ya no están escritas
 * en el código: se leen de config.properties (en src/main/resources). La conexión
 * se obtiene a través de un pool administrado por HikariCP en vez de abrir una
 * conexión nueva "a mano" cada vez.
 */
public class ConexionBD {

    private static HikariDataSource dataSource;
    private static Connection conexion;

    static {
        try {
            Properties propiedades = cargarConfiguracion();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(propiedades.getProperty("db.url"));
            config.setUsername(propiedades.getProperty("db.usuario"));
            config.setPassword(propiedades.getProperty("db.password"));
            config.setMaximumPoolSize(
                    Integer.parseInt(propiedades.getProperty("db.pool.maximo", "10")));
            config.setPoolName("PuntoVentaPool");

            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            System.err.println("No se pudo inicializar el pool de conexiones: " + e.getMessage());
        }
    }

    private static Properties cargarConfiguracion() throws Exception {
        Properties propiedades = new Properties();
        try (InputStream entrada = ConexionBD.class.getResourceAsStream("/config.properties")) {
            if (entrada == null) {
                throw new RuntimeException(
                        "No se encontró config.properties en src/main/resources.");
            }
            propiedades.load(entrada);
        }
        return propiedades;
    }

    // Patrón singleton: solo existe una conexión activa a la vez, tomada del pool de HikariCP.
    public static Connection getConexion() {
        try {
            if (conexion == null || conexion.isClosed()) {
                conexion = dataSource.getConnection();
                System.out.println("Conexión exitosa a la base de datos.");
            }
        } catch (Exception e) {
            System.err.println("Error de conexión: " + e.getMessage());
        }
        return conexion;
    }

    // Metodo de prueba rapida: puedes correr esta clase directamente
    public static void main(String[] args) {
        Connection c = getConexion();
        if (c != null) {
            System.out.println("La aplicación se conectó correctamente a sistema_punto_venta.");
        } else {
            System.out.println("No se pudo establecer conexión.");
        }
    }
}
