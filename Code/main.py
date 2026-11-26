"""
main.py

Programme principal, a lancer en ligne de commande (depuis SAE_Python) :

    python3 Code/main.py Inputs/Test1/config.txt
    python3 Code/main.py Inputs/Test1/config.txt Outputs/Test1

- 1er argument : le fichier config.txt
- 2e argument (facultatif) : le dossier ou ecrire les resultats.
  Par defaut : le dossier "Outputs/..." qui correspond a "Inputs/...".

Les chemins ecrits dans config.txt sont relatifs au dossier du config
(pour les entrees) et au dossier de sortie (pour les resultats).

Code retour : 0 si tout va bien, 1 en cas d'erreur.
"""
import os
import sys

from erreurs import ErreurLecture, ErreurFormat, ErreurEcriture
from lecture import lire_config, lire_ressources, lire_processus, lire_energie
from ecriture import ecrire_global, ecrire_allocations, ecrire_metriques, ecrire_energie
from simulateur import simuler, lignes_global, lignes_allocations, lignes_energie
from metriques import calculer_metriques


def chemin_complet(chemin, dossier):
    """Un chemin relatif est pris par rapport au dossier donne."""
    if os.path.isabs(chemin):
        return chemin
    return os.path.join(dossier, chemin)


def dossier_sortie_par_defaut(chemin_config):
    """Inputs/Test1/config.txt -> Outputs/Test1"""
    dossier = os.path.dirname(os.path.abspath(chemin_config))
    parent, nom_test = os.path.split(dossier)
    racine, nom_parent = os.path.split(parent)
    if nom_parent == "Inputs":
        return os.path.join(racine, "Outputs", nom_test)
    return dossier


def executer(chemin_config, dossier_sortie=None):
    config = lire_config(chemin_config)
    dossier_config = os.path.dirname(os.path.abspath(chemin_config))
    if dossier_sortie is None:
        dossier_sortie = dossier_sortie_par_defaut(chemin_config)

    ressources = lire_ressources(chemin_complet(config["fichier_ressources"], dossier_config))
    processus = lire_processus(chemin_complet(config["fichier_processus"], dossier_config))
    energie = lire_energie(chemin_complet(config["fichier_energie"], dossier_config))

    toutes_metriques = []
    for algo in config["algorithmes"]:
        print("Simulation %s..." % algo)
        resultat = simuler(processus, ressources, energie, algo, config["quantum"])

        sortie_global = chemin_complet(config["sorties"][algo]["global"], dossier_sortie)
        sortie_alloc = chemin_complet(config["sorties"][algo]["allocations"], dossier_sortie)
        sortie_energie = chemin_complet(config["sorties"][algo]["energie"], dossier_sortie)
        ecrire_global(sortie_global, lignes_global(resultat))
        ecrire_allocations(sortie_alloc, lignes_allocations(resultat))
        ecrire_energie(sortie_energie, lignes_energie(resultat))

        metriques = calculer_metriques(resultat)
        toutes_metriques.append(metriques)
        print("  attente moyenne = %s, reponse moyenne = %s, makespan = %s, taux energie = %s"
              % (metriques["temps_attente_moyen"], metriques["temps_reponse_moyen"],
                 metriques["makespan"], metriques["taux_utilisation"]))

    ecrire_metriques(chemin_complet(config["fichier_metriques"], dossier_sortie),
                     toutes_metriques)
    print("Resultats ecrits dans %s" % dossier_sortie)


def main():
    if len(sys.argv) < 2:
        print("Utilisation : python3 Code/main.py <config.txt> [dossier_sortie]",
              file=sys.stderr)
        return 1
    chemin_config = sys.argv[1]
    dossier_sortie = sys.argv[2] if len(sys.argv) > 2 else None
    try:
        executer(chemin_config, dossier_sortie)
    except (ErreurLecture, ErreurFormat, ErreurEcriture) as e:
        print("Erreur : %s" % e, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
