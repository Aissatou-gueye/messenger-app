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
 * Contrôleur pour la vue d'Inscription.
 */
public class RegisterController {

    @FXML
    private TextField usernameField; // Pseudo désiré

    @FXML
    private PasswordField passwordField; // Mot de passe souhaité

    /**
     * Gère l'action du bouton "S'inscrire".
     * Envoie une requête REGISTER au serveur (RG1).
     */
    @FXML
    private void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            return;
        }

        try {
            com.messenger.client.ClientSocket socket = com.messenger.client.SessionManager.getInstance()
                    .getClientSocket();

            if (!socket.isConnected() && !socket.connect()) {
                return;
            }

            // Envoi de la commande REGISTER:user:pass
            // Le serveur vérifiera l'unicité du pseudo (RG1) et hachera le mot de passe
            // (RG9)
            socket.sendMessage("REGISTER:" + username + ":" + password);

            // Attente du résultat de l'inscription
            String response = socket.getInput().readLine();

            if (response != null && response.startsWith("REGISTER_RESPONSE:SUCCESS")) {
                // Retour automatique au login après succès
                navigateTo(event, "/fxml/login.fxml");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retour à l'écran de connexion.
     */
    @FXML
    private void goBack(ActionEvent event) {
        navigateTo(event, "/fxml/login.fxml");
    }

    /**
     * Redirection JavaFX fluide.
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
