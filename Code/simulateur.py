"""
simulateur.py

Simulation de l'ordonnancement, UN SEUL moteur pour les 3 algorithmes.

Idee : on fait avancer le temps instant par instant (t = 0, 1, 2, ...).
A chaque instant, on regarde l'energie disponible et la RAM libre, puis on
donne les processeurs aux processus de la file d'attente.

Les 3 algorithmes ne different que sur 2 points :
  - l'ORDRE dans lequel on parcourt la file d'attente
        FIFO et RR : ordre d'arrivee dans la file
        EDF        : date butoir la plus proche d'abord
  - RR retire le processeur au bout de 'quantum' instants
    (FIFO et EDF gardent le processeur jusqu'a la fin du processus)

Deroulement d'un instant t :
  1. les processus soumis a la date t entrent dans la file
     (s'ils arrivent en meme temps : le plus prioritaire d'abord)
  2. (RR) les processus dont le quantum est fini retournent en fin de file
  3. on calcule combien de processeurs l'energie permet d'alimenter
  4. si l'energie a baisse, on met en pause les processus en trop
     (ils retournent en tete de file)
  5. on parcourt la file et on donne un processeur libre a chaque processus
     dont la RAM tient dans la RAM libre (sinon on passe au suivant)
  6. chaque processus en cours avance d'une unite de temps
"""

from erreurs import ErreurFormat

ALGORITHMES = ("FIFO", "RR", "EDF")

# Garde-fou : on arrete si la simulation depasse cette duree.
LIMITE_TEMPS = 100000


# =========================================================
# Etat d'un processus pendant la simulation
# =========================================================

class EtatProcessus:
    def __init__(self, proc):
        self.proc = proc                       # objet Processus (lecture.py)
        self.restant = proc.duree_execution    # temps d'execution qu'il reste
        self.date_debut = None                 # premier instant ou il a tourne
        self.date_fin = None                   # instant ou il a fini
        self.cpu = None                        # processeur actuel
        self.dernier_cpu = None                # dernier processeur utilise
        self.quantum_utilise = 0               # (RR) instants consecutifs sur le CPU


class Resultat:
    """Tout ce que produit la simulation d'un algorithme."""

    def __init__(self, algorithme):
        self.algorithme = algorithme
        self.etats = []           # un EtatProcessus par processus
        self.trace = []           # (id_processus, cpu, t) : qui tourne ou, a chaque instant
        self.energie_dispo = []   # energie disponible a chaque instant
        self.energie_conso = []   # energie consommee a chaque instant
        self.nb_cpu_actifs = []   # nombre de CPU allumes a chaque instant


# =========================================================
# Petites fonctions utiles
# =========================================================

def energie_a(energie, t):
    """Energie disponible a l'instant t.

    energie.csv decrit une journee (instants 0 a 23). Si la simulation dure
    plus longtemps, on recommence au debut du fichier (journee suivante).
    Sans cela, un processus pas fini le soir ne pourrait jamais finir.
    """
    periode = max(energie) + 1
    return energie.get(t % periode, 0)


def cle_arrivee(etat):
    """Ordre d'entree dans la file pour des processus soumis en meme temps :
    priorite la plus grande d'abord, puis identifiant."""
    return (-etat.proc.priorite, etat.proc.id_processus)


def cle_edf(etat):
    """Ordre EDF : date butoir la plus proche, puis priorite la plus grande."""
    return (etat.proc.date_butoir, -etat.proc.priorite)


def dans_l_ordre(liste, cle_ordre=None):
    """Renvoie une copie de la liste selon la politique d'ordonnancement."""
    if cle_ordre is None:
        return list(liste)
    # sorted() est stable : a egalite, l'ordre de la file est conserve.
    return sorted(liste, key=cle_ordre)


def processus_sur(en_cours, cpu):
    """Renvoie le processus qui tourne sur ce CPU, ou None."""
    for etat in en_cours:
        if etat.cpu == cpu:
            return etat
    return None


# =========================================================
# Le moteur
# =========================================================

def simuler_fifo(processus, ressources, energie):
    """Simule la politique FIFO, en conservant l'ordre de la file."""
    return _simuler(processus, ressources, energie, "FIFO")


def simuler_rr(processus, ressources, energie, quantum=None):
    """Simule Round Robin : un processus rend le CPU apres son quantum."""
    if quantum is None or quantum <= 0:
        raise ErreurFormat("RR a besoin d'un quantum > 0")
    return _simuler(processus, ressources, energie, "RR", quantum=quantum)


def simuler_edf(processus, ressources, energie):
    """Simule EDF : la date butoir la plus proche passe en premier."""
    return _simuler(processus, ressources, energie, "EDF", cle_ordre=cle_edf)


def simuler(processus, ressources, energie, algorithme, quantum=None):
    """Choisit la fonction correspondant a l'algorithme demande."""
    if algorithme not in ALGORITHMES:
        raise ErreurFormat("Algorithme inconnu : %s" % algorithme)
    if algorithme == "FIFO":
        return simuler_fifo(processus, ressources, energie)
    if algorithme == "RR":
        return simuler_rr(processus, ressources, energie, quantum)
    return simuler_edf(processus, ressources, energie)


def _simuler(processus, ressources, energie, algorithme, cle_ordre=None,
             quantum=None):
    """Moteur commun aux politiques FIFO, RR et EDF.

    processus  : liste de Processus (lire_processus)
    ressources : objet Ressources (lire_ressources)
    energie    : dictionnaire {instant: energie} (lire_energie)
    algorithme : nom conserve dans l'objet Resultat
    cle_ordre  : cle de tri de la file (absente pour FIFO et RR)
    quantum    : entier > 0 pour RR, absent pour FIFO et EDF
    """
    if max(energie.values()) < ressources.energie_par_proc:
        raise ErreurFormat("L'energie disponible ne suffit jamais pour allumer "
                           "un seul processeur : aucun processus ne peut tourner.")

    resultat = Resultat(algorithme)

    # Un processus qui demande plus que la RAM totale ne pourra jamais
    # tourner : on le garde dans les resultats, mais on ne le simule pas.
    a_traiter = []
    for p in processus:
        etat = EtatProcessus(p)
        resultat.etats.append(etat)
        if p.ram_necessaire <= ressources.ram_totale:
            a_traiter.append(etat)

    file = []          # file d'attente (processus prets mais pas en cours)
    en_cours = []      # processus qui ont un processeur
    expires = []       # (RR) processus dont le quantum vient de finir
    nb_termines = 0
    t = 0

    while nb_termines < len(a_traiter):
        if t > LIMITE_TEMPS:
            raise ErreurFormat("Simulation arretee : plus de %d instants." % LIMITE_TEMPS)

        # ---- 1. Arrivees ----
        arrivants = []
        for etat in a_traiter:
            if etat.proc.date_soumission == t:
                arrivants.append(etat)
        arrivants.sort(key=cle_arrivee)
        file = file + arrivants

        # ---- 2. (RR) quantum fini -> fin de file, apres les arrivants ----
        file = file + expires
        expires = []

        # ---- 3. Nombre de processeurs alimentables ----
        energie_t = energie_a(energie, t)
        nb_alimentables = min(ressources.nb_processeurs,
                              energie_t // ressources.energie_par_proc)

        # ---- 4. Pause si l'energie a baisse ----
        if len(en_cours) > nb_alimentables:
            ordre = dans_l_ordre(en_cours, cle_ordre)
            a_suspendre = ordre[nb_alimentables:]     # les moins prioritaires
            for etat in a_suspendre:
                en_cours.remove(etat)
                etat.cpu = None
                etat.quantum_utilise = 0
            file = a_suspendre + file                 # retour en tete de file

        # ---- 5. On donne les processeurs libres ----
        ram_libre = ressources.ram_totale
        for etat in en_cours:
            ram_libre = ram_libre - etat.proc.ram_necessaire

        cpus_libres = []
        for cpu in ressources.processeurs:
            if processus_sur(en_cours, cpu) is None:
                cpus_libres.append(cpu)

        for etat in dans_l_ordre(file, cle_ordre):
            if len(en_cours) >= nb_alimentables:
                break                                  # plus de CPU alimentable
            if etat.proc.ram_necessaire > ram_libre:
                continue                               # pas assez de RAM : suivant
            # On reprend le meme CPU qu'avant s'il est libre (Gantt plus lisible)
            if etat.dernier_cpu in cpus_libres:
                cpu = etat.dernier_cpu
            else:
                cpu = cpus_libres[0]
            cpus_libres.remove(cpu)
            file.remove(etat)
            etat.cpu = cpu
            etat.quantum_utilise = 0
            en_cours.append(etat)
            ram_libre = ram_libre - etat.proc.ram_necessaire
            if etat.date_debut is None:
                etat.date_debut = t

        # ---- 6. Execution de l'instant t ----
        resultat.energie_dispo.append(energie_t)
        resultat.energie_conso.append(len(en_cours) * ressources.energie_par_proc)
        resultat.nb_cpu_actifs.append(len(en_cours))

        for cpu in ressources.processeurs:            # dans l'ordre des CPU
            etat = processus_sur(en_cours, cpu)
            if etat is None:
                continue
            resultat.trace.append((etat.proc.id_processus, cpu, t))
            etat.restant = etat.restant - 1
            etat.quantum_utilise = etat.quantum_utilise + 1
            etat.dernier_cpu = cpu

            if etat.restant == 0:                      # termine
                etat.date_fin = t + 1
                etat.cpu = None
                en_cours.remove(etat)
                nb_termines = nb_termines + 1
            elif quantum is not None and etat.quantum_utilise >= quantum:
                etat.cpu = None                        # quantum fini
                en_cours.remove(etat)
                expires.append(etat)

        t = t + 1

    return resultat


# =========================================================
# Mise en forme des resultats pour ecriture.py
# =========================================================

def cle_trace(element):
    """Trie la trace par processus puis par instant."""
    return (element[0], element[2])


def lignes_allocations(resultat):
    """Regroupe les instants consecutifs d'un processus sur un meme CPU.

    Exemple : P1 sur CPU1 aux instants 7, 8, 9  ->  P1, debut 7, fin 10, CPU1
    (date_fin exclue).
    """
    lignes = []
    for id_proc, cpu, t in sorted(resultat.trace, key=cle_trace):
        if lignes:
            derniere = lignes[-1]
            if (derniere["id_processus"] == id_proc
                    and derniere["id_processeur"] == cpu
                    and derniere["date_fin"] == t):
                derniere["date_fin"] = t + 1           # on prolonge
                continue
        lignes.append({"id_processus": id_proc, "date_debut": t,
                       "date_fin": t + 1, "id_processeur": cpu})
    return lignes


def lignes_global(resultat):
    """Une ligne par processus. Processus rejete : dates vides, RAM utilisee 0."""
    lignes = []
    for etat in resultat.etats:
        p = etat.proc
        execute = etat.date_fin is not None
        lignes.append({
            "id_processus": p.id_processus,
            "date_soumission": p.date_soumission,
            "date_debut": etat.date_debut if execute else "",
            "date_fin": etat.date_fin if execute else "",
            "ram_demandee": p.ram_necessaire,
            "ram_utilisee": p.ram_necessaire if execute else 0,
        })
    return lignes


def lignes_energie(resultat):
    lignes = []
    for t in range(len(resultat.energie_dispo)):
        lignes.append({"instant": t,
                       "energie_disponible": resultat.energie_dispo[t],
                       "energie_consommee": resultat.energie_conso[t],
                       "nb_cpu_actifs": resultat.nb_cpu_actifs[t]})
    return lignes
