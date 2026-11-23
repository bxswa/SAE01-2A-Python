package iut.sae.modele;

/**
 * Resultat d'un lancement du programme Python : son code retour et ce
 * qu'il a affiche (sortie standard et sortie d'erreur melangees).
 *
 * <p>Par convention, main.py renvoie 0 si tout s'est bien passe et 1 en cas
 * d'erreur. Le code -1 est utilise quand Python n'a pas pu etre lance du
 * tout (Python ou main.py introuvable).</p>
 */
public class ResultatExecution {

    /** Code utilise quand le programme n'a pas pu etre lance. */
    public static final int CODE_LANCEMENT_IMPOSSIBLE = -1;

    /** Code retour du processus Python. */
    private final int codeRetour;

    /** Texte affiche par Python. */
    private final String sortie;

    /**
     * Cree un resultat d'execution.
     *
     * @param codeRetour code retour du programme
     * @param sortie     texte affiche par le programme
     */
    public ResultatExecution(int codeRetour, String sortie) {
        this.codeRetour = codeRetour;
        this.sortie = sortie == null ? "" : sortie.trim();
    }

    /**
     * Cree le resultat d'un lancement impossible.
     *
     * @param message explication du probleme
     * @return le resultat correspondant
     */
    public static ResultatExecution lancementImpossible(String message) {
        return new ResultatExecution(CODE_LANCEMENT_IMPOSSIBLE, message);
    }

    /** @return true si Python s'est termine sans erreur (code 0) */
    public boolean estReussie() {
        return codeRetour == 0;
    }

    /** @return le code retour */
    public int getCodeRetour() {
        return codeRetour;
    }

    /** @return le texte affiche par Python */
    public String getSortie() {
        return sortie;
    }
}
