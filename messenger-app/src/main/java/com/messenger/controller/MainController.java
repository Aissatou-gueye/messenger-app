package com.messenger.controller;

import com.messenger.model.*;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainController {

    @FXML
    private ListView<Object> conversationsList;
    @FXML
    private VBox messagesContainer;
    @FXML
    private TextField messageInput;
    @FXML
    private Label contactName;
    @FXML
    private Label contactStatus;
    @FXML
    private ScrollPane messagesScrollPane;
    @FXML
    private TextField searchField;
    @FXML
    private HBox chatHeader;
    @FXML
    private StackPane profileClickArea;
    @FXML
    private javafx.scene.shape.Circle activeChatPic;
    @FXML
    private Button sendButton;
    @FXML
    private javafx.scene.shape.Circle myProfilePicCircle;

    private User currentUser;
    private User selectedUser;
    private List<User> users = new ArrayList<>();
    private List<Message> messages = new ArrayList<>();
    private java.util.Map<String, Integer> unreadCounts = new java.util.HashMap<>();
    private java.util.Map<String, String> lastMessages = new java.util.HashMap<>();
    private java.util.Map<String, String> profilePictures = new java.util.HashMap<>();
    private java.util.Map<String, LocalDateTime> messageTimes = new java.util.HashMap<>();
    private java.util.Map<String, LocalDateTime> logoutTimes = new java.util.HashMap<>();

    private String currentFilter = "ALL"; // ALL, UNREAD, GROUPS

    /**
     * Initialisation du contrôleur JavaFX (appelé automatiquement au chargement du
     * FXML).
     */
    @FXML
    public void initialize() {
        // Récupère l'utilisateur connecté depuis le SessionManager
        currentUser = com.messenger.client.SessionManager.getInstance().getCurrentUser();

        // Cas de secours si le session manager est vide (pour le développement)
        if (currentUser == null) {
            currentUser = new User("Invite", "1234");
            currentUser.setId((long) "Invite".hashCode());
            currentUser.setStatus(User.Status.ONLINE);
        }

        // Chargement de l'image de profil Base64 de l'utilisateur actuel
        if (currentUser.getProfilePicture() != null && !currentUser.getProfilePicture().isEmpty()) {
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(
                        new java.io.ByteArrayInputStream(
                                java.util.Base64.getDecoder().decode(currentUser.getProfilePicture())));
                myProfilePicCircle.setFill(new javafx.scene.paint.ImagePattern(img));
                profilePictures.put(currentUser.getUsername(), currentUser.getProfilePicture());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Configuration de la liste des contacts et barre de recherche
        setupUserListCellFactory();
        setupSearchField();

        // Démarrage du thread d'écoute réseau (RG11)
        startMessageListener();

        // Demande la liste des contacts et l'historique dès le chargement
        com.messenger.client.SessionManager.getInstance().getClientSocket().sendMessage("GET_USERS:");
        com.messenger.client.SessionManager.getInstance().getClientSocket().sendMessage("GET_ALL_HISTORY:");

        // Gestion du clic sur un contact dans la liste
        conversationsList.setOnMouseClicked(event -> {
            Object item = conversationsList.getSelectionModel().getSelectedItem();
            if (item instanceof User) {
                selectedUser = (User) item;
                unreadCounts.put(selectedUser.getUsername(), 0); // Marque comme lu
                conversationsList.refresh();

                // Demande l'historique spécifique à cet utilisateur (RG8)
                com.messenger.client.SessionManager.getInstance().getClientSocket()
                        .sendMessage("GET_HISTORY:" + selectedUser.getUsername());

                openConversationLocally(); // Prépare l'affichage du chat
            }
        });
    }

    private void setupSearchField() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            sortAndRefreshUsers();
        });
    }

    private void setupUserListCellFactory() {
        conversationsList.setCellFactory(lv -> new ListCell<Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else if (item instanceof String) {
                    // Header de section ("EN LIGNE", etc.)
                    Label header = new Label(item.toString());
                    header.setStyle(
                            "-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-padding: 10 10 5 10;");
                    setGraphic(header);
                    setDisable(true);
                } else if (item instanceof User) {
                    User user = (User) item;
                    setDisable(false);
                    HBox box = new HBox(12);
                    box.setPadding(new Insets(8, 12, 8, 12));
                    box.setAlignment(Pos.CENTER_LEFT);

                    StackPane picStack = new StackPane();
                    javafx.scene.shape.Circle pic = new javafx.scene.shape.Circle(22);
                    pic.setFill(javafx.scene.paint.Color.web("#E2E8F0"));

                    // Si on a l'image encodée
                    String b64 = profilePictures.get(user.getUsername());
                    if (b64 != null && !b64.isEmpty()) {
                        try {
                            pic.setFill(new javafx.scene.paint.ImagePattern(new javafx.scene.image.Image(
                                    new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(b64)))));
                        } catch (Exception e) {
                        }
                    }

                    javafx.scene.shape.Circle statusDot = new javafx.scene.shape.Circle(6);
                    statusDot.getStyleClass()
                            .setAll(user.getStatus() == User.Status.ONLINE ? "status-online" : "status-offline");
                    statusDot.setStroke(javafx.scene.paint.Color.WHITE);
                    statusDot.setStrokeWidth(2);

                    picStack.getChildren().addAll(pic, statusDot);
                    StackPane.setAlignment(statusDot, Pos.BOTTOM_RIGHT);

                    VBox info = new VBox(2);
                    Label nameLbl = new Label(user.getUsername());
                    nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B;");

                    Label lastMsg = new Label(lastMessages.getOrDefault(user.getUsername(), "Pas de message"));
                    lastMsg.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
                    lastMsg.setMaxWidth(160);
                    info.getChildren().addAll(nameLbl, lastMsg);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    VBox rightSide = new VBox(5);
                    rightSide.setAlignment(Pos.TOP_RIGHT);

                    LocalDateTime time = messageTimes.get(user.getUsername());
                    Label timeLabel = new Label(time != null ? time.format(DateTimeFormatter.ofPattern("HH:mm")) : "");
                    timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");

                    rightSide.getChildren().add(timeLabel);

                    int count = unreadCounts.getOrDefault(user.getUsername(), 0);
                    if (count > 0) {
                        Label badge = new Label(String.valueOf(count));
                        badge.setStyle(
                                "-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-size: 9px; -fx-padding: 1 5; -fx-background-radius: 10;");
                        rightSide.getChildren().add(badge);
                    }

                    box.getChildren().addAll(picStack, info, spacer, rightSide);
                    setGraphic(box);
                }
            }
        });

        conversationsList.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal instanceof User) {
                selectedUser = (User) newVal;
                unreadCounts.put(selectedUser.getUsername(), 0);
                conversationsList.refresh();
                openConversationLocally();
                com.messenger.client.SessionManager.getInstance().getClientSocket()
                        .sendMessage("GET_HISTORY:" + selectedUser.getUsername());
            }
        });
    }

    @FXML
    private void filterAll() {
        currentFilter = "ALL";
        sortAndRefreshUsers();
    }

    @FXML
    private void filterUnread() {
        currentFilter = "UNREAD";
        sortAndRefreshUsers();
    }

    private void startMessageListener() {
        com.messenger.client.ClientSocket socket = com.messenger.client.SessionManager.getInstance().getClientSocket();
        if (socket != null && socket.isConnected()) {
            com.messenger.client.MessageReceiver receiver = new com.messenger.client.MessageReceiver(
                    socket.getInput(), socket, new com.messenger.client.MessageReceiver.Callback() {
                        @Override
                        public void onMessageReceived(String messageType, String[] data) {
                            javafx.application.Platform.runLater(() -> handleServerMessage(messageType, data));
                        }

                        @Override
                        public void onConnectionLost() {
                            javafx.application.Platform.runLater(() -> handleConnectionLost());
                        }
                    });
            Thread thread = new Thread(receiver);
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void handleConnectionLost() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de connexion");
        alert.setHeaderText("Connexion perdue");
        alert.setContentText(
                "La connexion au serveur a été interrompue. Vous allez être redirigé vers l'écran de connexion.");
        alert.showAndWait();
        handleLogout();
    }

    private void handleServerMessage(String messageType, String[] data) {
        if (data.length < 2)
            return;
        String payload = data[1];
        String[] parts = payload.split(":", 3); // On split le reste selon les besoins

        javafx.application.Platform.runLater(() -> {
            switch (messageType) {
                case "USERS_LIST":
                    updateUsersList(payload);
                    break;
                case "ALL_USERS_LIST":
                    showAllUsersDialog(payload);
                    break;
                case "ALL_HISTORY":
                    handleAllHistory(payload);
                    break;
                case "MESSAGE_RECEIVED":
                    if (parts.length >= 2)
                        handleNewMessage(parts[0], parts[1]);
                    break;
                case "HISTORY":
                    if (parts.length >= 2)
                        handleHistoryRefresh(parts[0], parts[1]);
                    break;
                case "USER_JOINED":
                case "USER_STATUS":
                    if (parts.length >= 2)
                        updateUserStatus(parts[0], parts[1]);
                    break;
                case "USER_LEFT":
                    updateUserStatus(parts[0], "OFFLINE");
                    logoutTimes.put(parts[0], LocalDateTime.now());
                    break;
                case "MESSAGE_READ_UPDATE":
                    updateMessageStatusToRead(payload);
                    break;
                case "PIC_UPDATE":
                    if (parts.length >= 2) {
                        String username = parts[0];
                        String b64 = parts[1];
                        profilePictures.put(username, b64);
                        if (selectedUser != null && selectedUser.getUsername().equals(username)) {
                            updateProfileDisplay(b64);
                        }
                        if (currentUser.getUsername().equals(username)) {
                            currentUser.setProfilePicture(b64);
                        }
                        conversationsList.refresh();
                    }
                    break;

            }
        });
    }

    private void updateProfileDisplay(String b64) {
        if (b64 == null || b64.isEmpty()) {
            activeChatPic.setFill(javafx.scene.paint.Color.web("#E2E8F0"));
            return;
        }
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(
                    new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(b64)));
            activeChatPic.setFill(new javafx.scene.paint.ImagePattern(img));
        } catch (Exception e) {
            activeChatPic.setFill(javafx.scene.paint.Color.web("#E2E8F0"));
        }
    }

    private void handleAllHistory(String historyData) {
        if (historyData == null || historyData.isEmpty())
            return;
        String[] entries = historyData.split("\\|");
        for (String entry : entries) {
            String[] parts = entry.split("=", 4);
            if (parts.length >= 3) {
                String sender = parts[0];
                String receiver = parts[1];
                String content = parts[2];
                String timeStr = parts.length > 3 ? parts[3] : "";
                String contact = sender.equals(currentUser.getUsername()) ? receiver : sender;

                User u = findUserInList(contact);
                if (u == null) {
                    u = new User(contact, "");
                    u.setId((long) contact.hashCode());
                    users.add(u);
                }

                String lastTxt = (sender.equals(currentUser.getUsername()) ? "Moi: " : "") + content;
                lastMessages.put(contact, lastTxt.replace("[[COLON]]", ":").replace("[[COMMA]]", ","));

                if (!timeStr.isEmpty()) {
                    try {
                        messageTimes.put(contact, LocalDateTime.parse(timeStr));
                    } catch (Exception e) {
                    }
                }
            }
        }
        sortAndRefreshUsers();
    }

    private void updateMessageStatusToRead(String senderName) {
        javafx.application.Platform.runLater(() -> {
            for (Message m : messages) {
                if (m.getReceiver().getUsername().equals(senderName) && m.getStatus() != Message.Status.LU) {
                    m.setStatus(Message.Status.LU);
                }
            }
            messagesContainer.getChildren().clear();
            for (Message m : messages)
                addMessageBubble(m);
            scrollToBottom();
        });
    }

    private void sortAndRefreshUsers() {
        javafx.application.Platform.runLater(() -> {
            List<Object> items = new ArrayList<>();
            String search = (searchField == null || searchField.getText() == null) ? ""
                    : searchField.getText().toLowerCase();

            if (currentFilter.equals("ALL")) {
                List<User> online = new ArrayList<>();
                List<User> offline = new ArrayList<>();

                for (User u : users) {
                    if (u.getUsername().toLowerCase().contains(search)) {
                        if (u.getStatus() == User.Status.ONLINE)
                            online.add(u);
                        else
                            offline.add(u);
                    }
                }

                // Tri par date de dernier message (plus récent en haut)
                java.util.Comparator<User> timeComparator = (u1, u2) -> {
                    LocalDateTime t1 = messageTimes.getOrDefault(u1.getUsername(), LocalDateTime.MIN);
                    LocalDateTime t2 = messageTimes.getOrDefault(u2.getUsername(), LocalDateTime.MIN);
                    return t2.compareTo(t1);
                };

                online.sort(timeComparator);
                offline.sort(timeComparator);

                if (!online.isEmpty()) {
                    items.add("EN LIGNE (" + online.size() + ")");
                    items.addAll(online);
                }
                if (!offline.isEmpty()) {
                    items.add("HORS LIGNE (" + offline.size() + ")");
                    items.addAll(offline);
                }
            } else if (currentFilter.equals("UNREAD")) {
                List<User> unreadUsers = new ArrayList<>();
                for (User u : users) {
                    if (unreadCounts.getOrDefault(u.getUsername(), 0) > 0
                            && u.getUsername().toLowerCase().contains(search)) {
                        unreadUsers.add(u);
                    }
                }
                if (!unreadUsers.isEmpty()) {
                    items.add("NON LUS (" + unreadUsers.size() + ")");
                    items.addAll(unreadUsers);
                }
            }

            if (conversationsList != null) {
                conversationsList.getItems().setAll(items);
            }
        });
    }

    private void handleIncomingCall(String type, String sender) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Appel Entrant");
            alert.setHeaderText("Appel " + type + " de " + sender);
            alert.setContentText("Voulez-vous accepter ?");
            alert.showAndWait();
        });
    }

    private void showAllUsersDialog(String usersStr) {
        javafx.application.Platform.runLater(() -> {
            List<String> userList = new ArrayList<>();
            if (usersStr != null && !usersStr.isEmpty()) {
                userList.addAll(Arrays.asList(usersStr.split(",")));
            }
            ChoiceDialog<String> dialog = new ChoiceDialog<>(userList.isEmpty() ? null : userList.get(0), userList);
            dialog.setTitle("Nouvelle Conversation");
            dialog.setHeaderText("Choisissez un utilisateur");
            dialog.showAndWait().ifPresent(username -> {
                User newUser = findUserInList(username);
                if (newUser == null) {
                    newUser = new User(username, "");
                    newUser.setId((long) username.hashCode());
                    newUser.setStatus(User.Status.OFFLINE); // Par défaut offline, sera mis à jour par le serveur
                    users.add(newUser);
                }
                conversationsList.getSelectionModel().select(newUser);
                selectedUser = newUser;
                openConversationLocally();
                // Demander l'historique et le statut réel
                com.messenger.client.SessionManager.getInstance().getClientSocket()
                        .sendMessage("GET_HISTORY:" + username);
            });
        });
    }

    private synchronized void updateUsersList(String usersStr) {
        if (usersStr == null || usersStr.isEmpty())
            return;

        String[] userArray = usersStr.split(",");
        for (String u : userArray) {
            String[] userInfo = u.split(":", 6);
            if (userInfo.length < 2)
                continue;

            String uname = userInfo[0].trim();
            if (uname.isEmpty() || uname.equals(currentUser.getUsername()))
                continue;

            String statusStr = userInfo[1];
            String lastMsg = userInfo.length > 2 ? userInfo[2].replace("[[COLON]]", ":").replace("[[COMMA]]", ",")
                    : "Pas de message";
            String timeStr = userInfo.length > 3 ? userInfo[3] : "";
            int unread = userInfo.length > 4 ? Integer.parseInt(userInfo[4]) : 0;
            if (userInfo.length > 5 && !userInfo[5].isEmpty())
                profilePictures.put(uname, userInfo[5]);

            User userItem = findUserInList(uname);
            if (userItem == null) {
                userItem = new User(uname, "");
                userItem.setId((long) uname.hashCode());
                users.add(userItem);
            }
            userItem.setStatus(statusStr.equals("OFFLINE") ? User.Status.OFFLINE : User.Status.ONLINE);

            lastMessages.put(uname, lastMsg);
            unreadCounts.put(uname, unread);
            if (!timeStr.isEmpty()) {
                try {
                    messageTimes.put(uname, LocalDateTime.parse(timeStr));
                } catch (Exception e) {
                }
            }
        }
        sortAndRefreshUsers();
    }

    private void updateUserStatus(String username, String statusStr) {
        User user = findUserInList(username);
        if (user != null) {
            user.setStatus(statusStr.equals("ONLINE") ? User.Status.ONLINE : User.Status.OFFLINE);
            if (statusStr.equals("OFFLINE")) {
                logoutTimes.put(username, LocalDateTime.now());
            }
            if (selectedUser != null && selectedUser.getUsername().equals(username)) {
                contactStatus.setText(statusStr.equals("ONLINE") ? "online" : "offline");
                contactStatus.setStyle(statusStr.equals("ONLINE") ? "-fx-text-fill: #10B981; -fx-font-weight: bold;"
                        : "-fx-text-fill: #94A3B8;");
                if (statusStr.equals("OFFLINE")) {
                    contactStatus.setText("offline (Last seen at "
                            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + ")");
                }
            }
            sortAndRefreshUsers();
        } else {
            // Si l'utilisateur n'est pas dans la liste, on demande la liste complète
            com.messenger.client.SessionManager.getInstance().getClientSocket().sendMessage("GET_USERS:");
        }
    }

    private synchronized void handleNewMessage(String senderUsername, String content) {
        String decodedContent = content.replace("[[COLON]]", ":");
        System.out.println("[CLIENT] Nouveau message de " + senderUsername + ": " + decodedContent);

        User sender = findUserInList(senderUsername);
        if (sender == null) {
            sender = new User(senderUsername, "");
            sender.setId((long) senderUsername.hashCode());
            sender.setStatus(User.Status.ONLINE);
            users.add(sender);
        }

        Message message = new Message(sender, currentUser, decodedContent);
        message.setDateEnvoi(LocalDateTime.now());
        message.setStatus(Message.Status.RECU);

        // Ajout systématique (les doublons sont gérés au niveau visuel si nécessaire)
        messages.add(message);

        lastMessages.put(senderUsername, decodedContent);
        messageTimes.put(senderUsername, LocalDateTime.now());

        if (selectedUser != null && selectedUser.getUsername().equals(senderUsername)) {
            addMessageBubble(message);
            scrollToBottom();
            com.messenger.client.SessionManager.getInstance().getClientSocket()
                    .sendMessage("MESSAGE_READ:" + senderUsername);
        } else {
            int currentCount = unreadCounts.getOrDefault(senderUsername, 0);
            unreadCounts.put(senderUsername, currentCount + 1);
        }
        sortAndRefreshUsers();
    }

    private void handleHistoryRefresh(String contactUsername, String historyData) {
        if (selectedUser == null || !selectedUser.getUsername().equals(contactUsername))
            return;
        messages.clear();
        messagesContainer.getChildren().clear();
        if (historyData != null && !historyData.isEmpty()) {
            String[] msgs = historyData.split("\\|");
            for (String m : msgs) {
                String[] parts = m.split("=", 2);
                if (parts.length == 2) {
                    String senderName = parts[0];
                    String content = parts[1];
                    User sender = senderName.equals(currentUser.getUsername()) ? currentUser : selectedUser;
                    User receiver = senderName.equals(currentUser.getUsername()) ? selectedUser : currentUser;
                    Message msg = new Message(sender, receiver, content);
                    msg.setDateEnvoi(LocalDateTime.now());
                    msg.setStatus(Message.Status.LU);
                    messages.add(msg);
                }
            }
            if (messages.size() > 0) {
                Message last = messages.get(messages.size() - 1);
                String lastText = (last.getSender().getUsername().equals(currentUser.getUsername()) ? "Moi: " : "")
                        + last.getContent();
                lastMessages.put(contactUsername, lastText);
                messageTimes.put(contactUsername, last.getDateEnvoi());
            }
        }
        for (Message message : messages) {
            addMessageBubble(message);
        }
        scrollToBottom();
        sortAndRefreshUsers();
    }

    private User findUserInList(String username) {
        for (User u : users)
            if (u.getUsername().equals(username))
                return u;
        return null;
    }

    private void openConversationLocally() {
        if (selectedUser == null)
            return;
        contactName.setText(selectedUser.getUsername());

        // Mise à jour de la photo de profil dans le chat
        String b64 = profilePictures.get(selectedUser.getUsername());
        if (b64 != null && !b64.isEmpty()) {
            try {
                activeChatPic.setFill(new javafx.scene.paint.ImagePattern(new javafx.scene.image.Image(
                        new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(b64)))));
            } catch (Exception e) {
            }
        } else {
            activeChatPic.setFill(javafx.scene.paint.Color.web("#E2E8F0"));
        }

        if (selectedUser.getStatus() == User.Status.ONLINE) {
            contactStatus.setText("online");
            contactStatus.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
        } else {
            LocalDateTime logoutTime = logoutTimes.get(selectedUser.getUsername());
            if (logoutTime != null) {
                contactStatus.setText(
                        "offline (Last seen at " + logoutTime.format(DateTimeFormatter.ofPattern("HH:mm")) + ")");
            } else {
                contactStatus.setText("offline");
            }
            contactStatus.setStyle("-fx-text-fill: #94A3B8;");
        }

        messagesContainer.getChildren().clear();
    }

    @FXML
    private void sendMessage() {
        String content = messageInput.getText().trim();

        // RG7: Validation contenu (non vide et max 1000 caractères)
        if (content.isEmpty())
            return;
        if (content.length() > 1000) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Message trop long");
            alert.setContentText("Le message ne doit pas dépasser 1000 caractères.");
            alert.show();
            return;
        }

        if (selectedUser != null) {
            String encodedContent = content.replace(":", "[[COLON]]");

            Message message = new Message(currentUser, selectedUser, content);
            message.setDateEnvoi(LocalDateTime.now());
            message.setStatus(Message.Status.ENVOYE);
            messages.add(message);

            // Mettre à jour la liste
            lastMessages.put(selectedUser.getUsername(), "Moi: " + content);
            messageTimes.put(selectedUser.getUsername(), LocalDateTime.now());
            sortAndRefreshUsers();

            addMessageBubble(message);
            messageInput.clear();
            scrollToBottom();

            com.messenger.client.SessionManager.getInstance().getClientSocket()
                    .sendMessage("SEND_MESSAGE:" + selectedUser.getUsername() + ":" + encodedContent);
        }
    }

    private void addMessageBubble(Message message) {
        HBox messageBox = new HBox();
        messageBox.setPadding(new Insets(5));
        messageBox.setSpacing(10);

        ContextMenu msgMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Supprimer");
        MenuItem favItem = new MenuItem("Ajouter aux favoris");
        deleteItem.setOnAction(e -> messagesContainer.getChildren().remove(messageBox));
        favItem.setOnAction(e -> System.out.println("Message favori : " + message.getContent()));
        msgMenu.getItems().addAll(deleteItem, favItem);

        boolean isSentByMe = message.getSender().getUsername().equals(currentUser.getUsername());
        VBox bubble = new VBox(5);
        bubble.setPadding(new Insets(10, 15, 10, 15));
        bubble.setMaxWidth(400);
        bubble.setOnContextMenuRequested(e -> msgMenu.show(bubble, e.getScreenX(), e.getScreenY()));

        Text text = new Text();
        if (message.getContent().contains("[Image :") || message.getContent().endsWith(".jpg")
                || message.getContent().endsWith(".png")) {
            text.setText("🖼 Image");
            text.getStyleClass().add("message-text");
            if (isSentByMe)
                text.setStyle("-fx-fill: #1E293B;");
        } else if (message.getContent().contains("[Fichier :") || message.getContent().endsWith(".pdf")) {
            text.setText("📄 Document PDF");
            text.getStyleClass().add("message-text");
            text.setStyle("-fx-font-weight: bold; " + (isSentByMe ? "-fx-fill: #1E293B;" : ""));
        } else if (message.getContent().contains("[Vocal")) {
            text.setText("🎤 Message Vocal");
            text.getStyleClass().add("message-text");
            if (isSentByMe)
                text.setStyle("-fx-fill: #1E293B;");
        } else {
            text.setText(message.getContent());
            text.getStyleClass().add("message-text");
            if (isSentByMe)
                text.setStyle("-fx-fill: #1E293B;");
        }

        Label timeLabel = new Label(message.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.getStyleClass().add("message-time");
        HBox meta = new HBox(5);
        meta.setAlignment(Pos.CENTER_RIGHT);
        meta.getChildren().add(timeLabel);
        if (isSentByMe) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            bubble.getStyleClass().setAll("message-bubble-sent");

            String checks = "✔";
            String checkColor = "#94A3B8"; // Gris par défaut (envoyé)

            // Si le destinataire est online, on met deux traits gris (reçu/distribué)
            if (selectedUser != null && selectedUser.getStatus() == User.Status.ONLINE) {
                checks = "✔✔";
            }

            // Si le message est marqué comme LU, on met deux traits bleus
            if (message.getStatus() == Message.Status.LU) {
                checks = "✔✔";
                checkColor = "#3B82F6";
            }

            Label checkmark = new Label(checks);
            checkmark.setStyle("-fx-text-fill: " + checkColor + "; -fx-font-size: 11px; -fx-font-weight: bold;");
            meta.getChildren().add(checkmark);
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
            bubble.getStyleClass().setAll("message-bubble-received");
            bubble.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 15;");
        }

        bubble.getChildren().addAll(text, meta);
        messageBox.getChildren().add(bubble);
        messagesContainer.getChildren().add(messageBox);
    }

    private void scrollToBottom() {
        javafx.application.Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }

    @FXML
    private void handleLogout() {
        // Redirection vers login sans confirmation (utilisé pour connection lost)
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            javafx.stage.Stage stage = (javafx.stage.Stage) conversationsList.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(javafx.event.ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Déconnexion");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Êtes-vous sûr de vouloir vous déconnecter ?");

        ButtonType btnOui = new ButtonType("Oui");
        ButtonType btnNon = new ButtonType("Non", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(btnOui, btnNon);

        confirmation.showAndWait().ifPresent(response -> {
            if (response == btnOui) {
                com.messenger.client.ClientSocket socket = com.messenger.client.SessionManager.getInstance()
                        .getClientSocket();
                if (socket != null && socket.isConnected())
                    socket.sendMessage("LOGOUT:");
                handleLogout(); // Appel de la redirection simple
            }
        });
    }

    @FXML
    private void showSettings(javafx.event.ActionEvent event) {
        VBox settingsContent = new VBox(15);
        settingsContent.setPadding(new Insets(20));

        Label title = new Label("Paramètres du Profil");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Button changePicBtn = new Button("Changer Photo de Profil");
        changePicBtn.setStyle("-fx-background-color: #3AB09E; -fx-text-fill: white; -fx-background-radius: 10;");
        changePicBtn.setMaxWidth(Double.MAX_VALUE);

        settingsContent.getChildren().addAll(title, changePicBtn);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Paramètres");
        dialog.getDialogPane().setContent(settingsContent);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType);

        changePicBtn.setOnAction(e -> changeProfilePicture(null));

        dialog.showAndWait();
    }

    @FXML
    private void showEmojiPicker(javafx.event.ActionEvent event) {
        ContextMenu emojiMenu = new ContextMenu();
        String[] emojis = { "😊", "😂", "😍", "👍", "🙏", "❤️", "🔥", "✨", "🤔", "😎" };
        for (String e : emojis) {
            MenuItem item = new MenuItem(e);
            item.setStyle("-fx-font-size: 18px;");
            item.setOnAction(ev -> messageInput.appendText(e));
            emojiMenu.getItems().add(item);
        }
        emojiMenu.show((Button) event.getSource(), javafx.geometry.Side.TOP, 0, 0);
    }

    @FXML
    private void changeProfilePicture(javafx.scene.input.MouseEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.getExtensionFilters()
                .add(new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        java.io.File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                javafx.scene.image.Image image = new javafx.scene.image.Image(file.toURI().toString());

                // Encoder l'image en Base64 pour l'envoyer au serveur
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(image, null), "png", baos);
                String b64 = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());

                // Mettre à jour MA photo (en haut à gauche)
                myProfilePicCircle.setFill(new javafx.scene.paint.ImagePattern(image));

                // Envoyer au serveur pour que les autres voient
                profilePictures.put(currentUser.getUsername(), b64);
                com.messenger.client.SessionManager.getInstance().getClientSocket()
                        .sendMessage("UPDATE_PIC:" + b64);

                conversationsList.refresh();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void voiceCall(javafx.event.ActionEvent event) {
        // Design seulement
    }

    @FXML
    private void videoCall(javafx.event.ActionEvent event) {
        // Design seulement
    }

    @FXML
    private void attachFile(javafx.event.ActionEvent event) {
        if (selectedUser == null) {
            showNoUserAlert();
            return;
        }
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        java.io.File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            messageInput.setText("[Fichier : " + file.getName() + "]");
            sendMessage();
        }
    }

    private void showNoUserAlert() {
        new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un utilisateur.").show();
    }
}