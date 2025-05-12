
package services;

import entities.Partenaire;
import java.util.List;

/**
 * Interface définissant les services pour la gestion des partenaires
 */
public interface IPartenaireService {

    /**
     * Récupère tous les partenaires
     * @return Liste de tous les partenaires
     */
    List<Partenaire> getAllPartenaires();

    /**
     * Ajoute un nouveau partenaire
     * @param partenaire Le partenaire à ajouter
     * @return Le partenaire ajouté avec son ID généré
     */
    Partenaire addPartenaire(Partenaire partenaire);

    /**
     * Modifie un partenaire existant
     * @param partenaire Le partenaire avec les informations modifiées
     * @return Le partenaire modifié
     */
    Partenaire modifyPartenaire(Partenaire partenaire);

    /**
     * Supprime un partenaire par son ID
     * @param id L'ID du partenaire à supprimer
     * @return true si la suppression est réussie, false sinon
     */
    boolean deletePartenaire(int id);

    /**
     * Recherche des partenaires selon un mot-clé
     * @param keyword Le mot-clé pour la recherche
     * @return Liste des partenaires correspondant au critère de recherche
     */
    List<Partenaire> searchPartenaires(String keyword);

    /**
     * Récupère un partenaire par son ID
     * @param id L'ID du partenaire à récupérer
     * @return Le partenaire correspondant à l'ID, ou null si non trouvé
     */
    Partenaire getPartenaireById(int id);
}