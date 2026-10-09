# TaskManager

Application de gestion de tâches collaborative en temps réel, réalisée seul pour le cours de Java (Bloc 2, 2025-2026) à la Haute École Robert Schuman (HERS). Le cours portait sur les WebSockets, les streams et JavaFX.

Plusieurs collaborateurs utilisent l'application en même temps : quand l'un d'eux crée, modifie ou clôture une tâche, les autres voient le changement immédiatement, sans recharger.

## Fonctionnalités

* inscription et connexion ;
* tableau de bord des tâches du jour ;
* liste de toutes les tâches et des tâches de l'utilisateur connecté ;
* création d'une tâche : nom, description, échéance (date et heure), assignation facultative à un collaborateur ;
* suivi d'une tâche : commencer, mettre en pause, bloquer, clôturer ;
* assignation et suppression ;
* synchronisation en temps réel entre tous les clients connectés.

Une tâche peut être non ouverte, en cours, en pause, bloquée, en retard, clôturée ou annulée.

## Architecture

Le code est découpé en trois parties :

| Package | Rôle |
| ------- | ---- |
| `client` | Application JavaFX : vues, contexte de session, client WebSocket |
| `server` | Serveur Spring Boot : configuration WebSocket, gestionnaires de messages, services, cache |
| `shared` | Code commun : modèles, DAO JDBC vers Oracle, exceptions |

Le client et le serveur échangent des messages JSON sur une connexion WebSocket (`ws://localhost:8080/ws`). Chaque message porte un champ `action` : `LOGIN`, `REGISTER`, `GET_DASHBOARD`, `GET_TASKS`, `GET_TASK`, `CREATE_TASK`, `UPDATE_TASK`, `ASSIGN_TASK`, `DELETE_TASK`, `GET_COLLABORATORS`. Le serveur aiguille chaque action vers le bon gestionnaire, puis diffuse les changements aux autres clients connectés.

## Choix techniques

* **Écouteurs côté client** : le client WebSocket associe à chaque action une liste d'écouteurs (`Map<String, List<ListenerEntry>>` dans une `ConcurrentHashMap`). Chaque écouteur reçoit un identifiant UUID, ce qui permet de le retirer quand une vue se ferme.
* **Sessions côté serveur** : les sessions actives sont gardées dans une `ConcurrentHashMap`, car plusieurs threads y accèdent en même temps. Chaque envoi est synchronisé sur la session (`synchronized (session)`), parce qu'une session WebSocket n'accepte pas deux écritures simultanées.
* **Diffusion** : après une modification, le serveur envoie la mise à jour à toutes les sessions sauf celle de l'émetteur.
* **Cache en mémoire** : les tâches sont gardées dans un cache côté serveur, vidé après chaque modification.
* **Requêtes SQL** : une tâche et toutes ses données liées (créateur, collaborateur en cours, temps passé) sont chargées en une seule requête avec des jointures, au lieu d'une requête par tâche (problème du N+1).
* **Transactions** : gérées dans la couche service (`setAutoCommit(false)`, `commit`, `rollback`), pas dans les DAO.

## Lancer l'application

### Prérequis

* JDK 23 et Maven ;
* une base Oracle ;
* le SDK JavaFX 21, à placer dans `lib/` (non versionné).

### Étapes

1. Créer la base avec `database/script_creation_bd.sql`, puis la remplir avec `database/initialisation_bd.sql`.
2. Renseigner l'URL, l'utilisateur et le mot de passe de la base dans `src/main/java/TaskManager/shared/DAOs/Database.java`. Dans la version publique, les identifiants sont remplacés par `XXXX`.
3. Lancer le serveur : `mvn spring-boot:run` (port 8080).
4. Lancer un ou plusieurs clients JavaFX depuis l'IDE, avec les modules `javafx.controls` et `javafx.fxml`, ou avec `make run-client`. Le `Makefile` a été écrit pour mon poste (WSL) : les chemins `JAVA`, `JAVAC`, `JAVAFX` et `M2` sont à adapter.

**Aucun identifiant n'est fourni avec ce dépôt.**

## Limites connues

* La connexion se fait par nom d'utilisateur, sans mot de passe.
* Le serveur accepte les connexions WebSocket de toutes les origines (`setAllowedOrigins("*")`), un réglage prévu pour le développement uniquement.

## Projet

Projet individuel réalisé en Bloc 2 (2025-2026) à la **Haute École Robert Schuman (HERS)**.

Version publiée à des fins de portfolio.
