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

## Répartition du travail

| Membre | Tâches réalisées | Python | Java |
|---|---|
| Besma Bezzaz | | | |
| Lilian Francisco | | | |
| Julien Faucher | | | |
| Alexis Sudre-Brito | | | |

### Liste des tâches réalisées 

**Partie Python**

- Lecture et vérification du fichier `config.txt` (`lecture.py`)
- Lecture et vérification de `ressources.json` (`lecture.py`)
- Lecture et vérification de `processus.csv` (`lecture.py`)
- Lecture et vérification de `energie.csv` (`lecture.py`)
- Exceptions de lecture, de format et d'écriture (`erreurs.py`)
- Écriture des fichiers CSV de sortie : global, allocations, métriques, énergie (`ecriture.py`)
- Programme principal en ligne de commande (`main.py`)
- Moteur de simulation commun aux algorithmes (`simulateur.py`)
- Politique FIFO (`simulateur.py`)
- Politique Round Robin avec quantum (`simulateur.py`)
- Politique EDF (`simulateur.py`)
- Contrainte de RAM (`simulateur.py`)
- Contrainte d'énergie verte et mise en pause des processus (`simulateur.py`)
- Calcul des métriques : temps d'attente, temps de réponse, makespan, énergie, taux d'utilisation (`metriques.py`)
- Tests unitaires (`tests/test_simulateur.py`)
- Jeux d'essai `Inputs/Test2` et `Inputs/Test3`
- Vérification des résultats à la main sur le Test1
- Rédaction du README

**Partie Java**

- Maquettes des 5 écrans : configuration, processus, Gantt par processus, Gantt par processeur, comparaison
- Mise en place du projet Maven / JavaFX (`pom.xml`, `Main`, `App`)
- Fenêtre principale à onglets (`Principale.fxml`)
- Classe des constantes et des chemins (`Constantes`, `Chemins`)
- Modèle de la configuration (`Configuration`, `Algorithme`, `ErreurConfiguration`)
- Lecture d'un `config.txt` existant (`LecteurConfig`)
- Écriture du `config.txt` (`EcrivainConfig`)
- Écran de configuration : vue (`Configuration.fxml`, `style.css`)
- Écran de configuration : contrôleur (`ConfigurationControleur`)
- Tests de l'écran de configuration (lecture, écriture, compatibilité avec Python)
