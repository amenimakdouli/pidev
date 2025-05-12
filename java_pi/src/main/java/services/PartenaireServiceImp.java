package services;

import entities.Partenaire;
import javafx.collections.FXCollections;
import utils.DataConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PartenaireServiceImp implements IPartenaireService {

    @Override
    public List<Partenaire> getAllPartenaires() {
        List<Partenaire> partenaires = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataConnection.getConnection();
            String sql = "SELECT * FROM partenaire";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    partenaires.add(mapResultSetToPartenaire(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving all partners: " + e.getMessage());
            e.printStackTrace();
        }
        return partenaires;
    }

    @Override
    public Partenaire addPartenaire(Partenaire partenaire) {
        if (!validatePartenaire(partenaire)) {
            return null;
        }
        
        Connection conn = null;
        try {
            conn = DataConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            String sql = "INSERT INTO partenaire (name, email, phone, address, website) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setPartenaireParameters(pstmt, partenaire);
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            partenaire.setId(generatedKeys.getInt(1));
                            conn.commit(); // Commit the transaction
                            return partenaire;
                        }
                    }
                }
            }
            conn.rollback(); // Rollback if we didn't return successfully
            return null;
            
        } catch (SQLException e) {
            System.err.println("Error adding partner: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error rolling back transaction: " + ex.getMessage());
                }
            }
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit
                } catch (SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public Partenaire modifyPartenaire(Partenaire partenaire) {
        if (!validatePartenaire(partenaire) || partenaire.getId() <= 0) {
            System.err.println("Invalid partner data or ID for update");
            return null;
        }
        
        Connection conn = null;
        try {
            conn = DataConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            String sql = "UPDATE partenaire SET name = ?, email = ?, phone = ?, address = ?, website = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                setPartenaireParameters(pstmt, partenaire);
                pstmt.setInt(6, partenaire.getId());
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    conn.commit(); // Commit the transaction
                    return partenaire;
                }
            }
            System.err.println("Updating partner failed, no rows affected. ID: " + partenaire.getId());
            conn.rollback(); // Rollback if update failed
            return null;
            
        } catch (SQLException e) {
            System.err.println("Error modifying partner: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error rolling back transaction: " + ex.getMessage());
                }
            }
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit
                } catch (SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public boolean deletePartenaire(int id) {
        if (id <= 0) {
            System.err.println("Invalid partner ID for deletion: " + id);
            return false;
        }

        // First check if there are any associated partnerships
        if (hasAssociatedPartnerships(id)) {
            System.err.println("Cannot delete partner: Associated partnerships exist for ID: " + id);
            return false;
        }

        Connection conn = null;
        try {
            conn = DataConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            String sql = "DELETE FROM partenaire WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                boolean result = pstmt.executeUpdate() > 0;
                if (result) {
                    conn.commit(); // Commit the transaction
                    return true;
                }
            }
            conn.rollback(); // Rollback if delete failed
            return false;

        } catch (SQLException e) {
            System.err.println("Error deleting partner: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error rolling back transaction: " + ex.getMessage());
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit
                } catch (SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public Partenaire getPartenaireById(int id) {
        if (id <= 0) {
            System.err.println("Invalid partner ID: " + id);
            return null;
        }
        
        Connection conn = null;
        try {
            conn = DataConnection.getConnection();
            String sql = "SELECT * FROM partenaire WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToPartenaire(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving partner by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Partenaire> searchPartenaires(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllPartenaires();
        }
        
        List<Partenaire> partenaires = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DataConnection.getConnection();
            String sql = "SELECT * FROM partenaire WHERE name LIKE ? OR email LIKE ? OR phone LIKE ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                String pattern = "%" + keyword.trim() + "%";
                pstmt.setString(1, pattern);
                pstmt.setString(2, pattern);
                pstmt.setString(3, pattern);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        partenaires.add(mapResultSetToPartenaire(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching partners: " + e.getMessage());
            e.printStackTrace();
        }
        return partenaires;
    }

    private Partenaire mapResultSetToPartenaire(ResultSet rs) throws SQLException {
        return new Partenaire(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("address"),
            rs.getString("website")
        );
    }

    private void setPartenaireParameters(PreparedStatement pstmt, Partenaire partenaire) throws SQLException {
        pstmt.setString(1, partenaire.getName());
        pstmt.setString(2, partenaire.getEmail());
        pstmt.setString(3, partenaire.getPhone());
        pstmt.setString(4, partenaire.getAddress());
        pstmt.setString(5, partenaire.getWebsite());
    }
    
    // Helper method to validate Partenaire fields
    private boolean validatePartenaire(Partenaire partenaire) {
        if (partenaire == null) {
            System.err.println("Partner object is null");
            return false;
        }
        if (partenaire.getName() == null || partenaire.getName().trim().isEmpty()) {
            System.err.println("Partner name is required");
            return false;
        }
        if (partenaire.getEmail() == null || !partenaire.getEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            System.err.println("Invalid partner email format");
            return false;
        }
        return true;
    }
    
    // Helper method to check for associated partnerships
    private boolean hasAssociatedPartnerships(int partnerId) {
        Connection conn = null;
        try {
            conn = DataConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM partnership WHERE partner_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, partnerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking for associated partnerships: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
