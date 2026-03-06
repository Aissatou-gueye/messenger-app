package com.messenger.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Classe principale du Serveur.
 * Gère l'acceptation des nouvelles connexions et lance des threads pour chaque
 * client.
 */
public class Server {
    private static final int PORT = 5000; // Port d'écoute par défaut
    private ServerSocket serverSocket; // Socket principal du serveur

    public Server() {
        try {
            // Création du socket serveur (RG12: Journalisation du démarrage)
            serverSocket = new ServerSocket(PORT);
            System.out.println("[SERVER] Serveur démarré sur le port " + PORT);
        } catch (IOException e) {
            System.err.println("Erreur: Impossible de créer le serveur sur le port " + PORT);
            e.printStackTrace();
        }
    }

    /**
     * Lance la boucle infinie d'écoute.
     */
    public void start() {
        try {
            while (true) {
                // Attend qu'un client se connecte (Bloquant)
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] Nouveau client connecté: " + clientSocket.getInetAddress());

                // RG11: Chaque client est géré dans un thread séparé pour ne pas bloquer les
                // autres
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'acceptation d'un client: " + e.getMessage());
        } finally {
            try {
                if (serverSocket != null)
                    serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Point d'entrée du serveur.
     */
    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
