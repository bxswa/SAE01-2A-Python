package iut.sae.modele;

/**
 * Une ligne du tableau des processus : un processus (fichier d'entree)
 * associe a son resultat pour UN algorithme (fichier global).
 *
 * <p>Les getters sont utilises par {@code PropertyValueFactory} dans le
 * TableView. Les dates et les temps valent null si le processus n'a
 * jamais ete execute.</p>
 */
public class LigneProcessus {

    /** Processus tel que decrit dans processus.csv. */
    private final Processus processus;

    /** Resultat de l'algorithme pour ce processus (peut etre null). */
    private final ResultatProcessus resultat;

    /**
     * Cree une ligne.
     *
     * @param processus le processus d'entree
     * @param resultat  son resultat pour l'algorithme choisi, ou null
     */
    public LigneProcessus(Processus processus, ResultatProcessus resultat) {
        this.processus = processus;
        this.resultat = resultat;
    }

    /** @return true si le processus a ete execute par l'algorithme */
    public boolean estExecute() {
        return resultat != null && resultat.estExecute();
    }

    /** @return l'identifiant du processus */
    public String getId() {
        return processus.getId();
    }

    /** @return la date de soumission */
    public int getDateSoumission() {
        return processus.getDateSoumission();
    }

    /** @return la duree d'execution demandee */
    public int getDureeExecution() {
        return processus.getDureeExecution();
    }

    /** @return la RAM necessaire */
    public int getRamNecessaire() {
        return processus.getRamNecessaire();
    }

    /** @return la priorite */
    public int getPriorite() {
        return processus.getPriorite();
    }

    /** @return la date butoir */
    public int getDateButoir() {
        return processus.getDateButoir();
    }

    /** @return la date de debut, ou null si le processus n'a pas tourne */
    public Integer getDateDebut() {
        return estExecute() ? resultat.getDateDebut() : null;
    }

    /** @return la date de fin, ou null si le processus n'a pas tourne */
    public Integer getDateFin() {
        return estExecute() ? resultat.getDateFin() : null;
    }

    /** @return le temps d'attente (fin - soumission - duree), ou null */
    public Integer getTempsAttente() {
        return estExecute()
                ? resultat.getDateFin() - processus.getDateSoumission() - processus.getDureeExecution()
                : null;
    }

    /** @return le temps de reponse (fin - soumission), ou null */
    public Integer getTempsReponse() {
        return estExecute() ? resultat.getDateFin() - processus.getDateSoumission() : null;
    }

    /** @return "Non execute", "En retard" ou "Dans les temps" */
    public String getStatut() {
        if (!estExecute()) {
            return "Non exécuté";
        }
        return resultat.getDateFin() > processus.getDateButoir() ? "En retard" : "Dans les temps";
    }
}
