# Antique Atlas TFOT

Fork **Forge 1.20.1 entièrement côté client** d’Antique Atlas. Le serveur n’a
pas besoin du mod et ne reçoit aucun paquet, scan de chunk ou fichier de carte.

## Fonctionnement

- `M` ouvre ou ferme la carte complète ; aucune minimap permanente n’est affichée.
- Un clic droit avec un livre-atlas en main ouvre également la carte. Un coffre,
  une porte ou une autre interaction utilisable conserve la priorité.
- Le bouton **Maps** de l’atlas ouvre le gestionnaire de cartes ; aucun second
  raccourci clavier n’est ajouté aux contrôles.
- Chaque adresse de serveur possède ses propres cartes locales.
- Les cartes sont liées au pseudo de connexion Minecraft : un transfert complet
  vers un nouveau PC fonctionne avec le même pseudo, tandis qu’un autre pseudo
  reçoit un espace vide et ne peut pas charger les profils ou régions copiés.
- Il est possible de créer, renommer, sélectionner et supprimer plusieurs cartes.
  La carte active est indiquée dans la liste et chaque autre carte possède son
  propre bouton **Ouvrir**. La liste est défilable lorsqu’elle dépasse la place
  disponible.
- La carte affiche uniquement la dimension où se trouve actuellement le joueur.
- Les données des dimensions sont séparées en interne pour éviter les collisions.
- Le terrain exploré est partagé entre toutes les cartes du même pseudo sur le
  même serveur : changer de carte ne recharge ni ne rescane les chunks déjà connus.
- Les marqueurs, les filtres et la position de navigation restent propres à chaque carte.
- Un marque-page avec l’icône de tombe, placé entre les filtres et le gestionnaire
  de cartes, active ou désactive le marqueur de mort automatique pour la carte
  courante. Une seule tombe « Vous êtes mort ici » est créée à chaque décès du
  joueur local. La détection réagit au paquet vanilla de mort envoyé par le
  serveur : elle n’effectue aucun contrôle permanent à chaque tick.
- Le bouton de filtre permet d’afficher ou masquer chaque type de marqueur ; le
  choix est enregistré séparément dans chaque profil de carte et s’applique
  aussi à l’atlas tenu en main.
- La colonne de gauche regroupe les marqueurs par icône. Son survol indique le
  nombre de marqueurs du groupe et un clic ouvre leur liste défilable, sans
  afficher leurs coordonnées.
- Les icônes TFOT supplémentaires couvrent notamment les quêtes, PNJ, boutiques,
  bateaux, cartes et artéfacts. Elles sont normalisées à la taille des marqueurs
  historiques ; le marqueur Portail du Nether a été retiré.
- Le marque-page **Actualiser la zone** permet de réexaminer ponctuellement les
  chunks déjà cartographiés et chargés autour du joueur lorsqu’une mise à jour a
  modifié le monde. Le bouton reste bloqué jusqu’à la fin de l’opération.
- Le scanner ne lit que les chunks déjà reçus et chargés par le client, avec un
  budget configurable ; il ne force jamais le chargement d’un chunk serveur.
- Lorsque Iris/Oculus utilise un shader pack, l’atlas tenu en main sélectionne
  des couches de rendu d’entité compatibles tout en conservant les chemins de
  textures originaux. Les resource packs modifient donc aussi le rendu en main.

Les fichiers se trouvent dans :

```text
.minecraft/config/antiqueatlas/servers/<adresse-hachée>/
└── players/<pseudo-haché>/
    ├── maps.json
    ├── maps/<profil>/profile.dat
    └── worlds/world_0001/
        ├── world.dat
        └── dimensions/<namespace>/<dimension>/terrain/r.<x>.<z>.dat
```

Une région contient au maximum `32 × 32` chunks avec une palette compressée.
Les écritures de régions sont effectuées sur un thread client dédié et regroupées
toutes les cinq secondes, afin de ne pas bloquer le rendu.

Chaque profil possède un identifiant aléatoire et une empreinte SHA-256 calculée
depuis le pseudo normalisé. Le terrain partagé possède une empreinte séparée et
chaque région couvre aussi la dimension, ses coordonnées et son contenu. Il ne
s’agit pas d’un chiffrement inviolable : cette liaison vise à empêcher le simple
échange de fichiers sans ajouter de service ou de mod côté serveur.

Au premier lancement de cette version, l’ancien dossier racine est déplacé dans
le dossier du pseudo. Le terrain de la carte active est utilisé en priorité, puis
les autres cartes complètent uniquement les chunks manquants. Les anciens fichiers
de régions restent en place comme sauvegarde pendant cette migration ; les nouvelles
écritures utilisent ensuite uniquement `worlds/world_0001`.

## Objet optionnel

`itemNeeded = false` (par défaut) permet d’ouvrir la carte directement avec `M`.

Avec `itemNeeded = true`, le joueur doit avoir dans son inventaire un **livre
vanilla renommé `Antique Atlas`**, ou un livre portant le tag NBT booléen
`antiqueatlas`. Le livre reconnu utilise l’icône d’Antique Atlas dans
l’inventaire et la barre rapide. Le tag permet de conserver cette apparence et
ce comportement avec un nom personnalisé :

```mcfunction
/give @s minecraft:book{antiqueatlas:1b}
```

Aucun objet personnalisé n’est enregistré : le client reste ainsi compatible
avec un serveur qui ne possède pas le mod. Le tag peut être fourni par une
commande, un datapack ou un autre système vanilla ; la méthode par renommage
reste disponible sans permission particulière. Tenir ce livre affiche la carte
dans les mains, uniquement à ce moment-là.
Les réglages sont accessibles depuis le bouton **Config** de la liste des mods et
sont enregistrés dans `.minecraft/config/antiqueatlas-client.json`.
`maxMaps` limite uniquement la création de nouvelles cartes (`10` par défaut) ;
réduire cette valeur ne supprime jamais une carte déjà enregistrée.
Les filtres de marqueurs ne font pas partie de ce fichier : ils sont sauvegardés
dans le `profile.dat` de chaque carte, tout comme le toggle de marqueur de mort.
L’actualisation du terrain est volontairement manuelle et utilise
`clientScanBudget` pour répartir le travail sur plusieurs ticks.

## Prérequis et build

- Minecraft `1.20.1` ;
- Forge `47.x`.

Cette édition n’utilise plus UnionLib. Il faut retirer UnionLib du dossier
`mods` lorsqu’aucun autre mod ne l’exige, sinon son propre canal réseau peut
continuer à imposer sa présence sur le serveur.

```bash
./gradlew --no-daemon clean build
```

GitHub Actions exécute le même build sur chaque push et pull request puis publie
le JAR dans l’artefact `antique-atlas-tfot-forge-1.20.1`.

## Vérifications conseillées

1. Installer uniquement Antique Atlas TFOT sur le client.
2. Rejoindre un serveur Forge 1.20.1 dépourvu du mod.
3. Explorer, fermer Minecraft, se reconnecter et vérifier la persistance.
4. Changer de dimension et vérifier que seule la dimension actuelle est affichée.
5. Tester la création, le renommage et la suppression de plusieurs cartes.
6. Tester les deux valeurs de `itemNeeded`.
7. Tester le clic droit dans le vide, puis sur un coffre, en main principale et
   en main secondaire.
8. Configurer des filtres différents sur deux cartes, les rouvrir et vérifier
   que chaque carte retrouve ses propres choix.
9. Explorer avec une carte, en ouvrir une autre et vérifier que le même terrain
   apparaît immédiatement alors que les marqueurs et filtres restent indépendants.
10. Désactiver le marqueur de mort sur une carte, le laisser activé sur une autre,
    puis vérifier qu’une seule tombe « Vous êtes mort ici » est créée par décès.
11. Utiliser **Actualiser la zone** et vérifier la progression, le verrouillage du
   bouton et le message final indiquant le nombre de chunks modifiés.
12. Copier une sauvegarde sur une seconde installation : vérifier qu’elle charge
    avec le même pseudo, puis qu’un pseudo différent obtient son propre espace et
    ne peut pas ouvrir les profils ou régions copiés.

## Crédits et licence

Ce projet dérive d’[AntiqueAtlasTeam/AntiqueAtlas](https://github.com/AntiqueAtlasTeam/AntiqueAtlas),
de la version Forge maintenue par [Stereowalker](https://github.com/Stereowalker/AntiqueAtlas)
et reprend certaines idées d’architecture de la génération moderne. Le code reste
distribué sous GNU GPL conformément au fichier `LICENSE`.
