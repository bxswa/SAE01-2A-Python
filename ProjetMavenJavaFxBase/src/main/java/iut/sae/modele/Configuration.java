package iut.sae.modele;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import iut.sae.util.Constantes;

/**
 * Le contenu d'un fichier config.txt : les fichiers d'entree, les
 * algorithmes choisis, le quantum et les noms des fichiers de sortie.
 *
 * <p>Les chemins des fichiers d'entree sont gardes en absolu dans cet
 * objet. C'est {@link iut.sae.service.EcrivainConfig} qui les ecrit en
 * relatif dans config.txt.</p>
 */
public class Configuration {

    /** Chemin du fichier des processus. */
    private String fichierProcessus = "";

    /** Chemin du fichier des ressources. */
    private String fichierRessources = "";

    /** Chemin du fichier d'energie. */
    private String fichierEnergie = "";

    /** Nom du fichier des metriques (dans le dossier de sortie). */
    private String fichierMetriques = Constantes.FICHIER_METRIQUES_DEFAUT;

    /** Quantum utilise par Round Robin. */
    private int quantum = Constantes.QUANTUM_DEFAUT;

    /** Algorithmes choisis (EnumSet garde l'ordre FIFO, RR, EDF). */
    private final EnumSet<Algorithme> algorithmes = EnumSet.noneOf(Algorithme.class);

    /** Nom du fichier global de chaque algorithme. */
    private final Map<Algorithme, String> fichiersGlobaux = new EnumMap<>(Algorithme.class);

    /** Nom du fichier des allocations de chaque algorithme. */
    private final Map<Algorithme, String> fichiersAllocations = new EnumMap<>(Algorithme.class);

    /** Nom du fichier des metriques d'energie de chaque algorithme. */
    private final Map<Algorithme, String> fichiersEnergie = new EnumMap<>(Algorithme.class);

    /**
     * Cree une configuration vide : aucun algorithme choisi, noms de
     * fichiers de sortie par defaut.
     */
    public Configuration() {
        for (Algorithme a : Algorithme.values()) {
            fichiersGlobaux.put(a, a.getFichierGlobalParDefaut());
            fichiersAllocations.put(a, a.getFichierAllocationsParDefaut());
            fichiersEnergie.put(a, a.getFichierEnergieParDefaut());
        }
    }

    /**
     * Cree la configuration proposee au demarrage : les fichiers d'un
     * dossier de test et les trois algorithmes.
     *
     * @param dossierTest le dossier qui contient processus.csv, ressources.json...
     * @return la configuration par defaut
     */
    public static Configuration parDefaut(Path dossierTest) {
        Configuration c = new Configuration();
        c.setFichierProcessus(dossierTest.resolve(Constantes.FICHIER_PROCESSUS_DEFAUT).toString());
        c.setFichierRessources(dossierTest.resolve(Constantes.FICHIER_RESSOURCES_DEFAUT).toString());
        c.setFichierEnergie(dossierTest.resolve(Constantes.FICHIER_ENERGIE_DEFAUT).toString());
        for (Algorithme a : Algorithme.values()) {
            c.selectionner(a, true);
        }
        return c;
    }

    // ------------------------------------------------------------------
    // Verification
    // ------------------------------------------------------------------

    /**
     * Verifie que la configuration est utilisable par Python.
     *
     * @return la liste des problemes trouves (vide si tout va bien)
     */
    public List<String> verifier() {
        List<String> erreurs = new ArrayList<>();
        verifierFichierEntree(fichierProcessus, "processus", erreurs);
        verifierFichierEntree(fichierRessources, "ressources", erreurs);
        verifierFichierEntree(fichierEnergie, "energie", erreurs);
        if (algorithmes.isEmpty()) {
            erreurs.add("Choisissez au moins un algorithme.");
        }
        if (estSelectionne(Algorithme.ROUND_ROBIN) && quantum < Constantes.QUANTUM_MIN) {
            erreurs.add("Le quantum doit etre superieur ou egal a " + Constantes.QUANTUM_MIN + ".");
        }
        for (Algorithme a : algorithmes) {
            verifierFichierSortie(fichiersGlobaux.get(a), "global " + a.getLibelle(), erreurs);
            verifierFichierSortie(fichiersAllocations.get(a), "allocations " + a.getLibelle(), erreurs);
            verifierFichierSortie(fichiersEnergie.get(a), "energie " + a.getLibelle(), erreurs);
        }
        verifierFichierSortie(fichierMetriques, "metriques", erreurs);
        return erreurs;
    }

    /**
     * Ajoute une erreur si un fichier d'entree est vide ou n'existe pas.
     *
     * @param chemin  le chemin du fichier
     * @param nom     le nom du fichier pour le message
     * @param erreurs la liste ou ajouter l'erreur
     */
    private void verifierFichierEntree(String chemin, String nom, List<String> erreurs) {
        if (chemin.isBlank()) {
            erreurs.add("Le fichier des " + nom + " n'est pas choisi.");
        } else if (!Files.isRegularFile(Paths.get(chemin))) {
            erreurs.add("Le fichier des " + nom + " est introuvable : " + chemin);
        }
    }

    /**
     * Ajoute une erreur si un nom de fichier de sortie est vide ou ne finit pas par .csv.
     *
     * @param nom      le nom du fichier
     * @param libelle  la description du fichier pour le message
     * @param erreurs  la liste ou ajouter l'erreur
     */
    private void verifierFichierSortie(String nom, String libelle, List<String> erreurs) {
        if (nom == null || nom.isBlank()) {
            erreurs.add("Le nom du fichier " + libelle + " est vide.");
        } else if (!nom.trim().endsWith(Constantes.EXTENSION_CSV)) {
            erreurs.add("Le fichier " + libelle + " doit finir par " + Constantes.EXTENSION_CSV + ".");
        }
    }

    // ------------------------------------------------------------------
    // Algorithmes
    // ------------------------------------------------------------------

    /**
     * Choisit ou retire un algorithme.
     *
     * @param algorithme l'algorithme concerne
     * @param choisi     true pour le choisir, false pour le retirer
     */
    public void selectionner(Algorithme algorithme, boolean choisi) {
        if (choisi) {
            algorithmes.add(algorithme);
        } else {
            algorithmes.remove(algorithme);
        }
    }

    /**
     * @param algorithme l'algorithme concerne
     * @return true si l'algorithme est choisi
     */
    public boolean estSelectionne(Algorithme algorithme) {
        return algorithmes.contains(algorithme);
    }

    /** @return les algorithmes choisis, dans l'ordre FIFO, RR, EDF */
    public Set<Algorithme> getAlgorithmes() {
        return EnumSet.copyOf(algorithmes);
    }

    // ------------------------------------------------------------------
    // Getters et setters
    // ------------------------------------------------------------------

    /** @return le chemin du fichier des processus */
    public String getFichierProcessus() {
        return fichierProcessus;
    }

    /** @param chemin le chemin du fichier des processus */
    public void setFichierProcessus(String chemin) {
        this.fichierProcessus = nettoyer(chemin);
    }

    /** @return le chemin du fichier des ressources */
    public String getFichierRessources() {
        return fichierRessources;
    }

    /** @param chemin le chemin du fichier des ressources */
    public void setFichierRessources(String chemin) {
        this.fichierRessources = nettoyer(chemin);
    }

    /** @return le chemin du fichier d'energie */
    public String getFichierEnergie() {
        return fichierEnergie;
    }

    /** @param chemin le chemin du fichier d'energie */
    public void setFichierEnergie(String chemin) {
        this.fichierEnergie = nettoyer(chemin);
    }

    /** @return le nom du fichier des metriques */
    public String getFichierMetriques() {
        return fichierMetriques;
    }

    /** @param nom le nom du fichier des metriques */
    public void setFichierMetriques(String nom) {
        this.fichierMetriques = nettoyer(nom);
    }

    /** @return le quantum de Round Robin */
    public int getQuantum() {
        return quantum;
    }

    /** @param quantum le quantum de Round Robin */
    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }

    /**
     * @param algorithme l'algorithme concerne
     * @return le nom de son fichier global
     */
    public String getFichierGlobal(Algorithme algorithme) {
        return fichiersGlobaux.get(algorithme);
    }

    /**
     * @param algorithme l'algorithme concerne
     * @param nom        le nom de son fichier global
     */
    public void setFichierGlobal(Algorithme algorithme, String nom) {
        fichiersGlobaux.put(algorithme, nettoyer(nom));
    }

    /**
     * @param algorithme l'algorithme concerne
     * @return le nom de son fichier des allocations
     */
    public String getFichierAllocations(Algorithme algorithme) {
        return fichiersAllocations.get(algorithme);
    }

    /**
     * @param algorithme l'algorithme concerne
     * @param nom        le nom de son fichier des allocations
     */
    public void setFichierAllocations(Algorithme algorithme, String nom) {
        fichiersAllocations.put(algorithme, nettoyer(nom));
    }

    /**
     * @param algorithme l'algorithme concerne
     * @return le nom de son fichier des metriques d'energie
     */
    public String getFichierMetriquesEnergie(Algorithme algorithme) {
        return fichiersEnergie.get(algorithme);
    }

    /**
     * @param algorithme l'algorithme concerne
     * @param nom        le nom de son fichier des metriques d'energie
     */
    public void setFichierMetriquesEnergie(Algorithme algorithme, String nom) {
        fichiersEnergie.put(algorithme, nettoyer(nom));
    }

    /**
     * Remplace null par une chaine vide et enleve les espaces autour.
     *
     * @param texte le texte a nettoyer
     * @return le texte nettoye
     */
    private static String nettoyer(String texte) {
        return texte == null ? "" : texte.trim();
    }
}
