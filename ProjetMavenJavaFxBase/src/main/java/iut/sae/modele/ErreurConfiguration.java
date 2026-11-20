package iut.sae.modele;

/**
 * Exception levee quand un fichier config.txt est mal forme
 * (ligne sans '=', algorithme inconnu, cle obligatoire absente...).
 */
public class ErreurConfiguration extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Cree l'exception avec un message lisible par l'utilisateur.
     *
     * @param message la description du probleme
     */
    public ErreurConfiguration(String message) {
        super(message);
    }
}
