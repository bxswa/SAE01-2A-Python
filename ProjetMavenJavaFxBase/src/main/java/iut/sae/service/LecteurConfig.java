package iut.sae.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iut.sae.modele.Algorithme;
import iut.sae.modele.Configuration;
import iut.sae.modele.ErreurConfiguration;
import iut.sae.util.Chemins;
import iut.sae.util.Constantes;

/**
 * Lit un fichier config.txt existant pour pouvoir le modifier dans
 * l'ecran de configuration.
 *
 * <p>Les chemins des fichiers d'entree, relatifs au dossier du config,
 * sont transformes en chemins absolus.</p>
 */
public class LecteurConfig {

    /**
     * Lit un fichier config.txt.
     *
     * @param fichier le fichier a lire
     * @return la configuration lue
     * @throws IOException          si le fichier ne peut pas etre lu
     * @throws ErreurConfiguration  si le contenu est mal forme
     */
    public Configuration lire(Path fichier) throws IOException, ErreurConfiguration {
        List<String> lignes = Files.readAllLines(fichier, StandardCharsets.UTF_8);
        Map<String, String> valeurs = lirePaires(lignes);
        Path dossier = fichier.toAbsolutePath().getParent();

        Configuration config = new Configuration();
        config.setFichierProcessus(Chemins.absolu(obligatoire(valeurs, Constantes.CLE_PROCESSUS), dossier));
        config.setFichierRessources(Chemins.absolu(obligatoire(valeurs, Constantes.CLE_RESSOURCES), dossier));
        config.setFichierEnergie(Chemins.absolu(cheminEnergie(valeurs), dossier));
        lireAlgorithmes(obligatoire(valeurs, Constantes.CLE_ALGORITHMES), config);
        lireFichiersDeSortie(valeurs, config);
        if (valeurs.containsKey(Constantes.CLE_METRIQUES)) {
            config.setFichierMetriques(valeurs.get(Constantes.CLE_METRIQUES));
        }
        if (valeurs.containsKey(Constantes.CLE_QUANTUM)) {
            config.setQuantum(lireEntier(valeurs.get(Constantes.CLE_QUANTUM)));
        }
        return config;
    }

    /**
     * Range les lignes "cle = valeur" dans une table.
     * Les lignes vides et les commentaires sont ignores.
     *
     * @param lignes les lignes du fichier
     * @return la table cle vers valeur
     * @throws ErreurConfiguration si une ligne n'a pas de '='
     */
    private Map<String, String> lirePaires(List<String> lignes) throws ErreurConfiguration {
        Map<String, String> valeurs = new HashMap<>();
        for (int i = 0; i < lignes.size(); i++) {
            String ligne = lignes.get(i).trim();
            if (ligne.isEmpty() || ligne.startsWith(Constantes.DEBUT_COMMENTAIRE)) {
                continue;
            }
            int position = ligne.indexOf(Constantes.SEPARATEUR_CLE_VALEUR);
            if (position < 0) {
                throw new ErreurConfiguration("Ligne " + (i + 1) + " : signe '=' attendu (" + ligne + ")");
            }
            valeurs.put(ligne.substring(0, position).trim(), ligne.substring(position + 1).trim());
        }
        return valeurs;
    }

    /**
     * Coche les algorithmes de la liste "FIFO,RR,EDF".
     *
     * @param liste  la valeur de la cle algorithmes
     * @param config la configuration a remplir
     * @throws ErreurConfiguration si un algorithme est inconnu
     */
    private void lireAlgorithmes(String liste, Configuration config) throws ErreurConfiguration {
        for (String code : liste.split(Constantes.SEPARATEUR_ALGORITHMES)) {
            if (!code.isBlank()) {
                config.selectionner(Algorithme.depuisCode(code), true);
            }
        }
    }

    /**
     * Recupere les noms des fichiers de sortie presents dans le fichier.
     * Les autres gardent leur nom par defaut.
     *
     * <p>La cle du fichier des metriques d'energie est facultative (les
     * anciens config.txt ne l'ont pas) : si elle manque, on prend le nom
     * que Python utilise dans ce cas, deduit du fichier global.</p>
     *
     * @param valeurs la table cle vers valeur
     * @param config  la configuration a remplir
     */
    private void lireFichiersDeSortie(Map<String, String> valeurs, Configuration config) {
        for (Algorithme a : Algorithme.values()) {
            if (valeurs.containsKey(a.getCleGlobal())) {
                config.setFichierGlobal(a, valeurs.get(a.getCleGlobal()));
            }
            if (valeurs.containsKey(a.getCleAllocations())) {
                config.setFichierAllocations(a, valeurs.get(a.getCleAllocations()));
            }
            if (valeurs.containsKey(a.getCleEnergie())) {
                config.setFichierMetriquesEnergie(a, valeurs.get(a.getCleEnergie()));
            } else {
                config.setFichierMetriquesEnergie(a, Chemins.fichierEnergie(config.getFichierGlobal(a)));
            }
        }
    }

    /**
     * Renvoie le fichier d'energie, sous la cle "energie" ou "fichier_energie".
     *
     * @param valeurs la table cle vers valeur
     * @return le chemin lu, ou energie.csv par defaut (comme Python)
     */
    private String cheminEnergie(Map<String, String> valeurs) {
        if (valeurs.containsKey(Constantes.CLE_ENERGIE)) {
            return valeurs.get(Constantes.CLE_ENERGIE);
        }
        return valeurs.getOrDefault(Constantes.CLE_ENERGIE_ALTERNATIVE, Constantes.FICHIER_ENERGIE_DEFAUT);
    }

    /**
     * Renvoie la valeur d'une cle obligatoire.
     *
     * @param valeurs la table cle vers valeur
     * @param cle     la cle cherchee
     * @return la valeur
     * @throws ErreurConfiguration si la cle est absente ou vide
     */
    private String obligatoire(Map<String, String> valeurs, String cle) throws ErreurConfiguration {
        String valeur = valeurs.get(cle);
        if (valeur == null || valeur.isBlank()) {
            throw new ErreurConfiguration("Cle obligatoire absente : " + cle);
        }
        return valeur;
    }

    /**
     * Convertit un texte en entier.
     *
     * @param texte le texte lu
     * @return l'entier
     * @throws ErreurConfiguration si le texte n'est pas un entier
     */
    private int lireEntier(String texte) throws ErreurConfiguration {
        try {
            return Integer.parseInt(texte.trim());
        } catch (NumberFormatException e) {
            throw new ErreurConfiguration("Le quantum doit etre un entier (lu : " + texte + ")");
        }
    }
}
