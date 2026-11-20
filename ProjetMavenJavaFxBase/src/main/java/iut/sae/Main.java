package iut.sae;

/**
 * Point d'entree du programme.
 *
 * <p>Cette classe n'herite pas de {@code Application} : c'est obligatoire
 * pour que le .jar genere par le plugin Maven "shade" puisse demarrer
 * JavaFX (voir la consigne "Generer un .jar avec Maven et Java17/JavaFX").</p>
 */
public class Main {

    /**
     * Demarre l'application.
     *
     * @param args les arguments de la ligne de commande (non utilises)
     */
    public static void main(String[] args) {
        App.lancer(args);
    }
}
