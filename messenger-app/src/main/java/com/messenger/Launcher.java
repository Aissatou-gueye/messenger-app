package com.messenger;

/**
 * Point d'entrée principal pour lancer l'application.
 * Nécessaire pour contourner certains problèmes de classpath avec JavaFX
 * lors de la création d'un fichier JAR exécutable.
 */
public class Launcher {
    public static void main(String[] args) {
        MessengerApp.main(args);
    }
}
