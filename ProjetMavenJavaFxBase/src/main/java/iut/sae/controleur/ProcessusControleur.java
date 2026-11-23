package iut.sae.controleur;

import java.util.List;

import iut.sae.modele.Algorithme;
import iut.sae.modele.EtatApplication;
import iut.sae.modele.LigneProcessus;
import iut.sae.modele.Processus;
import iut.sae.modele.ResultatAlgorithme;
import iut.sae.modele.ResultatsSimulation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

/**
 * Controleur de l'onglet "Processus" (vue Processus.fxml).
 *
 * <p>Affiche la liste des processus avec leurs caracteristiques
 * (processus.csv) et, pour l'algorithme choisi, leurs dates de debut et de
 * fin, leur temps d'attente et de reponse. L'ecran ecoute
 * {@link EtatApplication} : il se met a jour apres chaque simulation.</p>
 */
public class ProcessusControleur {

    private static final String TEXTE_VIDE = "—";
    private static final String STYLE_NON_EXECUTE = "ligne-non-executee";

    @FXML private ComboBox<Algorithme> cbAlgorithme;
    @FXML private TextField tfRecherche;
    @FXML private Label lblResume;
    @FXML private TableView<LigneProcessus> tableProcessus;

    @FXML private TableColumn<LigneProcessus, String> colId;
    @FXML private TableColumn<LigneProcessus, Integer> colSoumission;
    @FXML private TableColumn<LigneProcessus, Integer> colDuree;
    @FXML private TableColumn<LigneProcessus, Integer> colRam;
    @FXML private TableColumn<LigneProcessus, Integer> colPriorite;
    @FXML private TableColumn<LigneProcessus, Integer> colButoir;
    @FXML private TableColumn<LigneProcessus, Integer> colDebut;
    @FXML private TableColumn<LigneProcessus, Integer> colFin;
    @FXML private TableColumn<LigneProcessus, Integer> colAttente;
    @FXML private TableColumn<LigneProcessus, Integer> colReponse;
    @FXML private TableColumn<LigneProcessus, String> colStatut;

    /** Lignes du tableau (reconstruites a chaque changement). */
    private final ObservableList<LigneProcessus> lignes = FXCollections.observableArrayList();

    /** Lignes filtrees par la zone de recherche. */
    private FilteredList<LigneProcessus> lignesFiltrees;

    /** Appelee par le FXMLLoader apres le chargement de la vue. */
    @FXML
    private void initialize() {
        configurerColonnes();
        configurerTableau();
        configurerListeAlgorithmes();

        EtatApplication.resultatsProperty().addListener((obs, ancien, nouveau) -> afficher(nouveau));
        afficher(EtatApplication.getResultats());
    }

    // ------------------------------------------------------------------
    // Configuration de la vue
    // ------------------------------------------------------------------

    /** Relie chaque colonne au getter correspondant de LigneProcessus. */
    private void configurerColonnes() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        configurerColonneEntier(colSoumission, "dateSoumission");
        configurerColonneEntier(colDuree, "dureeExecution");
        configurerColonneEntier(colRam, "ramNecessaire");
        configurerColonneEntier(colPriorite, "priorite");
        configurerColonneEntier(colButoir, "dateButoir");
        configurerColonneEntier(colDebut, "dateDebut");
        configurerColonneEntier(colFin, "dateFin");
        configurerColonneEntier(colAttente, "tempsAttente");
        configurerColonneEntier(colReponse, "tempsReponse");
    }

    /**
     * Configure une colonne d'entiers : une valeur null (processus non
     * execute) est affichee par un tiret.
     *
     * @param colonne   la colonne a configurer
     * @param propriete le nom de la propriete (getter sans "get")
     */
    private static void configurerColonneEntier(TableColumn<LigneProcessus, Integer> colonne,
                                                String propriete) {
        colonne.setCellValueFactory(new PropertyValueFactory<>(propriete));
        colonne.setCellFactory(c -> new TableCell<LigneProcessus, Integer>() {
            @Override
            protected void updateItem(Integer valeur, boolean vide) {
                super.updateItem(valeur, vide);
                if (vide) {
                    setText(null);
                } else {
                    setText(valeur == null ? TEXTE_VIDE : valeur.toString());
                }
            }
        });
    }

    /** Branche le filtre, le tri et la coloration des processus non executes. */
    private void configurerTableau() {
        lignesFiltrees = new FilteredList<>(lignes, l -> true);
        SortedList<LigneProcessus> lignesTriees = new SortedList<>(lignesFiltrees);
        lignesTriees.comparatorProperty().bind(tableProcessus.comparatorProperty());
        tableProcessus.setItems(lignesTriees);

        tfRecherche.textProperty().addListener((obs, ancien, nouveau) -> {
            String filtre = nouveau == null ? "" : nouveau.trim().toLowerCase();
            lignesFiltrees.setPredicate(l -> filtre.isEmpty()
                    || l.getId().toLowerCase().contains(filtre));
        });

        tableProcessus.setRowFactory(t -> new TableRow<LigneProcessus>() {
            @Override
            protected void updateItem(LigneProcessus ligne, boolean vide) {
                super.updateItem(ligne, vide);
                getStyleClass().remove(STYLE_NON_EXECUTE);
                if (!vide && ligne != null && !ligne.estExecute()) {
                    getStyleClass().add(STYLE_NON_EXECUTE);
                }
            }
        });
    }

    /** Affiche le libelle de l'algorithme et recharge le tableau au changement. */
    private void configurerListeAlgorithmes() {
        cbAlgorithme.setConverter(new StringConverter<Algorithme>() {
            @Override
            public String toString(Algorithme algorithme) {
                return algorithme == null ? "" : algorithme.getLibelle();
            }

            @Override
            public Algorithme fromString(String texte) {
                return null;
            }
        });
        cbAlgorithme.valueProperty().addListener((obs, ancien, nouveau) -> rafraichirTableau());
    }

    // ------------------------------------------------------------------
    // Mise a jour de l'affichage
    // ------------------------------------------------------------------

    /**
     * Met a jour la liste des algorithmes puis le tableau.
     *
     * @param resultats les resultats de la derniere simulation (peut etre null)
     */
    private void afficher(ResultatsSimulation resultats) {
        Algorithme choisi = cbAlgorithme.getValue();
        if (resultats == null) {
            cbAlgorithme.getItems().clear();
        } else {
            List<Algorithme> algorithmes = resultats.getAlgorithmes();
            cbAlgorithme.getItems().setAll(algorithmes);
            if (choisi == null || !algorithmes.contains(choisi)) {
                choisi = algorithmes.isEmpty() ? null : algorithmes.get(0);
            }
            cbAlgorithme.setValue(choisi);
        }
        rafraichirTableau();
    }

    /** Reconstruit les lignes du tableau pour l'algorithme choisi. */
    private void rafraichirTableau() {
        lignes.clear();
        ResultatsSimulation simulation = EtatApplication.getResultats();
        Algorithme algorithme = cbAlgorithme.getValue();
        if (simulation == null || algorithme == null) {
            lblResume.setText("Aucune simulation : lancez-en une depuis l'onglet Configuration.");
            return;
        }

        ResultatAlgorithme resultat = simulation.getResultat(algorithme);
        int executes = 0;
        for (Processus p : simulation.getProcessus()) {
            LigneProcessus ligne = new LigneProcessus(p,
                    resultat == null ? null : resultat.getResultat(p.getId()));
            if (ligne.estExecute()) {
                executes++;
            }
            lignes.add(ligne);
        }

        int total = lignes.size();
        int nonExecutes = total - executes;
        String texte = total + " processus : " + executes + " exécuté(s)";
        if (nonExecutes > 0) {
            texte += ", " + nonExecutes + " jamais exécuté(s) (lignes en rouge : RAM demandée"
                    + " supérieure à la RAM totale)";
        }
        lblResume.setText(texte + ".");
    }
}
