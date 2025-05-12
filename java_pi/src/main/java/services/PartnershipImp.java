package services;

import entities.Partenaire;
import entities.Partnership;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import utils.DataConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class PartnershipImp implements IPartnershipService {
    private final ObservableList<Partnership> partnerships = FXCollections.observableArrayList();
    private Partnership selectedPartnership;
    private final IPartenaireService partenaireService;

    // Date formatter for conversion
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PartnershipImp() {
        // Initialize partner service
        this.partenaireService = new PartenaireServiceImp();
        // Load existing partnerships
        loadPartnerships();
    }

    @Override
    public Partnership createPartnership(int partnerId, String type, String details) {
        System.out.println("Creating new partnership - Partner ID: " + partnerId + ", Type: " + type);
        // Add validation before attempting to create
        if (!validatePartnershipFields(partnerId, type)) {
            return null;
        }

        Connection conn = null;
        String sql = "INSERT INTO partnership (partner_id, type, details, created_at) VALUES (?, ?, ?, ?)";
        try {
            conn = DataConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, partnerId);
                pstmt.setString(2, type);
                pstmt.setString(3, details != null ? details : ""); // Handle null details
                pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int generatedId = rs.getInt(1);
                            Partnership partnership = new Partnership();
                            partnership.setId(generatedId);
                            partnership.setPartnerId(partnerId);
                            partnership.setType(type);
                            partnership.setDetails(details);
                            partnership.setCreatedAt(LocalDateTime.now());
                            
                            // Load and set the associated Partenaire
                            Partenaire partenaire = partenaireService.getPartenaireById(partnerId);
                            partnership.setPartenaire(partenaire);
                            
                            // Add to the observable list
                            partnerships.add(partnership);
                            
                            conn.commit(); // Commit transaction
                            return partnership;
                        }
                    }
                }
            }
            
            // If we get here, something went wrong
            conn.rollback(); // Rollback transaction
            return null;
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // Ensure rollback on exception
                }
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }
            handleSQLException(e, "create");
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
        return null;
    }

    @Override
    public Partnership getPartnershipById(int id) {
        if (id <= 0) {
            System.err.println("Invalid partnership ID: " + id);
            return null;
        }
        
        String sql = "SELECT p.*, pa.name as partner_name, pa.email as partner_email, " +
                    "pa.phone as partner_phone, pa.address as partner_address, pa.website as partner_website " +
                    "FROM partnership p " +
                    "LEFT JOIN partenaire pa ON p.partner_id = pa.id " +
                    "WHERE p.id = ?";
        
        try (Connection conn = DataConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPartnership(rs);
                }
            }
        } catch (SQLException e) {
            handleSQLException(e, "retrieve");
        }
        return null;
    }

    @Override
    public ObservableList<Partnership> getAllPartnerships() {
        // Return the current observable list (already loaded and maintained)
        return FXCollections.unmodifiableObservableList(partnerships);
    }
    
    @Override
    public boolean updatePartnership(Partnership partnership, int partnerId, String type, String details) {
        if (partnership == null) {
            System.err.println("Attempted to update null partnership");
            showAlert("Error", "No partnership selected for update.");
            return false;
        }
        System.out.println("Updating partnership ID: " + partnership.getId() + " - New Partner ID: " + partnerId + ", New Type: " + type);
        
        if (!validatePartnershipFields(partnerId, type)) {
            return false;
        }

        Connection conn = null;
        String sql = "UPDATE partnership SET partner_id = ?, type = ?, details = ? WHERE id = ?";
        try {
            conn = DataConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, partnerId);
                pstmt.setString(2, type);
                pstmt.setString(3, details != null ? details : ""); // Handle null details
                pstmt.setInt(4, partnership.getId());

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    // Update the partnership object
                    partnership.setPartnerId(partnerId);
                    partnership.setType(type);
                    partnership.setDetails(details);
                    
                    // Load and set the updated Partenaire
                    Partenaire partenaire = partenaireService.getPartenaireById(partnerId);
                    partnership.setPartenaire(partenaire);
                    
                    // Update the observable list
                    partnerships.removeIf(p -> p.getId() == partnership.getId());
                    partnerships.add(partnership);
                    
                    conn.commit(); // Commit transaction
                    return true;
                }
            }
            
            conn.rollback(); // Rollback if no rows affected
            showAlert("Error", "Partnership not found or no changes made.");
            return false;
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // Ensure rollback on exception
                }
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }
            handleSQLException(e, "update");
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public boolean deletePartnership(Partnership partnership) {
        if (partnership == null || partnership.getId() <= 0) {
            System.err.println("Attempted to delete null or invalid partnership");
            return false;
        }
        System.out.println("Deleting partnership ID: " + partnership.getId());
        
        Connection conn = null;
        String sql = "DELETE FROM partnership WHERE id = ?";
        try {
            conn = DataConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, partnership.getId());
                
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    // Remove from observable list
                    partnerships.removeIf(p -> p.getId() == partnership.getId());
                    conn.commit(); // Commit transaction
                    return true;
                }
                
                conn.rollback(); // Rollback if no rows affected
                showAlert("Warning", "Partnership not found or already deleted.");
                return false;
            }
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // Ensure rollback on exception
                }
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }
            handleSQLException(e, "delete");
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }
    @Override
    public boolean validatePartnershipFields(int partnerId, String type) {
        // Check that all required fields are filled
        if (partnerId <= 0) {
            showAlert("Validation Error", "Please select a valid partner.");
            return false;
        }
        if (type == null || type.trim().isEmpty()) {
            showAlert("Validation Error", "Partnership type is required.");
            return false;
        }
        if (type.length() > 255) { // Add length validation
            showAlert("Validation Error", "Partnership type is too long (maximum 255 characters).");
            return false;
        }
        
        // Verify if partner exists using partenaireService
        Partenaire partenaire = partenaireService.getPartenaireById(partnerId);
        if (partenaire == null) {
            showAlert("Validation Error", "Selected partner no longer exists in the database.");
            return false;
        }
        
        return true;
    }

    @Override
    public void loadSamplePartnerships(ObservableList<Partenaire> partenaireList) {
        // This method should only be available in development/test environment
        if (!isDevelopmentEnvironment()) {
            System.err.println("loadSamplePartnerships is only available in development environment");
            return;
        }

        if (partenaireList.isEmpty()) {
            showAlert("Warning", "No partners available to create sample partnerships.");
            return;
        }

        Connection conn = null;
        try {
            conn = DataConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Clear existing partnerships
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DELETE FROM partnership");

            // Create sample partnerships for each partner
            for (Partenaire partenaire : partenaireList) {
                PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO partnership (partner_id, type, details, created_at) VALUES (?, ?, ?, ?)"
                );
                pstmt.setInt(1, partenaire.getId());
                pstmt.setString(2, "Sample Partnership with " + partenaire.getName());
                pstmt.setString(3, "Sample partnership details created for testing purposes.");
                pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.executeUpdate();
            }

            conn.commit(); // Commit transaction
            System.out.println("Successfully loaded sample partnerships");
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                } catch (SQLException ex) {
                    System.err.println("Failed to rollback transaction: " + ex.getMessage());
                }
            }
            handleSQLException(e, "create sample data");
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit
                } catch (SQLException e) {
                    System.err.println("Failed to reset auto-commit: " + e.getMessage());
                }
                try {
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("Error closing connection: " + ex.getMessage());
                }
            }
        }
    }
    
    private boolean isDevelopmentEnvironment() {
        // This should be configured via environment variable or configuration file
        String env = System.getProperty("app.environment", "production").toLowerCase();
        return env.equals("development") || env.equals("dev") || env.equals("test");
    }

    @Override
    public void clearSelection() {
        selectedPartnership = null;
    }

    @Override
    public Partnership getSelectedPartnership() {
        return selectedPartnership;
    }

    @Override
    public void setSelectedPartnership(Partnership partnership) {
        this.selectedPartnership = partnership;
    }
    
    // Method to load existing partnerships from the database
    private void loadPartnerships() {
        partnerships.clear();
        String sql = "SELECT p.*, pa.name as partner_name, pa.email as partner_email, " +
                    "pa.phone as partner_phone, pa.address as partner_address, pa.website as partner_website " +
                    "FROM partnership p " +
                    "LEFT JOIN partenaire pa ON p.partner_id = pa.id " +
                    "ORDER BY p.created_at DESC";
        
        try (Connection conn = DataConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Partnership partnership = mapResultSetToPartnership(rs);
                partnerships.add(partnership);
            }
            System.out.println("Successfully loaded " + partnerships.size() + " partnerships");
        } catch (SQLException e) {
            System.err.println("Error loading partnerships: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method to map ResultSet to Partnership object
    private Partnership mapResultSetToPartnership(ResultSet rs) throws SQLException {
        Partnership partnership = new Partnership();
        partnership.setId(rs.getInt("id"));
        partnership.setPartnerId(rs.getInt("partner_id"));
        partnership.setType(rs.getString("type"));
        partnership.setDetails(rs.getString("details"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            partnership.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        // Try to get Partenaire data from result set if included in JOIN
        if (rs.getString("partner_name") != null) {
            Partenaire partenaire = new Partenaire(
                rs.getString("partner_name"),
                rs.getString("partner_email"),
                rs.getString("partner_phone"),
                rs.getString("partner_address"),
                rs.getString("partner_website")
            );
            partenaire.setId(rs.getInt("partner_id"));
            partnership.setPartenaire(partenaire);
        } else {
            // If not included in result set, load from partenaireService
            partnership.setPartenaire(partenaireService.getPartenaireById(partnership.getPartnerId()));
        }
        
        return partnership;
    }
    // Helper method to check if a partnership exists
    private boolean partnershipExists(int id, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM partnership WHERE id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, id);
        ResultSet rs = pstmt.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    // Helper method for consistent error handling
    private void handleSQLException(SQLException e, String operation) {
        String errorMessage;
        if (e.getMessage().contains("foreign key constraint")) {
            errorMessage = "This operation cannot be completed because the partner no longer exists.";
        } else if (e.getMessage().contains("Duplicate entry")) {
            errorMessage = "A similar partnership already exists.";
        } else {
            errorMessage = String.format("Failed to %s partnership. Please try again later.", operation);
            // Log the actual SQL error for debugging
            System.err.println("SQL Error during " + operation + ": " + e.getMessage());
        }
        showAlert("Error", errorMessage);
    }
    
    // Helper method to show alert
    private void showAlert(String title, String message) {
        Alert.AlertType type = title.toLowerCase().contains("error") ? 
            Alert.AlertType.ERROR : 
            (title.toLowerCase().contains("warning") ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
        
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
