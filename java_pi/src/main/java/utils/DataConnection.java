package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

public class DataConnection {
    // Database configuration for XAMPP
    private static final String URL = "jdbc:mariadb://localhost:3306/test";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // Connection singleton
    private static Connection connection;

    private DataConnection() {
        // Private constructor to prevent instantiation
    }

    public static void initializeDatabase() throws SQLException {
        try {
            // First ensure we can connect to the database
            getConnection();
            
            // Only initialize if tables don't exist
            if (!tablesExist()) {
                System.out.println("Tables do not exist. Creating schema...");
                // Read and execute the schema.sql file
                String schemaSQL = readSchemaFile();
                if (schemaSQL != null && !schemaSQL.trim().isEmpty()) {
                    executeSchemaStatements(schemaSQL);
                }
            } else {
                System.out.println("Database tables already exist. Skipping initialization.");
            }
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            throw e; // Re-throw to let the application handle it
        }
    }

    private static boolean tablesExist() {
        try (Connection conn = getConnection()) {
            ResultSet tables = conn.getMetaData().getTables(null, null, "partenaire", null);
            boolean partenaireExists = tables.next();
            
            tables = conn.getMetaData().getTables(null, null, "partnership", null);
            boolean partnershipExists = tables.next();
            
            return partenaireExists && partnershipExists;
        } catch (SQLException e) {
            System.err.println("Error checking tables: " + e.getMessage());
            return false;
        }
    }

    private static String readSchemaFile() {
        try {
            var inputStream = DataConnection.class.getResourceAsStream("/sql/schema.sql");
            if (inputStream == null) {
                System.err.println("Could not find schema.sql in resources");
                return null;
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error reading schema.sql: " + e.getMessage());
            return null;
        }
    }

    private static void executeSchemaStatements(String schemaSQL) throws SQLException {
        // Split SQL into individual statements and filter out empty ones
        String[] statements = Arrays.stream(schemaSQL.split(";"))
                                  .map(String::trim)
                                  .filter(s -> !s.isEmpty())
                                  .filter(s -> !s.toLowerCase().contains("drop table")) // Skip DROP TABLE statements
                                  .toArray(String[]::new);

        // Execute each statement
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            for (String statement : statements) {
                try {
                    stmt.execute(statement);
                    System.out.println("Successfully executed SQL statement");
                } catch (SQLException e) {
                    // Log the error but continue with other statements
                    if (e.getMessage().contains("already exists")) {
                        System.out.println("Table or index already exists (this is okay)");
                    } else {
                        System.err.println("Error executing SQL statement: " + e.getMessage());
                        System.err.println("Statement was: " + statement);
                    }
                }
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Load MariaDB JDBC driver
                Class.forName("org.mariadb.jdbc.Driver");
                
                // Create new connection
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Successfully connected to database");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MariaDB JDBC Driver not found", e);
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("Database connection closed successfully");
                }
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
    public static void testConnection() throws SQLException {
        try {
            Connection conn = getConnection();
            System.out.println("Connexion réussie : " + conn);
            System.out.println("Connected to: " + conn.getMetaData().getURL());
            System.out.println("Database product: " + conn.getMetaData().getDatabaseProductName());
            System.out.println("Database version: " + conn.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            System.err.println("Échec de la connexion à la base de données : " + e.getMessage());
            throw new SQLException("Échec de la connexion à la base de données : " + e.getMessage(), e);
        }
    }
}
