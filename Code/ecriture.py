"""
ecriture.py

Fonctions d'ecriture des 4 fichiers de sortie :
- ecrire_global      : une ligne par processus
- ecrire_allocations : une ligne par segment CPU
- ecrire_metriques   : une ligne par algorithme
- ecrire_energie     : une ligne par instant (metriques d'energie)

Ces fonctions recoivent des dictionnaires deja remplis. Elles ne
calculent rien, elles ecrivent uniquement.

Les dictionnaires attendus ont la forme :
- ligne globale      : {"id_processus", "date_soumission", "date_debut",
                        "date_fin", "ram_demandee", "ram_utilisee"}
- ligne allocation   : {"id_processus", "date_debut", "date_fin",
                        "id_processeur"}
- ligne metrique     : {"algorithme", "temps_attente_moyen",
                        "temps_reponse_moyen", "makespan",
                        "energie_consommee_totale",
                        "energie_disponible_totale", "taux_utilisation"}
"""
import csv
import os

from erreurs import ErreurEcriture


COLONNES_GLOBAL = (
    "id_processus", "date_soumission", "date_debut", "date_fin",
    "ram_demandee", "ram_utilisee",
)
COLONNES_ALLOC = (
    "id_processus", "date_debut", "date_fin", "id_processeur",
)
COLONNES_METRIQUES = (
    "algorithme", "temps_attente_moyen", "temps_reponse_moyen", "makespan",
    "energie_consommee_totale", "energie_disponible_totale", "taux_utilisation",
)


def preparer_dossier(chemin):
    """Cree le dossier parent du fichier s'il n'existe pas deja."""
    dossier = os.path.dirname(os.path.abspath(chemin))
    try:
        os.makedirs(dossier, exist_ok=True)
    except OSError as e:
        raise ErreurEcriture(
            "Impossible de creer le dossier %r : %s" % (dossier, e)
        )


def _ecrire_csv(chemin, colonnes, lignes, tri):
    """Fonction utilitaire pour ecrire un fichier CSV a partir d'une liste
    de dictionnaires. Le parametre tri est une fonction cle pour trier
    les lignes avant ecriture."""
    if not lignes:
        raise ErreurEcriture("Aucune ligne a ecrire dans " + chemin)

    preparer_dossier(chemin)
    try:
        with open(chemin, "w", encoding="utf-8", newline="") as f:
            writer = csv.DictWriter(f, fieldnames=colonnes)
            writer.writeheader()
            for ligne in sorted(lignes, key=tri):
                # On ne garde que les colonnes attendues
                writer.writerow({c: ligne[c] for c in colonnes})
    except OSError as e:
        raise ErreurEcriture("Ecriture impossible dans %s : %s" % (chemin, e))


def ecrire_global(chemin, lignes):
    """Ecrit le fichier global. lignes = liste de dicts."""
    _ecrire_csv(chemin, COLONNES_GLOBAL, lignes,
                tri=lambda x: x["id_processus"])


def ecrire_allocations(chemin, lignes):
    """Ecrit le fichier des allocations. lignes = liste de dicts."""
    _ecrire_csv(
        chemin, COLONNES_ALLOC, lignes,
        tri=lambda x: (x["date_debut"], x["id_processeur"], x["id_processus"]),
    )


def ecrire_metriques(chemin, lignes):
    """Ecrit le fichier des metriques. lignes = liste de dicts."""
    _ecrire_csv(chemin, COLONNES_METRIQUES, lignes,
                tri=lambda x: x["algorithme"])


COLONNES_ENERGIE = (
    "instant", "energie_disponible", "energie_consommee", "nb_cpu_actifs",
)


def ecrire_energie(chemin, lignes):
    """Ecrit le fichier des metriques d'energie d'un algorithme : energie
    disponible / consommee a chaque instant. Son nom est choisi dans
    config.txt (cles fichier_metriques_energie_...). lignes = liste de dicts."""
    _ecrire_csv(chemin, COLONNES_ENERGIE, lignes,
                tri=lambda x: x["instant"])