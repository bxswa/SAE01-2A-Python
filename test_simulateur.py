import csv
import os
import tempfile
import unittest

# Import global depuis le fichier compressé "simulateur.py"
from simulateur import (
    Processus, Ressources, ErreurFormat, ErreurLecture, ErreurEcriture,
    lire_config, lire_ressources, lire_processus, lire_energie,
    ecrire_global, ecrire_allocations, ecrire_metriques,
    ordonnancer_edf, ordonnancer_fifo, executer_depuis_config
)


class TestOrdonnancementEDF(unittest.TestCase):

    def test_1_priorite_date_butoir(self):
        """Vérifie que le processus avec l'échéance la plus proche passe en premier."""
        ressources = Ressources(1, 100, 10, ["CPU1"])
        processus = [
            Processus("P1", date_soumission=0, duree_execution=2, ram_necessaire=10, priorite=1, date_butoir=10),
            Processus("P2", date_soumission=0, duree_execution=2, ram_necessaire=10, priorite=1, date_butoir=5),
        ]
        energie = {0: 20, 1: 20, 2: 20, 3: 20}

        global_res, alloc_res, metriques = ordonnancer_edf(processus, ressources, energie)
        res_par_id = {p["id_processus"]: p for p in global_res}

        self.assertEqual(res_par_id["P2"]["date_debut"], 0)
        self.assertEqual(res_par_id["P2"]["date_fin"], 2)
        self.assertEqual(res_par_id["P1"]["date_debut"], 2)
        self.assertEqual(res_par_id["P1"]["date_fin"], 4)
        self.assertEqual(metriques["makespan"], 4)

    def test_2_contrainte_ram_sur_multi_cpu(self):
        """Vérifie que 2 processus ne tournent pas en parallèle si la RAM totale est dépassée."""
        ressources = Ressources(2, 100, 10, ["CPU1", "CPU2"])
        processus = [
            Processus("P1", 0, 2, 60, 1, 5),
            Processus("P2", 0, 2, 60, 1, 8),
        ]
        energie = {0: 50, 1: 50, 2: 50, 3: 50}

        global_res, _, metriques = ordonnancer_edf(processus, ressources, energie)
        res_par_id = {p["id_processus"]: p for p in global_res}

        self.assertEqual(res_par_id["P1"]["date_fin"], 2)
        self.assertEqual(res_par_id["P2"]["date_debut"], 2)
        self.assertEqual(res_par_id["P2"]["date_fin"], 4)
        self.assertEqual(metriques["makespan"], 4)

    def test_3_contrainte_energie(self):
        """Vérifie que l'ordonnanceur met en pause l'exécution quand l'énergie est insuffisante."""
        ressources = Ressources(1, 100, 10, ["CPU1"])
        processus = [Processus("P1", 0, 2, 20, 1, 10)]
        energie = {0: 10, 1: 5, 2: 10}

        global_res, alloc_res, metriques = ordonnancer_edf(processus, ressources, energie)

        self.assertEqual(global_res[0]["date_debut"], 0)
        self.assertEqual(global_res[0]["date_fin"], 3)
        self.assertEqual(len(alloc_res), 2)
        self.assertEqual(alloc_res[0]["date_debut"], 0)
        self.assertEqual(alloc_res[0]["date_fin"], 1)
        self.assertEqual(alloc_res[1]["date_debut"], 2)
        self.assertEqual(alloc_res[1]["date_fin"], 3)

    def test_4_erreur_ram_impossible(self):
        """Vérifie que ErreurFormat est levée si un processus demande plus que la RAM totale."""
        ressources = Ressources(1, 50, 10, ["CPU1"])
        processus = [Processus("P_TROP_GROS", 0, 1, 100, 1, 5)]
        energie = {0: 20}

        with self.assertRaises(ErreurFormat):
            ordonnancer_edf(processus, ressources, energie)


class TestOrdonnancementFIFO(unittest.TestCase):

    def test_1_nominal(self):
        """Test nominal de FIFO converti pour utiliser les objets de simulateur."""
        ressources = Ressources(1, 4, 1, ["CPU1"])
        processus = [Processus("P1", 0, 2, 2, 1, 10)]
        energie = {0: 10, 1: 10, 2: 10}
        
        l_glob, l_alloc, metriques = ordonnancer_fifo(processus, ressources, energie)
        
        self.assertEqual(len(l_alloc), 1)
        self.assertEqual(l_alloc[0]["id_processus"], "P1")
        self.assertEqual(l_alloc[0]["date_debut"], 0)
        self.assertEqual(l_alloc[0]["date_fin"], 2)
        self.assertEqual(l_alloc[0]["id_processeur"], "CPU1")

    def test_2_ram_limit(self):
        """P1 prend toute la RAM (4), P2 doit attendre la libération."""
        ressources = Ressources(2, 4, 1, ["CPU1", "CPU2"])
        processus = [
            Processus("P1", 0, 2, 4, 1, 10),
            Processus("P2", 0, 2, 2, 1, 10),
        ]
        energie = {0: 10, 1: 10, 2: 10, 3: 10}
        
        l_glob, l_alloc, metriques = ordonnancer_fifo(processus, ressources, energie)
        
        alloc_p1 = next(a for a in l_alloc if a["id_processus"] == "P1")
        alloc_p2 = next(a for a in l_alloc if a["id_processus"] == "P2")
        
        self.assertEqual(alloc_p1["date_debut"], 0)
        self.assertEqual(alloc_p1["date_fin"], 2)
        self.assertEqual(alloc_p2["date_debut"], 2)

    def test_3_energie_limit(self):
        """Pas d'énergie au temps 0, suffisante ensuite."""
        ressources = Ressources(1, 4, 2, ["CPU1"])
        processus = [Processus("P1", 0, 2, 2, 1, 10)]
        energie = {0: 0, 1: 10, 2: 10, 3: 10}
        
        l_glob, l_alloc, metriques = ordonnancer_fifo(processus, ressources, energie)
        
        self.assertEqual(l_alloc[0]["date_debut"], 1)
        self.assertEqual(l_alloc[0]["date_fin"], 3)

    def test_4_priorite(self):
        """P2 a une priorité plus élevée, il doit passer en premier."""
        ressources = Ressources(1, 2, 1, ["CPU1"])
        processus = [
            Processus("P1", 0, 2, 2, 1, 10),
            Processus("P2", 0, 2, 2, 5, 10), 
        ]
        energie = {0: 10, 1: 10, 2: 10, 3: 10, 4: 10}
        
        l_glob, l_alloc, metriques = ordonnancer_fifo(processus, ressources, energie)
        
        alloc_p1 = next(a for a in l_alloc if a["id_processus"] == "P1")
        alloc_p2 = next(a for a in l_alloc if a["id_processus"] == "P2")
        
        self.assertEqual(alloc_p2["date_debut"], 0)
        self.assertEqual(alloc_p1["date_debut"], 2)


class TestLectureEcriture(unittest.TestCase):

    def test_erreur_json(self):
        """Vérifie le déclenchement dune erreur pour JSON invalide."""
        with tempfile.NamedTemporaryFile(mode="w", delete=False, encoding="utf-8") as f:
            f.write("{mauvais_json")
            chemin = f.name
        
        with self.assertRaises(ErreurFormat):
            lire_ressources(chemin)
        os.remove(chemin)

    def test_fichiers_introuvables(self):
        """Vérifie ErreurLecture si les fichiers nexistent pas."""
        with self.assertRaises(ErreurLecture):
            lire_config("fichier_fantome.txt")
        with self.assertRaises(ErreurLecture):
            lire_processus("fichier_fantome.csv")

    def test_ecriture_nominale(self):
        """Vérifie l'écriture correcte dans des fichiers temporaires."""
        with tempfile.TemporaryDirectory() as tmpdir:
            p_out = os.path.join(tmpdir, "out_global.csv")
            lignes = [{
                "id_processus": "P1", "date_soumission": 0, "date_debut": 0,
                "date_fin": 5, "ram_demandee": 512, "ram_utilisee": 512
            }]
            ecrire_global(p_out, lignes)
            self.assertTrue(os.path.isfile(p_out))


class TestIntegrationConfig(unittest.TestCase):

    def test_integration_complete(self):
        """Teste la lecture de config.txt et la génération des fichiers."""
        with tempfile.TemporaryDirectory() as tmpdir:
            p_proc = os.path.join(tmpdir, "processus.csv")
            p_res = os.path.join(tmpdir, "ressources.json")
            p_ene = os.path.join(tmpdir, "energie.csv")
            p_cfg = os.path.join(tmpdir, "config.txt")
            p_out_glob = os.path.join(tmpdir, "res", "global_edf.csv")
            p_out_alloc = os.path.join(tmpdir, "res", "alloc_edf.csv")
            p_out_met = os.path.join(tmpdir, "res", "metriques.csv")

            with open(p_proc, "w", encoding="utf-8") as f:
                f.write("id_processus,date_soumission,duree_execution,ram_necessaire,priorite,date_butoir\n")
                f.write("P1,0,2,50,1,10\n")
                f.write("P2,1,1,50,1,4\n")

            with open(p_res, "w", encoding="utf-8") as f:
                f.write('{"nbProcesseurs": 1, "ramTotale": 100, "energieParProc": 10, "processeur": ["CPU0"]}')

            with open(p_ene, "w", encoding="utf-8") as f:
                f.write("instant,energie_disponible\n0,20\n1,20\n2,20\n")

            with open(p_cfg, "w", encoding="utf-8") as f:
                f.write(f"fichier_processus = {p_proc}\n")
                f.write(f"fichier_ressources = {p_res}\n")
                f.write(f"fichier_energie = {p_ene}\n")
                f.write("algorithmes = EDF\n")
                f.write(f"fichier_global_EDF = {p_out_glob}\n")
                f.write(f"fichier_allocations_EDF = {p_out_alloc}\n")
                f.write(f"fichier_metriques = {p_out_met}\n")

            executer_depuis_config(p_cfg)

            self.assertTrue(os.path.isfile(p_out_glob))
            self.assertTrue(os.path.isfile(p_out_alloc))
            self.assertTrue(os.path.isfile(p_out_met))

            with open(p_out_glob, "r", encoding="utf-8") as f:
                lignes = list(csv.DictReader(f))
                self.assertEqual(len(lignes), 2)
                p1 = next(r for r in lignes if r["id_processus"] == "P1")
                self.assertEqual(p1["date_debut"], "0")
                self.assertEqual(p1["date_fin"], "3")


if __name__ == "__main__":
    unittest.main(verbosity=2)
