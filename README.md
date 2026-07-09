# Hold Clicker Android

Mini outil Android pour lancer un seul type d'action : un **press & hold** a l'endroit choisi.

## Fonctions

- Une seule option : duree du hold en millisecondes.
- Duree bornee entre `50` et `60000` ms, limite imposee par `GestureDescription`.
- Cible flottante deplacable.
- Boutons flottants START / STOP utilisables au-dessus des autres apps.
- Pas de root.
- Utilise `AccessibilityService.dispatchGesture()`.

## Installation dev

1. Ouvrir le dossier dans Android Studio.
2. Laisser Gradle synchroniser le projet.
3. Build APK : `Build > Build App Bundle(s) / APK(s) > Build APK(s)`.
4. Installer l'APK sur Android.

## A activer sur le telephone

1. Autorisation **Afficher par-dessus les autres applications**.
2. Accessibilite > Applications installees > **Hold Clicker Service** > Activer.

## Utilisation

1. Entrer la duree du maintien, par exemple `3000` pour 3 secondes.
2. Appuyer sur **Afficher les boutons flottants**.
3. Deplacer la cible orange sur la zone a maintenir.
4. Appuyer sur **START**.
5. Pendant le hold, la cible reste visible mais laisse passer le toucher vers l'app dessous.
6. Appuyer sur **STOP** pour annuler le geste en cours.
