# Antique Atlas TFOT 8.2.0-tfot.7

- Génération automatique de textures dédiées au rendu de l’atlas tenu en main
  avec Iris/Oculus :
  les tuiles sont précomposées sur la couleur du parchemin et n’utilisent plus
  d’alpha intermédiaire susceptible d’être mal traité par les shader packs.
- Le cadre tenu en main possède également une variante à alpha binaire.
- Les textures historiques restent utilisées dans l’écran de carte : le correctif
  shader ne modifie donc pas l’apparence de l’interface normale.
- Correctif fondé sur le resource pack partagé par Orgamorsh dans
  [l’issue Antique Atlas #416](https://github.com/AntiqueAtlasTeam/AntiqueAtlas/issues/416).

## 8.2.0-tfot.6

- `Échap` dans le filtre des marqueurs agit désormais comme le bouton **Done** :
  le filtre est retiré proprement et l’atlas reste ouvert et centré.
- Chaque carte possède maintenant son propre bouton **Ouvrir** dans la liste ;
  la carte utilisée est clairement indiquée comme **Active**.
- Correction de l’atlas vide juste après un changement de carte : le contexte du
  livre-atlas est conservé et les données du nouveau profil sont chargées avant
  son affichage.

## 8.2.0-tfot.5

- Correction forcée du modèle des livres reconnus comme atlas : l’icône
  Antique Atlas est désormais sélectionnée directement par le moteur de rendu
  pour le nom personnalisé comme pour le tag NBT.
- Remplacement du bouton d’export PNG par l’accès au gestionnaire **Maps**.
- Suppression de la variante `Maj` + `M` ; seul le raccourci configurable `M`
  reste enregistré dans les contrôles.
- Le gestionnaire s’intitule simplement **Maps** et n’affiche plus l’adresse du serveur.
- Remplacement du masquage global des marqueurs par un filtre persistant par type,
  avec actions « Tout afficher » et « Tout masquer ».

- Conversion en mod Forge 1.20.1 entièrement côté client.
- Suppression du canal réseau obligatoire, des événements serveur, du scan serveur
  et de l’enregistrement `SavedData` utilisé par le serveur.
- Suppression complète de la dépendance UnionLib et remplacement des quelques
  utilitaires encore nécessaires par des implémentations internes sans réseau.
- Compatibilité prévue avec les serveurs ne possédant ni Antique Atlas ni ses données.
- Stockage local séparé par adresse de serveur et par profil de carte.
- Gestion de plusieurs cartes nommées : création, renommage, sélection et suppression.
- Stockage terrain régional compressé en zones de `32 × 32` chunks avec palette.
- Sauvegardes terrain asynchrones et regroupées toutes les cinq secondes.
- Scanner client borné : uniquement les chunks déjà chargés, sans chargement forcé.
- Marqueurs, marqueurs de mort et position de navigation enregistrés localement.
- Affichage limité à la dimension actuelle, avec données distinctes par dimension.
- `M` ouvre la carte ; le gestionnaire de cartes est accessible depuis l’atlas.
- Aucune minimap permanente.
- L’option `itemNeeded` reste disponible : elle requiert désormais un livre vanilla
  renommé `Antique Atlas` ou portant le tag NBT `antiqueatlas:1b`, ce qui évite
  tout objet personnalisé côté serveur.
- Les livres reconnus utilisent désormais la texture d’Antique Atlas dans
  l’inventaire et la barre rapide.
- Un clic droit avec un livre-atlas en main ouvre désormais la même carte que
  la touche `M`. Les interactions utilisables avec les blocs et les entités
  conservent leur priorité.
- Recettes et objets réseau historiques désactivés dans cette édition client.
- Métadonnées, documentation, traductions française/anglaise et CI mises à jour.
- Écran de configuration client autonome et fichier `antiqueatlas-client.json`.
