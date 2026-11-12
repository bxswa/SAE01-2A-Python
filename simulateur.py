import sys
import csv
import json
import os
from typing import Dict, List, Optional

# =========================================================
# ERREURS
# =========================================================

class ErreurLecture(Exception):
    """Probleme lors de la lecture d'un fichier d'entree."""
    pass

class ErreurFormat(Exception):
    """Le contenu d'un fichier ne respecte pas le format attendu."""
    pass

class ErreurEcriture(Exception):
    """Probleme lors de l'ecriture d'un fichier de sortie."""
    pass


# =========================================================
# STRUCTURES DE DONNEES
# =========================================================

class Processus:
    def __init__(self, id_processus, date_soumission, duree_execution,
                 ram_necessaire, priorite, date_butoir):
        self.id_processus = id_processus
        self.date_soumission = date_soumission
        self.duree_execution = duree_execution
        self.ram_necessaire = ram_necessaire
        self.priorite = priorite
        self.date_butoir = date_butoir

class Ressources:
    def __init__(self, nb_processeurs, ram_totale, energie_par_proc, processeurs):
        self.nb_processeurs = nb_processeurs
        self.ram_totale = ram_totale
        self.energie_par_proc = energie_par_proc
        self.processeurs = processeurs


# =========================================================
# CONSTANTES
# =========================================================

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
COLONNES_PROCESSUS = (
    "id_processus", "date_soumission", "duree_execution",
    "ram_necessaire", "priorite", "date_butoir",
)


# =========================================================
# LECTURE
# =========================================================

def lire_config(chemin):
    if not os.path.isfile(chemin):
        raise ErreurLecture("Fichier de configuration introuvable : " + chemin)
    brut = {}
    try:
        with open(chemin, "r", encoding="utf-8") as f:
            for numero, ligne in enumerate(f, start=1):
                ligne = ligne.strip()
                if not ligne or ligne.startswith("#"): continue
                if "=" not in ligne:
                    raise ErreurFormat(f"{chemin}, ligne {numero} : signe '=' attendu.")
                cle, valeur = ligne.split("=", 1)
                brut[cle.strip()] = valeur.strip()
    except UnicodeDecodeError as e:
        raise ErreurFormat(f"{chemin} : encodage invalide ({e})")

    for cle in ("fichier_processus", "fichier_ressources", "algorithmes", "fichier_metriques"):
        if cle not in brut or brut[cle] == "":
            raise ErreurFormat("Cle obligatoire absente ou vide : " + cle)

    algos = [a.strip().upper() for a in brut["algorithmes"].split(",") if a.strip()]
    if not algos: raise ErreurFormat("Liste d'algorithmes vide.")
    
    connus = set(CLE_GLOBAL.keys())
    inconnus = [a for a in algos if a not in connus]
    if inconnus: raise ErreurFormat(f"Algorithme(s) inconnu(s) : {inconnus}")

    quantum = None
    if "quantum" in brut and brut["quantum"] != "":
        try: quantum = int(brut["quantum"])
        except ValueError: raise ErreurFormat("Quantum non entier")
        if quantum <= 0: raise ErreurFormat("Quantum doit etre > 0")

    if "RR" in algos and quantum is None:
        raise ErreurFormat("Algorithme RR demande mais 'quantum' absent.")

    sorties = {}
    for algo in algos:
        cle_g = CLE_GLOBAL[algo]
        cle_a = CLE_ALLOC[algo]
        if cle_g not in brut or brut[cle_g] == "":
            raise ErreurFormat(f"Cle de sortie manquante pour {algo} : {cle_g}")
        if cle_a not in brut or brut[cle_a] == "":
            raise ErreurFormat(f"Cle de sortie manquante pour {algo} : {cle_a}")
        sorties[algo] = {"global": brut[cle_g], "allocations": brut[cle_a]}

    return {
        "fichier_processus": brut["fichier_processus"],
        "fichier_ressources": brut["fichier_ressources"],
        "fichier_energie": brut.get("fichier_energie", "energie.csv"),
        "algorithmes": algos,
        "quantum": quantum,
        "fichier_metriques": brut["fichier_metriques"],
        "sorties": sorties,
    }

def lire_ressources(chemin):
    if not os.path.isfile(chemin):
        raise ErreurLecture("Fichier de ressources introuvable : " + chemin)
    try:
        with open(chemin, "r", encoding="utf-8") as f: data = json.load(f)
    except Exception as e:
        raise ErreurFormat(f"{chemin} : JSON invalide ({e})")
    
    nb_proc = int(data["nbProcesseurs"])
    ram = int(data["ramTotale"])
    energie = int(data["energieParProc"])
    processeurs = data["processeur"]
    return Ressources(nb_proc, ram, energie, processeurs)

def lire_processus(chemin):
    if not os.path.isfile(chemin):
        raise ErreurLecture("Fichier de processus introuvable : " + chemin)
    processus = []
    ids_vus = set()
    try:
        with open(chemin, "r", encoding="utf-8", newline="") as f:
            reader = csv.DictReader(f)
            for ligne in reader:
                pid = ligne["id_processus"].strip()
                if pid in ids_vus: raise ErreurFormat("ID en doublon")
                ids_vus.add(pid)
                processus.append(Processus(
                    pid, int(ligne["date_soumission"]), int(ligne["duree_execution"]),
                    int(ligne["ram_necessaire"]), int(ligne["priorite"]), int(ligne["date_butoir"])
                ))
    except UnicodeDecodeError as e:
        raise ErreurFormat(f"{chemin} : encodage invalide ({e})")
    return processus

def lire_energie(chemin):
    if not os.path.isfile(chemin):
        raise ErreurLecture("Fichier d'energie introuvable : " + chemin)
    energie = {}
    try:
        with open(chemin, "r", encoding="utf-8", newline="") as f:
            reader = csv.DictReader(f)
            for ligne in reader:
                energie[int(ligne["instant"])] = int(ligne["energie_disponible"])
    except UnicodeDecodeError as e:
        raise ErreurFormat(f"{chemin} : encodage invalide ({e})")
    return energie


# =========================================================
# ECRITURE
# =========================================================

def preparer_dossier(chemin):
    dossier = os.path.dirname(os.path.abspath(chemin))
    try: os.makedirs(dossier, exist_ok=True)
    except OSError as e: raise ErreurEcriture(f"Impossible de creer le dossier : {e}")

def _ecrire_csv(chemin, colonnes, lignes, tri):
    if not lignes: raise ErreurEcriture("Aucune ligne a ecrire dans " + chemin)
    preparer_dossier(chemin)
    try:
        with open(chemin, "w", encoding="utf-8", newline="") as f:
            writer = csv.DictWriter(f, fieldnames=colonnes)
            writer.writeheader()
            for ligne in sorted(lignes, key=tri):
                writer.writerow({c: ligne[c] for c in colonnes})
    except OSError as e:
        raise ErreurEcriture(f"Ecriture impossible dans {chemin} : {e}")

def ecrire_global(chemin, lignes):
    _ecrire_csv(chemin, COLONNES_GLOBAL, lignes, tri=lambda x: x["id_processus"])

def ecrire_allocations(chemin, lignes):
    _ecrire_csv(chemin, COLONNES_ALLOC, lignes, tri=lambda x: (x["date_debut"], x["id_processeur"], x["id_processus"]))

def ecrire_metriques(chemin, lignes):
    _ecrire_csv(chemin, COLONNES_METRIQUES, lignes, tri=lambda x: x["algorithme"])


# =========================================================
# ALGORITHMES
# =========================================================

class EtatProcessus:
    def __init__(self, proc: Processus):
        self.proc = proc
        self.restant = proc.duree_execution
        self.date_debut: Optional[int] = None
        self.date_fin: Optional[int] = None

def ordonnancer_edf(processus_liste: List[Processus], ressources: Ressources, energie_dispo: Dict[int, int]):
    etats = [EtatProcessus(p) for p in processus_liste]
    nb_total = len(etats)
    termines = 0
    t = 0
    alloc_en_cours: Dict[str, Optional[tuple]] = {cpu: None for cpu in ressources.processeurs}
    lignes_allocations = []
    energie_consommee_totale = 0
    energie_disponible_totale = 0
    temps_cpu_occupe = 0

    for e in etats:
        if e.proc.ram_necessaire > ressources.ram_totale:
            raise ErreurFormat(f"Processus {e.proc.id_processus} demande trop de RAM.")

    max_instant_energie = max(energie_dispo.keys()) if energie_dispo else 0

    while termines < nb_total:
        e_t = energie_dispo.get(t, energie_dispo.get(max_instant_energie, 0))
        energie_disponible_totale += e_t
        nb_cpus_autorises = min(ressources.nb_processeurs, e_t // ressources.energie_par_proc)

        prets = [e for e in etats if e.proc.date_soumission <= t and e.restant > 0]
        prets.sort(key=lambda e: (e.proc.date_butoir, e.proc.priorite, e.proc.date_soumission, e.proc.id_processus))

        selectionnes: List[EtatProcessus] = []
        ram_utilisee_t = 0
        for e in prets:
            if len(selectionnes) >= nb_cpus_autorises: break
            if ram_utilisee_t + e.proc.ram_necessaire <= ressources.ram_totale:
                selectionnes.append(e)
                ram_utilisee_t += e.proc.ram_necessaire

        affectation_t: Dict[str, Optional[EtatProcessus]] = {cpu: None for cpu in ressources.processeurs}
        restants_a_placer = []
        for e in selectionnes:
            place = False
            for cpu, en_cours in alloc_en_cours.items():
                if en_cours is not None and en_cours[0] == e.proc.id_processus:
                    affectation_t[cpu] = e
                    place = True
                    break
            if not place:
                restants_a_placer.append(e)

        for e in restants_a_placer:
            for cpu in ressources.processeurs:
                if affectation_t[cpu] is None:
                    affectation_t[cpu] = e
                    break

        for cpu in ressources.processeurs:
            e_nouveau = affectation_t[cpu]
            en_cours = alloc_en_cours[cpu]
            id_nouveau = e_nouveau.proc.id_processus if e_nouveau else None

            if en_cours is not None and en_cours[0] != id_nouveau:
                lignes_allocations.append({
                    "id_processus": en_cours[0], "date_debut": en_cours[1],
                    "date_fin": t, "id_processeur": cpu,
                })
                alloc_en_cours[cpu] = None

            if id_nouveau is not None and alloc_en_cours[cpu] is None:
                alloc_en_cours[cpu] = (id_nouveau, t)

        for e in selectionnes:
            if e.date_debut is None: e.date_debut = t
            e.restant -= 1
            temps_cpu_occupe += 1
            energie_consommee_totale += ressources.energie_par_proc
            if e.restant == 0:
                e.date_fin = t + 1
                termines += 1
        t += 1

        if t > max_instant_energie + sum(p.duree_execution for p in processus_liste) + 10000:
            raise ErreurFormat("Simulation EDF bloquée : énergie insuffisante.")

    for cpu, en_cours in alloc_en_cours.items():
        if en_cours is not None:
            lignes_allocations.append({
                "id_processus": en_cours[0], "date_debut": en_cours[1],
                "date_fin": t, "id_processeur": cpu,
            })

    lignes_global = []
    temps_attente_total = temps_reponse_total = 0
    for e in etats:
        temps_reponse = e.date_fin - e.proc.date_soumission
        temps_attente = temps_reponse - e.proc.duree_execution
        temps_attente_total += temps_attente
        temps_reponse_total += temps_reponse
        lignes_global.append({
            "id_processus": e.proc.id_processus, "date_soumission": e.proc.date_soumission,
            "date_debut": e.date_debut, "date_fin": e.date_fin,
            "ram_demandee": e.proc.ram_necessaire, "ram_utilisee": e.proc.ram_necessaire,
        })

    makespan = t
    capacite_totale_cpu = makespan * ressources.nb_processeurs
    taux_utilisation = round(temps_cpu_occupe / capacite_totale_cpu, 4) if capacite_totale_cpu > 0 else 0.0

    metriques_edf = {
        "algorithme": "EDF",
        "temps_attente_moyen": round(temps_attente_total / nb_total, 2) if nb_total else 0,
        "temps_reponse_moyen": round(temps_reponse_total / nb_total, 2) if nb_total else 0,
        "makespan": makespan,
        "energie_consommee_totale": energie_consommee_totale,
        "energie_disponible_totale": energie_disponible_totale,
        "taux_utilisation": taux_utilisation,
    }
    return lignes_global, lignes_allocations, metriques_edf


def ordonnancer_fifo(processus_liste: List[Processus], ressources: Ressources, energie_dispo: Dict[int, int]):
    a_venir = list(processus_liste)
    file = []
    en_cours = []
    ram_libre = ressources.ram_totale
    
    lignes_global = []
    lignes_allocations = []
    
    t = 0
    energie_consommee_totale = 0
    energie_disponible_totale = 0
    temps_cpu_occupe = 0
    nb_total = len(processus_liste)
    
    temps_attente_total = temps_reponse_total = 0
    max_instant_energie = max(energie_dispo.keys()) if energie_dispo else 0

    while t <= max_instant_energie + sum(p.duree_execution for p in processus_liste) + 10000 and (a_venir or file or en_cours):
        e_t = energie_dispo.get(t, energie_dispo.get(max_instant_energie, 0))
        energie_disponible_totale += e_t

        for e in en_cours[:]:
            if e["fin"] == t:
                ram_libre += e["proc"].ram_necessaire
                en_cours.remove(e)

        arrivants = [p for p in a_venir if p.date_soumission == t]
        arrivants.sort(key=lambda p: -p.priorite)
        for p in arrivants:
            file.append(p)
            a_venir.remove(p)

        nb_ok = min(ressources.nb_processeurs, e_t // ressources.energie_par_proc)
        for p in file[:]:
            if len(en_cours) >= nb_ok: break
            if p.ram_necessaire <= ram_libre:
                pris = [e["cpu"] for e in en_cours]
                cpu = next((c for c in ressources.processeurs if c not in pris), None)
                if cpu is not None:
                    fin = t + p.duree_execution
                    en_cours.append({"proc": p, "fin": fin, "cpu": cpu})
                    ram_libre -= p.ram_necessaire
                    file.remove(p)

                    lignes_allocations.append({
                        "id_processus": p.id_processus, "date_debut": t,
                        "date_fin": fin, "id_processeur": cpu
                    })
                    lignes_global.append({
                        "id_processus": p.id_processus, "date_soumission": p.date_soumission,
                        "date_debut": t, "date_fin": fin,
                        "ram_demandee": p.ram_necessaire, "ram_utilisee": p.ram_necessaire,
                    })
                    
                    temps_reponse = fin - p.date_soumission
                    temps_attente = temps_reponse - p.duree_execution
                    temps_attente_total += temps_attente
                    temps_reponse_total += temps_reponse

        for _ in en_cours:
            temps_cpu_occupe += 1
            energie_consommee_totale += ressources.energie_par_proc
            
        t += 1

    if a_venir or file or en_cours:
        raise ErreurFormat("Simulation FIFO bloquée : énergie insuffisante.")

    makespan = t if t > 0 else 0
    capacite_totale_cpu = makespan * ressources.nb_processeurs
    taux_utilisation = round(temps_cpu_occupe / capacite_totale_cpu, 4) if capacite_totale_cpu > 0 else 0.0
    
    metriques = {
        "algorithme": "FIFO",
        "temps_attente_moyen": round(temps_attente_total / nb_total, 2) if nb_total else 0,
        "temps_reponse_moyen": round(temps_reponse_total / nb_total, 2) if nb_total else 0,
        "makespan": makespan,
        "energie_consommee_totale": energie_consommee_totale,
        "energie_disponible_totale": energie_disponible_totale,
        "taux_utilisation": taux_utilisation,
    }
    return lignes_global, lignes_allocations, metriques


# =========================================================
# POINT D'ENTREE
# =========================================================

def executer_depuis_config(chemin_config: str = "config.txt"):
    try:
        config = lire_config(chemin_config)
        ressources = lire_ressources(config["fichier_ressources"])
        processus = lire_processus(config["fichier_processus"])
        energie = lire_energie(config["fichier_energie"])

        if not config["algorithmes"]:
            print("Aucun algorithme n'est demandé.")
            return

        toutes_metriques = []

        for algo in config["algorithmes"]:
            print(f"Lancement de la simulation {algo}...")
            if algo == "EDF":
                l_glob, l_alloc, metriques = ordonnancer_edf(processus, ressources, energie)
            elif algo == "FIFO":
                l_glob, l_alloc, metriques = ordonnancer_fifo(processus, ressources, energie)
            else:
                print(f"Algorithme {algo} non implémenté.")
                continue

            toutes_metriques.append(metriques)

            chemins = config["sorties"].get(algo)
            if chemins:
                ecrire_global(chemins["global"], l_glob)
                ecrire_allocations(chemins["allocations"], l_alloc)
                print(f"Simulation {algo} terminée avec succès.")
                print(f" - Fichier global      : {chemins['global']}")
                print(f" - Fichier allocations : {chemins['allocations']}")

        if toutes_metriques:
            ecrire_metriques(config["fichier_metriques"], toutes_metriques)
            print(f" - Fichier métriques   : {config['fichier_metriques']}")

    except (ErreurLecture, ErreurFormat, ErreurEcriture) as e:
        print(f"Erreur lors de l'exécution : {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    fichier_cfg = sys.argv[1] if len(sys.argv) > 1 else "config.txt"
    executer_depuis_config(fichier_cfg)
