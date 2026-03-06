package com.messenger.client;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Thread d'écoute qui tourne en arrière-plan côté client.
 * Il reçoit les messages du serveur de manière asynchrone (RG11).
 */
public class MessageReceiver implements Runnable {
    private BufferedReader input; // Flux d'entrée du serveur
    private ClientSocket clientSocket;
    private Callback callback; // Interface pour notifier le contrôleur UI

    /**
     * Interface de rappel pour transmettre les données à l'interface graphique.
     */
    public interface Callback {
        void onMessageReceived(String messageType, String[] data);

        void onConnectionLost(); // Appelé lors de la perte de connexion (RG10)
    }

    public MessageReceiver(BufferedReader input, ClientSocket clientSocket, Callback callback) {
        this.input = input;
        this.clientSocket = clientSocket;
        this.callback = callback;
    }

    /**
     * Boucle de lecture infinie.
     */
    @Override
    public void run() {
        try {
            String messageFromServer;
            // Lit chaque ligne envoyée par le serveur
            while ((messageFromServer = input.readLine()) != null) {
                parseAndHandle(messageFromServer);
            }
        } catch (IOException e) {
            System.out.println("[CLIENT] Connexion perdue: " + e.getMessage());
            // Si le socket se ferme, on informe l'UI pour rediriger vers le login (RG10)
            if (callback != null) {
                callback.onConnectionLost();
            }
        }
    }

    /**
     * Découpe la commande reçue et notifie le contrôleur principal.
     */
    private void parseAndHandle(String message) {
        // Format du serveur : TYPE_COMMANDE:données...
        String[] parts = message.split(":", 2);
        if (parts.length < 2)
            return;

        String messageType = parts[0];

        if (callback != null) {
            callback.onMessageReceived(messageType, parts);
        }

        System.out.println("[CLIENT RECEIVER] Reçu : " + message);
    }
}
