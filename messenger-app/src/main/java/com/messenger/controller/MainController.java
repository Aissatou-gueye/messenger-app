package com.messenger.controller;

import com.messenger.model.Conversation;
import com.messenger.model.Message;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.stage.FileChooser;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;
import javafx.scene.input.MouseEvent;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.layout.Priority;
import java.io.File;
import java.io.IOException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML
    private ListView<Conversation> conversationsList;
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
    private Circle activeChatPic;
    @FXML
    private Button sendButton;
    @FXML
    private TextField searchField;

    private Conversation currentConversation;
    private List<Conversation> conversations;
    private ObservableList<Conversation> observableConversations;
    private FilteredList<Conversation> filteredConversations;

    @FXML
    public void initialize() {
        loadDemoData(); // Load data first to populate 'conversations'
        setupConversationsList();
        setupSearchFilter();
    }

    private void setupSearchFilter() {
        if (searchField == null)
            return;

        // Populate observable list from current conversations
        observableConversations = FXCollections.observableArrayList(conversations);
        filteredConversations = new FilteredList<>(observableConversations, p -> true);

        // Add robust listener for real-time filtering
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredConversations.setPredicate(conversation -> {
                // If filter is empty, show all
                if (newValue == null || newValue.trim().isEmpty()) {
                    return true;
                }

                String query = newValue.toLowerCase().trim();

                // Match against contact name
                if (conversation.getName() != null &&
                        conversation.getName().toLowerCase().contains(query)) {
                    return true;
                }

                return false;
            });
        });

        conversationsList.setItems(filteredConversations);
    }

    private void setupConversationsList() {
        conversationsList.setCellFactory(param -> new ListCell<Conversation>() {
            @Override
            protected void updateItem(Conversation conversation, boolean empty) {
                super.updateItem(conversation, empty);
                if (empty || conversation == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(createConversationCell(conversation));
                }
            }
        });

        conversationsList.setOnMouseClicked(event -> {
            Conversation selected = conversationsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openConversation(selected);
            }
        });
    }

    private HBox createConversationCell(Conversation conversation) {
        HBox cell = new HBox(15);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setPadding(new Insets(10));
        cell.getStyleClass().add("conversation-cell");

        // Photo de profil
        Circle profilePic = new Circle(25);
        profilePic.getStyleClass().add("profile-pic");
        if (conversation.getProfileImagePath() != null) {
            try {
                Image img = new Image(new File(conversation.getProfileImagePath()).toURI().toString());
                profilePic.setFill(new ImagePattern(img));
            } catch (Exception e) {
                profilePic.setFill(Color.web("#E4E6EB"));
            }
        } else {
            profilePic.setFill(Color.web("#E4E6EB"));
        }

        StackPane picContainer = new StackPane(profilePic);
        Circle statusCircle = new Circle(7);
        statusCircle.getStyleClass().add(conversation.isOnline() ? "online-status" : "offline-status");
        StackPane.setAlignment(statusCircle, Pos.BOTTOM_RIGHT);
        picContainer.getChildren().add(statusCircle);

        // Informations du contact
        VBox info = new VBox(2);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(conversation.getName());
        name.getStyleClass().add("contact-name");

        Label status = new Label(conversation.isOnline() ? "En ligne" : "Hors ligne");
        status.getStyleClass().add("status-text");

        info.getChildren().addAll(name, status);

        Label time = new Label(conversation.getLastMessageTime());
        time.getStyleClass().add("message-time");

        cell.getChildren().addAll(picContainer, info, time);
        return cell;
    }

    private void openConversation(Conversation conversation) {
        currentConversation = conversation;
        contactName.setText(conversation.getName());
        contactStatus
                .setText(conversation.isOnline() ? "En ligne" : "Dernière connexion : " + conversation.getLastSeen());

        // Mise à jour de la photo de l'en-tête
        updateActiveChatPic();

        messagesContainer.getChildren().clear();
        loadMessages(conversation);

        // Marquer les messages comme lus quand on ouvre la discussion
        for (Message m : conversation.getMessages()) {
            if (m.isSent())
                m.setRead(true);
        }

        conversation.setUnreadCount(0);
        conversationsList.refresh();
    }

    private void updateActiveChatPic() {
        if (currentConversation != null) {
            if (currentConversation.getProfileImagePath() != null) {
                try {
                    Image img = new Image(new File(currentConversation.getProfileImagePath()).toURI().toString());
                    activeChatPic.setFill(new ImagePattern(img));
                } catch (Exception e) {
                    activeChatPic.setFill(Color.web("#E4E6EB"));
                }
            } else {
                activeChatPic.setFill(Color.web("#E4E6EB"));
            }
        }
    }

    @FXML
    private void changeProfilePicture() {
        if (currentConversation == null)
            return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        File selectedFile = fileChooser.showOpenDialog(activeChatPic.getScene().getWindow());
        if (selectedFile != null) {
            currentConversation.setProfileImagePath(selectedFile.getAbsolutePath());
            updateActiveChatPic();
            conversationsList.refresh();
        }
    }

    private void loadMessages(Conversation conversation) {
        String lastDate = "";
        for (Message message : conversation.getMessages()) {
            String msgDate = message.getTime().contains(":") ? "Aujourd'hui" : message.getTime();
            if (!msgDate.equals(lastDate)) {
                addDateSeparator(msgDate);
                lastDate = msgDate;
            }
            addMessageBubble(message);
        }
        scrollToBottom();
    }

    private void addDateSeparator(String date) {
        HBox container = new HBox();
        container.getStyleClass().add("date-separator");
        container.setAlignment(Pos.CENTER);

        Label label = new Label(date);
        label.getStyleClass().add("date-text");

        container.getChildren().add(label);
        messagesContainer.getChildren().add(container);
    }

    private void addMessageBubble(Message message) {
        HBox messageBox = new HBox();
        messageBox.setPadding(new Insets(5, 0, 5, 0));
        messageBox.setSpacing(10);

        VBox bubble = new VBox(5);
        bubble.setPadding(new Insets(10, 15, 10, 15));
        bubble.setMaxWidth(450);

        Text messageText = new Text(message.getContent());
        messageText.getStyleClass().add("message-text");

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_RIGHT);
        Label timeLabel = new Label(message.getTime());
        timeLabel.getStyleClass().add("message-time-small");
        meta.getChildren().add(timeLabel);

        if (message.isSent()) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            bubble.getStyleClass().add("message-bubble-sent");

            // Logique des traits (checkmarks)
            String checks = (currentConversation != null && currentConversation.isOnline()) ? "✔✔" : "✔";
            Label checkmark = new Label(checks);
            checkmark.getStyleClass().add("message-time-small");

            if (message.isRead()) {
                checkmark.setStyle("-fx-text-fill: #44CC77;"); // Vert si lu
            } else {
                checkmark.setStyle("-fx-text-fill: #9CA3AF;"); // Gris par défaut
            }

            meta.getChildren().add(checkmark);
            messageBox.getChildren().add(bubble);
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
            bubble.getStyleClass().add("message-bubble-received");

            Circle avatar = new Circle(18);
            avatar.getStyleClass().add("profile-pic");
            if (currentConversation != null && currentConversation.getProfileImagePath() != null) {
                try {
                    Image img = new Image(new File(currentConversation.getProfileImagePath()).toURI().toString());
                    avatar.setFill(new ImagePattern(img));
                } catch (Exception e) {
                    avatar.setFill(Color.web("#D8D8D8"));
                }
            } else {
                avatar.setFill(Color.web("#D8D8D8"));
            }

            VBox picWrapper = new VBox(avatar);
            picWrapper.setAlignment(Pos.TOP_LEFT);
            messageBox.getChildren().addAll(picWrapper, bubble);
        }

        bubble.getChildren().addAll(messageText, meta);
        messagesContainer.getChildren().add(messageBox);
    }

    @FXML
    private void sendMessage() {
        String text = messageInput.getText().trim();
        if (!text.isEmpty() && currentConversation != null) {
            Message message = new Message(
                    text,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                    true);

            currentConversation.getMessages().add(message);
            currentConversation.setLastMessage(text);
            currentConversation.setLastMessageTime("À l'instant");

            addMessageBubble(message);
            messageInput.clear();
            scrollToBottom();
            conversationsList.refresh();
        }
    }

    private void scrollToBottom() {
        messagesScrollPane.setVvalue(1.0);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void newConversation() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nouvelle Discussion");
        dialog.setHeaderText("Ajouter un utilisateur");
        dialog.setContentText("Nom de l'utilisateur :");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            Conversation newConv = new Conversation(name, "#4A90E2");
            newConv.setLastSeen("À l'instant");
            conversations.add(0, newConv);
            observableConversations.add(0, newConv);
            conversationsList.getSelectionModel().select(newConv);
        });
    }

    @FXML
    private void attachFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un document ou une photo");
        File file = fileChooser.showOpenDialog(messagesScrollPane.getScene().getWindow());
        if (file != null) {
            Message msg = new Message("Fichier envoyé : " + file.getName(), "Maintenant", true);
            currentConversation.addMessage(msg);
            addMessageBubble(msg);
            scrollToBottom();
        }
    }

    @FXML
    private void voiceCall() {
        showAlert("Appel Audio", "Appel en cours vers " + contactName.getText() + "...");
    }

    @FXML
    private void videoCall() {
        showAlert("Appel Vidéo", "Appel vidéo en cours vers " + contactName.getText() + "...");
    }

    @FXML
    private void showSettings() {
        showAlert("Paramètres", "Ouverture des paramètres du profil...");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void loadDemoData() {
        conversations = new ArrayList<>();

        // Conversation 1
        Conversation conv1 = new Conversation("Alice Martin", "#4A90E2");
        conv1.setOnline(true);
        Message m1 = new Message("Salut ! Comment ça va ?", "10:30", false);
        Message m2 = new Message("Ça va bien merci ! Et toi ?", "10:32", true);
        m2.setRead(true); // Déjà lu
        Message m3 = new Message("Super ! Tu es libre ce soir ?", "10:35", false);

        conv1.getMessages().addAll(List.of(m1, m2, m3));
        conv1.setLastMessage("Super ! Tu es libre ce soir ?");
        conv1.setLastMessageTime("10:35");
        conv1.setUnreadCount(1);

        // Conversation 2
        Conversation conv2 = new Conversation("Thomas Dubois", "#E94B4B");
        conv2.addMessage(new Message("Salut Thomas, tu as pu voir le dossier ?", "Lundi", false));
        conv2.addMessage(new Message("Oui je regarde ça demain", "Lundi", true));
        conv2.addMessage(new Message("D'accord, à demain alors", "Hier", true));
        conv2.setLastMessage("D'accord, à demain alors");
        conv2.setLastMessageTime("Hier");
        conv2.setLastSeen("hier à 22:45");

        // Conversation 3
        Conversation conv3 = new Conversation("Sophie Laurent", "#50C878");
        conv3.setOnline(true);
        conv3.addMessage(new Message("N'oublie pas la réunion de demain", "09:15", false));
        conv3.addMessage(new Message("Merci du rappel !", "09:20", true));
        conv3.setLastMessage("Merci du rappel !");
        conv3.setLastMessageTime("09:20");

        // Conversation 4
        Conversation conv4 = new Conversation("Groupe Projet", "#9B59B6");
        conv4.setLastMessage("Marc: J'ai envoyé les fichiers");
        conv4.setLastMessageTime("Hier");
        conv4.setUnreadCount(5);

        conversations.addAll(List.of(conv1, conv2, conv3, conv4));
    }
}
