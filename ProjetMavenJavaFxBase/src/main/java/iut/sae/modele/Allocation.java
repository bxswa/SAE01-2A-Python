package iut.sae.modele;

/**
 * Une ligne du fichier des allocations : un processus a tourne sur un
 * processeur de dateDebut a dateFin (dateFin exclue).
 *
 * <p>Exemple : P1, 7, 12, CPU1 signifie que P1 tourne sur CPU1 aux
 * instants 7, 8, 9, 10 et 11. C'est la donnee de base des diagrammes de
 * Gantt : un rectangle par allocation.</p>
 */
public class Allocation {

    /** Identifiant du processus. */
    private final String idProcessus;

    /** Premier instant de la periode. */
    private final int dateDebut;

    /** Fin de la periode (exclue). */
    private final int dateFin;

    /** Identifiant du processeur. */
    private final String idProcesseur;

    /**
     * Cree une allocation.
     *
     * @param idProcessus  identifiant du processus
     * @param dateDebut    premier instant
     * @param dateFin      fin (exclue)
     * @param idProcesseur identifiant du processeur
     */
    public Allocation(String idProcessus, int dateDebut, int dateFin, String idProcesseur) {
        this.idProcessus = idProcessus;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.idProcesseur = idProcesseur;
    }

    /** @return la duree de la periode (dateFin - dateDebut) */
    public int getDuree() {
        return dateFin - dateDebut;
    }

    /** @return l'identifiant du processus */
    public String getIdProcessus() {
        return idProcessus;
    }

    /** @return le premier instant */
    public int getDateDebut() {
        return dateDebut;
    }

    /** @return la fin de la periode (exclue) */
    public int getDateFin() {
        return dateFin;
    }

    /** @return l'identifiant du processeur */
    public String getIdProcesseur() {
        return idProcesseur;
    }
}
