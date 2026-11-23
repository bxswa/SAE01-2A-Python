package iut.sae.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Petites methodes pour retrouver les dossiers de l'application.
 *
 * <p>Les dossiers Inputs, Outputs et Code (Python) sont places a cote du
 * .jar. Pendant le developpement sous VSCode, le programme est lance depuis
 * le dossier du projet Maven : ces dossiers sont alors dans le dossier
 * parent. Cette classe gere les deux cas.</p>
 */
public final class Chemins {

    /** Classe utilitaire : on ne cree pas d'objet Chemins. */
    private Chemins() {
    }

    /**
     * Renvoie le dossier racine qui contient Inputs, Outputs et Code.
     *
     * @return le dossier courant s'il contient Inputs, sinon son parent
     *         s'il contient Inputs, sinon le dossier courant
     */
    public static Path racineDonnees() {
        Path courant = Paths.get("").toAbsolutePath();
        if (Files.isDirectory(courant.resolve(Constantes.DOSSIER_ENTREES))) {
            return courant;
        }
        Path parent = courant.getParent();
        if (parent != null && Files.isDirectory(parent.resolve(Constantes.DOSSIER_ENTREES))) {
            return parent;
        }
        return courant;
    }

    /**
     * Renvoie le dossier du jeu d'essai propose au demarrage.
     *
     * @return par exemple .../Inputs/Test1
     */
    public static Path dossierTestParDefaut() {
        return racineDonnees().resolve(Constantes.DOSSIER_ENTREES)
                              .resolve(Constantes.TEST_PAR_DEFAUT);
    }

    /**
     * Renvoie le programme Python principal.
     *
     * @return par exemple .../Code/main.py
     */
    public static Path scriptPython() {
        return racineDonnees().resolve(Constantes.DOSSIER_CODE).resolve(Constantes.SCRIPT_PYTHON);
    }

    /**
     * Renvoie le dossier ou Python doit ecrire les resultats d'un config.txt.
     *
     * <p>Meme regle que dans main.py : un config place dans Inputs/Test1
     * donne Outputs/Test1. Sinon, les resultats sont ecrits a cote du
     * config.</p>
     *
     * @param fichierConfig le fichier config.txt
     * @return le dossier de sortie
     */
    public static Path dossierSortie(Path fichierConfig) {
        Path dossier = fichierConfig.toAbsolutePath().normalize().getParent();
        Path parent = dossier.getParent();
        if (parent != null && parent.getFileName() != null
                && parent.getFileName().toString().equals(Constantes.DOSSIER_ENTREES)
                && parent.getParent() != null) {
            return parent.getParent().resolve(Constantes.DOSSIER_SORTIES)
                         .resolve(dossier.getFileName());
        }
        return dossier;
    }

    /**
     * Donne le nom du fichier d'energie ecrit par Python a partir du nom
     * du fichier global (meme regle que nom_fichier_energie dans main.py).
     *
     * @param fichierGlobal par exemple resultats_fifo_global.csv
     * @return par exemple resultats_fifo_energie.csv
     */
    public static String fichierEnergie(String fichierGlobal) {
        if (fichierGlobal.contains(Constantes.MOT_GLOBAL)) {
            return fichierGlobal.replace(Constantes.MOT_GLOBAL, Constantes.MOT_ENERGIE);
        }
        return fichierGlobal.replace(Constantes.EXTENSION_CSV, "") + "_"
                + Constantes.MOT_ENERGIE + Constantes.EXTENSION_CSV;
    }

    /**
     * Ecrit un chemin relativement a un dossier, avec des '/' (format
     * compris par Python sous Windows comme sous Linux).
     *
     * <p>Si le chemin ne peut pas etre exprime relativement (par exemple
     * un autre disque sous Windows), le chemin absolu est renvoye.</p>
     *
     * @param chemin  le chemin a convertir (absolu ou relatif au dossier courant)
     * @param dossier le dossier de reference
     * @return le chemin relatif a {@code dossier}, ou le chemin absolu
     */
    public static String relatif(String chemin, Path dossier) {
        if (chemin == null || chemin.isBlank()) {
            return "";
        }
        Path absolu = Paths.get(chemin.trim()).toAbsolutePath().normalize();
        try {
            Path relatif = dossier.toAbsolutePath().normalize().relativize(absolu);
            return relatif.toString().replace('\\', '/');
        } catch (IllegalArgumentException e) {
            return absolu.toString();
        }
    }

    /**
     * Transforme un chemin lu dans config.txt en chemin absolu.
     *
     * @param chemin  le chemin lu (relatif au dossier du config, ou absolu)
     * @param dossier le dossier du fichier config.txt
     * @return le chemin absolu correspondant
     */
    public static String absolu(String chemin, Path dossier) {
        Path p = Paths.get(chemin.trim());
        if (!p.isAbsolute()) {
            p = dossier.resolve(p);
        }
        return p.toAbsolutePath().normalize().toString();
    }
}
