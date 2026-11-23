package iut.sae.modele;

/**
 * Une ligne du fichier des metriques : les indicateurs globaux d'un
 * algorithme, utilises pour comparer les algorithmes entre eux.
 */
public class Metrique {

    /** Algorithme concerne. */
    private final Algorithme algorithme;

    /** Temps d'attente moyen (fin - soumission - duree). */
    private final double tempsAttenteMoyen;

    /** Temps de reponse moyen (fin - soumission). */
    private final double tempsReponseMoyen;

    /** Date de fin du dernier processus. */
    private final int makespan;

    /** Energie consommee par les processeurs. */
    private final int energieConsommee;

    /** Energie verte disponible pendant la simulation. */
    private final int energieDisponible;

    /** Energie consommee / energie disponible (entre 0 et 1). */
    private final double tauxUtilisation;

    /**
     * Cree une metrique.
     *
     * @param algorithme        algorithme concerne
     * @param tempsAttenteMoyen temps d'attente moyen
     * @param tempsReponseMoyen temps de reponse moyen
     * @param makespan          date de fin du dernier processus
     * @param energieConsommee  energie consommee
     * @param energieDisponible energie disponible
     * @param tauxUtilisation   taux d'utilisation de l'energie
     */
    public Metrique(Algorithme algorithme, double tempsAttenteMoyen, double tempsReponseMoyen,
                    int makespan, int energieConsommee, int energieDisponible,
                    double tauxUtilisation) {
        this.algorithme = algorithme;
        this.tempsAttenteMoyen = tempsAttenteMoyen;
        this.tempsReponseMoyen = tempsReponseMoyen;
        this.makespan = makespan;
        this.energieConsommee = energieConsommee;
        this.energieDisponible = energieDisponible;
        this.tauxUtilisation = tauxUtilisation;
    }

    /** @return l'algorithme concerne */
    public Algorithme getAlgorithme() {
        return algorithme;
    }

    /** @return le nom de l'algorithme, pour l'affichage */
    public String getNomAlgorithme() {
        return algorithme.getLibelle();
    }

    /** @return le temps d'attente moyen */
    public double getTempsAttenteMoyen() {
        return tempsAttenteMoyen;
    }

    /** @return le temps de reponse moyen */
    public double getTempsReponseMoyen() {
        return tempsReponseMoyen;
    }

    /** @return le makespan */
    public int getMakespan() {
        return makespan;
    }

    /** @return l'energie consommee */
    public int getEnergieConsommee() {
        return energieConsommee;
    }

    /** @return l'energie disponible */
    public int getEnergieDisponible() {
        return energieDisponible;
    }

    /** @return le taux d'utilisation de l'energie (entre 0 et 1) */
    public double getTauxUtilisation() {
        return tauxUtilisation;
    }
}
