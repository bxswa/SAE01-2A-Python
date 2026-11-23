package iut.sae.controleur;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import iut.sae.modele.Algorithme;
import iut.sae.modele.Configuration;
import iut.sae.modele.ErreurConfiguration;
import iut.sae.modele.EtatApplication;
import iut.sae.modele.ResultatExecution;
import iut.sae.modele.ResultatsSimulation;
import iut.sae.service.EcrivainConfig;
import iut.sae.service.LecteurConfig;
import iut.sae.service.LecteurResultats;
import iut.sae.service.PythonService;
import iut.sae.util.Chemins;
import iut.sae.util.Constantes;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Controleur de l'ecran de configuration (vue Configuration.fxml).
 *
 * <p>Il permet de creer ou de modifier un fichier config.txt : choix des
 * fichiers d'entree, des algorithmes, du quantum et des noms des fichiers
 * de sortie. Un apercu du fichier est mis a jour a chaque modification.</p>
 *
 * <p>Le bouton "Enregistrer et lancer" ecrit le config.txt,
 * lance Python dans un thread separe ({@link PythonService}), puis lit les
 * fichiers de resultats ({@link LecteurResultats}) et les range dans
 * {@link EtatApplication} pour les autres ecrans.</p>
 *
 * <p>Le controleur ne lit et n'ecrit pas lui-meme les fichiers : il
 * delegue aux classes du paquetage service.</p>
 */
public class ConfigurationControleur {

    // ------------------------------------------------------------------
    // Composants de la vue (injectes par le FXMLLoader)
    // ------------------------------------------------------------------

    @FXML private TextField tfProcessus;
    @FXML private TextField tfRessources;
    @FXML private TextField tfEnergie;

    @FXML private CheckBox cbFifo;
    @FXML private CheckBox cbRoundRobin;
    @FXML private CheckBox cbEdf;
    @FXML private Spinner<Integer> spQuantum;

    @FXML private TextField tfGlobalFifo;
    @FXML private TextField tfAllocationsFifo;
    @FXML private TextField tfGlobalRoundRobin;
    @FXML private TextField tfAllocationsRoundRobin;
    @FXML private TextField tfGlobalEdf;
    @FXML private TextField tfAllocationsEdf;
    @FXML private TextField tfEnergieFifo;
    @FXML private TextField tfEnergieRoundRobin;
    @FXML private TextField tfEnergieEdf;
    @FXML private TextField tfMetriques;

    @FXML private Label lblFichierConfig;
    @FXML private TextArea taApercu;
    @FXML private Label lblMessage;
    @FXML private Button btnLancer;
    @FXML private ProgressIndicator piCalcul;

    // ------------------------------------------------------------------
    // Donnees du controleur
    // ------------------------------------------------------------------

    /** Case a cocher de chaque algorithme. */
    private final Map<Algorithme, CheckBox> cases = new EnumMap<>(Algorithme.class);

    /** Champ du fichier global de chaque algorithme. */
    private final Map<Algorithme, TextField> champsGlobaux = new EnumMap<>(Algorithme.class);

    /** Champ du fichier des allocations de chaque algorithme. */
    private final Map<Algorithme, TextField> champsAllocations = new EnumMap<>(Algorithme.class);

    /** Champ du fichier des metriques d'energie de chaque algorithme. */
    private final Map<Algorithme, TextField> champsEnergie = new EnumMap<>(Algorithme.class);

    /** Lit les fichiers config.txt. */
    private final LecteurConfig lecteur = new LecteurConfig();

    /** Ecrit les fichiers config.txt. */
    private final EcrivainConfig ecrivain = new EcrivainConfig();

    /** Lance le programme Python. */
    private final PythonService python = new PythonService();

    /** Lit les fichiers de resultats ecrits par Python. */
    private final LecteurResultats lecteurResultats = new LecteurResultats();

    /** Fichier config.txt en cours de modification. */
    private Path fichierConfig;

    /**
     * Appelee automatiquement apres le chargement du FXML :
     * prepare les composants et affiche la configuration par defaut.
     */
    @FXML
    private void initialize() {
        associerComposantsAuxAlgorithmes();
        spQuantum.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                Constantes.QUANTUM_MIN, Constantes.QUANTUM_MAX, Constantes.QUANTUM_DEFAUT));
        ajouterEcouteurs();
        fichierConfig = Chemins.dossierTestParDefaut().resolve(Constantes.NOM_FICHIER_CONFIG);
        remplirFormulaire(Configuration.parDefaut(Chemins.dossierTestParDefaut()));
    }

    // ------------------------------------------------------------------
    // Actions des boutons (onAction dans le FXML)
    // ------------------------------------------------------------------

    /** Bouton "Parcourir" du fichier des processus. */
    @FXML
    private void parcourirProcessus() {
        choisirFichierEntree(tfProcessus, "Fichier CSV", "*.csv");
    }

    /** Bouton "Parcourir" du fichier des ressources. */
    @FXML
    private void parcourirRessources() {
        choisirFichierEntree(tfRessources, "Fichier JSON", "*.json");
    }

    /** Bouton "Parcourir" du fichier d'energie. */
    @FXML
    private void parcourirEnergie() {
        choisirFichierEntree(tfEnergie, "Fichier CSV", "*.csv");
    }

    /** Bouton "Ouvrir un config..." : charge un config.txt existant. */
    @FXML
    private void ouvrirConfig() {
        FileChooser choix = creerSelecteurConfig("Ouvrir un fichier de configuration");
        File fichier = choix.showOpenDialog(fenetre());
        if (fichier == null) {
            return;
        }
        try {
            remplirFormulaire(lecteur.lire(fichier.toPath()));
            fichierConfig = fichier.toPath();
            mettreAJourApercu();
            afficherMessage("Configuration chargee : " + fichier.getName(), false);
        } catch (IOException | ErreurConfiguration e) {
            afficherMessage("Impossible de lire ce fichier : " + e.getMessage(), true);
        }
    }

    /** Bouton "Enregistrer..." : verifie puis ecrit le config.txt. */
    @FXML
    private void enregistrerConfig() {
        Configuration config = lireFormulaire();
        List<String> erreurs = config.verifier();
        if (!erreurs.isEmpty()) {
            afficherMessage(String.join("\n", erreurs), true);
            return;
        }
        FileChooser choix = creerSelecteurConfig("Enregistrer la configuration");
        File fichier = choix.showSaveDialog(fenetre());
        if (fichier == null) {
            return;
        }
        try {
            ecrivain.ecrire(config, fichier.toPath());
            fichierConfig = fichier.toPath();
            mettreAJourApercu();
            afficherMessage("Configuration enregistree dans " + fichier.getAbsolutePath(), false);
        } catch (IOException e) {
            afficherMessage("Ecriture impossible : " + e.getMessage(), true);
        }
    }

    /**
     * Bouton "Enregistrer et lancer".
     *
     * <p>Etapes : verifier le formulaire, ecrire le config.txt en cours,
     * puis lancer Python dans un thread separe. Pendant le calcul, le
     * bouton est desactive et un indicateur tourne ; la fenetre reste
     * utilisable. A la fin, {@link #simulationTerminee} est appelee dans le
     * thread JavaFX.</p>
     */
    @FXML
    private void lancerSimulation() {
        Configuration config = lireFormulaire();
        List<String> erreurs = config.verifier();
        if (!erreurs.isEmpty()) {
            afficherMessage(String.join("\n", erreurs), true);
            return;
        }
        Path fichier = fichierConfig.toAbsolutePath();
        try {
            if (!estDejaEnregistree(config, fichier)) {
                ecrivain.ecrire(config, fichier);
            }
        } catch (IOException e) {
            afficherMessage("Ecriture de " + fichier + " impossible : " + e.getMessage(), true);
            return;
        }
        mettreAJourApercu();
        Path dossierSortie = Chemins.dossierSortie(fichier);

        afficherCalculEnCours(true);
        afficherMessage("Simulation en cours… (" + config.getAlgorithmes().size()
                + " algorithme(s), config : " + fichier.getFileName() + ")", false);
        python.lancer(fichier, dossierSortie,
                resultat -> simulationTerminee(resultat, config, dossierSortie));
    }

    /**
     * Appelee dans le thread JavaFX quand Python est termine : lit les
     * resultats et previent les autres ecrans, ou affiche l'erreur.
     *
     * @param execution     le resultat de l'execution de Python
     * @param config        la configuration simulee
     * @param dossierSortie le dossier des resultats
     */
    private void simulationTerminee(ResultatExecution execution, Configuration config,
                                    Path dossierSortie) {
        afficherCalculEnCours(false);
        if (!execution.estReussie()) {
            String code = execution.getCodeRetour() == ResultatExecution.CODE_LANCEMENT_IMPOSSIBLE
                    ? "" : " (code retour " + execution.getCodeRetour() + ")";
            afficherMessage("La simulation a echoue" + code + " :\n" + execution.getSortie(), true);
            return;
        }
        try {
            ResultatsSimulation resultats = lecteurResultats.lire(config, dossierSortie);
            EtatApplication.setResultats(resultats);
            afficherMessage("Simulation terminee : " + resultats.getAlgorithmes().size()
                    + " algorithme(s), " + resultats.getProcessus().size()
                    + " processus. Resultats dans " + dossierSortie, false);
        } catch (IOException e) {
            afficherMessage("Python a fini, mais les resultats sont illisibles : "
                    + e.getMessage(), true);
        }
    }

    /** Bouton "Valeurs par defaut" : revient au jeu d'essai Test1. */
    @FXML
    private void reinitialiser() {
        fichierConfig = Chemins.dossierTestParDefaut().resolve(Constantes.NOM_FICHIER_CONFIG);
        remplirFormulaire(Configuration.parDefaut(Chemins.dossierTestParDefaut()));
        afficherMessage("Valeurs par defaut restaurees.", false);
    }

    // ------------------------------------------------------------------
    // Passage formulaire <-> objet Configuration
    // ------------------------------------------------------------------

    /**
     * Construit un objet Configuration a partir de ce qui est saisi.
     *
     * @return la configuration saisie
     */
    private Configuration lireFormulaire() {
        Configuration config = new Configuration();
        config.setFichierProcessus(tfProcessus.getText());
        config.setFichierRessources(tfRessources.getText());
        config.setFichierEnergie(tfEnergie.getText());
        config.setQuantum(spQuantum.getValue());
        config.setFichierMetriques(tfMetriques.getText());
        for (Algorithme a : Algorithme.values()) {
            config.selectionner(a, cases.get(a).isSelected());
            config.setFichierGlobal(a, champsGlobaux.get(a).getText());
            config.setFichierAllocations(a, champsAllocations.get(a).getText());
            config.setFichierMetriquesEnergie(a, champsEnergie.get(a).getText());
        }
        return config;
    }

    /**
     * Affiche une configuration dans le formulaire.
     *
     * @param config la configuration a afficher
     */
    private void remplirFormulaire(Configuration config) {
        tfProcessus.setText(config.getFichierProcessus());
        tfRessources.setText(config.getFichierRessources());
        tfEnergie.setText(config.getFichierEnergie());
        spQuantum.getValueFactory().setValue(config.getQuantum());
        tfMetriques.setText(config.getFichierMetriques());
        for (Algorithme a : Algorithme.values()) {
            cases.get(a).setSelected(config.estSelectionne(a));
            champsGlobaux.get(a).setText(config.getFichierGlobal(a));
            champsAllocations.get(a).setText(config.getFichierAllocations(a));
            champsEnergie.get(a).setText(config.getFichierMetriquesEnergie(a));
        }
        mettreAJourEtatDesChamps();
        mettreAJourApercu();
    }

    // ------------------------------------------------------------------
    // Mise a jour de l'affichage
    // ------------------------------------------------------------------

    /** Recalcule l'apercu du fichier config.txt et le nom du fichier en cours. */
    private void mettreAJourApercu() {
        Path dossier = fichierConfig.toAbsolutePath().getParent();
        taApercu.setText(ecrivain.genererTexte(lireFormulaire(), dossier));
        lblFichierConfig.setText(fichierConfig.toAbsolutePath().toString());
    }

    /**
     * Active les champs de sortie des algorithmes coches seulement,
     * et le quantum seulement si Round Robin est coche.
     */
    private void mettreAJourEtatDesChamps() {
        for (Algorithme a : Algorithme.values()) {
            boolean coche = cases.get(a).isSelected();
            champsGlobaux.get(a).setDisable(!coche);
            champsAllocations.get(a).setDisable(!coche);
            champsEnergie.get(a).setDisable(!coche);
        }
        spQuantum.setDisable(!cbRoundRobin.isSelected());
    }

    /**
     * Affiche un message sous le formulaire.
     *
     * @param texte  le message
     * @param erreur true pour un message d'erreur (rouge), false sinon (vert)
     */
    private void afficherMessage(String texte, boolean erreur) {
        lblMessage.setText(texte);
        lblMessage.getStyleClass().removeAll(Constantes.STYLE_MESSAGE_OK, Constantes.STYLE_MESSAGE_ERREUR);
        lblMessage.getStyleClass().add(erreur ? Constantes.STYLE_MESSAGE_ERREUR : Constantes.STYLE_MESSAGE_OK);
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    /**
     * Indique si le fichier contient deja exactement cette configuration.
     * Dans ce cas on ne le reecrit pas : un config.txt fourni (avec ses
     * commentaires) reste intact tant qu'on ne modifie rien.
     *
     * @param config  la configuration saisie
     * @param fichier le fichier config.txt
     * @return true si le fichier existe et contient les memes valeurs
     */
    private boolean estDejaEnregistree(Configuration config, Path fichier) {
        if (!fichier.toFile().isFile()) {
            return false;
        }
        Path dossier = fichier.getParent();
        try {
            String actuel = ecrivain.genererTexte(lecteur.lire(fichier), dossier);
            return actuel.equals(ecrivain.genererTexte(config, dossier));
        } catch (IOException | ErreurConfiguration e) {
            // Fichier illisible : on le remplace
            return false;
        }
    }

    /**
     * Montre ou cache l'indicateur de calcul et bloque le bouton de
     * lancement pendant que Python tourne (pas deux simulations a la fois).
     *
     * @param enCours true pendant le calcul
     */
    private void afficherCalculEnCours(boolean enCours) {
        btnLancer.setDisable(enCours);
        piCalcul.setVisible(enCours);
    }

    /** Range les cases et les champs dans des tables indexees par algorithme. */
    private void associerComposantsAuxAlgorithmes() {
        cases.put(Algorithme.FIFO, cbFifo);
        cases.put(Algorithme.ROUND_ROBIN, cbRoundRobin);
        cases.put(Algorithme.EDF, cbEdf);
        champsGlobaux.put(Algorithme.FIFO, tfGlobalFifo);
        champsGlobaux.put(Algorithme.ROUND_ROBIN, tfGlobalRoundRobin);
        champsGlobaux.put(Algorithme.EDF, tfGlobalEdf);
        champsAllocations.put(Algorithme.FIFO, tfAllocationsFifo);
        champsAllocations.put(Algorithme.ROUND_ROBIN, tfAllocationsRoundRobin);
        champsAllocations.put(Algorithme.EDF, tfAllocationsEdf);
        champsEnergie.put(Algorithme.FIFO, tfEnergieFifo);
        champsEnergie.put(Algorithme.ROUND_ROBIN, tfEnergieRoundRobin);
        champsEnergie.put(Algorithme.EDF, tfEnergieEdf);
    }

    /** Met a jour l'apercu (et l'etat des champs) a chaque modification. */
    private void ajouterEcouteurs() {
        for (TextField champ : List.of(tfProcessus, tfRessources, tfEnergie, tfMetriques,
                tfGlobalFifo, tfAllocationsFifo, tfGlobalRoundRobin, tfAllocationsRoundRobin,
                tfGlobalEdf, tfAllocationsEdf, tfEnergieFifo, tfEnergieRoundRobin, tfEnergieEdf)) {
            champ.textProperty().addListener((obs, ancien, nouveau) -> mettreAJourApercu());
        }
        for (CheckBox caseACocher : List.of(cbFifo, cbRoundRobin, cbEdf)) {
            caseACocher.selectedProperty().addListener((obs, ancien, nouveau) -> {
                mettreAJourEtatDesChamps();
                mettreAJourApercu();
            });
        }
        spQuantum.valueProperty().addListener((obs, ancien, nouveau) -> mettreAJourApercu());
    }

    /**
     * Ouvre un selecteur de fichier et met le chemin choisi dans un champ.
     *
     * @param champ       le champ a remplir
     * @param description la description du filtre (ex : "Fichier CSV")
     * @param extension   le filtre d'extension (ex : "*.csv")
     */
    private void choisirFichierEntree(TextField champ, String description, String extension) {
        FileChooser choix = new FileChooser();
        choix.setTitle("Choisir un fichier");
        choix.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extension));
        File actuel = new File(champ.getText());
        if (actuel.getParentFile() != null && actuel.getParentFile().isDirectory()) {
            choix.setInitialDirectory(actuel.getParentFile());
        }
        File fichier = choix.showOpenDialog(fenetre());
        if (fichier != null) {
            champ.setText(fichier.getAbsolutePath());
        }
    }

    /**
     * Prepare un selecteur pour les fichiers config.txt, ouvert dans le
     * dossier du fichier en cours.
     *
     * @param titre le titre de la boite de dialogue
     * @return le selecteur
     */
    private FileChooser creerSelecteurConfig(String titre) {
        FileChooser choix = new FileChooser();
        choix.setTitle(titre);
        choix.getExtensionFilters().add(new FileChooser.ExtensionFilter("Configuration", "*.txt"));
        File dossier = fichierConfig.toAbsolutePath().getParent().toFile();
        if (dossier.isDirectory()) {
            choix.setInitialDirectory(dossier);
        }
        choix.setInitialFileName(Constantes.NOM_FICHIER_CONFIG);
        return choix;
    }

    /** @return la fenetre qui contient cet ecran (pour les boites de dialogue) */
    private Window fenetre() {
        return tfProcessus.getScene().getWindow();
    }
}
