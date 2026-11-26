package iut.sae.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import iut.sae.modele.Algorithme;
import iut.sae.modele.Allocation;
import iut.sae.modele.Configuration;
import iut.sae.modele.ErreurConfiguration;
import iut.sae.modele.Metrique;
import iut.sae.modele.PointEnergie;
import iut.sae.modele.Processus;
import iut.sae.modele.ResultatAlgorithme;
import iut.sae.modele.ResultatProcessus;
import iut.sae.modele.ResultatsSimulation;
import iut.sae.util.Constantes;

/**
 * Lit les fichiers ecrits par Python apres une simulation et les
 * transforme en objets Java ({@link ResultatsSimulation}).
 *
 * <p>Les noms des fichiers a lire sont ceux de la configuration : ce sont
 * les memes que ceux que Python a utilises pour ecrire.</p>
 */
public class LecteurResultats {

    /** Lecteur de fichiers CSV. */
    private final LecteurCSV csv = new LecteurCSV();

    /**
     * Lit tous les resultats d'une simulation.
     *
     * @param config        la configuration utilisee pour la simulation
     * @param dossierSortie le dossier ou Python a ecrit les resultats
     * @return les resultats lus
     * @throws IOException si un fichier obligatoire manque ou est mal forme
     */
    public ResultatsSimulation lire(Configuration config, Path dossierSortie) throws IOException {
        List<Processus> processus = lireProcessus(Paths.get(config.getFichierProcessus()));

        Map<Algorithme, ResultatAlgorithme> resultats = new EnumMap<>(Algorithme.class);
        for (Algorithme a : config.getAlgorithmes()) {
            Path global = dossierSortie.resolve(config.getFichierGlobal(a));
            Path allocations = dossierSortie.resolve(config.getFichierAllocations(a));
            Path energie = dossierSortie.resolve(config.getFichierMetriquesEnergie(a));
            resultats.put(a, new ResultatAlgorithme(a,
                    lireGlobal(global),
                    lireAllocations(allocations),
                    lireEnergie(energie)));
        }

        List<Metrique> metriques = lireMetriques(dossierSortie.resolve(config.getFichierMetriques()));
        return new ResultatsSimulation(dossierSortie, processus, resultats, metriques);
    }

    /**
     * Lit le fichier des processus (fichier d'entree).
     *
     * @param fichier le fichier processus.csv
     * @return les processus
     * @throws IOException si le fichier est absent ou mal forme
     */
    public List<Processus> lireProcessus(Path fichier) throws IOException {
        List<Processus> liste = new ArrayList<>();
        try {
            for (Map<String, String> l : csv.lire(fichier)) {
                liste.add(new Processus(
                        LecteurCSV.texte(l, Constantes.COL_ID_PROCESSUS),
                        LecteurCSV.entier(l, Constantes.COL_DATE_SOUMISSION),
                        LecteurCSV.entier(l, Constantes.COL_DUREE_EXECUTION),
                        LecteurCSV.entier(l, Constantes.COL_RAM_NECESSAIRE),
                        LecteurCSV.entier(l, Constantes.COL_PRIORITE),
                        LecteurCSV.entier(l, Constantes.COL_DATE_BUTOIR)));
            }
        } catch (IOException e) {
            throw erreur(fichier, e);
        }
        return liste;
    }

    /**
     * Lit le fichier global d'un algorithme.
     *
     * @param fichier le fichier global
     * @return une ligne par processus
     * @throws IOException si le fichier est absent ou mal forme
     */
    public List<ResultatProcessus> lireGlobal(Path fichier) throws IOException {
        List<ResultatProcessus> liste = new ArrayList<>();
        try {
            for (Map<String, String> l : csv.lire(fichier)) {
                liste.add(new ResultatProcessus(
                        LecteurCSV.texte(l, Constantes.COL_ID_PROCESSUS),
                        LecteurCSV.entier(l, Constantes.COL_DATE_SOUMISSION),
                        LecteurCSV.entierOuNull(l, Constantes.COL_DATE_DEBUT),
                        LecteurCSV.entierOuNull(l, Constantes.COL_DATE_FIN),
                        LecteurCSV.entier(l, Constantes.COL_RAM_DEMANDEE),
                        LecteurCSV.entier(l, Constantes.COL_RAM_UTILISEE)));
            }
        } catch (IOException e) {
            throw erreur(fichier, e);
        }
        return liste;
    }

    /**
     * Lit le fichier des allocations d'un algorithme.
     *
     * @param fichier le fichier des allocations
     * @return une ligne par periode d'execution
     * @throws IOException si le fichier est absent ou mal forme
     */
    public List<Allocation> lireAllocations(Path fichier) throws IOException {
        List<Allocation> liste = new ArrayList<>();
        try {
            for (Map<String, String> l : csv.lire(fichier)) {
                liste.add(new Allocation(
                        LecteurCSV.texte(l, Constantes.COL_ID_PROCESSUS),
                        LecteurCSV.entier(l, Constantes.COL_DATE_DEBUT),
                        LecteurCSV.entier(l, Constantes.COL_DATE_FIN),
                        LecteurCSV.texte(l, Constantes.COL_ID_PROCESSEUR)));
            }
        } catch (IOException e) {
            throw erreur(fichier, e);
        }
        return liste;
    }

    /**
     * Lit le fichier des metriques d'energie d'un algorithme. S'il
     * n'existe pas (ancienne version de Python), une liste vide est renvoyee.
     *
     * @param fichier le fichier d'energie
     * @return une ligne par instant (vide si le fichier est absent)
     * @throws IOException si le fichier existe mais est mal forme
     */
    public List<PointEnergie> lireEnergie(Path fichier) throws IOException {
        List<PointEnergie> liste = new ArrayList<>();
        if (!Files.isRegularFile(fichier)) {
            return liste;
        }
        try {
            for (Map<String, String> l : csv.lire(fichier)) {
                liste.add(new PointEnergie(
                        LecteurCSV.entier(l, Constantes.COL_INSTANT),
                        LecteurCSV.entier(l, Constantes.COL_ENERGIE_DISPONIBLE),
                        LecteurCSV.entier(l, Constantes.COL_ENERGIE_CONSOMMEE),
                        LecteurCSV.entier(l, Constantes.COL_NB_CPU_ACTIFS)));
            }
        } catch (IOException e) {
            throw erreur(fichier, e);
        }
        return liste;
    }

    /**
     * Lit le fichier des metriques.
     *
     * @param fichier le fichier des metriques
     * @return une metrique par algorithme
     * @throws IOException si le fichier est absent ou mal forme
     */
    public List<Metrique> lireMetriques(Path fichier) throws IOException {
        List<Metrique> liste = new ArrayList<>();
        try {
            for (Map<String, String> l : csv.lire(fichier)) {
                Algorithme algo;
                try {
                    algo = Algorithme.depuisCode(LecteurCSV.texte(l, Constantes.COL_ALGORITHME));
                } catch (ErreurConfiguration e) {
                    throw new IOException(e.getMessage());
                }
                liste.add(new Metrique(algo,
                        LecteurCSV.reel(l, Constantes.COL_ATTENTE_MOYENNE),
                        LecteurCSV.reel(l, Constantes.COL_REPONSE_MOYENNE),
                        LecteurCSV.entier(l, Constantes.COL_MAKESPAN),
                        LecteurCSV.entier(l, Constantes.COL_ENERGIE_CONSOMMEE_TOTALE),
                        LecteurCSV.entier(l, Constantes.COL_ENERGIE_DISPONIBLE_TOTALE),
                        LecteurCSV.reel(l, Constantes.COL_TAUX_UTILISATION)));
            }
        } catch (IOException e) {
            throw erreur(fichier, e);
        }
        // Meme ordre que l'enum (FIFO, RR, EDF), quel que soit l'ordre du fichier
        liste.sort((m1, m2) -> m1.getAlgorithme().compareTo(m2.getAlgorithme()));
        return liste;
    }

    /**
     * Ajoute le nom du fichier au message d'erreur s'il n'y est pas deja.
     *
     * @param fichier le fichier en cours de lecture
     * @param cause   l'erreur d'origine
     * @return l'erreur a lancer
     */
    private IOException erreur(Path fichier, IOException cause) {
        String nom = fichier.getFileName().toString();
        String message = cause.getMessage();
        if (message != null && message.contains(nom)) {
            return cause;
        }
        return new IOException(nom + " : " + message, cause);
    }
}
