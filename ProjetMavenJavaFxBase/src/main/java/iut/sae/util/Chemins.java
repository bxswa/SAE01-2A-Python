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
