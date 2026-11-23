"""
lecture.py

Fonctions de lecture des 4 fichiers d'entree :
- lire_config     : lit config.txt
- lire_ressources : lit ressources.json
- lire_processus  : lit processus.csv
- lire_energie    : lit energie.csv

Chaque fonction renvoie des donnees simples (dict, list, objet leger)
et leve une exception explicite si le format est invalide.
"""
import csv
import json
import os

from erreurs import ErreurLecture, ErreurFormat


# =========================================================
# Structures de donnees renvoyees par les lecteurs.
# Elles sont locales a la partie lecture/ecriture.
# Le groupe algo peut les utiliser telles quelles ou les copier.
# =========================================================

class Processus:
    """Un processus lu depuis processus.csv.

    Attributs :
        id_processus    : identifiant unique (str)
        date_soumission : entier >= 0
        duree_execution : entier > 0
        ram_necessaire  : entier > 0
        priorite        : entier
        date_butoir     : entier >= date_soumission
    """

    def __init__(self, id_processus, date_soumission, duree_execution,
                 ram_necessaire, priorite, date_butoir):
        self.id_processus = id_processus
        self.date_soumission = date_soumission
        self.duree_execution = duree_execution
        self.ram_necessaire = ram_necessaire
        self.priorite = priorite
        self.date_butoir = date_butoir

    def __repr__(self):
        return ("Processus(%s, soumis=%d, duree=%d, ram=%d, prio=%d, deadline=%d)"
                % (self.id_processus, self.date_soumission, self.duree_execution,
                   self.ram_necessaire, self.priorite, self.date_butoir))


class Ressources:
    """Ressources lues depuis ressources.json."""

    def __init__(self, nb_processeurs, ram_totale, energie_par_proc, processeurs):
        self.nb_processeurs = nb_processeurs
        self.ram_totale = ram_totale
        self.energie_par_proc = energie_par_proc
        self.processeurs = processeurs

    def __repr__(self):
        return ("Ressources(%d CPU, %d RAM, %d energie/CPU, %s)"
                % (self.nb_processeurs, self.ram_totale,
                   self.energie_par_proc, self.processeurs))


# =========================================================
# 1) config.txt
# =========================================================

# Association nom d'algo -> cle exacte dans config.txt.
# Ne pas utiliser de formule automatique, car les cles sont irregulieres
# (fichier_global_FIFO mais fichier_global_RoundRobin).
CLE_GLOBAL = {
    "FIFO": "fichier_global_FIFO",
    "RR":   "fichier_global_RoundRobin",
    "EDF":  "fichier_global_EDF",
}
CLE_ALLOC = {
    "FIFO": "fichier_allocations_FIFO",
    "RR":   "fichier_allocations_RoundRobin",
    "EDF":  "fichier_allocations_EDF",
}
# Fichier des metriques d'energie (energie disponible / consommee a chaque
# instant). Cle facultative : si elle est absente, le nom est deduit du
# fichier global (voir nom_fichier_energie).
CLE_ENERGIE = {
    "FIFO": "fichier_metriques_energie_FIFO",
    "RR":   "fichier_metriques_energie_RoundRobin",
    "EDF":  "fichier_metriques_energie_EDF",
}


def nom_fichier_energie(fichier_global):
    """Nom par defaut du fichier d'energie d'un algorithme :
    resultats_fifo_global.csv -> resultats_fifo_energie.csv"""
    if "global" in fichier_global:
        return fichier_global.replace("global", "energie")
    return fichier_global.replace(".csv", "") + "_energie.csv"


def lire_config(chemin):
    """Lit config.txt et renvoie un dictionnaire.

    Structure renvoyee :
        {
            "fichier_processus":  str,
            "fichier_ressources": str,
            "fichier_energie":    str,
            "algorithmes":        [str, ...],
            "quantum":            int ou None,
            "fichier_metriques":  str,
            "sorties": {
                "FIFO": {"global": str, "allocations": str, "energie": str},
                ...
            }
        }
    """
    if not os.path.isfile(chemin):
        raise ErreurLecture("Fichier de configuration introuvable : " + chemin)

    # Lecture brute : on garde toutes les paires cle = valeur
    brut = {}
    try:
        with open(chemin, "r", encoding="utf-8") as f:
            for numero, ligne in enumerate(f, start=1):
                ligne = ligne.strip()
                # On ignore les lignes vides et les commentaires
                if not ligne or ligne.startswith("#"):
                    continue
                if "=" not in ligne:
                    raise ErreurFormat(
                        "%s, ligne %d : signe '=' attendu, contenu : %r"
                        % (chemin, numero, ligne)
                    )
                cle, valeur = ligne.split("=", 1)
                brut[cle.strip()] = valeur.strip()
    except UnicodeDecodeError as e:
        raise ErreurFormat("%s : encodage invalide (%s)" % (chemin, e))

    # Verification des cles obligatoires
    for cle in ("fichier_processus", "fichier_ressources",
                "algorithmes", "fichier_metriques"):
        if cle not in brut or brut[cle] == "":
            raise ErreurFormat("Cle obligatoire absente ou vide : " + cle)

    # Liste des algorithmes
    algos = [a.strip().upper() for a in brut["algorithmes"].split(",") if a.strip()]
    if not algos:
        raise ErreurFormat("Liste d'algorithmes vide.")

    connus = set(CLE_GLOBAL.keys())
    inconnus = [a for a in algos if a not in connus]
    if inconnus:
        raise ErreurFormat(
            "Algorithme(s) inconnu(s) : %s. Attendus : %s"
            % (inconnus, sorted(connus))
        )

    # Quantum : obligatoire seulement si RR est demande
    quantum = None
    if "quantum" in brut and brut["quantum"] != "":
        try:
            quantum = int(brut["quantum"])
        except ValueError:
            raise ErreurFormat("Quantum non entier : %r" % brut["quantum"])
        if quantum <= 0:
            raise ErreurFormat("Quantum doit etre > 0 (recu %d)" % quantum)

    if "RR" in algos and quantum is None:
        raise ErreurFormat("Algorithme RR demande mais 'quantum' absent du config.")

    # Fichiers de sortie de chaque algo
    sorties = {}
    for algo in algos:
        cle_g = CLE_GLOBAL[algo]
        cle_a = CLE_ALLOC[algo]
        if cle_g not in brut or brut[cle_g] == "":
            raise ErreurFormat(
                "Cle de sortie manquante pour %s : %s" % (algo, cle_g)
            )
        if cle_a not in brut or brut[cle_a] == "":
            raise ErreurFormat(
                "Cle de sortie manquante pour %s : %s" % (algo, cle_a)
            )
        sorties[algo] = {
            "global": brut[cle_g],
            "allocations": brut[cle_a],
            # Facultatif (les anciens config.txt ne l'ont pas)
            "energie": brut.get(CLE_ENERGIE[algo]) or nom_fichier_energie(brut[cle_g]),
        }

    # Fichier d'energie : le config fourni utilise la cle "energie",
    # on accepte aussi "fichier_energie". Par defaut : energie.csv
    fichier_energie = brut.get("energie") or brut.get("fichier_energie") or "energie.csv"

    return {
        "fichier_processus": brut["fichier_processus"],
        "fichier_ressources": brut["fichier_ressources"],
        "fichier_energie": fichier_energie,
        "algorithmes": algos,
        "quantum": quantum,
        "fichier_metriques": brut["fichier_metriques"],
        "sorties": sorties,
    }


# =========================================================
# 2) ressources.json
# =========================================================

def lire_ressources(chemin):
    """Lit ressources.json et renvoie un objet Ressources."""
    if not os.path.isfile(chemin):
        raise ErreurLecture("Fichier de ressources introuvable : " + chemin)

    try:
        with open(chemin, "r", encoding="utf-8") as f:
            data = json.load(f)
    except json.JSONDecodeError as e:
        raise ErreurFormat("%s : JSON invalide (%s)" % (chemin, e))
    except UnicodeDecodeError as e:
        raise ErreurFormat("%s : encodage invalide (%s)" % (chemin, e))

    if not isinstance(data, dict):
        raise ErreurFormat(
            "%s : objet JSON attendu, recu %s" % (chemin, type(data).__name__)
        )

    for cle in ("nbProcesseurs", "ramTotale", "energieParProc", "processeur"):
        if cle not in data:
            raise ErreurFormat("%s : cle manquante %r" % (chemin, cle))

    try:
        nb_proc = int(data["nbProcesseurs"])
        ram = int(data["ramTotale"])
        energie = int(data["energieParProc"])
    except (TypeError, ValueError):
        raise ErreurFormat("%s : valeurs numeriques invalides" % chemin)

    processeurs = data["processeur"]
    if not isinstance(processeurs, list) or not all(isinstance(p, str) for p in processeurs):
        raise ErreurFormat(
            "%s : 'processeur' doit etre une liste de chaines" % chemin
        )

    if nb_proc <= 0:
        raise ErreurFormat("%s : nbProcesseurs doit etre > 0" % chemin)
    if ram <= 0:
        raise ErreurFormat("%s : ramTotale doit etre > 0" % chemin)
    if energie <= 0:
        raise ErreurFormat("%s : energieParProc doit etre > 0" % chemin)
    if len(processeurs) != nb_proc:
        raise ErreurFormat(
            "%s : nbProcesseurs=%d mais %d processeurs listes"
            % (chemin, nb_proc, len(processeurs))
        )
    if len(set(processeurs)) != len(processeurs):
        raise ErreurFormat(
            "%s : identifiants de processeurs en doublon" % chemin
        )

    return Ressources(nb_proc, ram, energie, processeurs)


# =========================================================
# 3) processus.csv
# =========================================================

COLONNES_PROCESSUS = (
    "id_processus", "date_soumission", "duree_execution",
    "ram_necessaire", "priorite", "date_butoir",
)


def lire_processus(chemin):
    """Lit processus.csv et renvoie une liste d'objets Processus."""
    if not os.path.isfile(chemin):
        raise ErreurLecture("Fichier de processus introuvable : " + chemin)

    processus = []
    ids_vus = set()

    try:
        with open(chemin, "r", encoding="utf-8", newline="") as f:
            reader = csv.DictReader(f)

            if reader.fieldnames is None:
                raise ErreurFormat(
                    "%s : fichier vide, aucune en-tete" % chemin
                )

            manquantes = [c for c in COLONNES_PROCESSUS if c not in reader.fieldnames]
            if manquantes:
                raise ErreurFormat(
                    "%s : colonne(s) manquante(s) %s. Attendu : %s"
                    % (chemin, manquantes, list(COLONNES_PROCESSUS))
                )

            for numero, ligne in enumerate(reader, start=2):
                try:
                    pid = ligne["id_processus"].strip()
                    ds = int(ligne["date_soumission"])
                    duree = int(ligne["duree_execution"])
                    ram = int(ligne["ram_necessaire"])
                    prio = int(ligne["priorite"])
                    deadline = int(ligne["date_butoir"])
                except (KeyError, ValueError, AttributeError) as e:
                    raise ErreurFormat(
                        "%s, ligne %d : %s" % (chemin, numero, e)
                    )

                if not pid:
                    raise ErreurFormat(
                        "%s, ligne %d : identifiant vide" % (chemin, numero)
                    )
                if pid in ids_vus:
                    raise ErreurFormat(
                        "%s, ligne %d : identifiant en doublon %r"
                        % (chemin, numero, pid)
                    )
                ids_vus.add(pid)

                if ds < 0:
                    raise ErreurFormat(
                        "%s, ligne %d : date_soumission negative"
                        % (chemin, numero)
                    )
                if duree <= 0:
                    raise ErreurFormat(
                        "%s, ligne %d : duree_execution <= 0"
                        % (chemin, numero)
                    )
                if ram <= 0:
                    raise ErreurFormat(
                        "%s, ligne %d : ram_necessaire <= 0"
                        % (chemin, numero)
                    )
                if deadline < ds:
                    raise ErreurFormat(
                        "%s, ligne %d : date_butoir (%d) inferieure a "
                        "date_soumission (%d)" % (chemin, numero, deadline, ds)
                    )

                processus.append(Processus(pid, ds, duree, ram, prio, deadline))
    except UnicodeDecodeError as e:
        raise ErreurFormat("%s : encodage invalide (%s)" % (chemin, e))

    if not processus:
        raise ErreurFormat("%s : aucun processus lu" % chemin)

    return processus


# =========================================================
# 4) energie.csv
# =========================================================

def lire_energie(chemin):
    """Lit energie.csv et renvoie un dictionnaire {instant: energie}."""
    if not os.path.isfile(chemin):
        raise ErreurLecture("Fichier d'energie introuvable : " + chemin)

    energie = {}

    try:
        with open(chemin, "r", encoding="utf-8", newline="") as f:
            reader = csv.DictReader(f)

            if reader.fieldnames is None:
                raise ErreurFormat(
                    "%s : fichier vide, aucune en-tete" % chemin
                )

            for col in ("instant", "energie_disponible"):
                if col not in reader.fieldnames:
                    raise ErreurFormat(
                        "%s : colonne manquante %r. Attendu : "
                        "['instant', 'energie_disponible']" % (chemin, col)
                    )

            for numero, ligne in enumerate(reader, start=2):
                try:
                    instant = int(ligne["instant"])
                    valeur = int(ligne["energie_disponible"])
                except (KeyError, ValueError, AttributeError) as e:
                    raise ErreurFormat(
                        "%s, ligne %d : %s" % (chemin, numero, e)
                    )

                if instant < 0:
                    raise ErreurFormat(
                        "%s, ligne %d : instant negatif" % (chemin, numero)
                    )
                if valeur < 0:
                    raise ErreurFormat(
                        "%s, ligne %d : energie negative" % (chemin, numero)
                    )
                if instant in energie:
                    raise ErreurFormat(
                        "%s, ligne %d : instant %d en doublon"
                        % (chemin, numero, instant)
                    )
                energie[instant] = valeur
    except UnicodeDecodeError as e:
        raise ErreurFormat("%s : encodage invalide (%s)" % (chemin, e))

    if not energie:
        raise ErreurFormat("%s : aucune donnee d'energie" % chemin)

    return energie