

INSERT INTO Collaborator (login, firstName, lastName) VALUES ('love','Ada', 'Lovelace');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Alan', 'Alan', 'Turing');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Hop','Grace', 'Hopper');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Edsger','Edsger', 'Dijkstra');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Donald','Donald', 'Knuth');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Ritchie','Dennis', 'Ritchie');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Ken','Ken', 'Thompson');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Linux','Linus', 'Torvalds');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Strous','Bjarne', 'Stroustrup');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('James','James', 'Gosling');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Ross','Guido', 'van Rossum');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Tim','Tim', 'Berners-Lee');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Barbie','Barbara', 'Liskov');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Magie','Margaret', 'Hamilton');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('France','Frances', 'Allen');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('John','John', 'McCarthy');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Mins','Marvin', 'Minsky');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Claude','Claude', 'Shannon');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Neumann','John', 'von Neumann');
INSERT INTO Collaborator (login, firstName, lastName) VALUES ('Kath','Katherine', 'Johnson');

COMMIT;

-- 50 tâches avec des statuts variés, des échéances passées et futures
-- Les ids des collaborateurs vont de 1 à 20 selon l'ordre d'insertion

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Rédiger le rapport annuel', TO_DATE('2026-05-10 09:00', 'YYYY-MM-DD HH24:MI'), 5, 1, TO_DATE('2026-05-01 09:00', 'YYYY-MM-DD HH24:MI'), 2);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Mettre à jour la documentation', TO_DATE('2026-05-20 17:00', 'YYYY-MM-DD HH24:MI'), 1, 2, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Corriger les bugs de production', TO_DATE('2026-05-08 12:00', 'YYYY-MM-DD HH24:MI'), 5, 3, TO_DATE('2026-05-07 08:00', 'YYYY-MM-DD HH24:MI'), 4);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Déployer la version 2.0', TO_DATE('2026-05-25 10:00', 'YYYY-MM-DD HH24:MI'), 2, 4, TO_DATE('2026-05-14 10:00', 'YYYY-MM-DD HH24:MI'), 5);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Analyser les logs serveur', TO_DATE('2026-05-15 14:00', 'YYYY-MM-DD HH24:MI'), 3, 5, TO_DATE('2026-05-12 08:00', 'YYYY-MM-DD HH24:MI'), NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Préparer la présentation client', TO_DATE('2026-05-09 11:00', 'YYYY-MM-DD HH24:MI'), 5, 6, TO_DATE('2026-05-06 09:00', 'YYYY-MM-DD HH24:MI'), 7);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Optimiser les requêtes SQL', TO_DATE('2026-05-28 16:00', 'YYYY-MM-DD HH24:MI'), 1, 7, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Configurer le serveur de staging', TO_DATE('2026-05-18 09:00', 'YYYY-MM-DD HH24:MI'), 4, 8, TO_DATE('2026-05-13 09:00', 'YYYY-MM-DD HH24:MI'), NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Écrire les tests unitaires', TO_DATE('2026-05-30 17:00', 'YYYY-MM-DD HH24:MI'), 1, 9, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Réviser le code de la PR#42', TO_DATE('2026-05-07 15:00', 'YYYY-MM-DD HH24:MI'), 5, 10, TO_DATE('2026-05-06 14:00', 'YYYY-MM-DD HH24:MI'), 11);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Migrer la base de données', TO_DATE('2026-06-01 10:00', 'YYYY-MM-DD HH24:MI'), 1, 11, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Mettre en place le monitoring', TO_DATE('2026-05-22 14:00', 'YYYY-MM-DD HH24:MI'), 2, 12, TO_DATE('2026-05-14 11:00', 'YYYY-MM-DD HH24:MI'), 13);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Former les nouveaux développeurs', TO_DATE('2026-05-11 10:00', 'YYYY-MM-DD HH24:MI'), 5, 13, TO_DATE('2026-05-09 09:00', 'YYYY-MM-DD HH24:MI'), NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Mettre à jour les dépendances', TO_DATE('2026-05-19 16:00', 'YYYY-MM-DD HH24:MI'), 1, 14, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Créer le pipeline CI/CD', TO_DATE('2026-05-26 11:00', 'YYYY-MM-DD HH24:MI'), 2, 15, TO_DATE('2026-05-13 10:00', 'YYYY-MM-DD HH24:MI'), 16);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Sécuriser les endpoints API', TO_DATE('2026-05-06 09:00', 'YYYY-MM-DD HH24:MI'), 5, 16, TO_DATE('2026-05-04 08:00', 'YYYY-MM-DD HH24:MI'), 17);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Rédiger les spécifications techniques', TO_DATE('2026-06-05 17:00', 'YYYY-MM-DD HH24:MI'), 1, 17, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Implémenter le système de cache', TO_DATE('2026-05-29 14:00', 'YYYY-MM-DD HH24:MI'), 6, 18, TO_DATE('2026-05-10 09:00', 'YYYY-MM-DD HH24:MI'), NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Planifier le sprint suivant', TO_DATE('2026-05-16 10:00', 'YYYY-MM-DD HH24:MI'), 3, 19, TO_DATE('2026-05-14 09:00', 'YYYY-MM-DD HH24:MI'), NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Corriger la faille XSS', TO_DATE('2026-05-05 08:00', 'YYYY-MM-DD HH24:MI'), 5, 20, TO_DATE('2026-05-04 07:00', 'YYYY-MM-DD HH24:MI'), 1);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Refactoriser le module de paiement', TO_DATE('2026-06-10 16:00', 'YYYY-MM-DD HH24:MI'), 1, 1, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Configurer le pare-feu', TO_DATE('2026-05-17 11:00', 'YYYY-MM-DD HH24:MI'), 2, 2, TO_DATE('2026-05-13 10:00', 'YYYY-MM-DD HH24:MI'), 3);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Rédiger les cas de test', TO_DATE('2026-05-31 17:00', 'YYYY-MM-DD HH24:MI'), 1, 3, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Organiser la réunion de retrospective', TO_DATE('2026-05-13 14:00', 'YYYY-MM-DD HH24:MI'), 3, 4, TO_DATE('2026-05-13 13:00', 'YYYY-MM-DD HH24:MI'), NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Analyser les performances frontend', TO_DATE('2026-05-21 15:00', 'YYYY-MM-DD HH24:MI'), 2, 5, TO_DATE('2026-05-14 09:00', 'YYYY-MM-DD HH24:MI'), 6);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Mettre en place le SSO', TO_DATE('2026-06-15 10:00', 'YYYY-MM-DD HH24:MI'), 1, 6, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Documenter les APIs REST', TO_DATE('2026-05-27 17:00', 'YYYY-MM-DD HH24:MI'), 1, 7, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Corriger les erreurs 500 en production', TO_DATE('2026-05-04 09:00', 'YYYY-MM-DD HH24:MI'), 5, 8, TO_DATE('2026-05-03 08:00', 'YYYY-MM-DD HH24:MI'), 9);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Implémenter la pagination', TO_DATE('2026-05-23 16:00', 'YYYY-MM-DD HH24:MI'), 2, 9, TO_DATE('2026-05-13 14:00', 'YYYY-MM-DD HH24:MI'), 10);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Créer les maquettes UI', TO_DATE('2026-05-12 10:00', 'YYYY-MM-DD HH24:MI'), 3, 10, TO_DATE('2026-05-10 09:00', 'YYYY-MM-DD HH24:MI'), NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Automatiser les sauvegardes', TO_DATE('2026-06-03 11:00', 'YYYY-MM-DD HH24:MI'), 1, 11, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Implémenter les notifications push', TO_DATE('2026-05-24 15:00', 'YYYY-MM-DD HH24:MI'), 6, 12, TO_DATE('2026-05-11 10:00', 'YYYY-MM-DD HH24:MI'), NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Résoudre le problème de mémoire', TO_DATE('2026-05-03 08:00', 'YYYY-MM-DD HH24:MI'), 5, 13, TO_DATE('2026-05-02 08:00', 'YYYY-MM-DD HH24:MI'), 14);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Mettre à jour le certificat SSL', TO_DATE('2026-05-16 09:00', 'YYYY-MM-DD HH24:MI'), 3, 14, TO_DATE('2026-05-15 08:00', 'YYYY-MM-DD HH24:MI'), NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Créer le tableau de bord analytique', TO_DATE('2026-06-08 17:00', 'YYYY-MM-DD HH24:MI'), 1, 15, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Corriger les tests qui échouent', TO_DATE('2026-05-02 12:00', 'YYYY-MM-DD HH24:MI'), 5, 16, TO_DATE('2026-05-01 09:00', 'YYYY-MM-DD HH24:MI'), 17);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Implémenter le mode sombre', TO_DATE('2026-06-12 16:00', 'YYYY-MM-DD HH24:MI'), 1, 17, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Optimiser le temps de chargement', TO_DATE('2026-05-20 14:00', 'YYYY-MM-DD HH24:MI'), 2, 18, TO_DATE('2026-05-13 11:00', 'YYYY-MM-DD HH24:MI'), 19);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Configurer les alertes Grafana', TO_DATE('2026-05-14 10:00', 'YYYY-MM-DD HH24:MI'), 4, 19, TO_DATE('2026-05-13 09:00', 'YYYY-MM-DD HH24:MI'), NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Revoir l architecture microservices', TO_DATE('2026-06-20 17:00', 'YYYY-MM-DD HH24:MI'), 1, 20, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Corriger le bug de session expirée', TO_DATE('2026-05-01 09:00', 'YYYY-MM-DD HH24:MI'), 5, 1, TO_DATE('2026-04-30 08:00', 'YYYY-MM-DD HH24:MI'), 2);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Implémenter le rate limiting', TO_DATE('2026-05-19 15:00', 'YYYY-MM-DD HH24:MI'), 1, 2, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Nettoyer les données obsolètes', TO_DATE('2026-05-13 16:00', 'YYYY-MM-DD HH24:MI'), 3, 3, TO_DATE('2026-05-13 10:00', 'YYYY-MM-DD HH24:MI'), NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Mettre à jour la politique RGPD', TO_DATE('2026-06-02 10:00', 'YYYY-MM-DD HH24:MI'), 1, 4, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Résoudre les conflits Git', TO_DATE('2026-05-14 12:00', 'YYYY-MM-DD HH24:MI'), 2, 5, TO_DATE('2026-05-14 09:00', 'YYYY-MM-DD HH24:MI'), 6);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Préparer le plan de reprise', TO_DATE('2026-06-18 17:00', 'YYYY-MM-DD HH24:MI'), 1, 6, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Analyser les feedbacks utilisateurs', TO_DATE('2026-05-22 11:00', 'YYYY-MM-DD HH24:MI'), 7, 7, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Corriger le problème de CORS', TO_DATE('2026-05-06 14:00', 'YYYY-MM-DD HH24:MI'), 5, 8, TO_DATE('2026-05-05 09:00', 'YYYY-MM-DD HH24:MI'), 9);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Implémenter la recherche full-text', TO_DATE('2026-06-07 16:00', 'YYYY-MM-DD HH24:MI'), 1, 9, NULL, NULL);

INSERT INTO Task (description, echeance, idState, creator, startTime, currentWorker)
VALUES ('Valider les exigences métier', TO_DATE('2026-05-15 10:00', 'YYYY-MM-DD HH24:MI'), 3, 10, TO_DATE('2026-05-14 14:00', 'YYYY-MM-DD HH24:MI'), NULL);

COMMIT;