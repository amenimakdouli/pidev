package controllers;

import entities.Partenaire;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import services.IPartenaireService;
import services.PartenaireServiceImp;
import utils.Main;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class PartenaireController implements Initializable {
    // Form fields
    @FXML private TextField lastNameField;
    @FXML private TextField firstNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private TextField searchField;

    // Buttons
    @FXML private Button saveButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;

    // Table view and columns
    @FXML private TableView<Partenaire> partenairesTable;
    @FXML private TableColumn<Partenaire, Integer> idColumn;
    @FXML private TableColumn<Partenaire, String> nameColumn;
    @FXML private TableColumn<Partenaire, String> emailColumn;
    @FXML private TableColumn<Partenaire, String> phoneColumn;
    @FXML private TableColumn<Partenaire, String> addressColumn;

    private ObservableList<Partenaire> partenairesList = FXCollections.observableArrayList();
    private Partenaire selectedPartenaire;
    private IPartenaireService partenaireService = new PartenaireServiceImp();
    private Main mainApp;

    public void setMain(Main mainApp) {
        this.mainApp = mainApp;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));

        partenairesTable.setItems(partenairesList);
        loadPartenaires();
        setupTableSelectionListener();
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        partenairesList.clear();
        List<Partenaire> results = partenaireService.searchPartenaires(keyword);
        partenairesList.addAll(results);
        partenairesTable.setItems(partenairesList);
    }

    private void loadPartenaires() {
        partenairesList.clear();
        List<Partenaire> partenaires = partenaireService.getAllPartenaires();
        partenairesList.addAll(partenaires);
    }

    private void setupTableSelectionListener() {
        partenairesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedPartenaire = newSelection;
            if (selectedPartenaire != null) {
                populateFormFields(selectedPartenaire);
            }
        });
    }

    private void populateFormFields(Partenaire partenaire) {
        String fullName = partenaire.getName();

        // Try to split the name into first and last name if it contains a space
        if (fullName != null && fullName.contains(" ")) {
            String[] nameParts = fullName.split(" ", 2);
            firstNameField.setText(nameParts[0]);
            lastNameField.setText(nameParts[1]);
        } else {
            // If no space or null, put the whole name in the lastName field
            firstNameField.setText("");
            lastNameField.setText(fullName != null ? fullName : "");
        }

        emailField.setText(partenaire.getEmail());
        phoneField.setText(partenaire.getPhone());
        addressField.setText(partenaire.getAddress());
    }

    @FXML
    private void handleSave() {
        try {
            if (!validateFields()) {
                return;
            }

            // Combine first and last name
            String fullName = firstNameField.getText().trim() + " " + lastNameField.getText().trim();

            Partenaire newPartenaire = new Partenaire(
                    fullName.trim(),
                    emailField.getText().trim(),
                    phoneField.getText().trim(),
                    addressField.getText().trim(),
                    null   // website
            );

            Partenaire savedPartenaire = partenaireService.addPartenaire(newPartenaire);
            partenairesList.add(savedPartenaire);

            showAlert("Success", "Partenaire saved successfully!");
            clearForm();
        } catch (Exception e) {
            showAlert("Error", "Failed to save partenaire: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedPartenaire == null) {
            showAlert("Error", "Please select a partenaire to update");
            return;
        }

        try {
            if (!validateFields()) {
                return;
            }

            // Combine first and last name
            String fullName = firstNameField.getText().trim() + " " + lastNameField.getText().trim();
            selectedPartenaire.setName(fullName.trim());
            selectedPartenaire.setEmail(emailField.getText().trim());
            selectedPartenaire.setPhone(phoneField.getText().trim());
            selectedPartenaire.setAddress(addressField.getText().trim());

            partenaireService.modifyPartenaire(selectedPartenaire);
            partenairesTable.refresh();

            showAlert("Success", "Partenaire updated successfully!");
        } catch (Exception e) {
            showAlert("Error", "Failed to update partenaire: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedPartenaire == null) {
            showAlert("Error", "Please select a partenaire to delete");
            return;
        }

        try {
            partenaireService.deletePartenaire(selectedPartenaire.getId());
            partenairesList.remove(selectedPartenaire);
            clearForm();
            showAlert("Success", "Partenaire deleted successfully!");
        } catch (Exception e) {
            showAlert("Error", "Failed to delete partenaire: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearForm() {
        lastNameField.clear();
        firstNameField.clear();
        emailField.clear();
        phoneField.clear();
        addressField.clear();
        searchField.clear();
        partenairesTable.getSelectionModel().clearSelection();
        selectedPartenaire = null;
    }

    private boolean validateFields() {
        if (lastNameField.getText().trim().isEmpty() ||
                firstNameField.getText().trim().isEmpty() ||
                emailField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Last Name, First Name, and Email are required fields");
            return false;
        }
        return true;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}