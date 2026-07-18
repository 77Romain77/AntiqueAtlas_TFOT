# Antique Atlas TFOT

Fork d’**Antique Atlas pour Forge 1.20.1**, adapté pour fonctionner entièrement
du côté client sur les serveurs de *The Fortress Of Thieves*.

Le serveur n’a pas besoin du mod : cette édition ne crée aucun canal réseau,
n’envoie aucune donnée de carte et ne demande jamais au serveur de charger des
chunks supplémentaires.

## Principales différences de l’édition TFOT

- fonctionnement entièrement côté client, compatible avec un serveur qui ne
  possède pas Antique Atlas ;
- suppression de la dépendance UnionLib et de son contrôle de canaux réseau ;
- carte complète ouvrable avec `M`, sans minimap permanente à l’écran ;
- livre vanilla facultatif servant d’objet atlas, sans enregistrer de nouvel
  objet que le serveur devrait connaître ;
- plusieurs cartes locales par serveur et par pseudo ;
- terrain exploré partagé entre les cartes, avec marqueurs et réglages propres à
  chaque carte ;
- sauvegarde locale découpée en régions et réalisée sur un thread dédié ;
- séparation automatique des données selon l’adresse du serveur et le pseudo ;
- liaison légère des fichiers au pseudo pour décourager leur échange direct ;
- filtres de marqueurs par carte, groupes de marqueurs par icône et nouvelles
  icônes TFOT ;
- marqueur de mort automatique activable séparément pour chaque carte ;
- actualisation manuelle d’une zone déjà explorée ;
- rendu de l’atlas en main compatible avec Iris/Oculus, les shaders et les
  resource packs ;
- interface recentrée et coordonnées corrigées à tous les niveaux de zoom ;
- rendu du terrain regroupé par texture pour limiter fortement les appels
  graphiques lorsque beaucoup de tuiles sont visibles ;
- traductions françaises et anglaises de toutes les fonctionnalités TFOT ;
- build Forge automatique avec GitHub Actions.

## Installation

Prérequis :

- Minecraft `1.20.1` ;
- Forge `47.x` ;
- Java `17`.

Placez le JAR uniquement dans le dossier `mods` du client. Il n’est pas
nécessaire de l’installer sur le serveur.

Retirez l’ancien Antique Atlas avant d’installer cette édition. UnionLib n’est
plus requis ; il peut également être retiré si aucun autre mod ne l’utilise.
Conserver UnionLib inutilement peut encore provoquer son propre message de
canaux incompatibles lors de la connexion à un serveur qui ne le possède pas.

## Ouverture de la carte

- `M` ouvre ou ferme la carte complète. Cette touche reste modifiable dans les
  contrôles Minecraft.
- Un clic droit avec un livre reconnu comme atlas ouvre également la carte.
- Le clic droit fonctionne avec l’objet en main principale ou secondaire.
- Les interactions avec un bloc ou une entité, comme un coffre ou une porte,
  conservent la priorité sur l’ouverture de l’atlas.
- Un clic droit dans l’interface ferme aussi le livre.
- Le gestionnaire de cartes est accessible avec le marque-page **Maps**. Il
  remplace l’ancien bouton d’export PNG.
- Aucun raccourci supplémentaire, comme `Maj + M`, n’est ajouté aux contrôles.

Il n’existe aucune minimap permanente : la petite carte visible dans les mains
fait uniquement partie du rendu du livre tenu par le joueur.

## Livre-atlas facultatif

Le réglage `itemNeeded` détermine si un objet est obligatoire :

- `false` par défaut : `M` ouvre directement la carte ;
- `true` : le joueur doit posséder un livre-atlas reconnu dans son inventaire
  ou sa main secondaire.

Le mod reconnaît uniquement un `minecraft:book` :

- renommé `Antique Atlas`, `Atlas Antique`, `Atlas Antiguo`, `Antyczny Atlas`
  ou `Античный Атлас` ;
- ou portant le tag NBT booléen `antiqueatlas`.

La comparaison du nom ignore les majuscules et les espaces placés au début ou à
la fin. Le tag NBT permet de conserver un nom entièrement personnalisé :

```mcfunction
/give @s minecraft:book{antiqueatlas:1b}
```

Un livre reconnu :

- utilise la texture d’objet Antique Atlas dans l’inventaire et la barre rapide ;
- affiche la carte tenue à une ou deux mains comme l’atlas historique ;
- ouvre la carte avec un clic droit ;
- continue de fonctionner après avoir été renommé si le tag NBT est présent.

Aucun objet personnalisé et aucune recette serveur ne sont enregistrés. Le
serveur continue donc de voir un simple livre vanilla.

## Gestion de plusieurs cartes

Le marque-page **Maps** ouvre un gestionnaire local permettant de :

- créer une carte ;
- renommer la carte sélectionnée ;
- sélectionner et ouvrir une autre carte ;
- identifier immédiatement la carte active ;
- supprimer une carte après confirmation ;
- faire défiler la liste avec la molette ou la barre de défilement.

Les actions **Nouvelle carte**, **Renommer**, **Supprimer** et **Retour** utilisent
des boutons de même taille. Le bouton **Ouvrir** reste placé directement à côté
de chaque carte afin de rendre la sélection explicite.

Il est impossible de supprimer la dernière carte. Le nombre maximal de cartes
est défini par `maxMaps`, avec une valeur par défaut de `10`. Une fois cette
limite atteinte, le bouton **Nouvelle carte** devient gris et son survol indique
qu’une carte doit être supprimée avant d’en créer une autre. Diminuer la limite
ne supprime jamais les cartes déjà présentes.

Changer de carte recharge immédiatement ses marqueurs et ses réglages, sans
ouvrir temporairement une carte vide.

### Données partagées ou propres à une carte

| Donnée | Portée |
|---|---|
| Terrain exploré | Partagé entre toutes les cartes du même serveur et du même pseudo |
| Dimensions du terrain | Séparées en interne dans le terrain partagé |
| Marqueurs | Propres à chaque carte |
| Types de marqueurs masqués | Propres à chaque carte |
| Marqueur de mort automatique | Activé ou désactivé par carte |
| Position, zoom et navigation | Propres à chaque carte et à chaque dimension |
| Carte active | Mémorisée pour le serveur et le pseudo concernés |

Le partage du terrain évite d’explorer et d’analyser plusieurs fois les mêmes
chunks simplement parce qu’une autre carte a été sélectionnée.

## Dimensions et terrain

- La carte affiche uniquement la dimension dans laquelle se trouve actuellement
  le joueur.
- L’Overworld, le Nether, l’End et les dimensions personnalisées utilisent des
  espaces internes séparés pour éviter toute collision de coordonnées.
- Toutes les dimensions restent dans le même monde local partagé ; elles ne
  nécessitent pas une nouvelle carte de profil.
- Les données de terrain sont déterminées uniquement à partir des chunks déjà
  reçus par le client.

Aux niveaux de dézoom inférieurs à `0.5`, plusieurs chunks sont regroupés pour
conserver des tuiles lisibles. Le dessin du terrain devient volontairement plus
simplifié, mais ses coordonnées restent alignées avec le joueur et les marqueurs.

Le terrain, le joueur, les marqueurs, les clics et le zoom sous la souris
utilisent tous le centre du livre comme origine. Le livre lui-même reste centré
à l’écran, indépendamment des marque-pages à droite ou des listes à gauche.

Pour limiter la baisse de FPS autour du zoom `×1`, les sous-tuiles visibles qui
utilisent la même texture sont envoyées au GPU dans un seul lot. Les parties
réellement hors de la page sont aussi écartées avant le rendu. Cette optimisation
ne modifie ni l’apparence de la carte ni les textures fournies par les resource
packs.

## Marqueurs

### Création et navigation

Le marque-page rouge permet d’ajouter un marqueur avec une icône et un nom. Dans
la fenêtre de création, **Annuler** se trouve à gauche et **Terminé** à droite.
Le marque-page jaune permet de supprimer un marqueur.

La colonne située à gauche du livre regroupe les marqueurs visibles par type
d’icône :

- le survol d’une catégorie affiche uniquement son nombre de marqueurs ;
- un clic ouvre la liste défilable des marqueurs de cette catégorie ;
- un clic sur un marqueur déplace la vue de la carte jusqu’à sa position ;
- aucune coordonnée n’est affichée en texte dans cette liste ;
- la catégorie ouverte est refermée lors de la fermeture de la carte.

### Filtres par carte

Le marque-page vert ouvre les filtres :

- chaque type peut être rendu **Visible** ou **Masqué** ;
- **Tout afficher** et **Tout masquer** permettent une modification globale ;
- le filtre s’applique à la carte complète et au livre tenu en main ;
- les choix sont enregistrés dans le `profile.dat` de la carte active ;
- la touche `Échap` ferme proprement le menu comme le bouton **Terminé**.

Le filtre affiche seulement l’icône et son état, sans répéter le nom du type.

### Icônes TFOT ajoutées

Les nouvelles textures ont été normalisées pour conserver la même taille et le
même alignement que les marqueurs historiques :

- parchemin d’artéfact ;
- bague ;
- bateau ;
- chapitre ;
- crochet ;
- point d’exclamation ;
- lingot d’or ;
- carte ;
- PNJ ;
- quête ;
- boutique ;
- pelle ;
- étoile.

Le marqueur **Portail du Nether** a été supprimé. Les anciennes occurrences de
ce type sont ignorées lors du chargement des sauvegardes.

### Marqueur de mort automatique

Le marque-page rouge avec une tombe, placé entre les filtres et le gestionnaire
de cartes, active ou désactive cette fonctionnalité pour la carte courante :

- le bouton devient gris lorsqu’elle est désactivée, tout en restant cliquable ;
- une seule tombe portant le texte **« Vous êtes mort ici »** est créée par mort ;
- la tombe est enregistrée dans la dimension et la carte actives ;
- changer de carte permet d’utiliser un réglage différent ;
- l’icône de tombe est recadrée et centrée comme les autres marque-pages.

La détection écoute le paquet vanilla de mort envoyé par le serveur. Elle ne
vérifie pas l’état du joueur à chaque tick et ne nécessite aucun mod serveur.

## Exploration et actualisation du terrain

### Découverte automatique

Le scanner client :

- réagit au déplacement, au changement de dimension et à l’arrivée de nouveaux
  chunks ;
- vérifie également les chunks chargés une fois par seconde lorsque le joueur
  reste immobile ;
- analyse uniquement les chunks réellement présents dans le cache client ;
- ne force jamais leur chargement sur le serveur ;
- répartit le travail selon `clientScanBudget` pour éviter un pic sur une seule
  frame.

`scanRadius` définit la zone circulaire vérifiée autour du joueur. Une valeur
supérieure à la distance de rendu serveur ne permet pas de récupérer des chunks
supplémentaires.

### Bouton Actualiser la zone

Le marque-page bleu avec la flèche permet de rescanner ponctuellement les chunks
déjà cartographiés et encore chargés autour du joueur. Il est utile lorsqu’une
mise à jour a modifié une zone déjà explorée.

- son tooltip explique son fonctionnement sur plusieurs lignes ;
- un clic crée une file bornée de chunks connus et chargés ;
- le bouton est bloqué jusqu’à la fin de l’opération pour éviter le spam ;
- la progression est affichée au survol ;
- un message final indique combien de chunks ont été analysés et modifiés ;
- le même `clientScanBudget` limite le travail effectué par tick.

Le rescan reste volontairement manuel : l’ancien réglage automatique `doRescan`
n’est pas utilisé par cette édition.

## Sauvegarde locale

Les données sont stockées sous :

```text
.minecraft/config/antiqueatlas/servers/<serveur>-<hash>/
└── players/<pseudo>-<empreinte>/
    ├── maps.json
    ├── maps/
    │   ├── map_0001/profile.dat
    │   ├── map_0002/profile.dat
    │   └── ...
    └── worlds/world_0001/
        ├── world.dat
        └── dimensions/
            └── <namespace>/<dimension>/terrain/
                ├── r.<x>.<z>.dat
                └── ...
```

- `maps.json` contient le pseudo propriétaire, la carte active et les noms des
  profils ;
- chaque `profile.dat` contient les marqueurs, filtres, positions de navigation
  et le réglage du marqueur de mort ;
- `world.dat` décrit le terrain partagé et sa liaison au propriétaire ;
- chaque fichier `r.<x>.<z>.dat` contient au maximum `32 × 32` chunks avec une
  palette compressée.

Seules les régions modifiées sont réécrites. Les sauvegardes sont regroupées
toutes les `100` ticks, soit environ cinq secondes, puis exécutées par un thread
client dédié. Une déconnexion ou la fermeture du client force l’écriture des
données restantes. Les fichiers temporaires sont remplacés de manière atomique
lorsque le système le permet.

Ces choix évitent de bloquer le rendu, même lorsque la zone explorée devient
importante. Ils n’ajoutent aucune charge de sauvegarde au serveur.

### Séparation par serveur

L’adresse saisie dans la liste multijoueur, normalisée puis complétée par un hash,
sélectionne le dossier du serveur. Une partie solo utilise le nom du monde.

- la même adresse recharge les mêmes données ;
- une adresse ou un port différent crée un autre espace local ;
- changer l’IP du serveur ne déplace pas automatiquement les anciennes cartes.

### Séparation et protection par pseudo

Cette édition utilise le pseudo du profil de connexion plutôt que l’UUID du
compte. Elle convient donc aussi à un serveur autorisant les launchers hors-ligne.

- un nouveau pseudo obtient son propre dossier et ses propres cartes ;
- revenir à un ancien pseudo recharge automatiquement son dossier précédent ;
- le pseudo lisible apparaît dans le nom du dossier et dans `maps.json` pour
  faciliter le diagnostic ;
- le pseudo normalisé est utilisé dans une empreinte SHA-256 ;
- les profils possèdent également un identifiant aléatoire ;
- le monde partagé et les régions contiennent des empreintes couvrant leur
  propriétaire, leur dimension, leurs coordonnées et leur contenu.

Il ne s’agit pas d’un chiffrement ni d’une protection inviolable. Le but est de
faire échouer le simple copier-coller des fichiers vers un autre pseudo sans
ajouter de compte central, de service externe ou de traitement lourd.

Pour transférer une carte sur un autre PC, il faut conserver la même adresse de
serveur et le même pseudo, puis copier le dossier complet du joueur. Copier
uniquement quelques régions ne suffit pas, car les fichiers de liaison doivent
rester cohérents.

### Migration des anciennes sauvegardes

Lors du premier lancement de ce format :

- l’ancien dossier racine est déplacé dans le dossier du pseudo actuel ;
- le terrain de la carte active est importé en priorité ;
- les autres cartes complètent uniquement les chunks encore manquants ;
- le nouveau terrain partagé est ensuite utilisé par tous les profils ;
- les anciens fichiers de régions restent sur place comme sauvegarde de migration.

## Configuration client

Le fichier est créé automatiquement ici :

```text
.minecraft/config/antiqueatlas-client.json
```

Le bouton **Config** de la liste des mods permet de modifier les options les plus
utiles : livre obligatoire, mémorisation de la position, rayon d’analyse et
budget d’analyse. Les autres valeurs avancées peuvent être modifiées dans le
JSON, de préférence lorsque le jeu est fermé.

### Options principales

| Option | Défaut | Fonction |
|---|---:|---|
| `itemNeeded` | `false` | Rend le livre-atlas obligatoire |
| `doSaveBrowsingPos` | `true` | Mémorise la position et le zoom de chaque carte |
| `maxMaps` | `10` | Limite la création de nouveaux profils |
| `scanRadius` | `11` | Rayon circulaire des chunks client vérifiés, limité entre `0` et `32` |
| `clientScanBudget` | `8` | Nombre maximal de chunks analysés par tick, limité entre `1` et `64` |
| `markerLimit` | `1024` | Nombre maximal de marqueurs enregistrés dans une carte |
| `defaultScale` | `0.5` | Zoom initial d’une nouvelle dimension |
| `minScale` | `0.03125` | Dézoom minimal, soit `1/32` |
| `maxScale` | `4.0` | Zoom maximal |
| `doReverseWheelZoom` | `false` | Inverse le sens de la molette |
| `doScaleMarkers` | `false` | Adapte la taille des marqueurs au niveau de zoom |
| `doScanPonds` | `false` | Active la détection historique des étendues d’eau ou de lave |
| `doScanRavines` | `false` | Active la détection historique des ravins |
| `debugRender` | `false` | Affiche les informations de diagnostic du rendu |
| `resourcePackLogging` | `false` | Journalise le chargement des ressources Antique Atlas |

### Apparence de l’atlas tenu en main

| Option | Défaut | Fonction |
|---|---:|---|
| `tileSize` | `8` | Taille des tuiles sur le livre tenu |
| `markerSize` | `16` | Taille des marqueurs sur le livre tenu ; `0` les masque |
| `playerIconWidth` | `14` | Largeur de la flèche du joueur en main |
| `playerIconHeight` | `16` | Hauteur de la flèche du joueur en main |

`autoVillageMarkers`, `forceChunkLoading` et `newScanInterval` sont conservés
pour la compatibilité du code ou des anciens fichiers de configuration, mais ne
pilotent aucune fonction de cette édition client-only. `doRescan` et
`rescanRate` ne sont plus sérialisés : le rescan est déclenché uniquement par le
bouton de la carte.

Pour un serveur configuré avec une distance de rendu de `8`, un réglage équilibré
est `scanRadius = 8` et `clientScanBudget = 4` ou `8`. Un budget plus faible
réduit le travail instantané mais remplit la carte un peu plus lentement.

Les filtres de marqueurs et le marqueur de mort ne sont pas enregistrés dans ce
JSON, car ils appartiennent au `profile.dat` de chaque carte.

## Shaders et resource packs

Le rendu du livre tenu détecte facultativement l’API Iris/Oculus sans imposer de
dépendance. Lorsqu’un shader pack est actif, il utilise des couches de rendu
d’entité solides ou translucides compatibles afin d’éviter les zones de carte
transparentes observées avec Oculus.

Le correctif ne remplace pas les textures originales par un pack intégré. Les
chemins de textures Antique Atlas restent donc inchangés et un resource pack peut
toujours modifier le livre, le terrain et les marqueurs. Le comportement a été
validé sur Forge 1.20.1 avec Oculus 1.8.0.

## Traductions

Tous les textes ajoutés par cette édition utilisent des clés dans le dossier
`assets/antiqueatlas/lang`.

- `en_us.json` contient la version anglaise complète ;
- `fr_fr.json` contient la version française complète ;
- les autres langues historiques restent présentes et utilisent le fallback
  anglais pour les nouvelles clés qui n’ont pas encore été traduites.

Cela couvre notamment le gestionnaire de cartes, les filtres, les compteurs de
marqueurs, le bouton d’actualisation, le marqueur de mort, la configuration et
les messages d’état.

## Fonctionnalités volontairement absentes

Cette édition n’ajoute pas :

- de minimap permanente ;
- de partage automatique des cartes entre joueurs ;
- de stockage des cartes sur le serveur ;
- d’import de marqueurs Xaero ;
- de seconde touche pour le gestionnaire de cartes ;
- de chargement forcé de chunks ;
- de service externe ou de compte en ligne pour protéger les fichiers.

## Build et GitHub Actions

Build local :

```bash
./gradlew --no-daemon clean build
```

GitHub Actions exécute le même build avec Java 17 après chaque push, pull request
ou lancement manuel. Le JAR est publié pendant 14 jours dans l’artefact :

```text
antique-atlas-tfot-forge-1.20.1
```

## Vérifications conseillées

1. Installer uniquement Antique Atlas TFOT sur le client.
2. Rejoindre un serveur Forge 1.20.1 qui ne possède pas le mod.
3. Vérifier que `M` ouvre la carte sans objet avec `itemNeeded = false`.
4. Tester un livre renommé et un livre portant `{antiqueatlas:1b}` dans les deux mains.
5. Tester le clic droit dans le vide, puis sur un coffre ou une porte.
6. Vérifier l’icône de l’objet dans l’inventaire et son rendu à une ou deux mains.
7. Créer, renommer, ouvrir et supprimer plusieurs cartes, puis tester le scroll.
8. Atteindre `maxMaps` et vérifier le bouton gris ainsi que son tooltip.
9. Explorer avec une carte puis en ouvrir une autre : le terrain doit être identique
   immédiatement, tandis que les marqueurs et filtres restent indépendants.
10. Changer de dimension et vérifier que seule la dimension actuelle est affichée.
11. Fermer puis rouvrir le jeu et vérifier la persistance de toutes les données.
12. Centrer le joueur et dézoomer jusqu’à `1/32` : terrain, joueur et marqueurs
    doivent rester alignés, et le livre doit rester centré à l’écran.
13. Ouvrir une catégorie de marqueurs, naviguer vers un marqueur, fermer puis
    rouvrir la carte et vérifier que la catégorie est refermée.
14. Configurer des filtres différents sur deux cartes et vérifier leur persistance.
15. Tester le marqueur de mort activé sur une carte et désactivé sur une autre.
16. Utiliser **Actualiser la zone** et vérifier le verrouillage, la progression
    ainsi que le message final.
17. Activer Oculus avec un shader pack puis un resource pack modifiant l’atlas.
18. Changer de pseudo, vérifier l’apparition d’un nouvel espace vide, puis revenir
    au premier pseudo et vérifier le retour de ses anciennes cartes.
19. Copier le dossier complet sur un second PC avec le même serveur et le même pseudo.

## Crédits et licence

Ce projet dérive d’[AntiqueAtlasTeam/AntiqueAtlas](https://github.com/AntiqueAtlasTeam/AntiqueAtlas),
de la version Forge maintenue par [Stereowalker](https://github.com/Stereowalker/AntiqueAtlas)
et reprend certaines idées d’architecture de la génération moderne.

Le code reste distribué sous GNU GPL conformément au fichier `LICENSE`.
