# Antique Atlas TFOT 8.2.0-tfot.3

- Conversion en mod Forge 1.20.1 entièrement côté client.
- Suppression du canal réseau obligatoire, des événements serveur, du scan serveur
  et de l’enregistrement `SavedData` utilisé par le serveur.
- Compatibilité prévue avec les serveurs ne possédant ni Antique Atlas ni ses données.
- Stockage local séparé par adresse de serveur et par profil de carte.
- Gestion de plusieurs cartes nommées : création, renommage, sélection et suppression.
- Stockage terrain régional compressé en zones de `32 × 32` chunks avec palette.
- Sauvegardes terrain asynchrones et regroupées toutes les cinq secondes.
- Scanner client borné : uniquement les chunks déjà chargés, sans chargement forcé.
- Marqueurs, marqueurs de mort et position de navigation enregistrés localement.
- Affichage limité à la dimension actuelle, avec données distinctes par dimension.
- `M` ouvre la carte ; `Maj` + `M` ouvre le gestionnaire de cartes.
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
- Écran de configuration client autonome et fichier `antiqueatlas-client.json`,
  sans initialiser le système réseau de la classe de base UnionLib.
