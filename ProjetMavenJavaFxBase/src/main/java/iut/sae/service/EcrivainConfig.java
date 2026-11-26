package iut.sae.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import iut.sae.modele.Algorithme;
import iut.sae.modele.Configuration;
import iut.sae.util.Chemins;
import iut.sae.util.Constantes;

/**
 * Ecrit un objet {@link Configuration} dans un fichier config.txt, au
 * format lu par le programme Python (lecture.py).
 *
 * <p>Les fichiers d'entree sont ecrits relativement au dossier du
 * config.txt : Python les retrouve ainsi quel que soit le dossier depuis
 * lequel il est lance. Les fichiers de sortie sont de simples noms :
 * Python les place dans le dossier de resultats.</p>
 */
public class EcrivainConfig {

    /**
     * Produit le texte complet du fichier config.txt.
     * Sert aussi a l'apercu affiche dans l'ecran de configuration.
     *
     * @param config        la configuration a ecrire
     * @param dossierConfig le dossier ou sera enregistre config.txt
     * @return le texte du fichier
     */
    public String genererTexte(Configuration config, Path dossierConfig) {
        StringBuilder texte = new StringBuilder();
        texte.append(Constantes.DEBUT_COMMENTAIRE)
             .append(" Configuration de l'ordonnanceur (ecrit par Java)\n");
        ajouterLigne(texte, Constantes.CLE_PROCESSUS, Chemins.relatif(config.getFichierProcessus(), dossierConfig));
        ajouterLigne(texte, Constantes.CLE_RESSOURCES, Chemins.relatif(config.getFichierRessources(), dossierConfig));
        ajouterLigne(texte, Constantes.CLE_ENERGIE, Chemins.relatif(config.getFichierEnergie(), dossierConfig));
        ajouterLigne(texte, Constantes.CLE_ALGORITHMES, listeDesCodes(config));
        ajouterFichiersDeSortie(texte, config);
        ajouterLigne(texte, Constantes.CLE_METRIQUES, config.getFichierMetriques());
        if (config.estSelectionne(Algorithme.ROUND_ROBIN)) {
            ajouterLigne(texte, Constantes.CLE_QUANTUM, String.valueOf(config.getQuantum()));
        }
        return texte.toString();
    }

    /**
     * Enregistre la configuration dans un fichier (encodage UTF-8).
     * Le dossier est cree s'il n'existe pas.
     *
     * @param config  la configuration a ecrire
     * @param fichier le fichier config.txt a creer ou remplacer
     * @throws IOException si l'ecriture est impossible
     */
    public void ecrire(Configuration config, Path fichier) throws IOException {
        Path dossier = fichier.toAbsolutePath().getParent();
        Files.createDirectories(dossier);
        Files.writeString(fichier, genererTexte(config, dossier), StandardCharsets.UTF_8);
    }

    /**
     * Ajoute les trois fichiers de sortie de chaque algorithme choisi
     * (global, allocations, metriques d'energie).
     *
     * @param texte  le texte en cours de construction
     * @param config la configuration
     */
    private void ajouterFichiersDeSortie(StringBuilder texte, Configuration config) {
        for (Algorithme a : config.getAlgorithmes()) {
            ajouterLigne(texte, a.getCleGlobal(), config.getFichierGlobal(a));
            ajouterLigne(texte, a.getCleAllocations(), config.getFichierAllocations(a));
            ajouterLigne(texte, a.getCleEnergie(), config.getFichierMetriquesEnergie(a));
        }
    }

    /**
     * Construit la liste des codes des algorithmes choisis, par exemple "FIFO,RR,EDF".
     *
     * @param config la configuration
     * @return les codes separes par des virgules
     */
    private String listeDesCodes(Configuration config) {
        StringBuilder liste = new StringBuilder();
        for (Algorithme a : config.getAlgorithmes()) {
            if (liste.length() > 0) {
                liste.append(Constantes.SEPARATEUR_ALGORITHMES);
            }
            liste.append(a.getCode());
        }
        return liste.toString();
    }

    /**
     * Ajoute une ligne "cle = valeur".
     *
     * @param texte  le texte en cours de construction
     * @param cle    la cle
     * @param valeur la valeur
     */
    private void ajouterLigne(StringBuilder texte, String cle, String valeur) {
        texte.append(cle).append(' ').append(Constantes.SEPARATEUR_CLE_VALEUR)
             .append(' ').append(valeur).append('\n');
    }
}
