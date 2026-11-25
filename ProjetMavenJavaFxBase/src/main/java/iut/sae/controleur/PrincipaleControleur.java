package iut.sae.controleur;

import iut.sae.modele.EtatApplication;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class PrincipaleControleur {

    @FXML
    private TabPane tabPane;

    @FXML
    private void initialize() {
        EtatApplication.resultatsProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null) {
                activerOnglets();
            }
        });

        // Si des résultats existent déjà au chargement
        if (EtatApplication.getResultats() != null) {
            activerOnglets();
        }
    }

    private void activerOnglets() {
        for (Tab tab : tabPane.getTabs()) {
            tab.setDisable(false);
        }
    }
}