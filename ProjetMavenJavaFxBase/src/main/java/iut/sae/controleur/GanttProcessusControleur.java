package iut.sae.controleur;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iut.sae.modele.Algorithme;
import iut.sae.modele.Allocation;
import iut.sae.modele.EtatApplication;
import iut.sae.modele.Processus;
import iut.sae.modele.ResultatAlgorithme;
import iut.sae.modele.ResultatProcessus;
import iut.sae.modele.ResultatsSimulation;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

/**
 * Controleur de l'ecran "Gantt par processus" (vue GanttProcessus.fxml).
 *
 * <p>Une ligne par processus, l'axe horizontal est le temps. Chaque
 * allocation est un rectangle colore selon le processeur utilise. Le
 * diagramme est redessine a chaque nouvelle simulation (ecoute de
 * {@link EtatApplication#resultatsProperty()}) et a chaque changement
 * d'algorithme.</p>
 */
public class GanttProcessusControleur {

    // ------------------------------------------------------------------
    // Constantes de dessin
    // ------------------------------------------------------------------

    /** Largeur de la colonne des noms de processus (pixels). */
    private static final double MARGE_GAUCHE = 80;

    /** Hauteur reservee a l'axe du temps (pixels). */
    private static final double MARGE_HAUT = 30;

    /** Marge a droite et en bas (pixels). */
    private static final double MARGE_BAS_DROITE = 20;

    /** Largeur d'une unite de temps (pixels). */
    private static final double LARGEUR_UNITE = 24;

    /** Hauteur d'une ligne de processus (pixels). */
    private static final double HAUTEUR_LIGNE = 34;

    /** Hauteur d'un rectangle d'allocation (pixels). */
    private static final double HAUTEUR_BARRE = 20;

    /** Hauteur de la barre grise d'attente (pixels). */
    private static final double HAUTEUR_ATTENTE = 4;

    /** Nombre maximal d'etiquettes sur l'axe du temps. */
    private static final int NB_GRADUATIONS_MAX = 40;

    /** Couleurs des processeurs (reprises dans l'ordre, en boucle). */
    private static final String[] PALETTE = {
        "#2563EB", "#16A34A", "#EA580C", "#9333EA", "#0891B2",
        "#CA8A04", "#DB2777", "#4B5563", "#65A30D", "#DC2626"
    };

    private static final Color COULEUR_GRILLE = Color.web("#E5E7EB");
    private static final Color COULEUR_LIGNE_PAIRE = Color.web("#F9FAFB");
    private static final Color COULEUR_ATTENTE = Color.web("#D1D5DB");
    private static final Color COULEUR_BUTOIR = Color.web("#DC2626");
    private static final Color COULEUR_SOUMISSION = Color.web("#111827");

    // ------------------------------------------------------------------
    // Composants de la vue
    // ------------------------------------------------------------------

    @FXML private ComboBox<Algorithme> cbAlgorithme;
    @FXML private Label lblInfo;
    @FXML private Pane paneGantt;
    @FXML private VBox legende;

    // ------------------------------------------------------------------
    // Initialisation
    // ------------------------------------------------------------------

    /** Appelee apres le chargement du FXML. */
    @FXML
    private void initialize() {
        cbAlgorithme.setConverter(new StringConverter<>() {
            @Override
            public String toString(Algorithme a) {
                return a == null ? "" : a.getLibelle();
            }

            @Override
            public Algorithme fromString(String texte) {
                return null;
            }
        });
        cbAlgorithme.valueProperty().addListener((obs, ancien, nouveau) -> dessiner());
        EtatApplication.resultatsProperty().addListener((obs, ancien, nouveau) -> mettreAJour(nouveau));
        mettreAJour(EtatApplication.getResultats());
    }

    /**
     * Recharge la liste des algorithmes puis redessine.
     *
     * @param resultats les nouveaux resultats (peut etre null)
     */
    private void mettreAJour(ResultatsSimulation resultats) {
        Algorithme choisi = cbAlgorithme.getValue();
        if (resultats == null) {
            cbAlgorithme.getItems().clear();
        } else {
            cbAlgorithme.getItems().setAll(resultats.getAlgorithmes());
            if (choisi != null && cbAlgorithme.getItems().contains(choisi)) {
                cbAlgorithme.setValue(choisi);
            } else if (!cbAlgorithme.getItems().isEmpty()) {
                cbAlgorithme.setValue(cbAlgorithme.getItems().get(0));
            }
        }
        dessiner();
    }

    // ------------------------------------------------------------------
    // Dessin
    // ------------------------------------------------------------------

    /** Dessine le diagramme de l'algorithme selectionne. */
    private void dessiner() {
        paneGantt.getChildren().clear();
        legende.getChildren().clear();

        ResultatsSimulation resultats = EtatApplication.getResultats();
        Algorithme algorithme = cbAlgorithme.getValue();
        if (resultats == null || algorithme == null || resultats.getResultat(algorithme) == null) {
            lblInfo.setText("");
            Text vide = new Text(16, 28, "Aucune simulation : lancez-en une depuis l'onglet Configuration.");
            paneGantt.getChildren().add(vide);
            paneGantt.setPrefSize(600, 60);
            return;
        }

        ResultatAlgorithme ra = resultats.getResultat(algorithme);
        List<Processus> processus = resultats.getProcessus();
        List<String> processeurs = resultats.getProcesseurs();

        int horizon = Math.max(ra.getDateFinMax(), 1);
        double largeur = MARGE_GAUCHE + horizon * LARGEUR_UNITE + MARGE_BAS_DROITE;
        double hauteur = MARGE_HAUT + processus.size() * HAUTEUR_LIGNE + MARGE_BAS_DROITE;
        paneGantt.setPrefSize(largeur, hauteur);

        lblInfo.setText(processus.size() + " processus, fin de la derniere execution : " + horizon);

        dessinerFonds(processus.size(), horizon, largeur);
        dessinerAxe(horizon, hauteur);

        Map<String, List<Allocation>> parProcessus = new HashMap<>();
        for (Allocation a : ra.getAllocations()) {
            parProcessus.computeIfAbsent(a.getIdProcessus(), k -> new ArrayList<>()).add(a);
        }

        for (int i = 0; i < processus.size(); i++) {
            dessinerLigne(i, processus.get(i), ra, parProcessus, processeurs, horizon);
        }
        remplirLegende(processeurs);
    }

    /**
     * Dessine les bandes alternees des lignes.
     *
     * @param nbLignes nombre de processus
     * @param horizon  nombre d'unites de temps affichees
     * @param largeur  largeur totale du dessin
     */
    private void dessinerFonds(int nbLignes, int horizon, double largeur) {
        for (int i = 0; i < nbLignes; i++) {
            if (i % 2 == 0) {
                Rectangle bande = new Rectangle(0, MARGE_HAUT + i * HAUTEUR_LIGNE,
                        largeur, HAUTEUR_LIGNE);
                bande.setFill(COULEUR_LIGNE_PAIRE);
                paneGantt.getChildren().add(bande);
            }
        }
    }

    /**
     * Dessine la grille verticale et les graduations du temps.
     *
     * @param horizon nombre d'unites de temps
     * @param hauteur hauteur totale du dessin
     */
    private void dessinerAxe(int horizon, double hauteur) {
        int pas = Math.max(1, (int) Math.ceil((double) horizon / NB_GRADUATIONS_MAX));
        for (int t = 0; t <= horizon; t += pas) {
            double x = MARGE_GAUCHE + t * LARGEUR_UNITE;
            Line trait = new Line(x, MARGE_HAUT - 4, x, hauteur - MARGE_BAS_DROITE + 4);
            trait.setStroke(COULEUR_GRILLE);
            Text etiquette = new Text(x - 4, MARGE_HAUT - 8, String.valueOf(t));
            paneGantt.getChildren().addAll(trait, etiquette);
        }
    }

    /**
     * Dessine la ligne d'un processus.
     *
     * @param index        numero de la ligne
     * @param p            le processus
     * @param ra           resultats de l'algorithme
     * @param parProcessus allocations regroupees par processus
     * @param processeurs  liste triee des processeurs (pour les couleurs)
     * @param horizon      nombre d'unites de temps affichees
     */
    private void dessinerLigne(int index, Processus p, ResultatAlgorithme ra,
                               Map<String, List<Allocation>> parProcessus,
                               List<String> processeurs, int horizon) {
        double y = MARGE_HAUT + index * HAUTEUR_LIGNE;
        double milieu = y + HAUTEUR_LIGNE / 2;

        Text nom = new Text(8, milieu + 4, p.getId());
        paneGantt.getChildren().add(nom);

        ResultatProcessus rp = ra.getResultat(p.getId());

        // Barre d'attente : de la soumission a la fin
        if (rp != null && rp.estExecute()) {
            double x1 = xDe(p.getDateSoumission());
            double x2 = xDe(rp.getDateFin());
            Rectangle attente = new Rectangle(x1, milieu - HAUTEUR_ATTENTE / 2,
                    Math.max(0, x2 - x1), HAUTEUR_ATTENTE);
            attente.setFill(COULEUR_ATTENTE);
            paneGantt.getChildren().add(attente);
        }

        // Rectangles d'execution
        for (Allocation a : parProcessus.getOrDefault(p.getId(), List.of())) {
            Rectangle r = new Rectangle(xDe(a.getDateDebut()), milieu - HAUTEUR_BARRE / 2,
                    a.getDuree() * LARGEUR_UNITE, HAUTEUR_BARRE);
            r.setFill(couleurDe(a.getIdProcesseur(), processeurs));
            r.setStroke(Color.WHITE);
            r.setArcWidth(4);
            r.setArcHeight(4);
            Tooltip.install(r, new Tooltip(p.getId() + " sur " + a.getIdProcesseur()
                    + "\nde " + a.getDateDebut() + " a " + a.getDateFin()
                    + " (duree " + a.getDuree() + ")"));
            paneGantt.getChildren().add(r);
        }

        // Date butoir (trait rouge) si elle est dans la zone affichee
        if (p.getDateButoir() >= 0 && p.getDateButoir() <= horizon) {
            double xb = xDe(p.getDateButoir());
            Line butoir = new Line(xb, y + 3, xb, y + HAUTEUR_LIGNE - 3);
            butoir.setStroke(COULEUR_BUTOIR);
            butoir.setStrokeWidth(2);
            Tooltip.install(butoir, new Tooltip("Date butoir de " + p.getId() + " : " + p.getDateButoir()));
            paneGantt.getChildren().add(butoir);
        }

        // Date de soumission (petit triangle)
        double xs = xDe(p.getDateSoumission());
        Polygon triangle = new Polygon(xs, y + HAUTEUR_LIGNE - 2,
                xs - 4, y + HAUTEUR_LIGNE - 9, xs + 4, y + HAUTEUR_LIGNE - 9);
        triangle.setFill(COULEUR_SOUMISSION);
        Tooltip.install(triangle, new Tooltip("Soumission de " + p.getId() + " : " + p.getDateSoumission()));
        paneGantt.getChildren().add(triangle);

        // Processus jamais execute
        if (rp != null && !rp.estExecute()) {
            Text info = new Text(xs + 8, milieu + 4,
                    "non execute (RAM demandee : " + rp.getRamDemandee() + ")");
            info.setFill(COULEUR_BUTOIR);
            paneGantt.getChildren().add(info);
        }
    }

    /**
     * Remplit la legende sous le diagramme.
     *
     * @param processeurs liste des processeurs
     */
    private void remplirLegende(List<String> processeurs) {
        // --- Groupe 1 : processeurs ---
        Label titreProcesseurs = new Label("Couleur = processeur qui exécute le processus");
        titreProcesseurs.getStyleClass().add("legende-titre");

        FlowPane listeProcesseurs = new FlowPane(20, 6);
        for (String id : processeurs) {
            Rectangle carre = new Rectangle(20, 14, couleurDe(id, processeurs));
            carre.setArcWidth(4);
            carre.setArcHeight(4);
            listeProcesseurs.getChildren().add(element(carre, id));
        }

        // --- Groupe 2 : symboles ---
        Label titreSymboles = new Label("Symboles");
        titreSymboles.getStyleClass().add("legende-titre");

        FlowPane listeSymboles = new FlowPane(28, 6);
        listeSymboles.getChildren().addAll(
                element(iconeExecution(), "Rectangle : période d'exécution (survolez-le pour les détails)"),
                element(iconeAttente(), "Barre grise : attente, de la soumission à la fin"),
                element(iconeSoumission(), "Date de soumission"),
                element(iconeButoir(), "Date butoir (deadline)"),
                element(iconeNonExecute(), "Processus jamais exécuté (RAM insuffisante)"));

        legende.getChildren().addAll(titreProcesseurs, listeProcesseurs,
                titreSymboles, listeSymboles);
    }

    /**
     * Associe une icone et un texte.
     *
     * @param icone le dessin
     * @param texte l'explication
     * @return l'element de legende
     */
    private HBox element(Node icone, String texte) {
        Label label = new Label(texte);
        label.getStyleClass().add("legende-texte");
        HBox element = new HBox(8, icone, label);
        element.setAlignment(Pos.CENTER_LEFT);
        return element;
    }

    /** @return l'icone d'une periode d'execution */
    private Node iconeExecution() {
        Rectangle r = new Rectangle(24, 14, Color.web(PALETTE[0]));
        r.setArcWidth(4);
        r.setArcHeight(4);
        return r;
    }

    /** @return l'icone de la barre d'attente */
    private Node iconeAttente() {
        Rectangle r = new Rectangle(24, 4, COULEUR_ATTENTE);
        StackPane zone = new StackPane(r);
        zone.setMinSize(24, 14);
        zone.setPrefSize(24, 14);
        return zone;
    }

    /** @return l'icone de la date de soumission (triangle) */
    private Node iconeSoumission() {
        Polygon triangle = new Polygon(6, 0, 0, 10, 12, 10);
        triangle.setFill(COULEUR_SOUMISSION);
        StackPane zone = new StackPane(triangle);
        zone.setMinSize(14, 14);
        return zone;
    }

    /** @return l'icone de la date butoir (trait rouge) */
    private Node iconeButoir() {
        Line trait = new Line(0, 0, 0, 16);
        trait.setStroke(COULEUR_BUTOIR);
        trait.setStrokeWidth(2);
        StackPane zone = new StackPane(trait);
        zone.setMinSize(14, 16);
        return zone;
    }

    /** @return l'icone d'un processus non execute */
    private Node iconeNonExecute() {
        Label l = new Label("non exécuté");
        l.setTextFill(COULEUR_BUTOIR);
        l.setStyle("-fx-font-size: 11px; -fx-font-style: italic;");
        return l;
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    /**
     * @param instant un instant
     * @return sa position horizontale dans le dessin
     */
    private double xDe(int instant) {
        return MARGE_GAUCHE + instant * LARGEUR_UNITE;
    }

    /**
     * @param processeur  identifiant du processeur
     * @param processeurs liste triee des processeurs
     * @return la couleur associee
     */
    private Color couleurDe(String processeur, List<String> processeurs) {
        int index = Math.max(0, processeurs.indexOf(processeur));
        return Color.web(PALETTE[index % PALETTE.length]);
    }
}