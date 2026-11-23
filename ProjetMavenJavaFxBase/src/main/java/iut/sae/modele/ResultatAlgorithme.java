package iut.sae.modele;

import java.util.List;

/**
 * Tous les resultats detailles d'UN algorithme : les dates de chaque
 * processus, les periodes d'execution sur les processeurs et l'energie a
 * chaque instant.
 */
public class ResultatAlgorithme {

    /** Algorithme concerne. */
    private final Algorithme algorithme;

    /** Une ligne par processus (fichier global). */
    private final List<ResultatProcessus> resultats;

    /** Une ligne par periode d'execution (fichier des allocations). */
    private final List<Allocation> allocations;

    /** Une ligne par instant (fichier d'energie). Vide si le fichier est absent. */
    private final List<PointEnergie> energie;

    /**
     * Cree le resultat d'un algorithme.
     *
     * @param algorithme  algorithme concerne
     * @param resultats   resultats par processus
     * @param allocations periodes d'execution
     * @param energie     energie par instant
     */
    public ResultatAlgorithme(Algorithme algorithme, List<ResultatProcessus> resultats,
                              List<Allocation> allocations, List<PointEnergie> energie) {
        this.algorithme = algorithme;
        this.resultats = List.copyOf(resultats);
        this.allocations = List.copyOf(allocations);
        this.energie = List.copyOf(energie);
    }

    /** @return l'algorithme concerne */
    public Algorithme getAlgorithme() {
        return algorithme;
    }

    /** @return les resultats par processus (liste non modifiable) */
    public List<ResultatProcessus> getResultats() {
        return resultats;
    }

    /** @return les periodes d'execution (liste non modifiable) */
    public List<Allocation> getAllocations() {
        return allocations;
    }

    /** @return l'energie par instant (liste non modifiable) */
    public List<PointEnergie> getEnergie() {
        return energie;
    }

    /**
     * Cherche le resultat d'un processus.
     *
     * @param idProcessus identifiant du processus
     * @return son resultat, ou null s'il n'existe pas
     */
    public ResultatProcessus getResultat(String idProcessus) {
        for (ResultatProcessus r : resultats) {
            if (r.getIdProcessus().equals(idProcessus)) {
                return r;
            }
        }
        return null;
    }

    /** @return la date de fin la plus grande parmi les allocations (0 s'il n'y en a pas) */
    public int getDateFinMax() {
        int max = 0;
        for (Allocation a : allocations) {
            max = Math.max(max, a.getDateFin());
        }
        return max;
    }
}
