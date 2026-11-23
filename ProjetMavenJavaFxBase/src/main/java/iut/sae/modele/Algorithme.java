package iut.sae.modele;

import iut.sae.util.Constantes;

/**
 * Les algorithmes d'ordonnancement proposes par le programme Python.
 *
 * <p>Chaque algorithme connait les noms qu'il porte dans config.txt :
 * son code dans la ligne {@code algorithmes = ...} et le suffixe des cles
 * de ses fichiers de sortie (le config fourni ecrit {@code RR} dans la
 * liste mais {@code fichier_global_RoundRobin} pour les fichiers).</p>
 */
public enum Algorithme {

    /** Premier arrive, premier servi. */
    FIFO("FIFO", "FIFO", "FIFO", "fifo"),

    /** Tourniquet avec quantum de temps. */
    ROUND_ROBIN("RR", "RoundRobin", "Round Robin", "roundrobin"),

    /** Date butoir la plus proche d'abord. */
    EDF("EDF", "EDF", "EDF", "EDF");

    /** Code dans la ligne "algorithmes = ..." de config.txt. */
    private final String code;

    /** Fin des cles fichier_global_XXX et fichier_allocations_XXX. */
    private final String suffixeCle;

    /** Nom affiche a l'utilisateur. */
    private final String libelle;

    /** Morceau utilise dans les noms de fichiers par defaut. */
    private final String prefixeFichier;

    /**
     * Cree un algorithme.
     *
     * @param code           code dans la liste des algorithmes
     * @param suffixeCle     suffixe des cles de fichiers de sortie
     * @param libelle        nom affiche
     * @param prefixeFichier morceau des noms de fichiers par defaut
     */
    Algorithme(String code, String suffixeCle, String libelle, String prefixeFichier) {
        this.code = code;
        this.suffixeCle = suffixeCle;
        this.libelle = libelle;
        this.prefixeFichier = prefixeFichier;
    }

    /** @return le code dans config.txt (FIFO, RR ou EDF) */
    public String getCode() {
        return code;
    }

    /** @return le nom affiche a l'utilisateur */
    public String getLibelle() {
        return libelle;
    }

    /** @return la cle du fichier global, par exemple fichier_global_RoundRobin */
    public String getCleGlobal() {
        return Constantes.PREFIXE_CLE_GLOBAL + suffixeCle;
    }

    /** @return la cle du fichier des allocations */
    public String getCleAllocations() {
        return Constantes.PREFIXE_CLE_ALLOCATIONS + suffixeCle;
    }

    /**
     * Renvoie la cle du fichier des metriques d'energie dans config.txt.
     *
     * @return par exemple fichier_metriques_energie_RoundRobin
     */
    public String getCleEnergie() {
        return Constantes.PREFIXE_CLE_ENERGIE_SORTIE + suffixeCle;
    }

    /** @return le nom par defaut du fichier global, par exemple resultats_fifo_global.csv */
    public String getFichierGlobalParDefaut() {
        return "resultats_" + prefixeFichier + "_global" + Constantes.EXTENSION_CSV;
    }

    /** @return le nom par defaut du fichier des allocations */
    public String getFichierAllocationsParDefaut() {
        return "resultats_" + prefixeFichier + "_allocations" + Constantes.EXTENSION_CSV;
    }

    /**
     * Renvoie le nom propose par defaut pour le fichier des metriques
     * d'energie (meme nom que Python quand la cle est absente).
     *
     * @return par exemple resultats_fifo_energie.csv
     */
    public String getFichierEnergieParDefaut() {
        return "resultats_" + prefixeFichier + "_" + Constantes.MOT_ENERGIE + Constantes.EXTENSION_CSV;
    }

    /**
     * Retrouve un algorithme a partir de son code dans config.txt.
     *
     * @param code le code lu (majuscules ou minuscules)
     * @return l'algorithme correspondant
     * @throws ErreurConfiguration si le code ne correspond a aucun algorithme
     */
    public static Algorithme depuisCode(String code) throws ErreurConfiguration {
        for (Algorithme a : values()) {
            if (a.code.equalsIgnoreCase(code.trim())) {
                return a;
            }
        }
        throw new ErreurConfiguration("Algorithme inconnu : " + code.trim()
                + " (attendus : FIFO, RR, EDF)");
    }
}
