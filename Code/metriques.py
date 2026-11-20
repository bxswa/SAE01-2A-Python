"""
metriques.py

Calcul des metriques d'un algorithme, a partir d'un Resultat (simulateur.py).

Definitions (en unites de temps) :
  - temps de reponse d'un processus = date_fin - date_soumission
  - temps d'attente d'un processus  = temps de reponse - duree_execution
        (temps passe dans le systeme sans tourner, pauses comprises)
  - makespan = date de fin du dernier processus
  - energie consommee = somme sur chaque instant de (nb CPU actifs x energie par CPU)
  - energie disponible = somme de l'energie verte entre 0 et le makespan
  - taux d'utilisation = energie consommee / energie disponible

Les processus rejetes (trop de RAM) ne comptent pas dans les moyennes.
"""


def calculer_metriques(resultat):
    """Renvoie un dictionnaire au format attendu par ecrire_metriques."""
    total_attente = 0
    total_reponse = 0
    nb = 0
    makespan = 0

    for etat in resultat.etats:
        if etat.date_fin is None:          # processus rejete
            continue
        reponse = etat.date_fin - etat.proc.date_soumission
        attente = reponse - etat.proc.duree_execution
        total_reponse = total_reponse + reponse
        total_attente = total_attente + attente
        nb = nb + 1
        if etat.date_fin > makespan:
            makespan = etat.date_fin

    if nb > 0:
        attente_moyenne = total_attente / nb
        reponse_moyenne = total_reponse / nb
    else:
        attente_moyenne = 0
        reponse_moyenne = 0

    # On ne compte que les instants 0 a makespan-1
    energie_conso = sum(resultat.energie_conso[:makespan])
    energie_dispo = sum(resultat.energie_dispo[:makespan])
    if energie_dispo > 0:
        taux = energie_conso / energie_dispo
    else:
        taux = 0

    return {
        "algorithme": resultat.algorithme,
        "temps_attente_moyen": round(attente_moyenne, 2),
        "temps_reponse_moyen": round(reponse_moyenne, 2),
        "makespan": makespan,
        "energie_consommee_totale": energie_conso,
        "energie_disponible_totale": energie_dispo,
        "taux_utilisation": round(taux, 4),
    }
