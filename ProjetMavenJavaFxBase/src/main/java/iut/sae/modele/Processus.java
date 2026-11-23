package iut.sae.modele;

/**
 * Un processus a ordonnancer, tel qu'il est decrit dans processus.csv.
 *
 * <p>Les getters permettent d'afficher directement un Processus dans un
 * TableView (PropertyValueFactory utilise les getters).</p>
 */
public class Processus {

    /** Identifiant du processus (ex : P1). */
    private final String id;

    /** Instant ou le processus arrive dans la file d'attente. */
    private final int dateSoumission;

    /** Temps d'execution total necessaire. */
    private final int dureeExecution;

    /** RAM occupee pendant l'execution. */
    private final int ramNecessaire;

    /** Priorite : plus la valeur est grande, plus le processus est prioritaire. */
    private final int priorite;

    /** Date butoir (deadline) : le processus devrait avoir fini avant. */
    private final int dateButoir;

    /**
     * Cree un processus.
     *
     * @param id             identifiant
     * @param dateSoumission date de soumission
     * @param dureeExecution duree d'execution
     * @param ramNecessaire  RAM necessaire
     * @param priorite       priorite
     * @param dateButoir     date butoir
     */
    public Processus(String id, int dateSoumission, int dureeExecution,
                     int ramNecessaire, int priorite, int dateButoir) {
        this.id = id;
        this.dateSoumission = dateSoumission;
        this.dureeExecution = dureeExecution;
        this.ramNecessaire = ramNecessaire;
        this.priorite = priorite;
        this.dateButoir = dateButoir;
    }

    /** @return l'identifiant du processus */
    public String getId() {
        return id;
    }

    /** @return la date de soumission */
    public int getDateSoumission() {
        return dateSoumission;
    }

    /** @return la duree d'execution */
    public int getDureeExecution() {
        return dureeExecution;
    }

    /** @return la RAM necessaire */
    public int getRamNecessaire() {
        return ramNecessaire;
    }

    /** @return la priorite */
    public int getPriorite() {
        return priorite;
    }

    /** @return la date butoir */
    public int getDateButoir() {
        return dateButoir;
    }
}
