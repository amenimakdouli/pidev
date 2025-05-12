package utils;

import controllers.PartenaireController;
import controllers.PartnershipController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

public class Main extends Application {
    private Stage primaryStage;
    private String currentFxmlPath = "/fxml/Partnership.fxml"; // Default FXML path, changeable
    private String currentTitle = "Partenaire Management";    // Default title, changeable

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        Thread.currentThread().setContextClassLoader(Main.class.getClassLoader());

        // Initialize database and test connection
        try {
            DataConnection.testConnection();
            System.out.println("Database connection successful!");
            DataConnection.initializeDatabase();
            System.out.println("Database schema initialized successfully");
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            showErrorAndExit("Database Error",
                    "Failed to initialize database",
                    "Error: " + e.getMessage());
            return;
        }

        // Load the initial interface
        loadScene(currentFxmlPath, currentTitle);
    }

    @Override
    public void stop() {
        try {
            System.out.println("Closing database connection...");
            DataConnection.closeConnection();
        } catch (Exception e) {
            System.err.println("Error while closing database connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("Application shutdown complete.");
        }
    }

    public void switchToPartenaire() {
        System.out.println("DEBUG: Switching to Partenaire view");
        currentFxmlPath = "/fxml/partenaire.fxml";
        currentTitle = "Partenaire Management";
        loadScene(currentFxmlPath, currentTitle);
    }

    public void switchToPartnership() {
        System.out.println("DEBUG: Switching to Partnership view");
        currentFxmlPath = "/fxml/Partnership.fxml";
        currentTitle = "Partnership Management";
        loadScene(currentFxmlPath, currentTitle);
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            System.out.println("DEBUG: Loading scene from path: " + fxmlPath);
            URL fxmlUrl = Main.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IOException("Cannot find resource: " + fxmlPath);
            }
            System.out.println("DEBUG: Found FXML at: " + fxmlUrl);

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller == null) {
                throw new RuntimeException("Controller is null for " + fxmlPath);
            }
            System.out.println("DEBUG: Loaded controller: " + controller.getClass().getName());

            // Set the main reference based on controller type
            if (controller instanceof PartnershipController) {
                ((PartnershipController) controller).setMain(this);
                System.out.println("DEBUG: PartnershipController initialized");
            } else if (controller instanceof PartenaireController) {
                ((PartenaireController) controller).setMain(this);
                System.out.println("DEBUG: PartenaireController initialized");
            } else {
                throw new RuntimeException("Unexpected controller type: " + controller.getClass().getName());
            }

            Scene scene = new Scene(root, 800, 600);
            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
            primaryStage.show();
            System.out.println("DEBUG: Scene loaded successfully");

        } catch (Exception e) {
            System.err.println("DEBUG: Error loading scene: " + e.getMessage());
            System.err.println("DEBUG: Exception type: " + e.getClass().getName());
            e.printStackTrace();
            showErrorAndExit("Loading Error",
                    "Failed to load view",
                    "Could not load the FXML file: " + fxmlPath + "\n\nError: " + e.getMessage());
        }
    }

    private void showError(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private void showErrorAndExit(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
            Platform.exit();
        });
    }

    // Getter and setter for changing the FXML path
    public String getCurrentFxmlPath() {
        return currentFxmlPath;
    }

    public void setCurrentFxmlPath(String fxmlPath, String title) {
        this.currentFxmlPath = fxmlPath;
        this.currentTitle = title;
        loadScene(currentFxmlPath, currentTitle);
    }

    public static void main(String[] args) {
        try {
            System.out.println("Starting JavaFX application...");
            launch(args);
        } catch (Exception e) {
            System.err.println("Fatal error in main application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}