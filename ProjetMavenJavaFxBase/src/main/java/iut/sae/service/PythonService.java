package iut.sae.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import iut.sae.modele.ResultatExecution;
import iut.sae.util.Chemins;
import iut.sae.util.Constantes;
import javafx.application.Platform;

/**
 * Lance le programme Python de simulation (Code/main.py).
 *
 * <p>La simulation peut prendre du temps : si on attendait la fin de Python
 * dans le thread JavaFX, la fenetre serait figee. On lance donc Python dans
 * un <b>thread separe</b>, puis on revient dans le thread JavaFX avec
 * {@link Platform#runLater(Runnable)} pour afficher le resultat (seul le
 * thread JavaFX a le droit de modifier l'interface).</p>
 *
 * <p>Commande executee (depuis le dossier qui contient Code et Inputs) :</p>
 * <pre>python Code/main.py &lt;config.txt&gt; &lt;dossier de sortie&gt;</pre>
 */
public class PythonService {

    /**
     * Lance Python dans un thread separe.
     *
     * @param fichierConfig le fichier config.txt a simuler
     * @param dossierSortie le dossier ou Python doit ecrire les resultats
     * @param quandFini     action executee dans le thread JavaFX quand Python
     *                      est termine (avec le resultat de l'execution)
     */
    public void lancer(Path fichierConfig, Path dossierSortie,
                       Consumer<ResultatExecution> quandFini) {
        Thread thread = new Thread(() -> {
            ResultatExecution resultat = executer(fichierConfig, dossierSortie);
            Platform.runLater(() -> quandFini.accept(resultat));
        }, Constantes.NOM_THREAD_PYTHON);
        // Le thread ne doit pas empecher l'application de se fermer
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Execute Python et attend sa fin (methode bloquante : a appeler
     * uniquement depuis un thread separe, jamais depuis le thread JavaFX).
     *
     * <p>Plusieurs commandes sont essayees (python3, python, py) car le nom
     * de Python change selon le systeme et l'installation.</p>
     *
     * @param fichierConfig le fichier config.txt
     * @param dossierSortie le dossier de sortie
     * @return le code retour et le texte affiche par Python
     */
    public ResultatExecution executer(Path fichierConfig, Path dossierSortie) {
        Path script = Chemins.scriptPython();
        if (!Files.isRegularFile(script)) {
            return ResultatExecution.lancementImpossible(
                    "Programme Python introuvable : " + script
                    + "\nLe dossier " + Constantes.DOSSIER_CODE
                    + " doit etre a cote du dossier " + Constantes.DOSSIER_ENTREES + ".");
        }
        if (!Files.isRegularFile(fichierConfig)) {
            return ResultatExecution.lancementImpossible(
                    "Fichier de configuration introuvable : " + fichierConfig);
        }

        for (String commande : commandesPython()) {
            List<String> arguments = new ArrayList<>();
            arguments.add(commande);
            arguments.add(script.toString());
            arguments.add(fichierConfig.toAbsolutePath().toString());
            arguments.add(dossierSortie.toAbsolutePath().toString());

            ProcessBuilder constructeur = new ProcessBuilder(arguments);
            constructeur.directory(Chemins.racineDonnees().toFile());
            // Sortie d'erreur et sortie standard dans le meme flux
            constructeur.redirectErrorStream(true);
            // Python ecrit en UTF-8 (les accents des messages restent lisibles)
            constructeur.environment().put("PYTHONIOENCODING", "utf-8");
            try {
                Process processus = constructeur.start();
                // On lit TOUT ce qu'affiche Python avant d'attendre sa fin,
                // sinon Python peut se bloquer si le tampon de sortie est plein.
                String sortie = lireTout(processus.getInputStream());
                int code = processus.waitFor();
                return new ResultatExecution(code, sortie);
            } catch (IOException e) {
                // Cette commande n'existe pas sur ce PC : on essaie la suivante
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ResultatExecution.lancementImpossible("Simulation interrompue.");
            }
        }
        return ResultatExecution.lancementImpossible(
                "Python est introuvable. Commandes essayees : "
                + String.join(", ", commandesPython())
                + ".\nInstallez Python 3 et verifiez qu'il est dans le PATH.");
    }

    /** @return les commandes a essayer pour lancer Python sur ce systeme */
    private String[] commandesPython() {
        String systeme = System.getProperty("os.name", "").toLowerCase();
        return systeme.contains("win")
                ? Constantes.COMMANDES_PYTHON_WINDOWS
                : Constantes.COMMANDES_PYTHON_AUTRES;
    }

    /**
     * Lit tout le contenu d'un flux.
     *
     * @param flux le flux a lire
     * @return le texte lu
     * @throws IOException en cas d'erreur de lecture
     */
    private String lireTout(InputStream flux) throws IOException {
        try (InputStream entree = flux; ByteArrayOutputStream octets = new ByteArrayOutputStream()) {
            entree.transferTo(octets);
            return octets.toString(StandardCharsets.UTF_8);
        }
    }
}
