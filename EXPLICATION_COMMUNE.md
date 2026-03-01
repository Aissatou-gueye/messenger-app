# 📘 Guide Ultra-Détaillé du Projet MyChat

Ce document décortique chaque mécanisme, chaque ligne de style et chaque logique Java que nous avons implémentés pour transformer l'application.

---

## 1. 🎨 Le Design Système (CSS Avancé)
### Le "Glassmorphism" et l'Effet Bokeh (`style.css`)
C'est la partie qui donne l'aspect "Premium" à la connexion.
- **Le Fond (`.auth-root`)** : 
    ```css
    -fx-background-color: radial-gradient(focus-angle 0deg, focus-distance 0%, center 20% 20%, radius 70%, #3AB09E, #2A9D8F),
                          radial-gradient(focus-angle 0deg, focus-distance 0%, center 80% 80%, radius 70%, #4A90E2, #2A9D8F);
    ```
    *Détail* : On superpose deux dégradés radiaux. Le premier est en haut à gauche (20% 20%) et le second en bas à droite (80% 80%). Cela simule des taches de lumière colorées et floues (Bokeh).
- **La Boîte de Verre (`.glass-pane`)** :
    - `-fx-background-color: rgba(255, 255, 255, 0.15);` -> Translucidité (15% d'opacité).
    - `-fx-border-color: rgba(255, 255, 255, 0.3);` -> Une bordure très fine et claire qui capte la "lumière" sur les bords, renforçant l'effet de verre.
    - `-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 30, 0, 0, 0);` -> Un ombrage diffus pour détacher la boîte du fond.

### Les Bulles de Message
- **Envoyé (`.message-bubble-sent`)** : Couleur `#E2F9FF`. On utilise des coins arrondis asymétriques pour donner une forme de "goutte" pointant vers la droite.
- **Reçu (`.message-bubble-received`)** : Couleur blanche pure avec une légère ombre pour la profondeur.

---

## 2. 🔍 Logique de Recherche (`MainController.java`)
La barre de recherche n'est pas un simple filtre statique, elle est dynamique.

- **`searchField`** : C'est le champ de texte injecté via `@FXML`.
- **`FilteredList`** : C'est une "vue" de la liste des conversations. Au lieu de supprimer des éléments de la vraie liste, on décide juste de ce qui est visible ou non.
- **L'Ecouteur (Listener)** :
    ```java
    searchField.textProperty().addListener((observable, oldValue, newValue) -> {
        filteredConversations.setPredicate(conversation -> { ... });
    });
    ```
    *Explication* : Chaque fois que vous tapez une lettre (`newValue`), Java exécute le "Prédicat". Si le prédicat renvoie `true`, la conversation reste affichée. J'ai ajouté `.trim()` pour que la recherche ignore les espaces accidentels au début ou à la fin.

---

## 3. 🖼️ Gestion des Photos de Profil
Chaque utilisateur a une photo qui s'affiche à deux endroits : la liste à gauche et l'en-tête de la discussion.

- **Classe `Conversation`** : Ajout du champ `profileImagePath` pour stocker le lien vers le fichier sur votre ordinateur.
- **`updateActiveChatPic()`** :
    ```java
    Image img = new Image(new File(path).toURI().toString());
    activeChatPic.setFill(new ImagePattern(img));
    ```
    *Détail* : `ImagePattern` est l'astuce ici. Au lieu d'afficher une image carrée, on prend l'image et on l'utilise comme "peinture" pour remplir le cercle (`Circle`). Cela garantit que la photo est toujours ronde, peu importe sa taille d'origine.

---

## 📜 Historique avec Séparateurs de Date
Pour que la conversation soit lisible, j'ai implémenté un système de "marqueurs temporels".

- **La Logique dans `loadMessages`** :
    On boucle sur les messages. On garde en mémoire la date du dernier message affiché (`lastDate`).
    Si le message suivant a une date différente (ex: on passe du message de "Hier" à celui de "Aujourd'hui"), on appelle `addDateSeparator("Aujourd'hui")`.
- **Le Style (`.date-separator`)** :
    C'est un `HBox` qui prend toute la largeur, avec un `Label` au milieu. Le label a un fond gris clair arrondi pour ressembler à ce qu'on voit sur WhatsApp ou Messenger.

---

## 🔘 Les Boutons et leurs Fonctions
- **`newConversation` (Bouton +)** : Utilise `TextInputDialog`. C'est une petite fenêtre popup native JavaFX qui demande un nom. Très efficace pour éviter de créer un nouvel écran complet pour un simple ajout.
- **`handleLogout` (Bouton Déconnexion)** : Utilise `FXMLLoader.load()`. Il décharge l'interface de chat et recharge le fichier `login.fxml`. Il remplace radicalement la "Scene" actuelle de la fenêtre.
- **`voiceCall` / `videoCall`** : Utilisent `Alert`. C'est une boîte de dialogue d'information simple pour montrer que l'action est reconnue, en attendant une implémentation réelle de serveurs audio/vidéo.

## 🏁 Indicateurs de Statut (Checkmarks)
J'ai ajouté un système de "traits" (comme sur WhatsApp/Messenger) pour suivre l'envoi et la lecture :
- **1 Trait (`✔`)** : Le message est envoyé, mais le destinataire est **Hors ligne**.
- **2 Traits (`✔✔`)** : Le message est reçu, le destinataire est **En ligne**.
- **2 Traits Verts (`✔✔`)** : Le message a été **Lu** par le destinataire.

### Logique Technique :
```java
String checks = (currentConversation.isOnline()) ? "✔✔" : "✔";
if (message.isRead()) {
    checkmark.setStyle("-fx-text-fill: #44CC77;"); // Vert
}
```
*Note* : J'ai mis à jour `openConversation` pour que les messages passent automatiquement en **Vert** dès que vous ouvrez une discussion, simulant ainsi la lecture.

---
## 📁 Récapitulatif des fichiers clés
1.  **`MainController.java`** : Le cerveau. Gère les clics, le filtrage et l'affichage des messages.
2.  **`style.css`** : Le maquilleur. Gère l'effet de verre, les couleurs MyChat et les animations.
3.  **`main.fxml`** : Le squelette. Définit où se trouvent la barre de recherche et la zone de texte.
4.  **`Conversation.java`** : La base de données locale. Contient le nom, la photo et la liste des messages de chaque contact.
