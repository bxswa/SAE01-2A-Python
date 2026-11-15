import csv
import os
import tempfile
import unittest

# Import global depuis le fichier compressé "simulateur.py"
from simulateur import (
    Processus, Ressources, ErreurFormat, ErreurLecture, ErreurEcriture,
    lire_config, lire_ressources, lire_processus, lire_energie,
    ecrire_global, ecrire_allocations, ecrire_metriques,
    ordonnancer_edf, ordonnancer_fifo, ordonnancer_rr, executer_depuis_config
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

    class TestOrdonnancementRR(unittest.TestCase):
        def test_normal():
            ressource_test = Ressources(nb_processeurs=2, ram_totale=512, energie_par_proc=50, processeurs=["CPU1", "CPU2"])
            table_processus = [
                Processus(id_processus="P1", date_soumission=0, duree_execution=3, ram_necessaire=256, priorite=1, date_butoir=10),
                Processus(id_processus="P2", date_soumission=0, duree_execution=3, ram_necessaire=256, priorite=1, date_butoir=10),
                Processus(id_processus="P3", date_soumission=2, duree_execution=5, ram_necessaire=128, priorite=0, date_butoir=12)
            ]
            energie_dispo = [{"instant": i, "energie_disponible": 100} for i in range(10)]

            res_glob, res_alloc, metriques = ordonnancer_rr(table_processus, ressource_test, energie_dispo)

            assert res_glob == [
                                {"processus" : "P1", "soumission" : 0, "execution" : 0, "fin" : 2, "RAM" : 256},
                                {"processus" : "P2", "soumission" : 0, "execution" : 0, "fin" : 2, "RAM" : 256},
                                {"processus" : "P3", "soumission" : 2, "execution" : 3, "fin" : 7, "RAM" : 128}
                            ]
            
            assert res_alloc == [
                                {"processus" : "P1", "debut" : 0, "fin" : 2, "processeur" : "CPU1"},
                                {"processus" : "P2", "debut" : 0, "fin" : 2, "processeur" : "CPU2"},
                                {"processus" : "P3", "debut" : 3, "fin" : 7, "processeur" : "CPU1"}
                            ]

        def test_depasser_date_butoir():
            ressources = Ressources(nb_processeurs=1, ram_totale=1024, energie_par_proc=10, processeurs=["CPU1"])
            processus = [
                Processus(id_processus="P_Presse", date_soumission=0, duree_execution=4, ram_necessaire=256, priorite=1, date_butoir=-1)
            ]
            energie = [{"instant": i, "energie_disponible": 20} for i in range(99)]

            res_glob, res_alloc, metriques = ordonnancer_rr(processus, ressources, energie, quantum=2)

            attendu_global = []
                    
            attendu_alloc = []
            
            attendu_metriques = {
                "algorithme": "Round-Robin",
                "attente_moyenne": 0,
                "reponse_moyenne": 0,
                "makespan": 0,
                "energie_consommee_totale": 0,
                "energie_disponible_totale": 20,
                "taux_utilisation": 0
            }

            assert res_glob == attendu_global, f"Mauvais résultat global : {res_glob}"
            assert res_alloc == attendu_alloc, f"Mauvais résultat allocations : {res_alloc}"
            assert metriques == attendu_metriques, f"Métriques incorrectes : {metriques}"
            print("Fin de test_depassement_date_butoir avec succes")

        def test_surcharge_ram():
            ressources = Ressources(nb_processeurs=2, ram_totale=512, energie_par_proc=10, processeurs=["CPU1", "CPU2"])
            processus = [
                Processus(id_processus="P_Lourd", date_soumission=0, duree_execution=3, ram_necessaire=1024, priorite=1, date_butoir=10),
                Processus(id_processus="P_Normal", date_soumission=0, duree_execution=2, ram_necessaire=256, priorite=1, date_butoir=10)
            ]
            energie = [{"instant": i, "energie_disponible": 100} for i in range(5)]

            res_glob, res_alloc, metriques = ordonnancer_rr(processus, ressources, energie, quantum=2)

            attendu_global = [
                                {"processus" : "P_Normal", "soumission" : 0, "execution" : 0, "fin" : 1, "RAM" : 256}
                            ]
                
            attendu_alloc = [
                                {"processus" : "P_Normal", "debut" : 0, "fin" : 1, "processeur" : "CPU1"}
                            ]

            attendu_metriques = {
                "algorithme": "Round-Robin",
                "attente_moyenne": 0,
                "reponse_moyenne": 2,
                "makespan": 1,
                "energie_consommee_totale": 20,
                "energie_disponible_totale": 200,
                "taux_utilisation": 1 / 10
            }

            assert res_glob == attendu_global, f"Mauvais résultat global : {res_glob}"
            assert res_alloc == attendu_alloc, f"Mauvais résultat allocations : {res_alloc}"
            assert metriques == attendu_metriques, f"Métriques incorrectes : {metriques}"
            print("Fin test_surcharge_ram...")


        def test_panne_energie():
            ressources = Ressources(nb_processeurs=2, ram_totale=1024, energie_par_proc=99, processeurs=["CPU1", "CPU2"])
            processus = [
                Processus(id_processus="P1", date_soumission=0, duree_execution=2, ram_necessaire=256, priorite=1, date_butoir=10)
            ]
            energie = [{"instant": i, "energie_disponible": 1} for i in range(12)]
            
            res_glob, res_alloc, metriques = ordonnancer_rr(processus, ressources, energie, quantum=2)

            attendu_global = []
                
            attendu_alloc = []

            attendu_metriques = {
                "algorithme": "Round-Robin",
                "attente_moyenne": 0,
                "reponse_moyenne": 0,
                "makespan": 11,
                "energie_consommee_totale": 0,
                "energie_disponible_totale": 12,
                "taux_utilisation": 0
            }

            assert res_glob == attendu_global, f"Mauvais résultat global : {res_glob}"
            assert res_alloc == attendu_alloc, f"Mauvais résultat allocations : {res_alloc}"
            assert metriques == attendu_metriques, f"Métriques incorrectes : {metriques}"
            print("Fin test_priorites_simulatanees...")


        def test_priorites_simultanees():
            ressources = Ressources(nb_processeurs=1, ram_totale=1024, energie_par_proc=10, processeurs=["CPU1"])
            processus = [
                Processus(id_processus="P_Basse", date_soumission=0, duree_execution=1, ram_necessaire=256, priorite=1, date_butoir=10),
                Processus(id_processus="P_Haute", date_soumission=0, duree_execution=1, ram_necessaire=256, priorite=10, date_butoir=10),
                Processus(id_processus="P_Moyenne", date_soumission=0, duree_execution=1, ram_necessaire=256, priorite=5, date_butoir=10)
            ]
            energie = [{"instant": i, "energie_disponible": 50} for i in range(5)]

            res_glob, res_alloc, metriques = ordonnancer_rr(processus, ressources, energie, quantum=2)

            attendu_global = [
                                {"processus" : "P_Haute", "soumission" : 0, "execution" : 0, "fin" : 0, "RAM" : 256},
                                {"processus" : "P_Moyenne", "soumission" : 0, "execution" : 1, "fin" : 1, "RAM" : 256},
                                {"processus" : "P_Basse", "soumission" : 0, "execution" : 2, "fin" : 2, "RAM" : 256}
                            ]
            
            attendu_alloc = [
                                {"processus" : "P_Haute", "debut" : 0, "fin" : 0, "processeur" : "CPU1"},
                                {"processus" : "P_Moyenne", "debut" : 1, "fin" : 1, "processeur" : "CPU1"},
                                {"processus" : "P_Basse", "debut" : 2, "fin" : 2, "processeur" : "CPU1"}
                            ]

            attendu_metriques = {
                "algorithme": "Round-Robin",
                "attente_moyenne": 1,
                "reponse_moyenne": 1,
                "makespan": 2,
                "energie_consommee_totale": 30,
                "energie_disponible_totale": 150,
                "taux_utilisation": 1 / 5
            }

            assert res_glob == attendu_global, f"Mauvais résultat global : {res_glob}"
            assert res_alloc == attendu_alloc, f"Mauvais résultat allocations : {res_alloc}"
            assert metriques == attendu_metriques, f"Métriques incorrectes : {metriques}"
            print("Fin test_priorites_simulatanees...")


        def test_rotation_quantum():
            ressources = Ressources(nb_processeurs=1, ram_totale=1024, energie_par_proc=10, processeurs=["CPU1"])
            processus = [
                Processus(id_processus="P1", date_soumission=0, duree_execution=3, ram_necessaire=256, priorite=1, date_butoir=10),
                Processus(id_processus="P2", date_soumission=0, duree_execution=3, ram_necessaire=256, priorite=1, date_butoir=10)
            ]
            energie = [{"instant": i, "energie_disponible": 50} for i in range(10)]

            res_glob, res_alloc, metriques = ordonnancer_rr(processus, ressources, energie, quantum=2)

            attendu_global = [
                                {"processus" : "P1", "soumission" : 0, "execution" : 0, "fin" : 4, "RAM" : 256},
                                {"processus" : "P2", "soumission" : 0, "execution" : 2, "fin" : 5, "RAM" : 256}
                            ]
        
            attendu_alloc = [
                                {"processus" : "P1", "debut" : 0, "fin" : 4, "processeur" : "CPU1"},
                                {"processus" : "P2", "debut" : 2, "fin" : 5, "processeur" : "CPU1"}
                            ]

            attendu_metriques = {
                "algorithme": "Round-Robin",
                "attente_moyenne": 1,
                "reponse_moyenne": 4,
                "makespan": 5,
                "energie_consommee_totale": 60,
                "energie_disponible_totale": 300,
                "taux_utilisation": 60 / 300
            }

            assert res_glob == attendu_global, f"Mauvais résultat global : {res_glob}"
            assert res_alloc == attendu_alloc, f"Mauvais résultat allocations : {res_alloc}"
            assert metriques == attendu_metriques, f"Métriques incorrectes : {metriques}"
            print("Fin test_rotation_quantum...")

        def test_plus_de_processeurs_que_de_processus():
            ressources = Ressources(
                nb_processeurs=4,
                ram_totale=2048,
                energie_par_proc=10,
                processeurs=["CPU1", "CPU2", "CPU3", "CPU4"]
            )
            processus = [
                Processus(id_processus="P_Seul", date_soumission=0, duree_execution=5, ram_necessaire=256, priorite=1, date_butoir=10)
            ]
            energie = [{"instant": i, "energie_disponible": 100} for i in range(8)]

            res_glob, res_alloc, metriques = ordonnancer_rr(processus, ressources, energie, quantum=2)

            attendu_global = [
                                {"processus" : "P_Seul", "soumission" : 0, "execution" : 0, "fin" : 4, "RAM" : 256}
                            ]
            
            attendu_alloc = [
                                {"processus" : "P_Seul", "debut" : 0, "fin" : 4, "processeur" : "CPU1"}
                            ]

            attendu_metriques = {
                "algorithme": "Round-Robin",
                "attente_moyenne": 0,
                "reponse_moyenne": 5,
                "makespan": 4,
                "energie_consommee_totale": 50,
                "energie_disponible_totale": 500,
                "taux_utilisation": 1 / 10
            }

            assert res_glob == attendu_global, f"Mauvais résultat global : {res_glob}"
            assert res_alloc == attendu_alloc, f"Mauvais résultat allocations : {res_alloc}"
            assert metriques == attendu_metriques, f"Métriques incorrectes : {metriques}"
            print("Fin test_processeur_processus...")


        if __name__ == "__main__":
            test_depasser_date_butoir()
            test_surcharge_ram()
            test_panne_energie()
            test_priorites_simultanees()
            test_rotation_quantum()
            test_plus_de_processeurs_que_de_processus()

            test_normal()


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
