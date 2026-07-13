# Antique Atlas TFOT 8.1.0-tfot.1

- Ajout de la touche `M` dans les deux modes de configuration.
- En mode `itemNeeded = true`, la touche vérifie que le joueur possède un atlas.
- Suppression de la minimap permanente à l’écran.
- Correction de la synchronisation en direct de la carte en mode sans objet.
- Correction d’un format de paquet réseau incompatible entre l’écriture et la lecture.
- Validation renforcée des requêtes de marqueurs, de tuiles et de position de navigation.
- Envoi des mises à jour d’atlas uniquement aux joueurs concernés.
- Nettoyage des caches de carte lors de la déconnexion afin d’éviter les données fantômes entre serveurs.
- Protection des valeurs d’analyse invalides pouvant provoquer une division par zéro.
- Traduction française complétée et libellés de l’interface rafraîchis à chaque ouverture.
- Build GitHub Actions modernisé pour Java 17 avec publication automatique des JAR.
- Suppression d’un ancien mixin de recette vide qui empêchait la compilation Forge 1.20.1.
