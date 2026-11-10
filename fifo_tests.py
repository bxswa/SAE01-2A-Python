from fifo import fifo


def test_nominal():
  processus = [{"id": "P1", "date_soumission": 0, "duree": 2, "ram": 2, "priorite": 1}]
  res = fifo(processus, nb_cpu=1, ram_totale=4, energie_par_cpu=1, energie=[10, 10, 10])
  assert res == [("P1", 0, 2, "CPU1")]


def test_ram_limit():
  processus = [
      {"id": "P1", "date_soumission": 0, "duree": 2, "ram": 4, "priorite": 1},
      {"id": "P2", "date_soumission": 0, "duree": 2, "ram": 2, "priorite": 1},
  ]
  res = fifo(processus, nb_cpu=2, ram_totale=4, energie_par_cpu=1, energie=[10, 10, 10, 10])
  # P1 prend toute la RAM (4), P2 doit attendre la libération à t=2
  assert ("P1", 0, 2, "CPU1") in res
  p2_res = [r for r in res if r[0] == "P2"][0]
  assert p2_res[1] == 2  # Démarre à t=2


def test_energie_limit():
  processus = [{"id": "P1", "date_soumission": 0, "duree": 2, "ram": 2, "priorite": 1}]
  # Pas d'énergie au temps 0, suffisante ensuite
  res = fifo(processus, nb_cpu=1, ram_totale=4, energie_par_cpu=2, energie=[0, 10, 10])
  # P1 démarre à t=1 faute d'énergie à t=0
  assert res == [("P1", 1, 3, "CPU1")]


def test_priorite():
  processus = [
      {"id": "P1", "date_soumission": 0, "duree": 2, "ram": 2, "priorite": 1},
      {"id": "P2", "date_soumission": 0, "duree": 2, "ram": 2, "priorite": 5},
  ]
  res = fifo(processus, nb_cpu=1, ram_totale=2, energie_par_cpu=1, energie=[10, 10, 10])
  # P2 a une priorité plus élevée, il doit passer en premier
  assert res[0][0] == "P2"


if __name__ == "__main__":
  test_nominal()
  test_ram_limit()
  test_energie_limit()
  test_priorite()
  print("Tous les tests ont réussi !")