package controllers;

import entities.Partenaire;
import entities.Partnership;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.List;
import services.IPartnershipService;
import services.PartnershipImp;
import services.IPartenaireService;
import services.PartenaireServiceImp;
import utils.Main;

public class PartnershipController implements Initializable {
    @FXML private TextField typeField;
    @FXML private TextArea detailsField;
    @FXML private ComboBox<Partenaire> partnerComboBox;
    @FXML private Button saveButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private TableView<Partnership> partnershipsTable;
    @FXML private TableColumn<Partnership, Integer> idColumn;
    @FXML private TableColumn<Partnership, String> partnerColumn;
    @FXML private TableColumn<Partnership, String> typeColumn;
    @FXML private TableColumn<Partnership, String> detailsColumn;
    @FXML private TableColumn<Partnership, String> createdAtColumn;

    private Main mainApp;

    // Observable list to store partners
    private ObservableList<Partenaire> partenaireList = FXCollections.observableArrayList();

    // Service layer
    private IPartnershipService partnershipService;
    private IPartenaireService partenaireService;

    // Date formatter for display
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PartnershipController() {
        // Initialize the service layers
        this.partnershipService = new PartnershipImp();
        this.partenaireService = new PartenaireServiceImp();
    }

    public void setMain(Main mainApp) {
        this.mainApp = mainApp;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("Initializing PartnershipController...");

        // Load real partners from database for combo box
        loadPartenaires();

        // Setup combo box
        partnerComboBox.setItems(partenaireList);
        partnerComboBox.setCellFactory(param -> new ListCell<Partenaire>() {
            @Override
            protected void updateItem(Partenaire item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s (%s)", item.getName(), item.getEmail()));
                }
            }
        });
        partnerComboBox.setButtonCell(new ListCell<Partenaire>() {
            @Override
            protected void updateItem(Partenaire item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s (%s)", item.getName(), item.getEmail()));
                }
            }
        });

        // Setup table columns
        setupTableColumns();
        
        // Load initial data with partners
        refreshTableData();

        // Remove sample data loading
        // partnershipService.loadSamplePartnerships(partenaireList);

        // Set selection listener for table
        partnershipsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            partnershipService.setSelectedPartnership(newSelection);
            if (newSelection != null) {
                typeField.setText(newSelection.getType());
                detailsField.setText(newSelection.getDetails() != null ? newSelection.getDetails() : "");
                
                // Find and select the correct partner in the combo box
                Partenaire partner = newSelection.getPartenaire();
                if (partner != null) {
                    partnerComboBox.setValue(partner);
                } else if (newSelection.getPartnerId() > 0) {
                    // If the partner object is null but we have a partner ID, try to load it
                    Partenaire loadedPartner = partenaireService.getPartenaireById(newSelection.getPartnerId());
                    if (loadedPartner != null) {
                        newSelection.setPartenaire(loadedPartner);
                        partnerComboBox.setValue(loadedPartner);
                    }
                }
            } else {
                clearFields();
            }
        });

        System.out.println("PartnershipController initialization complete.");
    }

    private void setupTableColumns() {
        // Configure ID column
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        // Configure partner column
        partnerColumn.setCellValueFactory(cellData -> {
            Partnership partnership = cellData.getValue();
            Partenaire partner = partnership.getPartenaire();
            if (partner == null && partnership.getPartnerId() > 0) {
                // Try to load the partner if it's not set but we have the ID
                partner = partenaireService.getPartenaireById(partnership.getPartnerId());
                if (partner != null) {
                    partnership.setPartenaire(partner);
                }
            }
            return new SimpleStringProperty(partner != null ? 
                String.format("%s (%s)", partner.getName(), partner.getEmail()) : "");
        });

        // Configure type column
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));

        // Configure details column
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));

        // Configure createdAt column
        createdAtColumn.setCellValueFactory(cellData -> {
            Partnership partnership = cellData.getValue();
            LocalDateTime createdAt = partnership.getCreatedAt();
            return new SimpleStringProperty(createdAt != null ? createdAt.format(DATE_FORMATTER) : "");
        });

        System.out.println("Table columns setup complete");
    }

    private void loadPartenaires() {
        // Load actual partners from the database
        List<Partenaire> partners = partenaireService.getAllPartenaires();
        partenaireList.clear();
        partenaireList.addAll(partners);
        System.out.println("Loaded " + partners.size() + " partners from database");
    }

    @FXML
    private void handleSave() {
        try {
            String type = typeField.getText();
            String details = detailsField.getText();
            Partenaire partner = partnerComboBox.getValue();

            // Validate input fields using service
            if (!partnershipService.validatePartnershipFields(partner != null ? partner.getId() : 0, type)) {
                showAlert("Error", "Please fill in all required fields (Partner and Type).");
                return;
            }

            // Create new partnership using service
            Partnership partnership = partnershipService.createPartnership(
                    partner.getId(), type, details
            );
            
            if (partnership != null) {
                // Set the partner object directly
                partnership.setPartenaire(partner);
                
                // Refresh the table data
                refreshTableData();
                
                clearFields();
                showAlert("Success", "Partnership saved successfully!");
            } else {
                showAlert("Error", "Failed to create partnership.");
            }
        } catch (Exception e) {
            System.err.println("Error saving partnership: " + e.getMessage());
            showAlert("Error", "Failed to save partnership: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        Partnership selectedPartnership = partnershipService.getSelectedPartnership();
        if (selectedPartnership != null) {
            try {
                String type = typeField.getText();
                String details = detailsField.getText();
                Partenaire partner = partnerComboBox.getValue();

                // Validate input fields using service
                if (!partnershipService.validatePartnershipFields(partner != null ? partner.getId() : 0, type)) {
                    showAlert("Error", "Please fill in all required fields (Partner and Type).");
                    return;
                }

                // Update partnership using service
                boolean updated = partnershipService.updatePartnership(
                        selectedPartnership, partner.getId(), type, details
                );

                if (updated) {
                    // Update the partner reference
                    selectedPartnership.setPartenaire(partner);
                    
                    // Refresh the table data
                    refreshTableData();
                    
                    clearFields();
                    showAlert("Success", "Partnership updated successfully!");
                } else {
                    showAlert("Error", "Failed to update partnership. Please try again.");
                }
            } catch (Exception e) {
                System.err.println("Error updating partnership: " + e.getMessage());
                showAlert("Error", "Failed to update partnership: " + e.getMessage());
            }
        } else {
            showAlert("Error", "Please select a partnership to update.");
        }
    }

    @FXML
    private void handleDelete() {
        Partnership selectedPartnership = partnershipService.getSelectedPartnership();
        if (selectedPartnership != null) {
            try {
                // Delete partnership using service
                boolean deleted = partnershipService.deletePartnership(selectedPartnership);
                if (deleted) {
                    // Refresh table after deletion
                    refreshTableData();
                    clearFields();
                    showAlert("Success", "Partnership deleted successfully!");
                } else {
                    showAlert("Error", "Failed to delete partnership.");
                }
            } catch (Exception e) {
                System.err.println("Error deleting partnership: " + e.getMessage());
                showAlert("Error", "Failed to delete partnership: " + e.getMessage());
            }
        } else {
            showAlert("Error", "Please select a partnership to delete.");
        }
    }

    private void clearFields() {
        typeField.clear();
        detailsField.clear();
        partnerComboBox.setValue(null);
        partnershipsTable.getSelectionModel().clearSelection();
        partnershipService.clearSelection();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Helper method to refresh the table data with complete partner information
    private void refreshTableData() {
        ObservableList<Partnership> partnerships = partnershipService.getAllPartnerships();
        for (Partnership p : partnerships) {
            if (p.getPartenaire() == null && p.getPartnerId() > 0) {
                p.setPartenaire(partenaireService.getPartenaireById(p.getPartnerId()));
            }
        }
        partnershipsTable.setItems(partnerships);
        partnershipsTable.refresh();
    }
}
