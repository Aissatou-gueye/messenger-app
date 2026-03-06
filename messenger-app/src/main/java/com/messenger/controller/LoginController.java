package com.messenger.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Contrôleur pour la vue de Connexion (Login).
 */
public class LoginController {

    @FXML
    private TextField usernameField; // Champ pour le pseudo

    @FXML
    private PasswordField passwordField; // Champ mot de passe masqué

    @FXML
    private TextField passwordTextField; // Champ mot de passe en clair (pour le mode "œil")

    /**
     * Gère l'action du bouton "Se connecter".
     * Envoie une requête LOGIN au serveur (RG2).
     */
    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.isVisible() ? passwordField.getText().trim()
                : passwordTextField.getText().trim();

        // Validation basique des champs vides
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Champs vides",
                    "Veuillez remplir tous les champs.");
            return;
        }

        try {
            com.messenger.client.ClientSocket socket = com.messenger.client.SessionManager.getInstance()
                    .getClientSocket();

            // Vérifie si on est bien connecté au serveur (RG10)
            if (!socket.isConnected() && !socket.connect()) {
                showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur Serveur",
                        "Impossible de se connecter au serveur.");
                return;
            }

            // Envoi de la commande LOGIN:user:pass
            socket.sendMessage("LOGIN:" + username + ":" + password);

            // Lecture de la réponse du serveur
            String response;
            while ((response = socket.getInput().readLine()) != null) {
                if (response.startsWith("LOGIN_RESPONSE:SUCCESS")) {
                    // Si succès : Création de l'objet utilisateur et redirection vers le chat (RG4)
                    String[] parts = response.split(":", 4);
                    com.messenger.model.User user = new com.messenger.model.User(username, password);
                    user.setId((long) username.hashCode());
                    user.setStatus(com.messenger.model.User.Status.ONLINE);
                    if (parts.length > 3) {
                        user.setProfilePicture(parts[3]);
                    }
                    com.messenger.client.SessionManager.getInstance().setCurrentUser(user);

                    showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Succès",
                            "Connexion réussie ! Bienvenue " + username);

                    navigateTo(event, "/fxml/main.fxml");
                    return;
                } else if (response.startsWith("LOGIN_RESPONSE:FAILED")) {
                    // Si échec (Identifiants faux ou déjà connecté - RG3)
                    String errorMsg = response.substring(response.lastIndexOf(":") + 1);
                    showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Échec de connexion",
                            "Identifiants incorrects : " + errorMsg);
                    return;
                } else if (response.startsWith("ERROR:")) {
                    showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur Serveur",
                            response.substring(6));
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur",
                    "Une erreur est survenue lors de la connexion.");
        }
    }

    /**
     * Alterne entre l'affichage masqué et visible du mot de passe.
     */
    @FXML
    private void togglePasswordVisible(ActionEvent event) {
        if (passwordField.isVisible()) {
            passwordTextField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordTextField.setVisible(true);
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordTextField.setVisible(false);
            passwordField.setVisible(true);
        }
    }

    /**
     * Affiche une boîte de dialogue d'alerte.
     */
    private void showAlert(javafx.scene.control.Alert.AlertType type, String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Redirige vers la vue d'inscription.
     */
    @FXML
    private void goToRegister(ActionEvent event) {
        navigateTo(event, "/fxml/register.fxml");
    }

    /**
     * Utilitaire pour changer de scène JavaFX.
     */
    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
