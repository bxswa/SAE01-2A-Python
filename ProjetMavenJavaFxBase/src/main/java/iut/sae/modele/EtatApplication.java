package iut.sae.modele;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Donnees partagees entre tous les ecrans de l'application.
 *
 * <p>Apres chaque simulation, l'ecran de configuration range les resultats
 * ici. Les autres ecrans (processus, Gantt, comparaison) ecoutent la
 * propriete {@link #resultatsProperty()} et se mettent a jour tout seuls
 * quand elle change (patron Observateur, fourni par les proprietes
 * JavaFX).</p>
 *
 * <p>Exemple d'utilisation dans le controleur d'un ecran d'affichage :</p>
 * <pre>
 * EtatApplication.resultatsProperty().addListener(
 *         (obs, ancien, nouveau) -&gt; afficher(nouveau));
 * </pre>
 */
public final class EtatApplication {

    /** Resultats de la derniere simulation (null tant qu'aucune n'a ete lancee). */
    private static final ObjectProperty<ResultatsSimulation> RESULTATS =
            new SimpleObjectProperty<>();

    /** Classe utilitaire : on ne cree pas d'objet EtatApplication. */
    private EtatApplication() {
    }

    /** @return la propriete observable des resultats de la derniere simulation */
    public static ObjectProperty<ResultatsSimulation> resultatsProperty() {
        return RESULTATS;
    }

    /** @return les resultats de la derniere simulation, ou null */
    public static ResultatsSimulation getResultats() {
        return RESULTATS.get();
    }

    /**
     * Remplace les resultats : tous les ecrans qui ecoutent sont prevenus.
     *
     * @param resultats les nouveaux resultats
     */
    public static void setResultats(ResultatsSimulation resultats) {
        RESULTATS.set(resultats);
    }
}
