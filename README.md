# SAE01-2A-Python : simulateur d'ordonnancement

Groupe : Besma Bezzaz, Lilian Francisco, Julien Faucher, Alexis Sudre-Brito

## But

Le programme simule l'ordonnancement d'un ensemble de processus sur plusieurs
processeurs, avec trois algorithmes : **FIFO**, **Round Robin** et **EDF**.
Il écrit les résultats dans des fichiers CSV, que l'application Java affiche.

Deux contraintes sont respectées à chaque instant :
- **RAM** : la somme des RAM des processus en cours ne dépasse jamais la RAM totale ;
- **énergie verte** : on n'allume que `energie_disponible // energieParProc` processeurs.

## Lancement

On utilise Python 3, sans bibliothèque externe. Les commandes se lancent depuis le dossier du projet :

```bash
python3 Code/main.py Inputs/Test1/config.txt                  # résultats dans Outputs/Test1
python3 Code/main.py Inputs/Test1/config.txt mon/dossier      # dossier de sortie au choix
python3 -m unittest discover -s Code/tests -v                 # 25 tests
```

Le code retour vaut 0 si tout s'est bien passé, et 1 en cas d'erreur (message sur `stderr`).

## Organisation du code

| Fichier | Rôle | Sous-groupe |
|---|---|---|
| `erreurs.py` | les 3 exceptions (lecture, format, écriture) | lecture/écriture |
| `lecture.py` | lecture et vérification des 4 fichiers d'entrée | lecture/écriture |
| `ecriture.py` | écriture des fichiers CSV de sortie | lecture/écriture |
| `simulateur.py` | **le moteur commun** aux 3 algorithmes | algorithmes |
| `metriques.py` | calcul des métriques | algorithmes |
| `main.py` | enchaîne lecture → simulation → écriture | tous |
| `tests/test_simulateur.py` | 25 tests unitaires | tous |

## Le moteur (simulateur.py)

Un seul moteur sert aux trois algorithmes. Ils ne diffèrent que sur deux points :

| | Ordre de parcours de la file | Garde le CPU jusqu'à la fin ? |
|---|---|---|
| FIFO | ordre d'arrivée | oui |
| RR | ordre d'arrivée | non : retour en fin de file après `quantum` instants |
| EDF | date butoir la plus proche (puis priorité) | oui |

Voici ce qui se passe à chaque instant `t` :

1. Les processus soumis à `t` entrent dans la file. S'ils arrivent en même temps, le plus prioritaire passe d'abord.
2. (RR) Les processus dont le quantum est fini retournent en fin de file.
3. On calcule `nb_alimentables = min(nbProcesseurs, energie(t) // energieParProc)`.
4. Si l'énergie a baissé, on met en pause les processus en trop. Ils retournent en tête de file.
5. On parcourt la file et on donne un CPU libre à chaque processus dont la RAM tient dans la RAM libre. Sinon, on passe au suivant.
6. Chaque processus en cours avance d'une unité de temps. S'il a fini, `date_fin = t + 1`.

## Fichiers de sortie

- **global** : `id_processus, date_soumission, date_debut, date_fin, ram_demandee, ram_utilisee`
- **allocations** : `id_processus, date_debut, date_fin, id_processeur`. `date_fin` est exclue : `P1,7,12,CPU1` veut dire que P1 tourne aux instants 7 à 11. Un processus a plusieurs lignes s'il a été interrompu (quantum RR ou manque d'énergie).
- **métriques** : `algorithme, temps_attente_moyen, temps_reponse_moyen, makespan, energie_consommee_totale, energie_disponible_totale, taux_utilisation`
- **énergie** (bonus pour le Java) : `instant, energie_disponible, energie_consommee, nb_cpu_actifs`

| Métrique | Calcul |
|---|---|
| temps de réponse | `date_fin − date_soumission` |
| temps d'attente | `temps de réponse − duree_execution` |
| makespan | date de fin du dernier processus |
| taux d'utilisation | énergie consommée / énergie disponible (sur `[0, makespan[`) |

## Hypothèses

1. Après le dernier instant d'`energie.csv`, on **recommence au début du fichier** (journée suivante). Sinon, un processus non terminé le soir ne finirait jamais.
2. Si l'énergie baisse, un processus en cours est **mis en pause**, même en FIFO ou EDF, car le sujet interdit de dépasser l'énergie disponible.
3. Un processus qui demande **plus que la RAM totale** n'est pas exécuté. Il apparaît avec des dates vides et `ram_utilisee = 0`.
4. Un processus reprend **sur le même CPU** qu'avant si ce CPU est libre.

## Trace à la main : Test1, FIFO

La configuration est de 2 CPU, 1024 de RAM et 40 d'énergie par CPU.

| t | énergie | CPU allumables | Ce qui se passe |
|---|---|---|---|
| 0 à 6 | 0 à 15 | 0 | rien ne tourne ; P1, P2 et P3 arrivent dans la file |
| 7 | 45 | 1 | P1 prend CPU1 (RAM 512) |
| 8 | 85 | 2 | P2 (256) tient → CPU2 |
| 9 à 10 | ≥ 120 | 2 | P3 (1024) ne tient pas à côté de P1 + P2 |
| 11 | 175 | 2 | P2 a fini (fin 11) ; P3 ne tient toujours pas (512 libres) |
| 12 à 17 | ≥ 65 | ≥ 1 | P1 a fini (fin 12) ; P3 prend CPU1 et finit à 18 |

- **Temps d'attente moyen** = (7 + 7 + 10) / 3 = **8,00**
- **Temps de réponse moyen** = (12 + 10 + 16) / 3 = **12,67**
- **Makespan** = **18**
- **Énergie consommée** = 14 × 40 = **560** ; disponible sur [0, 18[ = **1450** ; taux = **0,3862**

Avec **EDF**, P3 tourne de 13 à 17, puis l'énergie tombe à 30 à t = 18. P3 est mis en pause et reprend à t = 31 (le lendemain matin). Il finit à 32 et **rate sa deadline (30)**.

## Résultats

| Test | Algo | Attente moy. | Réponse moy. | Makespan | Taux énergie |
|---|---|---|---|---|---|
| Test1 | FIFO | 8,00 | 12,67 | 18 | 0,3862 |
| Test1 | RR (q=2) | 8,67 | 13,33 | 18 | 0,3862 |
| Test1 | EDF | 12,67 | 17,33 | 32 | 0,3613 |
| Test2 | FIFO | 2,50 | 6,50 | 21 | 0,6226 |
| Test2 | RR (q=3) | 2,50 | 6,50 | 19 | 0,6957 |
| Test2 | EDF | 2,62 | 6,62 | 22 | 0,6084 |
| Test3 | FIFO | 0,75 | 3,00 | 7 | 0,5143 |
| Test3 | RR (q=1) | 0,50 | 2,75 | 6 | 0,6000 |
| Test3 | EDF | 0,75 | 3,00 | 7 | 0,5143 |

Contenu des jeux d'essai :
- **Test1** : fichiers fournis par les enseignants.
- **Test2** : 8 processus, 3 CPU, baisse d'énergie en pleine exécution.
- **Test3** : arrivées simultanées, processus trop gros pour la RAM, blocage RAM, quantum 1.
