package iut.sae.util;

/**
 * Regroupe toutes les constantes de l'application (noms de fichiers,
 * cles du fichier config.txt, textes, tailles, styles...).
 *
 * <p>Centraliser les constantes ici evite d'ecrire les memes valeurs
 * a plusieurs endroits du code : pour changer un nom de fichier ou une
 * couleur, il suffit de le modifier dans cette classe.</p>
 */
public final class Constantes {

    /** Classe utilitaire : on ne cree pas d'objet Constantes. */
    private Constantes() {
    }

    // ------------------------------------------------------------------
    // Fenetre et vues
    // ------------------------------------------------------------------

    /** Titre de la fenetre principale. */
    public static final String TITRE_APPLICATION = "Visualiseur d'ordonnancement";

    /** Largeur initiale de la fenetre principale (pixels). */
    public static final double LARGEUR_FENETRE = 1150;

    /** Hauteur initiale de la fenetre principale (pixels). */
    public static final double HAUTEUR_FENETRE = 780;

    /** Chemin (dans les ressources) de la vue principale. */
    public static final String VUE_PRINCIPALE = "/iut/sae/vue/Principale.fxml";

    /** Chemin (dans les ressources) de la feuille de style. */
    public static final String FEUILLE_STYLE = "/iut/sae/vue/style.css";

    // ------------------------------------------------------------------
    // Arborescence des donnees (a cote du .jar)
    // ------------------------------------------------------------------

    /** Dossier qui contient les jeux d'essai (un sous-dossier par test). */
    public static final String DOSSIER_ENTREES = "Inputs";

    /** Dossier ou Python ecrit les resultats (un sous-dossier par test). */
    public static final String DOSSIER_SORTIES = "Outputs";

    /** Jeu d'essai propose au demarrage. */
    public static final String TEST_PAR_DEFAUT = "Test1";

    /** Nom du fichier de configuration lu par Python. */
    public static final String NOM_FICHIER_CONFIG = "config.txt";

    /** Nom par defaut du fichier des processus. */
    public static final String FICHIER_PROCESSUS_DEFAUT = "processus.csv";

    /** Nom par defaut du fichier des ressources. */
    public static final String FICHIER_RESSOURCES_DEFAUT = "ressources.json";

    /** Nom par defaut du fichier d'energie. */
    public static final String FICHIER_ENERGIE_DEFAUT = "energie.csv";

    /** Nom par defaut du fichier des metriques. */
    public static final String FICHIER_METRIQUES_DEFAUT = "metriques.csv";

    // ------------------------------------------------------------------
    // Quantum (Round Robin)
    // ------------------------------------------------------------------

    /** Quantum propose par defaut. */
    public static final int QUANTUM_DEFAUT = 2;

    /** Plus petit quantum autorise. */
    public static final int QUANTUM_MIN = 1;

    /** Plus grand quantum autorise. */
    public static final int QUANTUM_MAX = 100;

    // ------------------------------------------------------------------
    // Format du fichier config.txt (doit correspondre a lecture.py)
    // ------------------------------------------------------------------

    /** Cle du fichier des processus. */
    public static final String CLE_PROCESSUS = "fichier_processus";

    /** Cle du fichier des ressources. */
    public static final String CLE_RESSOURCES = "fichier_ressources";

    /** Cle du fichier d'energie (celle du config fourni par les enseignants). */
    public static final String CLE_ENERGIE = "energie";

    /** Autre cle acceptee en lecture pour le fichier d'energie. */
    public static final String CLE_ENERGIE_ALTERNATIVE = "fichier_energie";

    /** Cle de la liste des algorithmes. */
    public static final String CLE_ALGORITHMES = "algorithmes";

    /** Cle du fichier des metriques. */
    public static final String CLE_METRIQUES = "fichier_metriques";

    /** Cle du quantum. */
    public static final String CLE_QUANTUM = "quantum";

    /** Debut de la cle du fichier global d'un algorithme. */
    public static final String PREFIXE_CLE_GLOBAL = "fichier_global_";

    /** Debut de la cle du fichier des allocations d'un algorithme. */
    public static final String PREFIXE_CLE_ALLOCATIONS = "fichier_allocations_";

    /** Separateur entre une cle et sa valeur. */
    public static final String SEPARATEUR_CLE_VALEUR = "=";

    /** Separateur entre les algorithmes de la liste. */
    public static final String SEPARATEUR_ALGORITHMES = ",";

    /** Debut d'une ligne de commentaire. */
    public static final String DEBUT_COMMENTAIRE = "#";

    /** Extension attendue pour les fichiers de sortie. */
    public static final String EXTENSION_CSV = ".csv";

    // ------------------------------------------------------------------
    // Lancement du programme Python
    // ------------------------------------------------------------------

    /** Dossier (a cote de Inputs) qui contient le code Python. */
    public static final String DOSSIER_CODE = "Code";

    /** Programme principal Python. */
    public static final String SCRIPT_PYTHON = "main.py";

    /** Commandes essayees, dans l'ordre, pour lancer Python sous Windows. */
    public static final String[] COMMANDES_PYTHON_WINDOWS = {"python", "py"};

    /** Commandes essayees, dans l'ordre, pour lancer Python sous Linux / macOS. */
    public static final String[] COMMANDES_PYTHON_AUTRES = {"python3", "python"};

    /** Nom du thread qui attend la fin de Python. */
    public static final String NOM_THREAD_PYTHON = "thread-python";

    // ------------------------------------------------------------------
    // Colonnes des fichiers CSV (doivent correspondre a ecriture.py)
    // ------------------------------------------------------------------

    /** Separateur des fichiers CSV. */
    public static final String SEPARATEUR_CSV = ",";

    /** Colonne : identifiant du processus. */
    public static final String COL_ID_PROCESSUS = "id_processus";

    /** Colonne : date de soumission. */
    public static final String COL_DATE_SOUMISSION = "date_soumission";

    /** Colonne : duree d'execution (processus.csv). */
    public static final String COL_DUREE_EXECUTION = "duree_execution";

    /** Colonne : RAM necessaire (processus.csv). */
    public static final String COL_RAM_NECESSAIRE = "ram_necessaire";

    /** Colonne : priorite (processus.csv). */
    public static final String COL_PRIORITE = "priorite";

    /** Colonne : date butoir (processus.csv). */
    public static final String COL_DATE_BUTOIR = "date_butoir";

    /** Colonne : date de debut. */
    public static final String COL_DATE_DEBUT = "date_debut";

    /** Colonne : date de fin (exclue). */
    public static final String COL_DATE_FIN = "date_fin";

    /** Colonne : RAM demandee (fichier global). */
    public static final String COL_RAM_DEMANDEE = "ram_demandee";

    /** Colonne : RAM utilisee (fichier global). */
    public static final String COL_RAM_UTILISEE = "ram_utilisee";

    /** Colonne : identifiant du processeur (fichier des allocations). */
    public static final String COL_ID_PROCESSEUR = "id_processeur";

    /** Colonne : instant (fichier d'energie). */
    public static final String COL_INSTANT = "instant";

    /** Colonne : energie disponible (fichier d'energie). */
    public static final String COL_ENERGIE_DISPONIBLE = "energie_disponible";

    /** Colonne : energie consommee (fichier d'energie). */
    public static final String COL_ENERGIE_CONSOMMEE = "energie_consommee";

    /** Colonne : nombre de processeurs actifs (fichier d'energie). */
    public static final String COL_NB_CPU_ACTIFS = "nb_cpu_actifs";

    /** Colonne : algorithme (fichier des metriques). */
    public static final String COL_ALGORITHME = "algorithme";

    /** Colonne : temps d'attente moyen. */
    public static final String COL_ATTENTE_MOYENNE = "temps_attente_moyen";

    /** Colonne : temps de reponse moyen. */
    public static final String COL_REPONSE_MOYENNE = "temps_reponse_moyen";

    /** Colonne : makespan. */
    public static final String COL_MAKESPAN = "makespan";

    /** Colonne : energie consommee totale. */
    public static final String COL_ENERGIE_CONSOMMEE_TOTALE = "energie_consommee_totale";

    /** Colonne : energie disponible totale. */
    public static final String COL_ENERGIE_DISPONIBLE_TOTALE = "energie_disponible_totale";

    /** Colonne : taux d'utilisation de l'energie. */
    public static final String COL_TAUX_UTILISATION = "taux_utilisation";

    /** Mot remplace dans le nom du fichier global pour obtenir le fichier d'energie. */
    public static final String MOT_GLOBAL = "global";

    /** Mot qui le remplace (resultats_fifo_global.csv -> resultats_fifo_energie.csv). */
    public static final String MOT_ENERGIE = "energie";

    // ------------------------------------------------------------------
    // Styles des messages
    // ------------------------------------------------------------------

    /** Classe CSS d'un message de reussite. */
    public static final String STYLE_MESSAGE_OK = "message-ok";

    /** Classe CSS d'un message d'erreur. */
    public static final String STYLE_MESSAGE_ERREUR = "message-erreur";
}
