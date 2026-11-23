package iut.sae.modele;

/**
 * Une ligne du fichier global d'un algorithme : quand un processus a
 * commence et fini.
 *
 * <p>Un processus qui demande plus que la RAM totale n'est jamais execute :
 * ses dates de debut et de fin valent alors null.</p>
 */
public class ResultatProcessus {

    /** Identifiant du processus. */
    private final String idProcessus;

    /** Date de soumission. */
    private final int dateSoumission;

    /** Premier instant d'execution, ou null si le processus n'a pas tourne. */
    private final Integer dateDebut;

    /** Instant de fin (exclu), ou null si le processus n'a pas tourne. */
    private final Integer dateFin;

    /** RAM demandee par le processus. */
    private final int ramDemandee;

    /** RAM reellement utilisee (0 si le processus n'a pas tourne). */
    private final int ramUtilisee;

    /**
     * Cree un resultat.
     *
     * @param idProcessus    identifiant du processus
     * @param dateSoumission date de soumission
     * @param dateDebut      date de debut, ou null
     * @param dateFin        date de fin, ou null
     * @param ramDemandee    RAM demandee
     * @param ramUtilisee    RAM utilisee
     */
    public ResultatProcessus(String idProcessus, int dateSoumission, Integer dateDebut,
                             Integer dateFin, int ramDemandee, int ramUtilisee) {
        this.idProcessus = idProcessus;
        this.dateSoumission = dateSoumission;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.ramDemandee = ramDemandee;
        this.ramUtilisee = ramUtilisee;
    }

    /** @return true si le processus a ete execute */
    public boolean estExecute() {
        return dateFin != null;
    }

    /** @return l'identifiant du processus */
    public String getIdProcessus() {
        return idProcessus;
    }

    /** @return la date de soumission */
    public int getDateSoumission() {
        return dateSoumission;
    }

    /** @return la date de debut, ou null si le processus n'a pas tourne */
    public Integer getDateDebut() {
        return dateDebut;
    }

    /** @return la date de fin, ou null si le processus n'a pas tourne */
    public Integer getDateFin() {
        return dateFin;
    }

    /** @return la RAM demandee */
    public int getRamDemandee() {
        return ramDemandee;
    }

    /** @return la RAM utilisee */
    public int getRamUtilisee() {
        return ramUtilisee;
    }
}
