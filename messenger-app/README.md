# Application Messenger JavaFX

Application de messagerie instantanée style WhatsApp développée avec JavaFX.

## Fonctionnalités

### ✅ Implémenté
- 📱 Interface moderne et responsive
- 💬 Liste des conversations avec aperçu
- 🔍 Barre de recherche
- 💭 Bulles de messages (envoyés/reçus)
- ⏰ Affichage de l'heure des messages
- 🔴 Badge de messages non lus
- 👤 Statut en ligne/hors ligne
- 🎨 Design inspiré de WhatsApp
- ✏️ Envoi de messages en temps réel

### 🚧 À implémenter
- 😊 Sélecteur d'emojis
- 📎 Envoi de fichiers/images
- 📞 Appels vocaux
- 📹 Appels vidéo
- 🔐 Authentification utilisateur
- 🌐 Communication réseau (WebSocket/REST API)
- 💾 Persistance des données
- 🔔 Notifications
- 🌙 Mode sombre

## Structure du projet

```
messenger-app/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/messenger/
│       │       ├── MessengerApp.java          # Classe principale
│       │       ├── controller/
│       │       │   └── MainController.java    # Contrôleur principal
│       │       └── model/
│       │           ├── Conversation.java      # Modèle conversation
│       │           └── Message.java           # Modèle message
│       └── resources/
│           ├── fxml/
│           │   └── main.fxml                  # Interface FXML
│           └── css/
│               └── style.css                  # Styles CSS
├── pom.xml                                    # Configuration Maven
└── README.md
```

## Prérequis

- Java 17 ou supérieur
- Maven 3.6+

## Installation et exécution

### Option 1 : Avec Maven
```bash
cd messenger-app
mvn clean javafx:run
```

### Option 2 : Compiler et créer un JAR
```bash
mvn clean package
java -jar target/messenger-app-1.0-SNAPSHOT.jar
```

## Utilisation

1. Lancez l'application
2. Cliquez sur une conversation dans la liste de gauche
3. Tapez un message dans le champ en bas
4. Appuyez sur Entrée ou cliquez sur le bouton d'envoi ➤

## Personnalisation

### Modifier les couleurs
Éditez [src/main/resources/css/style.css](src/main/resources/css/style.css) :
- `#00a884` : Couleur principale (vert WhatsApp)
- `#f0f2f5` : Couleur de fond
- `#d9fdd3` : Couleur des bulles envoyées

### Ajouter des contacts
Modifiez la méthode `loadDemoData()` dans [MainController.java](src/main/java/com/messenger/controller/MainController.java)

## Technologies utilisées

- **JavaFX 21** : Framework d'interface graphique
- **FXML** : Définition déclarative de l'interface
- **CSS** : Stylisation avancée
- **Maven** : Gestion de dépendances

## Prochaines étapes

Pour transformer cette application en messagerie complète :

1. **Backend** : Créer un serveur (Spring Boot + WebSocket)
2. **Base de données** : Ajouter MySQL/PostgreSQL pour la persistance
3. **Authentification** : Implémenter login/inscription
4. **Temps réel** : Utiliser WebSocket pour les messages instantanés
5. **Chiffrement** : Sécuriser les communications (E2E encryption)

## Licence

Projet éducatif - Utilisation libre
