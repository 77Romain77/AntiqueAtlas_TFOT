# Antique Atlas TFOT

Fork Forge 1.20.1 d’Antique Atlas, centré sur une carte persistante côté serveur et une interface qui n’encombre pas l’écran.

## Fonctionnement

- La carte complète s’ouvre avec `M` et se ferme avec `M`, `Échap` ou un clic droit.
- Aucune minimap permanente n’est affichée à l’écran.
- L’atlas affiche automatiquement la dimension actuelle du joueur.
- Les données de chaque dimension restent séparées en interne pour éviter les collisions de coordonnées.
- Les tuiles et marqueurs sont enregistrés dans les données du monde du serveur.
- La progression cartographique appartient toujours au joueur et reste sur le serveur.
- `itemNeeded = true` impose de posséder un atlas pour utiliser `M`, sans lier les données à cet objet.
- `itemNeeded = false` permet d’ouvrir exactement la même carte sans objet.

Le mod doit être installé sur le serveur et sur tous les clients. Il nécessite :

- Minecraft `1.20.1` ;
- Forge `47.x` ;
- UnionLib `12.0.18` ou une version compatible antérieure à `12.1.0`.

## Build automatique

GitHub Actions lance `./gradlew clean build` avec Java 17 après chaque push et pour chaque pull request. Le JAR est disponible dans l’artefact `antique-atlas-tfot-forge-1.20.1` du workflow.

Pour construire localement :

```bash
./gradlew clean build
```

Les fichiers générés se trouvent dans `build/libs`.

## Navigation et personnalisation

La carte se déplace à la souris, avec les flèches de l’interface ou avec les touches fléchées. La molette et les touches `+`/`-` contrôlent le zoom. Le bouton d’export produit une image PNG de la dimension actuellement affichée.

Le système historique de textures et l’API `TileAPI`/`MarkerAPI` sont conservés pour la compatibilité avec les ressources et intégrations existantes.

## Vérifications conseillées

1. Installer le mod et UnionLib sur un serveur Forge 1.20.1 et sur le client.
2. Tester `itemNeeded = true`, avec et sans atlas dans l’inventaire.
3. Tester `itemNeeded = false` puis reconnecter le joueur pour vérifier la sauvegarde.
4. Explorer l’Overworld, changer de dimension et vérifier que seule la carte courante s’affiche.
5. Revenir dans l’Overworld et vérifier que sa progression est toujours présente.
6. Ajouter et supprimer un marqueur, puis reconnecter le joueur.
7. Se connecter à un autre serveur et vérifier qu’aucune ancienne tuile n’est conservée côté client.

## Crédits et licence

Ce projet dérive d’[AntiqueAtlasTeam/AntiqueAtlas](https://github.com/AntiqueAtlasTeam/AntiqueAtlas) et de la version Forge maintenue par [Stereowalker](https://github.com/Stereowalker/AntiqueAtlas). Le code reste distribué sous GNU GPL conformément au fichier `LICENSE`.
