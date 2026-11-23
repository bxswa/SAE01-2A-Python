package iut.sae.modele;

/**
 * Une ligne du fichier d'energie d'un algorithme : l'energie disponible et
 * consommee a un instant donne. Sert a tracer la courbe d'energie.
 */
public class PointEnergie {

    /** Instant concerne. */
    private final int instant;

    /** Energie verte disponible a cet instant. */
    private final int energieDisponible;

    /** Energie consommee par les processeurs allumes a cet instant. */
    private final int energieConsommee;

    /** Nombre de processeurs actifs a cet instant. */
    private final int nbCpuActifs;

    /**
     * Cree un point d'energie.
     *
     * @param instant           instant
     * @param energieDisponible energie disponible
     * @param energieConsommee  energie consommee
     * @param nbCpuActifs       nombre de processeurs actifs
     */
    public PointEnergie(int instant, int energieDisponible, int energieConsommee, int nbCpuActifs) {
        this.instant = instant;
        this.energieDisponible = energieDisponible;
        this.energieConsommee = energieConsommee;
        this.nbCpuActifs = nbCpuActifs;
    }

    /** @return l'instant */
    public int getInstant() {
        return instant;
    }

    /** @return l'energie disponible */
    public int getEnergieDisponible() {
        return energieDisponible;
    }

    /** @return l'energie consommee */
    public int getEnergieConsommee() {
        return energieConsommee;
    }

    /** @return le nombre de processeurs actifs */
    public int getNbCpuActifs() {
        return nbCpuActifs;
    }
}
