package iut.sae.controleur;

import java.lang.reflect.Method;
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
import iut.sae.modele.ResultatAlgorithme;
import iut.sae.modele.ResultatsSimulation;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
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

public class TabGanttProcesseursControleur implements Initializable {

    private static final List<String> PALETTE_COULEURS = List.of(
            "#4E79A7", "#F28E2B", "#E15759", "#76B7B2",
            "#59A14F", "#EDC948", "#B07AA1", "#FF9DA7",
            "#9C755F", "#BAB0AC"
    );

    private static final double LARGEUR_COLONNE_CPU = 70.0;
    private static final double LARGEUR_CASE = 28.0;
    private static final double HAUTEUR_CASE = 26.0;
    private static final double LARGEUR_SEPARATEUR = 15.0;
    private static final double ECART_VERTICAL_LIGNES = 4.0;
    private static final double HAUTEUR_BARRE_TEMPS = 20.0;

    private static final class BornesProcessus {
        final Integer soumission;
        final Integer expiration;

        BornesProcessus(Integer soumission, Integer expiration) {
            this.soumission = soumission;
            this.expiration = expiration;
        }
    }

    private ResultatsSimulation resultatsLancement;
    private final Map<String, String> mapCouleursProcessus = new LinkedHashMap<>();
    private final Map<String, BornesProcessus> mapBornesProcessus = new LinkedHashMap<>();

    private final Label lblEtatVide = new Label("Aucun résultat montré...");
    private final FlowPane zoneLegende = new FlowPane();

    @FXML
    private ScrollPane scrollPaneRacine;

    @FXML
    private VBox boiteConteneurElements;

    public TabGanttProcesseursControleur() {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //Pour etablir la taille du ScrollPane a l'ensemble du Tab dans la fenetre principale
        this.scrollPaneRacine.setFitToWidth(true);
        this.scrollPaneRacine.setFitToHeight(true);
        this.scrollPaneRacine.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        this.boiteConteneurElements.setMaxWidth(Double.MAX_VALUE);
        this.boiteConteneurElements.setFillWidth(true);

        //Listener, pour que la page se charge d'elle meme, et eviter les cas d'erreurs
        //Peut-etre qu'une methode moins couteuse existe...
        EtatApplication.resultatsProperty().addListener((obs, ancien, nouveau) ->
                lancerAffichageParProcesseurs(nouveau));
        lancerAffichageParProcesseurs(EtatApplication.getResultats());
    }

    /**
     * Permet d'afficher l'ensemble de la page
     * Si aucun resultat de montre, affichage d'"erreur"
     * @param resultats resultats de la derniere simulation
     */
    public void lancerAffichageParProcesseurs(ResultatsSimulation resultats) {
        this.resultatsLancement = resultats;

        //Pour chaque nouveaux resultats, on enleve le contenu du ScrollPane et de la VBox principale
        this.boiteConteneurElements.getChildren().clear();
        //Mise a zero de la legende
        this.zoneLegende.getChildren().clear();
        this.mapCouleursProcessus.clear();
        this.mapBornesProcessus.clear();

        //Si pas de resultat soumis, message exclusif montre
        if (resultats == null || resultats.getAlgorithmes().isEmpty()) {
            afficherEtatVide();
        } else {
            //Autrement, affichage classique

            //Association des processus et des couleurs
            analyserProcessusEtCouleurs();
            //Creation et affichage de tous les diagrammes
            afficherGanttProcesseur();
            //Creation et affichage de la legende
            afficherLegende();
        }
    }

    /**
     * Affichage du message cree en cas de resultats vides
     * S'ouvre quand l'application viens de se lancer, ou en cas d'erreurs des metriques
     */
    private void afficherEtatVide() {
        this.lblEtatVide.setMaxWidth(Double.MAX_VALUE);
        this.lblEtatVide.setAlignment(Pos.CENTER);
        this.lblEtatVide.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
        VBox.setVgrow(this.lblEtatVide, Priority.ALWAYS);
        this.boiteConteneurElements.getChildren().add(this.lblEtatVide);
    }

    /**
     * Creation de chaque diagrammes,
     * Et ajout dans la VBox principale
     */
    private void afficherGanttProcesseur() {
        List<String> processeurs = this.resultatsLancement.getProcesseurs();
        List<Algorithme> algorithmes = this.resultatsLancement.getAlgorithmes();

        //Chaque algorithme utilise une politique d'ordonnancement
        //Pour eviter les superpositions, creation d'un diagramme par algorithme
        for (Algorithme algo : algorithmes) {
            ResultatAlgorithme resultatAlgo = this.resultatsLancement.getResultat(algo);

            //Verification de si un resultat est, d'une façon ou une autre, inexistant
            if (resultatAlgo == null) {
                continue;
            }

            int nbPas = retrouverNombrePas(algo, resultatAlgo);
            Map<String, Map<Integer, String>> occupationParProcEtTemps = indexerAllocations(resultatAlgo.getAllocations());

            //Chaque daigramme est dans une VBox, car nous voulons un espacement different
            //entre les diagrammes et les digrammes, et les diagrammes et leur titre
            VBox blocAlgo = new VBox(8);
            blocAlgo.setPadding(new Insets(10, 14, 10, 14));
            blocAlgo.setMaxWidth(Double.MAX_VALUE);
            blocAlgo.setFillWidth(true);
            blocAlgo.setAlignment(Pos.CENTER);

            Label titreAlgo = new Label(algo.getLibelle());
            titreAlgo.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
            titreAlgo.setMaxWidth(Double.MAX_VALUE);
            titreAlgo.setAlignment(Pos.CENTER);

            HBox barreTemps = construireBarreTemps(nbPas);
            GridPane grid = construireGrilleAlgorithme(processeurs, nbPas, occupationParProcEtTemps);

            int nbProcs = processeurs.size();
            double hauteurGrille = (nbProcs * HAUTEUR_CASE)
                    + (Math.max(0, nbProcs - 1) * ECART_VERTICAL_LIGNES);

            Pane calqueBornes = construireCalqueBornes(nbPas, hauteurGrille);
            StackPane grilleAvecBornes = new StackPane(grid, calqueBornes);
            grilleAvecBornes.setAlignment(Pos.TOP_LEFT);
            grilleAvecBornes.setMinWidth(Region.USE_PREF_SIZE);
            grilleAvecBornes.setMaxWidth(Region.USE_PREF_SIZE);

            //Conteneur interne qui garde barreTemps et grilleAvecBornes alignes a gauche l'un sur l'autre
            VBox diagrammeAssemble = new VBox(3, barreTemps, grilleAvecBornes);
            diagrammeAssemble.setMinWidth(Region.USE_PREF_SIZE);
            diagrammeAssemble.setMaxWidth(Region.USE_PREF_SIZE);

            //Conteneur du ScrollPane. Il centre l'ensemble quand il est plus petit que l'onglet
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

    private HBox construireBarreTemps(int nbPas) {
        HBox barreTemps = new HBox(0);
        barreTemps.setAlignment(Pos.CENTER_LEFT);
        barreTemps.setPrefHeight(HAUTEUR_BARRE_TEMPS);

        Region espaceGauche = new Region();
        espaceGauche.setMinWidth(LARGEUR_COLONNE_CPU);
        espaceGauche.setPrefWidth(LARGEUR_COLONNE_CPU);
        barreTemps.getChildren().add(espaceGauche);

        double pasColonne = LARGEUR_CASE + LARGEUR_SEPARATEUR;

        //Chaque label de largeur `pasColonne` est centre et decale de `-LARGEUR_CASE / 2`
        //Pour que son centre tombe exactement sur le milieu de l'écart entre deux colonnes.
        for (int t = 0; t <= nbPas; t++) {
            Label lblTemps = new Label("" + t);
            lblTemps.setMinWidth(pasColonne);
            lblTemps.setPrefWidth(pasColonne);
            lblTemps.setAlignment(Pos.CENTER);
            lblTemps.setTranslateX(-(LARGEUR_CASE));
            lblTemps.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #444444;");
            barreTemps.getChildren().add(lblTemps);
        }

        return barreTemps;
    }

    private GridPane construireGrilleAlgorithme(List<String> processeurs, int nbPas,
                                                Map<String, Map<Integer, String>> occupation) {
        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(ECART_VERTICAL_LIGNES);
        grid.setAlignment(Pos.CENTER_LEFT);

        //Contraintes de largeur fixes pour aligner précisement la barre des temps et les traits
        ColumnConstraints colCpu = new ColumnConstraints(LARGEUR_COLONNE_CPU);
        colCpu.setMinWidth(LARGEUR_COLONNE_CPU);
        colCpu.setMaxWidth(LARGEUR_COLONNE_CPU);
        grid.getColumnConstraints().add(colCpu);

        for (int t = 0; t < nbPas; t++) {
            ColumnConstraints colCase = new ColumnConstraints(LARGEUR_CASE);
            colCase.setMinWidth(LARGEUR_CASE);
            colCase.setMaxWidth(LARGEUR_CASE);
            grid.getColumnConstraints().add(colCase);

            if (t < nbPas - 1) {
                ColumnConstraints colSep = new ColumnConstraints(LARGEUR_SEPARATEUR);
                colSep.setMinWidth(LARGEUR_SEPARATEUR);
                colSep.setMaxWidth(LARGEUR_SEPARATEUR);
                grid.getColumnConstraints().add(colSep);
            }
        }

        int nbProcs = processeurs.size();
        double hauteurTotaleContinue = (nbProcs * HAUTEUR_CASE)
                + (Math.max(0, nbProcs - 1) * ECART_VERTICAL_LIGNES);

        for (int row = 0; row < nbProcs; row++) {
            String idProc = processeurs.get(row);
            Label lblProc = new Label(idProc);
            lblProc.setMinWidth(LARGEUR_COLONNE_CPU);
            lblProc.setPrefWidth(LARGEUR_COLONNE_CPU);
            lblProc.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");
            grid.add(lblProc, 0, row);

            Map<Integer, String> tempsVersProcessus = occupation.getOrDefault(idProc, Map.of());

            for (int t = 0; t < nbPas; t++) {
                int colCase = 1 + (t * 2);
                Pane cellule = creerCase(tempsVersProcessus.get(t), t);
                grid.add(cellule, colCase, row);
            }
        }

        if (nbProcs > 0) {
            for (int t = 0; t < nbPas - 1; t++) {
                int colSeparateur = 2 + (t * 2);
                StackPane conteneurLigne = new StackPane(creerSeparateurContinu(hauteurTotaleContinue));
                conteneurLigne.setMinWidth(LARGEUR_SEPARATEUR);
                conteneurLigne.setPrefWidth(LARGEUR_SEPARATEUR);
                conteneurLigne.setAlignment(Pos.CENTER);
                grid.add(conteneurLigne, colSeparateur, 0);
                GridPane.setRowSpan(conteneurLigne, nbProcs);
            }
        }

        return grid;
    }

    private Pane construireCalqueBornes(int nbPas, double hauteurGrille) {
        Pane calque = new Pane();
        calque.setPickOnBounds(false);
        calque.setMouseTransparent(true);

        if (hauteurGrille <= 0) {
            return calque;
        }

        double pasColonne = LARGEUR_CASE + LARGEUR_SEPARATEUR;

        //Regroupe tous les événements (soumission ou expiration) par instant t
        //Afin de les decaler legerement cote a cote s'ils tombent au même instant.
        Map<Integer, List<Line>> traitsParInstant = new HashMap<>();

        for (Map.Entry<String, BornesProcessus> entry : mapBornesProcessus.entrySet()) {
            String idProc = entry.getKey();
            BornesProcessus bornes = entry.getValue();
            Color couleur = Color.web(mapCouleursProcessus.getOrDefault(idProc, "#4E79A7"));

            if (bornes.soumission != null && bornes.soumission >= 0 && bornes.soumission <= nbPas) {
                Line traitSoumission = new Line(0, 0, 0, hauteurGrille);
                traitSoumission.setStroke(couleur);
                traitSoumission.setStrokeWidth(2.2);
                traitsParInstant.computeIfAbsent(bornes.soumission, k -> new ArrayList<>())
                        .add(traitSoumission);
            }

            if (bornes.expiration != null && bornes.expiration >= 0 && bornes.expiration <= nbPas) {
                Line traitExpiration = new Line(0, 0, 0, hauteurGrille);
                traitExpiration.setStroke(couleur);
                traitExpiration.setStrokeWidth(2.2);
                traitExpiration.getStrokeDashArray().addAll(5.0, 3.0);
                traitsParInstant.computeIfAbsent(bornes.expiration, k -> new ArrayList<>())
                        .add(traitExpiration);
            }
        }

        for (Map.Entry<Integer, List<Line>> entry : traitsParInstant.entrySet()) {
            int t = entry.getKey();
            List<Line> traits = entry.getValue();

            //Centre de la frontiere correspondant à l'instant t
            double xCentre = (t == 0)
                    ? LARGEUR_COLONNE_CPU - 2.0
                    : LARGEUR_COLONNE_CPU + (t * pasColonne) - (LARGEUR_SEPARATEUR / 2.0);

            int n = traits.size();
            double ecartEntreTraits = 3.2;
            for (int i = 0; i < n; i++) {
                double decalage = (i - (n - 1) / 2.0) * ecartEntreTraits;
                Line trait = traits.get(i);
                trait.setStartX(xCentre + decalage);
                trait.setEndX(xCentre + decalage);
                calque.getChildren().add(trait);
            }
        }

        return calque;
    }

    private Pane creerCase(String idProcessusAlloue, int pasTemps) {
        Pane cellule = new Pane();
        cellule.setPrefSize(LARGEUR_CASE, HAUTEUR_CASE);
        cellule.setMinSize(LARGEUR_CASE, HAUTEUR_CASE);

        if (idProcessusAlloue != null) {
            String couleurHex = mapCouleursProcessus.getOrDefault(idProcessusAlloue, "#4E79A7");
            cellule.setStyle(
                    "-fx-background-color: " + couleurHex + ";" +
                    "-fx-background-radius: 3;" +
                    "-fx-border-color: rgba(0,0,0,0.25);" +
                    "-fx-border-radius: 3;"
            );
            BornesProcessus b = mapBornesProcessus.get(idProcessusAlloue);
            String bornesTxt = (b != null)
                    ? " | Soumission : " + (b.soumission != null ? b.soumission : "-")
                            + " | Expiration : " + (b.expiration != null ? b.expiration : "-")
                    : "";
            Tooltip.install(cellule, new Tooltip(
                    idProcessusAlloue + " (t = " + pasTemps + " → " + (pasTemps + 1) + ")" + bornesTxt
            ));
        } else {
            cellule.setStyle(
                    "-fx-background-color: rgba(0,0,0,0.03);" +
                    "-fx-border-color: rgba(0,0,0,0.08);" +
                    "-fx-border-radius: 3;"
            );
            Tooltip.install(cellule, new Tooltip("t = " + pasTemps + " → " + (pasTemps + 1) + " (inactif)"));
        }
        return cellule;
    }

    /**
     * Cree une ligne separant les colonnes entre elles
     * @param hauteur la longueur de la ligne
     * @return ligne construite (Line)
     */
    private Line creerSeparateurContinu(double hauteur) {
        Line ligne = new Line(0, 0, 0, hauteur);
        ligne.setStroke(Color.web("#CCCCCC"));
        ligne.setStrokeWidth(1.0);
        return ligne;
    }

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
     * Retourne le nombre de colonnes a utiliser
     * @param algo algorithme en cours de traitement
     * @param resultatAlgo resultats obtenus
     * @return
     */
    private int retrouverNombrePas(Algorithme algo, ResultatAlgorithme resultatAlgo) {
        int makespan = retrouverMetrique(algo);
        if (makespan > 0) {
            return makespan;
        }
        return resultatAlgo.getDateFinMax();
    }

    private void analyserProcessusEtCouleurs() {
        int index = 0;
        if (this.resultatsLancement.getProcessus() != null) {
            for (var p : this.resultatsLancement.getProcessus()) {
                String id = p.getId();
                if (!mapCouleursProcessus.containsKey(id)) {
                    mapCouleursProcessus.put(id, PALETTE_COULEURS.get(index % PALETTE_COULEURS.size()));
                    index++;
                }
                Integer soumission = lireEntier(p,
                        "getDateSoumission", "getSoumission", "getDateArrivee", "getArrivee");
                Integer expiration = lireEntier(p,
                        "getDateExpiration", "getExpiration", "getEcheance", "getDeadline", "getDateLimite");
                mapBornesProcessus.put(id, new BornesProcessus(soumission, expiration));
            }
        }
        for (Algorithme algo : this.resultatsLancement.getAlgorithmes()) {
            ResultatAlgorithme r = this.resultatsLancement.getResultat(algo);
            if (r == null) {
                continue;
            }
            for (Allocation alloc : r.getAllocations()) {
                String idProc = alloc.getIdProcessus();
                if (!mapCouleursProcessus.containsKey(idProc)) {
                    mapCouleursProcessus.put(idProc, PALETTE_COULEURS.get(index % PALETTE_COULEURS.size()));
                    index++;
                }
                mapBornesProcessus.putIfAbsent(idProc, new BornesProcessus(null, null));
            }
        }
    }

    private Integer lireEntier(Object cible, String... nomsMethodes) {
        if (cible == null) {
            return null;
        }
        for (String nom : nomsMethodes) {
            try {
                Method m = cible.getClass().getMethod(nom);
                Object val = m.invoke(cible);
                if (val instanceof Number num) {
                    return num.intValue();
                }
                if (val != null) {
                    return Integer.parseInt(val.toString().trim());
                }
            } catch (Exception ignored) {
                // Essaie le getter suivant
            }
        }
        return null;
    }

    /**
     * Construit la legende a partir des processus disponibles
     * Est ajoutee dans la VBox principale ensuite
     */
    private void afficherLegende() {
        this.zoneLegende.getChildren().clear();
        this.zoneLegende.setHgap(18);
        this.zoneLegende.setVgap(8);
        this.zoneLegende.setPadding(new Insets(10, 14, 10, 14));
        this.zoneLegende.setMaxWidth(Double.MAX_VALUE);
        this.zoneLegende.setStyle("-fx-background-color: #EAEAEA; -fx-background-radius: 4;");

        for (Map.Entry<String, String> entry : mapCouleursProcessus.entrySet()) {
            String idProc = entry.getKey();
            String couleur = entry.getValue();

            Region carre = new Region();
            carre.setPrefSize(14, 14);
            carre.setStyle(
                    "-fx-background-color: " + couleur + ";" +
                    "-fx-background-radius: 2;" +
                    "-fx-border-color: #333333;"
            );

            BornesProcessus bornes = mapBornesProcessus.get(idProc);
            String texteBornes = "";
            if (bornes != null && (bornes.soumission != null || bornes.expiration != null)) {
                String s = (bornes.soumission != null) ? "soum. " + bornes.soumission : "soum. -";
                String e = (bornes.expiration != null) ? "exp. " + bornes.expiration : "exp. -";
                texteBornes = " (" + s + " | " + e + ")";
            }

            Label lblProc = new Label(idProc + texteBornes);
            lblProc.setStyle("-fx-font-size: 12px;");

            HBox item = new HBox(6, carre, lblProc);
            item.setAlignment(Pos.CENTER_LEFT);
            this.zoneLegende.getChildren().add(item);
        }

        this.boiteConteneurElements.getChildren().add(this.zoneLegende);
    }

    /**
     * Cherche le makespan qui correspon a l'algorithme
     * @param algo algorithme utilise
     * @return makespan de l'algorithme (int)
     */
    private int retrouverMetrique(Algorithme algo) {
        List<Metrique> metriques = this.resultatsLancement.getMetriques();
        int makespan = -1;

        for (Metrique metrique : metriques) {
            if (metrique.getAlgorithme() == algo) {
                makespan = metrique.getMakespan();
            }
        }
        return makespan;
    }
}