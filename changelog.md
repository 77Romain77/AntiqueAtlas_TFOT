# Antique Atlas TFOT 8.2.0-tfot.13

- Ajout de quatorze types de marqueurs TFOT : parchemin d’artéfact, bague,
  bateau, chapitre, crochet, point d’exclamation, lingot d’or, carte, PNJ,
  quête, boutique, pelle, étoile et inconnu.
- Toutes les nouvelles textures conservent un canevas `32 × 32`, mais leur
  dessin visible est ramené à environ `14–16` pixels afin de correspondre à la
  taille des marqueurs historiques dans la carte et dans les menus.
- L’image de PNJ fournie séparément étant identique à celle du ZIP, elle n’est
  enregistrée qu’une seule fois sous le type `PNJ`.
- Suppression du marqueur **Portail du Nether**, de son ancienne option
  automatique et du code serveur historique associé. Les anciens marqueurs de
  ce type ne sont plus chargés dans les cartes locales.
- Le filtre des marqueurs n’affiche plus le nom du type au survol : seule
  l’indication **Visible** ou **Masqué** est conservée.
- Le grand symbole du bouton **Actualiser la zone** est légèrement décalé vers
  le haut et la droite afin d’être visuellement centré dans son marque-page.

## 8.2.0-tfot.12

- Le survol d’une catégorie de marqueurs affiche uniquement son compteur, sans
  répéter le nom du type d’icône.
- La catégorie ouverte et sa liste sont systématiquement refermées quand le
  joueur ferme l’atlas ; chaque nouvelle ouverture repart donc avec la seule
  colonne des catégories.
- Le symbole du bouton **Actualiser la zone** est encore agrandi afin d’occuper
  visuellement la même place que les autres icônes de marque-page.

## 8.2.0-tfot.11

- La colonne gauche regroupe désormais les marqueurs par type d’icône au lieu
  d’afficher immédiatement tous les marqueurs de la dimension.
- Le survol d’une catégorie indique son nom et son nombre de marqueurs. Un clic
  ouvre une seconde liste défilable à gauche ; un nouveau clic referme la
  catégorie et une autre catégorie remplace celle déjà ouverte.
- Les marqueurs individuels conservent uniquement leur nom au survol, sans
  afficher leurs coordonnées, et un clic recentre toujours la carte dessus.
- Le symbole du bouton **Actualiser la zone** est agrandi, séparé du bouton
  **Maps** et son texte d’aide est réparti sur plusieurs lignes.

## 8.2.0-tfot.10

- Les types de marqueurs masqués ne sont plus stockés dans
  `antiqueatlas-client.json` : chaque profil de carte conserve désormais ses
  propres filtres dans son fichier `profile.dat`.
- Les filtres s’appliquent à l’écran complet ainsi qu’à l’atlas tenu en main.
- Ajout d’un marque-page **Actualiser la zone** dans l’atlas. Il réanalyse une
  seule fois les chunks déjà cartographiés et actuellement chargés autour du
  joueur, sans jamais demander de chunk au serveur.
- L’actualisation partage le budget `clientScanBudget`, compare les nouvelles
  tuiles aux anciennes et n’écrit que les régions réellement modifiées.
- Le bouton explique son rôle au survol, affiche la progression et reste bloqué
  jusqu’à la fin du passage afin d’empêcher les rescans en double.
- Les anciennes options automatiques `doRescan` et `rescanRate` sont retirées du
  JSON client au profit de cette action manuelle et sans charge permanente.

## 8.2.0-tfot.9

- Suppression des 190 textures shader-safe générées pendant le build : le rendu
  en main utilise de nouveau exactement les ressources originales.
- Les resource packs peuvent donc remplacer le livre, le cadre et toutes les
  tuiles de la carte tenue en main comme dans l’écran normal.
- Nouveau correctif de rendu : lorsque Iris/Oculus indique qu’un shader pack est
  actif, l’atlas emploie les couches d’entité adaptées aux parties opaques et
  translucides, avec le format de sommets complet attendu par ces couches.
- Iris/Oculus reste entièrement optionnel et est détecté par réflexion, sans
  nouvelle dépendance ni installation nécessaire sur le serveur.
- Correctif porté de l’approche fusionnée dans
  [Antique Atlas 4 #332](https://github.com/sleepingdragoninn/antique-atlas/pull/332).

## 8.2.0-tfot.8

- La liste de l’écran **Maps** s’adapte à la hauteur disponible et devient
  défilable à la molette ou avec sa barre de défilement.
- Les boutons **Nouvelle carte**, **Supprimer**, **Retour** et **Renommer** ont
  maintenant exactement la même largeur.
- Ajout de la limite configurable `maxMaps`, fixée à `10` par défaut dans
  `config/antiqueatlas-client.json`.
- Atteindre la limite ne supprime aucune carte existante : seule la création est
  bloquée. Le bouton **Nouvelle carte** est alors grisé et explique au survol
  qu’une carte doit être supprimée avant d’en créer une autre.
- Le compteur de cartes et la limite sont affichés en haut du gestionnaire.

## 8.2.0-tfot.7

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
