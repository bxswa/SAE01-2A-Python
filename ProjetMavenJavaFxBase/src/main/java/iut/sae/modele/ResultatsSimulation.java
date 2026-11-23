package iut.sae.modele;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tout ce que produit une simulation : les processus d'entree, les
 * resultats de chaque algorithme et les metriques de comparaison.
 *
 * <p>C'est l'objet que les ecrans d'affichage (processus, Gantt,
 * comparaison) recoivent apres chaque lancement de Python.</p>
 */
public class ResultatsSimulation {

    /** Dossier ou Python a ecrit les fichiers de resultats. */
    private final Path dossierResultats;

    /** Processus lus dans processus.csv. */
    private final List<Processus> processus;

    /** Resultats detailles de chaque algorithme simule. */
    private final Map<Algorithme, ResultatAlgorithme> resultatsParAlgorithme;

    /** Metriques de chaque algorithme (fichier des metriques). */
    private final List<Metrique> metriques;

    /**
     * Cree les resultats d'une simulation.
     *
     * @param dossierResultats       dossier des fichiers de resultats
     * @param processus              processus d'entree
     * @param resultatsParAlgorithme resultats de chaque algorithme
     * @param metriques              metriques de chaque algorithme
     */
    public ResultatsSimulation(Path dossierResultats, List<Processus> processus,
                               Map<Algorithme, ResultatAlgorithme> resultatsParAlgorithme,
                               List<Metrique> metriques) {
        this.dossierResultats = dossierResultats;
        this.processus = List.copyOf(processus);
        this.resultatsParAlgorithme = new EnumMap<>(resultatsParAlgorithme);
        this.metriques = List.copyOf(metriques);
    }

    /** @return le dossier des fichiers de resultats */
    public Path getDossierResultats() {
        return dossierResultats;
    }

    /** @return les processus d'entree (liste non modifiable) */
    public List<Processus> getProcessus() {
        return processus;
    }

    /** @return les metriques de chaque algorithme (liste non modifiable) */
    public List<Metrique> getMetriques() {
        return metriques;
    }

    /** @return les algorithmes simules, dans l'ordre FIFO, RR, EDF */
    public List<Algorithme> getAlgorithmes() {
        return new ArrayList<>(resultatsParAlgorithme.keySet());
    }

    /**
     * @param algorithme un algorithme
     * @return ses resultats, ou null s'il n'a pas ete simule
     */
    public ResultatAlgorithme getResultat(Algorithme algorithme) {
        return resultatsParAlgorithme.get(algorithme);
    }

    /** @return la liste des processeurs qui apparaissent dans les allocations, tries */
    public List<String> getProcesseurs() {
        List<String> processeurs = new ArrayList<>();
        for (ResultatAlgorithme r : resultatsParAlgorithme.values()) {
            for (Allocation a : r.getAllocations()) {
                if (!processeurs.contains(a.getIdProcesseur())) {
                    processeurs.add(a.getIdProcesseur());
                }
            }
        }
        processeurs.sort(null);
        return processeurs;
    }
}
