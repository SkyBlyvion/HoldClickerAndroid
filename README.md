# Hold Clicker Android

Mini auto-clicker Android qui fait uniquement un **press & hold**.

## Fonctions

- 1 seul réglage : durée du hold en millisecondes.
- Cible flottante orange déplaçable.
- Boutons flottants START / STOP.
- Pas de root.
- Utilise `AccessibilityService.dispatchGesture()`.

## Installation dev

1. Ouvrir le dossier dans Android Studio.
2. Laisser Gradle synchroniser le projet.
3. Build APK : `Build > Build App Bundle(s) / APK(s) > Build APK(s)`.
4. Installer l’APK sur Android.

## À activer sur le téléphone

1. Autorisation **Afficher par-dessus les autres applications**.
2. Accessibilité > Applications installées > **Hold Clicker Service** > Activer.

## Utilisation

1. Entrer la durée du maintien, par exemple `3000` pour 3 secondes.
2. Appuyer sur **Afficher les boutons flottants**.
3. Déplacer la cible orange sur la zone à maintenir.
4. Appuyer sur **START**.
5. Appuyer sur **STOP** pour annuler/stopper le geste en cours.
