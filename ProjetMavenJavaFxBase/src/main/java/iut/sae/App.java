package iut.sae;

import java.io.IOException;
import java.net.URL;

import iut.sae.util.Constantes;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * Application JavaFX : charge la vue principale (les onglets) et
 * l'affiche dans la fenetre.
 */
public class App extends Application {

    /**
     * Lance l'application JavaFX. Appelee par {@link Main#main(String[])}.
     *
     * @param args les arguments de la ligne de commande
     */
    public static void lancer(String[] args) {
        Application.launch(args);
    }

    /**
     * Construit et affiche la fenetre principale.
     *
     * @param fenetre la fenetre fournie par JavaFX
     */
    @Override
    public void start(Stage fenetre) {
        try {
            FXMLLoader chargeur = new FXMLLoader(App.class.getResource(Constantes.VUE_PRINCIPALE));
            Parent racine = chargeur.load();
            Scene scene = new Scene(racine, Constantes.LARGEUR_FENETRE, Constantes.HAUTEUR_FENETRE);
            URL style = App.class.getResource(Constantes.FEUILLE_STYLE);
            if (style != null) {
                scene.getStylesheets().add(style.toExternalForm());
            }
            fenetre.setTitle(Constantes.TITRE_APPLICATION);
            fenetre.setScene(scene);
            fenetre.show();
        } catch (IOException e) {
            Alert alerte = new Alert(Alert.AlertType.ERROR,
                    "Impossible de charger l'interface : " + e.getMessage());
            alerte.showAndWait();
        }
    }
}
