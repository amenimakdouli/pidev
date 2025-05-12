package entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Partnership {
    private int id;
    private int partnerId;  // Matches the partner_id field in the database
    private String type;    // Matches the type field in the database
    private String details; // Matches the details field in the database
    private LocalDateTime createdAt; // Matches the created_at field in the database

    // Reference to the partner entity (not directly in the table, but referenced via partner_id)
    private Partenaire partenaire;

    // Date formatter for conversion between String and LocalDateTime
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * No-args constructor
     */
    public Partnership() {
    }

    /**
     * Constructor with basic fields
     */
    public Partnership(int partnerId, String type, String details) {
        this.partnerId = partnerId;
        this.type = type;
        this.details = details;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor with all fields
     */
    public Partnership(int id, int partnerId, String type, String details, LocalDateTime createdAt) {
        this.id = id;
        this.partnerId = partnerId;
        this.type = type;
        this.details = details;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPartnerId() { return partnerId; }
    public void setPartnerId(int partnerId) { this.partnerId = partnerId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // String version of createdAt for compatibility with views
    public String getCreatedAtString() {
        return createdAt != null ? createdAt.format(DATE_FORMATTER) : "";
    }

    public void setCreatedAt(String createdAtStr) {
        try {
            this.createdAt = LocalDateTime.parse(createdAtStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            System.err.println("Invalid datetime format: " + createdAtStr);
        }
    }

    // Reference to the full partner entity
    public Partenaire getPartenaire() { return partenaire; }
    public void setPartenaire(Partenaire partenaire) {
        this.partenaire = partenaire;
        if (partenaire != null) {
            this.partnerId = partenaire.getId();
        }
    }

    // Helper method to get partner name
    public String getPartenaireName() {
        return partenaire != null ? partenaire.getName() : "";
    }
}