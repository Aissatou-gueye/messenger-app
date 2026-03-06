package com.messenger.client;

import java.io.*;
import java.net.Socket;

/**
 * Gère la connexion réseau bas niveau entre le client et le serveur.
 */
public class ClientSocket {
    private Socket socket; // Objet Socket Java pour la connexion TCP
    private PrintWriter output; // Flux pour envoyer des données au serveur
    private BufferedReader input; // Flux pour recevoir des données du serveur
    private String serverAddress;
    private int serverPort;

    public ClientSocket(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    /**
     * Tente d'établir une connexion avec le serveur.
     * 
     * @return true si la connexion est réussie.
     */
    public boolean connect() {
        try {
            socket = new Socket(serverAddress, serverPort);
            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("[CLIENT] Connecté au serveur " + serverAddress + ":" + serverPort);
            return true;
        } catch (IOException e) {
            System.out.println("[CLIENT] Erreur de connexion: " + e.getMessage());
            return false;
        }
    }

    /**
     * Envoie une chaîne de caractères (commande) au serveur.
     */
    public void sendMessage(String message) {
        if (output != null) {
            output.println(message);
        }
    }

    public BufferedReader getInput() {
        return input;
    }

    /**
     * Ferme proprement la connexion (RG4).
     */
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("[CLIENT] Déconnecté du serveur");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Vérifie si le socket est toujours ouvert et connecté (RG10).
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
