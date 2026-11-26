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
import iut.sae.modele.ResultatProcessus;
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
 * @Version 1.9
 */
public class TabGanttProcesseursControleur implements Initializable {

    //Liste des couleurs associees aux processus
    //Limitees a 10 pour le moment...
    private static final List<String> PALETTE_COULEURS = List.of(
            "#4E79A7", "#F28E2B", "#E15759", "#76B7B2",
            "#59A14F", "#EDC948", "#B07AA1", "#FF9DA7",
            "#9C755F", "#BAB0AC"
    );

    //Longueur de la colonne contenant le nom des processeurs
    private static final double LONGUEUR_COLONNE_DEBUT = 70.0;
    //Longueur d'une case normale
    private static final double LONGUEUR_CASE = 34.0;
    private static final double LARGEUR_CASE = 32.0;
    private static final double ECART_COLONNE = LONGUEUR_CASE / 0.8;
    private static final double ECART_VERTICAL_LIGNES = 6.0;
    private static final double HAUTEUR_BARRE_TEMPS = 20.0;

    //Resultats de la derniere simulation
    private ResultatsSimulation resultatsLancement;
    private final Map<String, String> couleursParProcessus = new LinkedHashMap<>();
    private final Map<String, Processus> processusParId = new LinkedHashMap<>();

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
            Label lblTitrePage = new Label("Dernieres relevés de l'application");
            lblTitrePage.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #140d0d;");
            lblTitrePage.setPadding(new Insets(12, 14, 4, 14));
            lblTitrePage.setAlignment(Pos.TOP_CENTER);

            this.boiteConteneurElements.getChildren().add(lblTitrePage);

            //Association de chaque processus a une couleur
            associationProcessusCouleurs();
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
        Label lblEtatVide = new Label("Aucun résultat n'a été obtenu...");

        lblEtatVide.setMaxWidth(Double.MAX_VALUE);
        lblEtatVide.setAlignment(Pos.CENTER);
        VBox.setVgrow(lblEtatVide, Priority.ALWAYS);

        this.boiteConteneurElements.getChildren().add(lblEtatVide);
    }

    /**
     * Association entre les processus et leurs couleurs
     */
    private void associationProcessusCouleurs() {
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
                    //Si plus de 10 processus, on repette la palette de couleur depuis le debut
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
        double hauteurGrille = (nbProcs * LARGEUR_CASE)
                + (Math.max(0, nbProcs - 1) * ECART_VERTICAL_LIGNES);

        int nbLignesRef = Math.max(nbProcs, 2);
        double hauteurDeuxLignes = (nbLignesRef * LARGEUR_CASE)
                + (Math.max(0, nbLignesRef - 1) * ECART_VERTICAL_LIGNES);
        double hauteurScroll = HAUTEUR_BARRE_TEMPS + hauteurDeuxLignes + 24.0;

        for (Algorithme algo : this.resultatsLancement.getAlgorithmes()) {
            ResultatAlgorithme resultatAlgo = this.resultatsLancement.getResultat(algo);
            if (resultatAlgo == null) {
                continue;
            }

            int makespanAlgo = retrouverNombrePas(algo, resultatAlgo);
            Map<String, Map<Integer, String>> occupation = indexerAllocations(resultatAlgo.getAllocations());

            //VBox intermediare (nous voulons des espacements differents
            //entre les diagrammes que entre les titres et leur diagramme)
            VBox blocAlgo = new VBox(5);
            blocAlgo.setPadding(new Insets(10, 14, 10, 14));
            blocAlgo.setMaxWidth(Double.MAX_VALUE);
            blocAlgo.setFillWidth(true);
            blocAlgo.setAlignment(Pos.CENTER_LEFT);

            Label titreAlgo = new Label(algo.getLibelle());
            titreAlgo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            VBox diagrammeAssemble = new VBox(
                    3,
                    construireBarreTemps(makespanAlgo),
                    construireGrilleAvecBornes(processeurs, makespanAlgo, occupation, hauteurGrille, resultatAlgo)
            );
            diagrammeAssemble.setMinWidth(Region.USE_PREF_SIZE);
            diagrammeAssemble.setMaxWidth(Region.USE_PREF_SIZE);

            VBox zoneDiagramme = new VBox(diagrammeAssemble);
            zoneDiagramme.setAlignment(Pos.CENTER_LEFT);
            zoneDiagramme.setPadding(new Insets(4, 14, 12, 14));
            zoneDiagramme.setMinWidth(Region.USE_PREF_SIZE);

            ScrollPane scrollDiagramme = new ScrollPane(zoneDiagramme);
            scrollDiagramme.setFitToWidth(false);
            scrollDiagramme.setFitToHeight(false);
            scrollDiagramme.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollDiagramme.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollDiagramme.setMaxWidth(Double.MAX_VALUE);
            scrollDiagramme.setMinHeight(hauteurScroll);
            scrollDiagramme.setPrefHeight(hauteurScroll);
            scrollDiagramme.setMaxHeight(hauteurScroll);
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
        espaceGauche.setMinWidth(LONGUEUR_COLONNE_DEBUT);
        espaceGauche.setPrefWidth(LONGUEUR_COLONNE_DEBUT);
        barreEtapes.getChildren().add(espaceGauche);

        //Decalement entre les etapes de temps
        double pasColonne = LONGUEUR_CASE + ECART_COLONNE;
        double decalageVersEcart = -((LONGUEUR_CASE / 2.0) + ECART_COLONNE);

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
     * @param nbTemps nombre d'etape de temps qu'a pris l'algorithme (Makespan)
     * @param occupation representation de toutes les allocations par processeurs (@see indexerAllocation())
     * @param largeurGrille largeur que doit avoir la grille
     * @param resultatAlgo resultats de l'algorithme courant
     * @return StackPane contenant la grilleS
     */
    private StackPane construireGrilleAvecBornes(List<String> processeurs, int nbTemps,
                                               Map<String, Map<Integer, String>> occupation,
                                               double largeurGrille,
                                               ResultatAlgorithme resultatAlgo) {
        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(ECART_VERTICAL_LIGNES);
        grid.setAlignment(Pos.CENTER_LEFT);

        configurerColonnesGrille(grid, nbTemps);
        remplirLignesProcesseurs(grid, processeurs, nbTemps, occupation, resultatAlgo);
        ajouterSeparateursContinus(grid, processeurs.size(), nbTemps, largeurGrille);

        StackPane pile = new StackPane(grid, construireCalqueBornes(nbTemps, largeurGrille));
        pile.setAlignment(Pos.TOP_LEFT);
        pile.setMinWidth(Region.USE_PREF_SIZE);
        pile.setMaxWidth(Region.USE_PREF_SIZE);
        return pile;
    }

    /**
     * Ajoute autant de colonne au diagramme que necessaire
     * @param grid diagramme Gantt par processeur
     * @param nbTemps  nombre d'etape de temps de l'algorithme
     */
    private void configurerColonnesGrille(GridPane grid, int nbTemps) {
        grid.getColumnConstraints().add(new ColumnConstraints(LONGUEUR_COLONNE_DEBUT));
        for (int t = 0; t <= nbTemps; t++) {
            grid.getColumnConstraints().add(new ColumnConstraints(LONGUEUR_CASE));
            grid.getColumnConstraints().add(new ColumnConstraints(ECART_COLONNE));
        }
    }

    /**
     * Configure les lignes du diagramme
     * @param grid diagramme de Gantt par processeur
     * @param processeurs Liste des identifiants pour chaque processeur utilise
     * @param nbTemps nombre d'etape de temps de l'algorithme
     * @param occupation
     * @param resultatAlgo
     */
    private void remplirLignesProcesseurs(GridPane grid, List<String> processeurs, int nbTemps,
                                          Map<String, Map<Integer, String>> occupation,
                                          ResultatAlgorithme resultatAlgo) {
        int nbProcs = processeurs.size();
        for (int row = 0; row < nbProcs; row++) {
            String idProc = processeurs.get(row);
            Label lblProc = new Label(idProc);
            lblProc.setMinWidth(LONGUEUR_COLONNE_DEBUT);
            lblProc.setPrefWidth(LONGUEUR_COLONNE_DEBUT);
            lblProc.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");
            grid.add(lblProc, 0, row);

            Map<Integer, String> tempsVersProcessus = occupation.getOrDefault(idProc, Map.of());
            for (int t = 0; t < nbTemps; t++) {
                String idAlloue = tempsVersProcessus.get(t);
                grid.add(creerCase(idAlloue, estDerniereAllocation(resultatAlgo, idAlloue, t)), 1 + (t * 2), row);
            }
        }
    }

    /**
     * Verifie quand est-ce qu'un processus est traite pour la derniere fois
     * @param resultatAlgo resultats de l'algorithme
     * @param idAlloue identifiant du processus traite
     * @param tempsFin date de fin du processus
     * @return booleen en fonction de si l'etape traite par le processeur est bien la derniere allocation du processus
     */
    private boolean estDerniereAllocation(ResultatAlgorithme resultatAlgo, String idAlloue, int tempsFin) {
        if (idAlloue == null || resultatAlgo == null) {
            return false;
        }
        ResultatProcessus resProc = resultatAlgo.getResultat(idAlloue);
        return resProc != null && resProc.estExecute() && tempsFin == resProc.getDateFin() - 1;
    }

    /**
     * Ajoute des lignes grises afin d'etablir les etapes de temps dans le diagramme
     * Ajuste la longueur de ces lignes en fonction du nombre de ligne du diagramme
     * @param grid diagramme Gantt par processeur
     * @param nbProcesseurs nombre de processeur au total
     * @param nbTemps nombre d'etapes de temps total de l'algorithme
     * @param largeurGrille largeur (ou "hauteur") du diagramme
     */
    private void ajouterSeparateursContinus(GridPane grid, int nbProcesseurs, int nbTemps, double largeurGrille) {
        if (nbProcesseurs <= 0) {
            return;
        }
        for (int t = 0; t < nbTemps - 1; t++) {
            Line ligne = new Line(0, 0, 0, largeurGrille);
            ligne.setStroke(Color.web("#CCCCCC"));
            ligne.setStrokeWidth(1.0);

            StackPane conteneurLigne = new StackPane(ligne);
            conteneurLigne.setMinWidth(ECART_COLONNE);
            conteneurLigne.setPrefWidth(ECART_COLONNE);
            conteneurLigne.setAlignment(Pos.CENTER);
            grid.add(conteneurLigne, 2 + (t * 2), 0);
            GridPane.setRowSpan(conteneurLigne, nbProcesseurs);
        }
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
            Color couleur = Color.web(couleursParProcessus.getOrDefault(p.getId(), PALETTE_COULEURS.get(0)));

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

        double pasColonne = LONGUEUR_CASE + ECART_COLONNE;
        for (Map.Entry<Integer, List<Line>> entry : traitsParInstant.entrySet()) {
            int t = entry.getKey();
            List<Line> traits = entry.getValue();

            double xCentre = (t == 0)
                    ? LONGUEUR_COLONNE_DEBUT - 2.0
                    : LONGUEUR_COLONNE_DEBUT + (t * pasColonne) - (ECART_COLONNE / 2.0);

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
     * @param estDerniereAllocation vrai si c'est le dernier pas de temps ou ce processus est alloue
     * @return Pane correspondant a l'apparence d'une case sur le diagramme
     */
    private Pane creerCase(String idProcessusAlloue, boolean estDerniereAllocation) {
        Pane cellule = new Pane();
        cellule.setPrefSize(LONGUEUR_CASE, LARGEUR_CASE);
        cellule.setMinSize(LONGUEUR_CASE, LARGEUR_CASE);

        if (idProcessusAlloue != null) {
            String couleurHex = couleursParProcessus.getOrDefault(idProcessusAlloue, PALETTE_COULEURS.get(0));
            if (estDerniereAllocation) {
                affecterStyleCase(cellule, couleurHex, "#111111", 2.5, 3, 3);
            } else {
                affecterStyleCase(cellule, couleurHex, "rgba(0,0,0,25)", 1, 3, 3);
            }
        } else {
            affecterStyleCase(cellule, "rgba(0,0,0,0.03)", "rgba(0,0,0,0.08)", 0, 0, 3);
        }
        return cellule;
    }

    private void affecterStyleCase(Pane cellule, String couleurFond, String couleurBord, double longueurBord, double angleCase, double angleBord){
        cellule.setStyle(
            "-fx-background-color: " + couleurFond + ";" +
            "-fx-border-color: " + couleurBord +";" +
            "-fx-border-width: " + longueurBord +";" +
            "-fx-background-radius: " + angleCase + ";" +
            "-fx-border-radius: " + angleBord + ";"
        );
    }

    /**
     * Permet de construire la legende, ainsi que
     * de la placer dans la placer dans la VBox principale
     */
    private void afficherLegende() {
        String texte = "";
        this.zoneLegende.setHgap(18);
        this.zoneLegende.setVgap(8);
        this.zoneLegende.setPadding(new Insets(10, 14, 10, 14));
        this.zoneLegende.setMaxWidth(Double.MAX_VALUE);
        this.zoneLegende.setStyle("-fx-background-color: #EAEAEA; -fx-background-radius: 4;");

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