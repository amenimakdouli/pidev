package services;

import entities.Partenaire;
import entities.Partnership;
import javafx.collections.ObservableList;

public interface IPartnershipService {
    // CRUD Operations
    Partnership createPartnership(int partnerId, String type, String details);

    Partnership getPartnershipById(int id);

    ObservableList<Partnership> getAllPartnerships();

    boolean updatePartnership(Partnership partnership, int partnerId, String type, String details);

    boolean deletePartnership(Partnership partnership);

    // Validation methods
    boolean validatePartnershipFields(int partnerId, String type);

    // Data loading methods
    void loadSamplePartnerships(ObservableList<Partenaire> partenaireList);

    // Utility methods
    void clearSelection();

    Partnership getSelectedPartnership();

    void setSelectedPartnership(Partnership partnership);
}