package com.messenger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Classe de lancement de l'application cliente JavaFX.
 */
public class MessengerApp extends Application {

    /**
     * Point d'entrée JavaFX : charge la première vue (Login).
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Chargement du fichier FXML correspondant à la page de connexion
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 700);

        // Application de la feuille de style CSS globale
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setTitle("Polytech Messenger"); // Titre de la fenêtre
        primaryStage.setScene(scene);

        // Contraintes de taille minimale pour préserver le design
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        primaryStage.show(); // Affichage de la fenêtre
    }

    public static void main(String[] args) {
        launch(args); // Déclenche le cycle de vie JavaFX
    }
}
