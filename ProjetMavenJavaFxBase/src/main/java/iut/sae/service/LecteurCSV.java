package iut.sae.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import iut.sae.util.Constantes;

/**
 * Lecture generique d'un fichier CSV avec une ligne d'en-tete.
 *
 * <p>Chaque ligne est rendue sous forme de table "nom de colonne -&gt;
 * valeur". On peut donc lire une valeur par son nom de colonne, sans
 * dependre de l'ordre des colonnes dans le fichier.</p>
 */
public class LecteurCSV {

    /**
     * Lit un fichier CSV.
     *
     * @param fichier le fichier a lire
     * @return une table par ligne de donnees (les lignes vides sont ignorees)
     * @throws IOException si le fichier est absent, vide, ou si une ligne
     *                     n'a pas le meme nombre de colonnes que l'en-tete
     */
    public List<Map<String, String>> lire(Path fichier) throws IOException {
        if (!Files.isRegularFile(fichier)) {
            throw new IOException("fichier introuvable : " + fichier);
        }
        List<Map<String, String>> lignes = new ArrayList<>();
        try (BufferedReader lecteur = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
            String entete = lecteur.readLine();
            if (entete == null || entete.isBlank()) {
                throw new IOException("fichier vide : " + fichier.getFileName());
            }
            String[] colonnes = decouper(enleverBom(entete));
            String ligne;
            int numero = 1;
            while ((ligne = lecteur.readLine()) != null) {
                numero++;
                if (ligne.isBlank()) {
                    continue;
                }
                String[] valeurs = decouper(ligne);
                if (valeurs.length != colonnes.length) {
                    throw new IOException(fichier.getFileName() + ", ligne " + numero + " : "
                            + valeurs.length + " valeurs au lieu de " + colonnes.length);
                }
                Map<String, String> table = new LinkedHashMap<>();
                for (int i = 0; i < colonnes.length; i++) {
                    table.put(colonnes[i], valeurs[i]);
                }
                lignes.add(table);
            }
        }
        return lignes;
    }

    /**
     * Lit une valeur obligatoire dans une ligne.
     *
     * @param ligne   la ligne (table colonne -&gt; valeur)
     * @param colonne le nom de la colonne
     * @return la valeur (sans espaces autour)
     * @throws IOException si la colonne n'existe pas
     */
    public static String texte(Map<String, String> ligne, String colonne) throws IOException {
        String valeur = ligne.get(colonne);
        if (valeur == null) {
            throw new IOException("colonne manquante : " + colonne);
        }
        return valeur;
    }

    /**
     * Lit un entier ("18" ou "18.0" sont acceptes).
     *
     * @param ligne   la ligne
     * @param colonne le nom de la colonne
     * @return l'entier lu
     * @throws IOException si la valeur n'est pas un nombre
     */
    public static int entier(Map<String, String> ligne, String colonne) throws IOException {
        return (int) Math.round(reel(ligne, colonne));
    }

    /**
     * Lit un entier qui peut etre vide (processus non execute).
     *
     * @param ligne   la ligne
     * @param colonne le nom de la colonne
     * @return l'entier lu, ou null si la case est vide
     * @throws IOException si la valeur n'est ni vide ni un nombre
     */
    public static Integer entierOuNull(Map<String, String> ligne, String colonne) throws IOException {
        if (texte(ligne, colonne).isEmpty()) {
            return null;
        }
        return entier(ligne, colonne);
    }

    /**
     * Lit un nombre a virgule.
     *
     * @param ligne   la ligne
     * @param colonne le nom de la colonne
     * @return le nombre lu
     * @throws IOException si la valeur n'est pas un nombre
     */
    public static double reel(Map<String, String> ligne, String colonne) throws IOException {
        String valeur = texte(ligne, colonne);
        try {
            return Double.parseDouble(valeur);
        } catch (NumberFormatException e) {
            throw new IOException("valeur non numerique dans la colonne " + colonne
                    + " : \"" + valeur + "\"");
        }
    }

    /**
     * Decoupe une ligne au separateur et enleve les espaces autour des valeurs.
     *
     * @param ligne la ligne lue
     * @return les valeurs
     */
    private String[] decouper(String ligne) {
        String[] morceaux = ligne.split(Constantes.SEPARATEUR_CSV, -1);
        for (int i = 0; i < morceaux.length; i++) {
            morceaux[i] = morceaux[i].trim();
        }
        return morceaux;
    }

    /**
     * Enleve le caractere invisible (BOM) que certains editeurs Windows
     * placent au debut des fichiers UTF-8.
     *
     * @param ligne la premiere ligne du fichier
     * @return la ligne sans BOM
     */
    private String enleverBom(String ligne) {
        return ligne.startsWith("﻿") ? ligne.substring(1) : ligne;
    }
}
