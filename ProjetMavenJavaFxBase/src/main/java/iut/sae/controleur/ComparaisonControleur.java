package iut.sae.controleur;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

import iut.sae.modele.EtatApplication;
import iut.sae.modele.Metrique;
import iut.sae.modele.ResultatsSimulation;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

/**
 * Controleur de l'onglet "Comparaison" (vue Comparaison.fxml).
 *
 * <p>Affiche les metriques globales de chaque algorithme (tableau),
 * l'algorithme le plus performant par critere et trois graphiques :
 * temps (attente, reponse, makespan), energie consommee / disponible et
 * taux d'utilisation de l'energie verte.</p>
 */
public class ComparaisonControleur {

    private static final String CAT_ATTENTE = "Temps d'attente moyen";
    private static final String CAT_REPONSE = "Temps de réponse moyen";
    private static final String CAT_MAKESPAN = "Makespan";

    @FXML private Label lblMessage;
    @FXML private TableView<Metrique> tableMetriques;
    @FXML private TableColumn<Metrique, String> colAlgorithme;
    @FXML private TableColumn<Metrique, Double> colAttente;
    @FXML private TableColumn<Metrique, Double> colReponse;
    @FXML private TableColumn<Metrique, Integer> colMakespan;
    @FXML private TableColumn<Metrique, Integer> colConsommee;
    @FXML private TableColumn<Metrique, Integer> colDisponible;
    @FXML private TableColumn<Metrique, Double> colTaux;

    @FXML private VBox vbMeilleurs;
    @FXML private BarChart<String, Number> graphTemps;
    @FXML private BarChart<String, Number> graphEnergie;
    @FXML private BarChart<String, Number> graphTaux;

    /** Appelee par le FXMLLoader apres le chargement de la vue. */
    @FXML
    private void initialize() {
        colAlgorithme.setCellValueFactory(new PropertyValueFactory<>("nomAlgorithme"));
        configurerColonneDecimale(colAttente, "tempsAttenteMoyen", false);
        configurerColonneDecimale(colReponse, "tempsReponseMoyen", false);
        colMakespan.setCellValueFactory(new PropertyValueFactory<>("makespan"));
        colConsommee.setCellValueFactory(new PropertyValueFactory<>("energieConsommee"));
        colDisponible.setCellValueFactory(new PropertyValueFactory<>("energieDisponible"));
        configurerColonneDecimale(colTaux, "tauxUtilisation", true);

        // Pas d'animation : evite les graphiques mal dessines quand on les remplit plusieurs fois.
        graphTemps.setAnimated(false);
        graphEnergie.setAnimated(false);
        graphTaux.setAnimated(false);

        EtatApplication.resultatsProperty().addListener((obs, ancien, nouveau) -> afficher(nouveau));
        afficher(EtatApplication.getResultats());
    }

    /**
     * Configure une colonne de nombres decimaux avec 2 chiffres apres la
     * virgule, ou en pourcentage.
     *
     * @param colonne     la colonne
     * @param propriete   le nom de la propriete (getter sans "get")
     * @param enPourcent  true pour afficher 0,3862 sous la forme 38,6 %
     */
    private static void configurerColonneDecimale(TableColumn<Metrique, Double> colonne,
                                                  String propriete, boolean enPourcent) {
        colonne.setCellValueFactory(new PropertyValueFactory<>(propriete));
        colonne.setCellFactory(c -> new TableCell<Metrique, Double>() {
            @Override
            protected void updateItem(Double valeur, boolean vide) {
                super.updateItem(valeur, vide);
                if (vide || valeur == null) {
                    setText(null);
                } else if (enPourcent) {
                    setText(String.format(Locale.FRANCE, "%.1f %%", valeur * 100));
                } else {
                    setText(String.format(Locale.FRANCE, "%.2f", valeur));
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Mise a jour de l'affichage
    // ------------------------------------------------------------------

    /**
     * Remplit le tableau, les meilleurs et les graphiques.
     *
     * @param resultats les resultats de la derniere simulation (peut etre null)
     */
    private void afficher(ResultatsSimulation resultats) {
        tableMetriques.getItems().clear();
        vbMeilleurs.getChildren().clear();
        graphTemps.getData().clear();
        graphEnergie.getData().clear();
        graphTaux.getData().clear();

        if (resultats == null || resultats.getMetriques().isEmpty()) {
            lblMessage.setText("Aucune métrique : lancez une simulation depuis l'onglet Configuration.");
            return;
        }
        lblMessage.setText("");

        List<Metrique> metriques = resultats.getMetriques();
        tableMetriques.getItems().setAll(metriques);
        remplirMeilleurs(metriques);
        remplirGraphiqueTemps(metriques);
        remplirGraphiqueEnergie(metriques);
        remplirGraphiqueTaux(metriques);
    }

    /** Ajoute une ligne "meilleur algorithme" par critere. */
    private void remplirMeilleurs(List<Metrique> metriques) {
        ajouterMeilleur("Plus petit temps d'attente moyen",
                metriques, Metrique::getTempsAttenteMoyen, true);
        ajouterMeilleur("Plus petit temps de réponse moyen",
                metriques, Metrique::getTempsReponseMoyen, true);
        ajouterMeilleur("Plus petit makespan",
                metriques, Metrique::getMakespan, true);
        ajouterMeilleur("Meilleur taux d'utilisation de l'énergie verte",
                metriques, Metrique::getTauxUtilisation, false);
    }

    /**
     * Ajoute une ligne "critere : algorithme(s)". En cas d'egalite, tous
     * les algorithmes a egalite sont cites.
     *
     * @param critere   le texte du critere
     * @param metriques les metriques de tous les algorithmes
     * @param valeur    la valeur a comparer
     * @param minimiser true si la plus petite valeur est la meilleure
     */
    private void ajouterMeilleur(String critere, List<Metrique> metriques,
                                 ToDoubleFunction<Metrique> valeur, boolean minimiser) {
        double meilleure = valeur.applyAsDouble(metriques.get(0));
        for (Metrique m : metriques) {
            double v = valeur.applyAsDouble(m);
            meilleure = minimiser ? Math.min(meilleure, v) : Math.max(meilleure, v);
        }
        List<String> noms = new ArrayList<>();
        for (Metrique m : metriques) {
            if (Math.abs(valeur.applyAsDouble(m) - meilleure) < 1e-9) {
                noms.add(m.getNomAlgorithme());
            }
        }
        vbMeilleurs.getChildren().add(new Label(critere + " : " + String.join(" / ", noms)));
    }

    /** Graphique 1 : une serie par algorithme, trois groupes (attente, reponse, makespan). */
    private void remplirGraphiqueTemps(List<Metrique> metriques) {
        for (Metrique m : metriques) {
            XYChart.Series<String, Number> serie = new XYChart.Series<>();
            serie.setName(m.getNomAlgorithme());
            serie.getData().add(new XYChart.Data<>(CAT_ATTENTE, m.getTempsAttenteMoyen()));
            serie.getData().add(new XYChart.Data<>(CAT_REPONSE, m.getTempsReponseMoyen()));
            serie.getData().add(new XYChart.Data<>(CAT_MAKESPAN, m.getMakespan()));
            graphTemps.getData().add(serie);
        }
    }

    /** Graphique 2 : energie consommee et disponible pour chaque algorithme. */
    private void remplirGraphiqueEnergie(List<Metrique> metriques) {
        XYChart.Series<String, Number> consommee = new XYChart.Series<>();
        consommee.setName("Énergie consommée");
        XYChart.Series<String, Number> disponible = new XYChart.Series<>();
        disponible.setName("Énergie disponible");
        for (Metrique m : metriques) {
            consommee.getData().add(new XYChart.Data<>(m.getNomAlgorithme(), m.getEnergieConsommee()));
            disponible.getData().add(new XYChart.Data<>(m.getNomAlgorithme(), m.getEnergieDisponible()));
        }
        graphEnergie.getData().add(consommee);
        graphEnergie.getData().add(disponible);
    }

    /** Graphique 3 : taux d'utilisation de l'energie verte en pourcentage. */
    private void remplirGraphiqueTaux(List<Metrique> metriques) {
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Taux d'utilisation (%)");
        for (Metrique m : metriques) {
            serie.getData().add(new XYChart.Data<>(m.getNomAlgorithme(), m.getTauxUtilisation() * 100));
        }
        graphTaux.getData().add(serie);
    }
}
