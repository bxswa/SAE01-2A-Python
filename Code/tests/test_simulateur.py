"""
test_simulateur.py

Lancement (depuis le dossier SAE_Python) :
    python3 -m unittest discover -s Code/tests -v

Contenu :
  1. Lecture / ecriture (repris de nos tests)
  2. EDF et FIFO (repris de nos tests, adaptes au moteur commun)
  3. Round Robin (tests repares : ils ne s'executaient pas avant)
  4. Test1 : resultats calcules A LA MAIN, pas a pas (voir README)
  5. Verifications automatiques sur tous les jeux d'essai
"""
import os
import sys
import tempfile
import unittest

# Pour pouvoir importer les fichiers du dossier Code
DOSSIER_CODE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOSSIER_INPUTS = os.path.join(os.path.dirname(DOSSIER_CODE), "Inputs")
sys.path.insert(0, DOSSIER_CODE)

from erreurs import ErreurLecture, ErreurFormat
from lecture import (Processus, Ressources, lire_config, lire_ressources,
                     lire_processus, lire_energie)
from ecriture import ecrire_global
from simulateur import simuler, lignes_global, lignes_allocations
from metriques import calculer_metriques


# ---------------------------------------------------------
# Fonctions d'aide pour les tests
# ---------------------------------------------------------

def dates(resultat):
    """{id: (debut, fin)} pour chaque processus."""
    d = {}
    for etat in resultat.etats:
        d[etat.proc.id_processus] = (etat.date_debut, etat.date_fin)
    return d


def allocations(resultat):
    """Liste de tuples (id, debut, fin, cpu), triee."""
    liste = []
    for a in lignes_allocations(resultat):
        liste.append((a["id_processus"], a["date_debut"], a["date_fin"], a["id_processeur"]))
    liste.sort()
    return liste


def charger_test(nom):
    """Lit les 4 fichiers d'un dossier Inputs/<nom>."""
    dossier = os.path.join(DOSSIER_INPUTS, nom)
    config = lire_config(os.path.join(dossier, "config.txt"))
    ressources = lire_ressources(os.path.join(dossier, config["fichier_ressources"]))
    processus = lire_processus(os.path.join(dossier, config["fichier_processus"]))
    energie = lire_energie(os.path.join(dossier, config["fichier_energie"]))
    return config, ressources, processus, energie


# =========================================================
# 1. Lecture / ecriture
# =========================================================

class TestLectureEcriture(unittest.TestCase):

    def test_erreur_json(self):
        with tempfile.NamedTemporaryFile(mode="w", delete=False, encoding="utf-8") as f:
            f.write("{mauvais_json")
            chemin = f.name
        with self.assertRaises(ErreurFormat):
            lire_ressources(chemin)
        os.remove(chemin)

    def test_fichiers_introuvables(self):
        with self.assertRaises(ErreurLecture):
            lire_config("fichier_fantome.txt")
        with self.assertRaises(ErreurLecture):
            lire_processus("fichier_fantome.csv")

    def test_config_fourni(self):
        config, ressources, processus, energie = charger_test("Test1")
        self.assertEqual(config["algorithmes"], ["FIFO", "RR", "EDF"])
        self.assertEqual(config["quantum"], 2)
        self.assertEqual(config["fichier_energie"], "energie.csv")   # cle "energie"
        self.assertEqual(config["sorties"]["RR"]["global"], "resultats_roundrobin_global.csv")
        self.assertEqual(ressources.nb_processeurs, 2)
        self.assertEqual(len(processus), 3)
        self.assertEqual(energie[12], 190)
        # Pas de cle fichier_metriques_energie_... : nom deduit du fichier global
        self.assertEqual(config["sorties"]["FIFO"]["energie"], "resultats_fifo_energie.csv")

    def test_fichier_energie_choisi_dans_config(self):
        with tempfile.TemporaryDirectory() as dossier:
            chemin = os.path.join(dossier, "config.txt")
            with open(chemin, "w", encoding="utf-8") as f:
                f.write("fichier_processus = p.csv\n"
                        "fichier_ressources = r.json\n"
                        "energie = e.csv\n"
                        "algorithmes = FIFO\n"
                        "fichier_global_FIFO = g.csv\n"
                        "fichier_allocations_FIFO = a.csv\n"
                        "fichier_metriques_energie_FIFO = mon_energie.csv\n"
                        "fichier_metriques = m.csv\n")
            config = lire_config(chemin)
            self.assertEqual(config["sorties"]["FIFO"]["energie"], "mon_energie.csv")

    def test_ecriture_nominale(self):
        with tempfile.TemporaryDirectory() as dossier:
            chemin = os.path.join(dossier, "out_global.csv")
            ecrire_global(chemin, [{"id_processus": "P1", "date_soumission": 0,
                                    "date_debut": 0, "date_fin": 5,
                                    "ram_demandee": 512, "ram_utilisee": 512}])
            self.assertTrue(os.path.isfile(chemin))


# =========================================================
# 2. EDF et FIFO
# =========================================================

class TestEDF(unittest.TestCase):

    def test_date_butoir_la_plus_proche_d_abord(self):
        ressources = Ressources(1, 100, 10, ["CPU1"])
        processus = [Processus("P1", 0, 2, 10, 1, 10),
                     Processus("P2", 0, 2, 10, 1, 5)]
        r = simuler(processus, ressources, {0: 20, 1: 20, 2: 20, 3: 20}, "EDF")
        self.assertEqual(dates(r), {"P2": (0, 2), "P1": (2, 4)})
        self.assertEqual(calculer_metriques(r)["makespan"], 4)

    def test_meme_deadline_priorite_la_plus_grande_d_abord(self):
        ressources = Ressources(1, 100, 10, ["CPU1"])
        processus = [Processus("Basse", 0, 1, 10, 1, 5),
                     Processus("Haute", 0, 1, 10, 9, 5)]
        r = simuler(processus, ressources, {0: 10}, "EDF")
        self.assertEqual(dates(r), {"Haute": (0, 1), "Basse": (1, 2)})

    def test_non_preemptif(self):
        # Q (deadline 3) arrive pendant que L tourne : L garde le CPU jusqu'a la fin
        ressources = Ressources(1, 100, 10, ["CPU1"])
        processus = [Processus("L", 0, 4, 10, 1, 50), Processus("Q", 1, 1, 10, 1, 3)]
        r = simuler(processus, ressources, {0: 10}, "EDF")
        self.assertEqual(dates(r), {"L": (0, 4), "Q": (4, 5)})

    def test_contrainte_ram_sur_multi_cpu(self):
        ressources = Ressources(2, 100, 10, ["CPU1", "CPU2"])
        processus = [Processus("P1", 0, 2, 60, 1, 5), Processus("P2", 0, 2, 60, 1, 8)]
        r = simuler(processus, ressources, {0: 50, 1: 50, 2: 50, 3: 50}, "EDF")
        self.assertEqual(dates(r), {"P1": (0, 2), "P2": (2, 4)})

    def test_pause_quand_energie_insuffisante(self):
        ressources = Ressources(1, 100, 10, ["CPU1"])
        processus = [Processus("P1", 0, 2, 20, 1, 10)]
        r = simuler(processus, ressources, {0: 10, 1: 5, 2: 10}, "EDF")
        self.assertEqual(dates(r), {"P1": (0, 3)})
        self.assertEqual(allocations(r), [("P1", 0, 1, "CPU1"), ("P1", 2, 3, "CPU1")])


class TestFIFO(unittest.TestCase):

    def test_nominal(self):
        ressources = Ressources(1, 4, 1, ["CPU1"])
        r = simuler([Processus("P1", 0, 2, 2, 1, 10)], ressources, {0: 10}, "FIFO")
        self.assertEqual(allocations(r), [("P1", 0, 2, "CPU1")])

    def test_ram_limite(self):
        ressources = Ressources(2, 4, 1, ["CPU1", "CPU2"])
        processus = [Processus("P1", 0, 2, 4, 1, 10), Processus("P2", 0, 2, 2, 1, 10)]
        r = simuler(processus, ressources, {0: 10}, "FIFO")
        self.assertEqual(dates(r), {"P1": (0, 2), "P2": (2, 4)})

    def test_saute_un_processus_trop_gros_pour_la_ram_libre(self):
        # A (900) ne tient pas a cote de X (600) : B (300), derriere, passe avant
        ressources = Ressources(2, 1000, 10, ["CPU1", "CPU2"])
        processus = [Processus("X", 0, 3, 600, 9, 99), Processus("A", 0, 1, 900, 5, 99),
                     Processus("B", 0, 1, 300, 1, 99)]
        r = simuler(processus, ressources, {0: 100}, "FIFO")
        self.assertEqual(dates(r), {"X": (0, 3), "B": (0, 1), "A": (3, 4)})

    def test_energie_au_depart(self):
        ressources = Ressources(1, 4, 2, ["CPU1"])
        r = simuler([Processus("P1", 0, 2, 2, 1, 10)], ressources,
                    {0: 0, 1: 10, 2: 10, 3: 10}, "FIFO")
        self.assertEqual(allocations(r), [("P1", 1, 3, "CPU1")])

    def test_priorite_si_arrivee_simultanee(self):
        ressources = Ressources(1, 2, 1, ["CPU1"])
        processus = [Processus("P1", 0, 2, 2, 1, 10), Processus("P2", 0, 2, 2, 5, 10)]
        r = simuler(processus, ressources, {0: 10}, "FIFO")
        self.assertEqual(dates(r), {"P2": (0, 2), "P1": (2, 4)})

    def test_energie_coupe_un_processus_en_cours(self):
        # Avant, FIFO reservait le CPU pour toute la duree sans regarder l'energie
        ressources = Ressources(1, 100, 10, ["CPU1"])
        r = simuler([Processus("P1", 0, 3, 10, 1, 10)], ressources,
                    {0: 10, 1: 0, 2: 10, 3: 10}, "FIFO")
        self.assertEqual(allocations(r), [("P1", 0, 1, "CPU1"), ("P1", 2, 4, "CPU1")])


# =========================================================
# 3. Round Robin
# =========================================================

class TestRoundRobin(unittest.TestCase):

    def test_rotation_quantum(self):
        # quantum 2 : P1 P1 P2 P2 P1 P2
        ressources = Ressources(1, 1024, 10, ["CPU1"])
        processus = [Processus("P1", 0, 3, 256, 1, 10), Processus("P2", 0, 3, 256, 1, 10)]
        r = simuler(processus, ressources, {0: 50}, "RR", quantum=2)
        self.assertEqual(allocations(r), [("P1", 0, 2, "CPU1"), ("P1", 4, 5, "CPU1"),
                                          ("P2", 2, 4, "CPU1"), ("P2", 5, 6, "CPU1")])
        m = calculer_metriques(r)
        self.assertEqual(m["makespan"], 6)
        self.assertEqual(m["temps_reponse_moyen"], 5.5)    # (5 + 6) / 2
        self.assertEqual(m["temps_attente_moyen"], 2.5)    # (2 + 3) / 2

    def test_priorites_simultanees(self):
        ressources = Ressources(1, 1024, 10, ["CPU1"])
        processus = [Processus("P_Basse", 0, 1, 256, 1, 10),
                     Processus("P_Haute", 0, 1, 256, 10, 10),
                     Processus("P_Moyenne", 0, 1, 256, 5, 10)]
        r = simuler(processus, ressources, {0: 50}, "RR", quantum=2)
        self.assertEqual(dates(r), {"P_Haute": (0, 1), "P_Moyenne": (1, 2),
                                    "P_Basse": (2, 3)})

    def test_deux_processus_jamais_sur_le_meme_cpu(self):
        ressources = Ressources(2, 512, 50, ["CPU1", "CPU2"])
        processus = [Processus("P1", 0, 3, 256, 1, 10), Processus("P2", 0, 3, 256, 1, 10),
                     Processus("P3", 2, 5, 128, 0, 12)]
        r = simuler(processus, ressources, {0: 100}, "RR", quantum=2)
        for t in range(calculer_metriques(r)["makespan"]):
            cpus = [a[3] for a in allocations(r) if a[1] <= t < a[2]]
            self.assertEqual(len(cpus), len(set(cpus)), "instant %d" % t)

    def test_processus_trop_gros_rejete(self):
        ressources = Ressources(2, 512, 10, ["CPU1", "CPU2"])
        processus = [Processus("P_Lourd", 0, 3, 1024, 1, 10),
                     Processus("P_Normal", 0, 2, 256, 1, 10)]
        r = simuler(processus, ressources, {0: 100}, "RR", quantum=2)
        self.assertEqual(dates(r), {"P_Lourd": (None, None), "P_Normal": (0, 2)})
        ligne = [l for l in lignes_global(r) if l["id_processus"] == "P_Lourd"][0]
        self.assertEqual(ligne["ram_utilisee"], 0)

    def test_quantum_obligatoire(self):
        ressources = Ressources(1, 100, 10, ["CPU1"])
        with self.assertRaises(ErreurFormat):
            simuler([Processus("P1", 0, 1, 10, 1, 5)], ressources, {0: 10}, "RR")

    def test_energie_jamais_suffisante(self):
        ressources = Ressources(2, 1024, 99, ["CPU1", "CPU2"])
        with self.assertRaises(ErreurFormat):
            simuler([Processus("P1", 0, 2, 256, 1, 10)], ressources, {0: 1}, "RR", quantum=2)


# =========================================================
# 4. Test1 calcule a la main (voir la trace du README)
# =========================================================

class TestTest1CalculeALaMain(unittest.TestCase):

    def setUp(self):
        self.config, self.ressources, self.processus, self.energie = charger_test("Test1")

    def lancer(self, algo):
        return simuler(self.processus, self.ressources, self.energie, algo,
                       self.config["quantum"])

    def test_fifo(self):
        r = self.lancer("FIFO")
        self.assertEqual(dates(r), {"P1": (7, 12), "P2": (8, 11), "P3": (12, 18)})
        m = calculer_metriques(r)
        self.assertEqual(m["temps_attente_moyen"], 8.0)      # (7 + 7 + 10) / 3
        self.assertEqual(m["temps_reponse_moyen"], 12.67)    # (12 + 10 + 16) / 3
        self.assertEqual(m["makespan"], 18)
        self.assertEqual(m["energie_consommee_totale"], 560)  # 14 instants-CPU x 40
        self.assertEqual(m["energie_disponible_totale"], 1450)
        self.assertEqual(m["taux_utilisation"], 0.3862)

    def test_round_robin(self):
        r = self.lancer("RR")
        self.assertEqual(dates(r), {"P1": (7, 14), "P2": (8, 11), "P3": (11, 18)})
        self.assertEqual(allocations(r), [("P1", 7, 11, "CPU1"), ("P1", 13, 14, "CPU1"),
                                          ("P2", 8, 11, "CPU2"),
                                          ("P3", 11, 13, "CPU1"), ("P3", 14, 18, "CPU1")])

    def test_edf(self):
        # P3 est coupe a t=18 (energie 30 < 40) et ne reprend qu'a t=31
        r = self.lancer("EDF")
        self.assertEqual(dates(r), {"P1": (8, 13), "P2": (7, 10), "P3": (13, 32)})
        self.assertEqual(calculer_metriques(r)["makespan"], 32)


# =========================================================
# 5. Verifications sur tous les jeux d'essai et tous les algorithmes
# =========================================================

class TestContraintesPartout(unittest.TestCase):

    def test_ram_energie_cpu_duree(self):
        for nom in sorted(os.listdir(DOSSIER_INPUTS)):
            config, ressources, processus, energie = charger_test(nom)
            ram = {}
            for p in processus:
                ram[p.id_processus] = p.ram_necessaire
            periode = max(energie) + 1
            for algo in ("FIFO", "RR", "EDF"):
                with self.subTest(test=nom, algo=algo):
                    r = simuler(processus, ressources, energie, algo, config["quantum"] or 2)
                    allocs = allocations(r)
                    temps_execute = {}
                    for t in range(calculer_metriques(r)["makespan"]):
                        actifs = [a for a in allocs if a[1] <= t < a[2]]
                        # RAM jamais depassee
                        self.assertLessEqual(sum(ram[a[0]] for a in actifs),
                                             ressources.ram_totale)
                        # energie jamais depassee
                        self.assertLessEqual(len(actifs) * ressources.energie_par_proc,
                                             energie.get(t % periode, 0))
                        # un seul processus par CPU
                        cpus = [a[3] for a in actifs]
                        self.assertEqual(len(cpus), len(set(cpus)))
                        for a in actifs:
                            temps_execute[a[0]] = temps_execute.get(a[0], 0) + 1
                    # chaque processus termine a tourne exactement sa duree
                    for etat in r.etats:
                        if etat.date_fin is not None:
                            self.assertEqual(temps_execute[etat.proc.id_processus],
                                             etat.proc.duree_execution)


if __name__ == "__main__":
    unittest.main(verbosity=2)
