# Antique Atlas TFOT

Fork **Forge 1.20.1 entièrement côté client** d’Antique Atlas. Le serveur n’a
pas besoin du mod et ne reçoit aucun paquet, scan de chunk ou fichier de carte.

## Fonctionnement

- `M` ouvre ou ferme la carte complète ; aucune minimap permanente n’est affichée.
- `Maj` + `M` ouvre le gestionnaire de cartes du serveur courant.
- Chaque adresse de serveur possède ses propres cartes locales.
- Il est possible de créer, renommer, sélectionner et supprimer plusieurs cartes.
- La carte affiche uniquement la dimension où se trouve actuellement le joueur.
- Les données des dimensions sont séparées en interne pour éviter les collisions.
- Les marqueurs, la position de navigation et les tuiles sont sauvegardés localement.
- Le scanner ne lit que les chunks déjà reçus et chargés par le client, avec un
  budget configurable ; il ne force jamais le chargement d’un chunk serveur.

Les fichiers se trouvent dans :

```text
.minecraft/config/antiqueatlas/servers/<adresse-hachée>/
├── maps.json
└── maps/<profil>/
    ├── profile.dat
    └── dimensions/<namespace>/<dimension>/terrain/r.<x>.<z>.dat
```

Une région contient au maximum `32 × 32` chunks avec une palette compressée.
Les écritures de régions sont effectuées sur un thread client dédié et regroupées
toutes les cinq secondes, afin de ne pas bloquer le rendu.

## Objet optionnel

`itemNeeded = false` (par défaut) permet d’ouvrir la carte directement avec `M`.

Avec `itemNeeded = true`, le joueur doit avoir dans son inventaire un **livre
vanilla renommé `Antique Atlas`**. Aucun objet personnalisé n’est enregistré :
le client reste ainsi compatible avec un serveur qui ne possède pas le mod.
Tenir ce livre affiche la carte dans les mains, uniquement à ce moment-là.
Les réglages sont accessibles depuis le bouton **Config** de la liste des mods et
sont enregistrés dans `.minecraft/config/antiqueatlas-client.json`.

## Prérequis et build

- Minecraft `1.20.1` ;
- Forge `47.x` ;
- UnionLib `12.0.18` à `< 12.1.0`, installé uniquement sur le client.

```bash
./gradlew --no-daemon clean build
```

GitHub Actions exécute le même build sur chaque push et pull request puis publie
le JAR dans l’artefact `antique-atlas-tfot-forge-1.20.1`.

## Vérifications conseillées

1. Installer Antique Atlas TFOT et UnionLib uniquement sur le client.
2. Rejoindre un serveur Forge 1.20.1 dépourvu du mod.
3. Explorer, fermer Minecraft, se reconnecter et vérifier la persistance.
4. Changer de dimension et vérifier que seule la dimension actuelle est affichée.
5. Tester la création, le renommage et la suppression de plusieurs cartes.
6. Tester les deux valeurs de `itemNeeded`.

## Crédits et licence

Ce projet dérive d’[AntiqueAtlasTeam/AntiqueAtlas](https://github.com/AntiqueAtlasTeam/AntiqueAtlas),
de la version Forge maintenue par [Stereowalker](https://github.com/Stereowalker/AntiqueAtlas)
et reprend certaines idées d’architecture de la génération moderne. Le code reste
distribué sous GNU GPL conformément au fichier `LICENSE`.
