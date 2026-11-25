package iut.sae.controleur;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import iut.sae.modele.Algorithme;
import iut.sae.modele.Allocation;
import iut.sae.modele.EtatApplication;
import iut.sae.modele.Metrique;
import iut.sae.modele.Processus;
import iut.sae.modele.ResultatAlgorithme;
import iut.sae.modele.ResultatsSimulation;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * TabGanttProcesseursControleur
 * 
 * @Author Alexis Sudre Brito
 * Niveau de Vibe-Coding assez consequant...
 * 
 * @Version 1.8
 */
public class TabGanttProcesseursControleur implements Initializable {

    private static final List<String> PALETTE_COULEURS = List.of(
            "#4E79A7", "#F28E2B", "#E15759", "#76B7B2",
            "#59A14F", "#EDC948", "#B07AA1", "#FF9DA7",
            "#9C755F", "#BAB0AC"
    );

    private static final double LARGEUR_COLONNE_CPU = 70.0;
    private static final double LARGEUR_CASE = 28.0;
    private static final double HAUTEUR_CASE = 26.0;
    private static final double LARGEUR_SEPARATEUR = LARGEUR_CASE/0.8;
    private static final double ECART_VERTICAL_LIGNES = 6.0;
    private static final double HAUTEUR_BARRE_TEMPS = 20.0;

    private ResultatsSimulation resultatsLancement;
    private final Map<String, String> couleursParProcessus = new LinkedHashMap<>();
    private final Map<String, Processus> processusParId = new LinkedHashMap<>();

    private final Label lblEtatVide = new Label("Aucun résultat n'a été obtenu...");
    private final FlowPane zoneLegende = new FlowPane();

    @FXML
    private ScrollPane scrollPaneRacine;

    @FXML
    private VBox boiteConteneurElements;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.scrollPaneRacine.setFitToWidth(true);
        this.scrollPaneRacine.setFitToHeight(true);
        this.scrollPaneRacine.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        this.boiteConteneurElements.setMaxWidth(Double.MAX_VALUE);
        this.boiteConteneurElements.setFillWidth(true);

        //Listener temporaire (esperons)
        EtatApplication.resultatsProperty().addListener((obs, ancien, nouveau) ->
                lancerAffichageParProcesseurs(nouveau));
        lancerAffichageParProcesseurs(EtatApplication.getResultats());
    }

    /**
     * Fonction "principale" du travail
     * Devrait se faire lancer par le controleur de la page principale
     * Cela pour chaque simulation de lancee
     * @param resultats resultats de la derniere simulation
     */
    public void lancerAffichageParProcesseurs(ResultatsSimulation resultats) {
        this.resultatsLancement = resultats;

        this.boiteConteneurElements.getChildren().clear();
        zoneLegende.getChildren().clear();
        couleursParProcessus.clear();
        processusParId.clear();

        if (resultats == null || resultats.getAlgorithmes().isEmpty()) {
            afficherEtatVide();
        }
        else{
            //Association de chaque processus a une couleur
            indexerProcessusEtCouleurs();
            //Affichage de tous les diagrammes
            afficherDiagrammes();
            //Affichage de la legende a la fin
            afficherLegende();
        }
    }

    /**
     * Affiche l'hypothetique etat vide
     */
    private void afficherEtatVide() {
        lblEtatVide.setMaxWidth(Double.MAX_VALUE);
        lblEtatVide.setAlignment(Pos.CENTER);
        VBox.setVgrow(lblEtatVide, Priority.ALWAYS);
        this.boiteConteneurElements.getChildren().add(lblEtatVide);
    }

    /**
     * Association entre les processus et leurs couleurs
     */
    private void indexerProcessusEtCouleurs() {
        int index = 0;
        if (this.resultatsLancement.getProcessus() != null) {
            for (Processus p : this.resultatsLancement.getProcessus()) {
                processusParId.put(p.getId(), p);
                if (!couleursParProcessus.containsKey(p.getId())) {
                    couleursParProcessus.put(p.getId(), PALETTE_COULEURS.get(index % PALETTE_COULEURS.size()));
                    index++;
                }
            }
        }
        for (Algorithme algo : this.resultatsLancement.getAlgorithmes()) {
            ResultatAlgorithme r = this.resultatsLancement.getResultat(algo);
            if (r == null) {
                continue;
            }
            for (Allocation alloc : r.getAllocations()) {
                String idProc = alloc.getIdProcessus();
                if (!couleursParProcessus.containsKey(idProc)) {
                    couleursParProcessus.put(idProc, PALETTE_COULEURS.get(index % PALETTE_COULEURS.size()));
                    index++;
                }
            }
        }
    }

    /**
     * Construit et affiches les diagrammes
     * dans la VBox principale
     */
    private void afficherDiagrammes() {
        List<String> processeurs = this.resultatsLancement.getProcesseurs();
        int nbProcs = processeurs.size();
        double hauteurGrille = (nbProcs * HAUTEUR_CASE)
                + (Math.max(0, nbProcs - 1) * ECART_VERTICAL_LIGNES);

        for (Algorithme algo : this.resultatsLancement.getAlgorithmes()) {
            ResultatAlgorithme resultatAlgo = this.resultatsLancement.getResultat(algo);
            if (resultatAlgo == null) {
                continue;
            }

            int nbPas = retrouverNombrePas(algo, resultatAlgo);
            Map<String, Map<Integer, String>> occupation = indexerAllocations(resultatAlgo.getAllocations());

            VBox blocAlgo = new VBox(8);
            blocAlgo.setPadding(new Insets(10, 14, 10, 14));
            blocAlgo.setMaxWidth(Double.MAX_VALUE);
            blocAlgo.setFillWidth(true);
            blocAlgo.setAlignment(Pos.CENTER);

            Label titreAlgo = new Label(algo.getLibelle());
            titreAlgo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            //VBox intermediare (nous voulons des espacements differents
            //entre les diagrammes que entre les titres et leur diagramme)
            VBox diagrammeAssemble = new VBox(
                    3,
                    construireBarreTemps(nbPas),
                    construireGrilleAvecBornes(processeurs, nbPas, occupation, hauteurGrille)
            );
            diagrammeAssemble.setMinWidth(Region.USE_PREF_SIZE);
            diagrammeAssemble.setMaxWidth(Region.USE_PREF_SIZE);

            VBox zoneDiagramme = new VBox(diagrammeAssemble);
            zoneDiagramme.setAlignment(Pos.CENTER);
            zoneDiagramme.setPadding(new Insets(4, 14, 6, 14));
            zoneDiagramme.setMinWidth(Region.USE_PREF_SIZE);

            ScrollPane scrollDiagramme = new ScrollPane(zoneDiagramme);
            scrollDiagramme.setFitToWidth(true);
            scrollDiagramme.setFitToHeight(false);
            scrollDiagramme.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollDiagramme.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollDiagramme.setMaxWidth(Double.MAX_VALUE);
            scrollDiagramme.setMinHeight(Region.USE_PREF_SIZE);
            scrollDiagramme.setPrefHeight(HAUTEUR_BARRE_TEMPS + hauteurGrille + 24.0);
            scrollDiagramme.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

            blocAlgo.getChildren().addAll(titreAlgo, scrollDiagramme);
            this.boiteConteneurElements.getChildren().add(blocAlgo);
        }
    }

    /**
     * Permet la construction de chaque barre
     * indiquant une etape de temps
     * @param nbEtapes nombre d'etapes de temps
     * @return HBox contenant toutes les etapes de temps 
     */
    private HBox construireBarreTemps(int nbEtapes) {
        HBox barreEtapes = new HBox(0);
        barreEtapes.setAlignment(Pos.CENTER_LEFT);
        barreEtapes.setPrefHeight(HAUTEUR_BARRE_TEMPS);

        //Permet l'espacement necessaire pour aligner les etapes de temps
        //Avec la moitie de chaque espaces entre les colonnes
        Region espaceGauche = new Region();
        espaceGauche.setMinWidth(LARGEUR_COLONNE_CPU);
        espaceGauche.setPrefWidth(LARGEUR_COLONNE_CPU);
        barreEtapes.getChildren().add(espaceGauche);

        //Decalement entre les etapes de temps
        double pasColonne = LARGEUR_CASE + LARGEUR_SEPARATEUR;
        double decalageVersEcart = -((LARGEUR_CASE / 2.0) + LARGEUR_SEPARATEUR);

        for (int t = 0; t <= nbEtapes; t++) {
            Label lblTemps = new Label("" + t);
            lblTemps.setMinWidth(pasColonne);
            lblTemps.setPrefWidth(pasColonne);
            lblTemps.setAlignment(Pos.CENTER);
            lblTemps.setTranslateX(t == 0 ? -(pasColonne / 2.0) : decalageVersEcart);
            lblTemps.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #444444;");
            barreEtapes.getChildren().add(lblTemps);
        }

        return barreEtapes;
    }

    /**
     * Cree un GridPane, avec une barre entre chaque colonne
     * Est mise dans un StackPane, afin de pouvoir placer les barres indiquant les dates de soumissions, ou butoirs
     * @param processeurs liste des identifiants de chaque processeur (aplacer tout a gauche)
     * @param nbPas nombre d'etape de temps qu'a pris l'algorithme (Makespan)
     * @param occupation representation de toutes les allocations par processeurs (@see indexerAllocation())
     * @param largeurGrille largeur que doit avoir la grille
     * @return StackPane contenant la grilleS
     */
    private StackPane construireGrilleAvecBornes(List<String> processeurs, int nbPas,
                                               Map<String, Map<Integer, String>> occupation,
                                               double largeurGrille) {
        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(ECART_VERTICAL_LIGNES);
        grid.setAlignment(Pos.CENTER_LEFT);

        grid.getColumnConstraints().add(new ColumnConstraints(LARGEUR_COLONNE_CPU));
        for (int t = 0; t < nbPas; t++) {
            grid.getColumnConstraints().add(new ColumnConstraints(LARGEUR_CASE));
            if (t < nbPas - 1) {
                grid.getColumnConstraints().add(new ColumnConstraints(LARGEUR_SEPARATEUR));
            }
        }

        int nbProcs = processeurs.size();
        for (int row = 0; row < nbProcs; row++) {
            String idProc = processeurs.get(row);
            Label lblProc = new Label(idProc);
            lblProc.setMinWidth(LARGEUR_COLONNE_CPU);
            lblProc.setPrefWidth(LARGEUR_COLONNE_CPU);
            lblProc.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");
            grid.add(lblProc, 0, row);

            Map<Integer, String> tempsVersProcessus = occupation.getOrDefault(idProc, Map.of());
            for (int t = 0; t < nbPas; t++) {
                grid.add(creerCase(tempsVersProcessus.get(t), t), 1 + (t * 2), row);
            }
        }

        if (nbProcs > 0) {
            for (int t = 0; t < nbPas - 1; t++) {
                Line ligne = new Line(0, 0, 0, largeurGrille);
                ligne.setStroke(Color.web("#CCCCCC"));
                ligne.setStrokeWidth(1.0);

                StackPane conteneurLigne = new StackPane(ligne);
                conteneurLigne.setMinWidth(LARGEUR_SEPARATEUR);
                conteneurLigne.setPrefWidth(LARGEUR_SEPARATEUR);
                conteneurLigne.setAlignment(Pos.CENTER);
                grid.add(conteneurLigne, 2 + (t * 2), 0);
                GridPane.setRowSpan(conteneurLigne, nbProcs);
            }
        }

        StackPane pile = new StackPane(grid, construireCalqueBornes(nbPas, largeurGrille));
        pile.setAlignment(Pos.TOP_LEFT);
        pile.setMinWidth(Region.USE_PREF_SIZE);
        pile.setMaxWidth(Region.USE_PREF_SIZE);
        return pile;
    }

    /**
     * Permet de placer les bornes correspondant aux dates de soumissions
     * et butoirs des processus
     * @param nbEtapes nombre d'etapes de la simulation
     * @param hauteurGrille longueur que doit avoir la barre
     * @return
     */
    private Pane construireCalqueBornes(int nbEtapes, double hauteurGrille) {
        Pane calque = new Pane();
        calque.setPickOnBounds(false);
        calque.setMouseTransparent(true);

        if (hauteurGrille <= 0) {
            return calque;
        }

        Map<Integer, List<Line>> traitsParInstant = new HashMap<>();
        for (Processus p : processusParId.values()) {
            Color couleur = Color.web(couleursParProcessus.getOrDefault(p.getId(), "#4E79A7"));

            int soumission = p.getDateSoumission();
            if (soumission >= 0 && soumission <= nbEtapes) {
                Line trait = new Line(0, 0, 0, hauteurGrille);
                trait.setStroke(couleur);
                trait.setStrokeWidth(2.0);
                traitsParInstant.computeIfAbsent(soumission, k -> new ArrayList<>()).add(trait);
            }

            int butoir = p.getDateButoir();
            if (butoir >= 0 && butoir <= nbEtapes) {
                Line trait = new Line(0, 0, 0, hauteurGrille);
                trait.setStroke(couleur);
                trait.setStrokeWidth(2.0);
                trait.getStrokeDashArray().addAll(5.0, 3.0);
                traitsParInstant.computeIfAbsent(butoir, k -> new ArrayList<>()).add(trait);
            }
        }

        double pasColonne = LARGEUR_CASE + LARGEUR_SEPARATEUR;
        for (Map.Entry<Integer, List<Line>> entry : traitsParInstant.entrySet()) {
            int t = entry.getKey();
            List<Line> traits = entry.getValue();

            double xCentre = (t == 0)
                    ? LARGEUR_COLONNE_CPU - 2.0
                    : LARGEUR_COLONNE_CPU + (t * pasColonne) - (LARGEUR_SEPARATEUR / 2.0);

            int n = traits.size();
            for (int i = 0; i < n; i++) {
                double decalage = (i - (n - 1) / 2.0) * 2.5;
                Line trait = traits.get(i);
                trait.setStartX(xCentre + decalage);
                trait.setEndX(xCentre + decalage);
                calque.getChildren().add(trait);
            }
        }
        return calque;
    }

    /**
     * Remplie une case de la couleur correspondant a un processus,
     * si l'etape de temps concorde a un instant d'allocation pour celui-ci
     * @param idProcessusAlloue identifiant du processus
     * @param pasTemps etape de temps testee
     * @return Pane correspondant a l'apparence d'une case sur le diagramme
     */
    private Pane creerCase(String idProcessusAlloue, int pasTemps) {
        Pane cellule = new Pane();
        cellule.setPrefSize(LARGEUR_CASE, HAUTEUR_CASE);
        cellule.setMinSize(LARGEUR_CASE, HAUTEUR_CASE);

        if (idProcessusAlloue != null) {
            String couleurHex = couleursParProcessus.getOrDefault(idProcessusAlloue, "#4E79A7");
            cellule.setStyle(
                    "-fx-background-color: " + couleurHex + ";" +
                    "-fx-background-radius: 3;" +
                    "-fx-border-color: rgba(0,0,0,0.25);" +
                    "-fx-border-radius: 3;"
            );
        } else {
            cellule.setStyle(
                    "-fx-background-color: rgba(0,0,0,0.03);" +
                    "-fx-border-color: rgba(0,0,0,0.08);" +
                    "-fx-border-radius: 3;"
            );
        }
        return cellule;
    }

    /**
     * Permet de construire la legende, ainsi que
     * de la placer dans la placer dans la VBox principale
     */
    private void afficherLegende() {
        String texte = "";
        zoneLegende.setHgap(18);
        zoneLegende.setVgap(8);
        zoneLegende.setPadding(new Insets(10, 14, 10, 14));
        zoneLegende.setMaxWidth(Double.MAX_VALUE);
        zoneLegende.setStyle("-fx-background-color: #EAEAEA; -fx-background-radius: 4;");

        for (Map.Entry<String, String> entry : couleursParProcessus.entrySet()) {
            String idProc = entry.getKey();
            Region carre = new Region();
            carre.setPrefSize(14, 14);
            carre.setStyle(
                    "-fx-background-color: " + entry.getValue() + ";" +
                    "-fx-background-radius: 2;" +
                    "-fx-border-color: #333333;"
            );

            Processus p = processusParId.get(idProc);
            if(p != null){
                texte = p.getId();
            }

            Label lblProc = new Label(texte);
            lblProc.setStyle("-fx-font-size: 12px;");

            HBox item = new HBox(6, carre, lblProc);
            item.setAlignment(Pos.CENTER_LEFT);
            zoneLegende.getChildren().add(item);
        }

        boiteConteneurElements.getChildren().add(zoneLegende);
    }

    /**
     * Enregistre chaque allocation dans un objet Map
     * @param allocations liste des allocations enregistrees dans la simulation
     * @return Map dont les cles sont les identifiants des processeurs,
     * Et les valeurs sont des Map (etape de temps -> idProcessus)
     */
    private Map<String, Map<Integer, String>> indexerAllocations(List<Allocation> allocations) {
        Map<String, Map<Integer, String>> map = new HashMap<>();
        for (Allocation alloc : allocations) {
            Map<Integer, String> parTemps =
                    map.computeIfAbsent(alloc.getIdProcesseur(), k -> new HashMap<>());
            for (int t = alloc.getDateDebut(); t < alloc.getDateFin(); t++) {
                parTemps.put(t, alloc.getIdProcessus());
            }
        }
        return map;
    }

    /**
     * Permet de recuperer le Makespan d'une simulatio,
     * @param algo algorithme traite
     * @param resultatAlgo resultats de toute la simulation (ou se trouve le Makespan)
     * @return Makespan concordant avec l'algorithme
     */
    private int retrouverNombrePas(Algorithme algo, ResultatAlgorithme resultatAlgo) {
        for (Metrique m : resultatsLancement.getMetriques()) {
            if (m.getAlgorithme() == algo && m.getMakespan() > 0) {
                return m.getMakespan();
            }
        }
        return resultatAlgo.getDateFinMax();
    }
}