package com.messenger.server;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire centralisé des connexions actives sur le serveur.
 * Permet de savoir qui est en ligne et d'envoyer des messages ciblés.
 */
public class UserConnectionManager {
    // Singleton pour accès global sur le serveur
    private static final UserConnectionManager instance = new UserConnectionManager();

    // Map thread-safe associant un pseudo à son gestionnaire de thread (RG11)
    private final Map<String, ClientHandler> connectedUsers = new ConcurrentHashMap<>();

    private UserConnectionManager() {
    }

    public static UserConnectionManager getInstance() {
        return instance;
    }

    /**
     * Enregistre un utilisateur dans la liste des actifs (RG4).
     */
    public void addUser(String username, ClientHandler handler) {
        connectedUsers.put(username, handler);
        System.out.println("[SERVER] " + username + " est maintenant ONLINE.");
    }

    /**
     * Retire un utilisateur lors de sa déconnexion (RG4).
     */
    public void removeUser(String username) {
        connectedUsers.remove(username);
        System.out.println("[SERVER] " + username + " est maintenant OFFLINE.");
    }

    /**
     * Vérifie si l'utilisateur possède déjà une session active (RG3).
     */
    public boolean isUserOnline(String username) {
        return connectedUsers.containsKey(username);
    }

    /**
     * Envoie une commande spécifique à un utilisateur précis (Privé).
     * 
     * @return true si le message a pu être envoyé (utilisateur en ligne).
     */
    public boolean sendMessageToUser(String recipient, String message) {
        ClientHandler handler = connectedUsers.get(recipient);
        if (handler != null) {
            handler.sendMessage(message);
            return true;
        }
        return false;
    }

    /**
     * Envoie la liste mise à jour de tous les utilisateurs à tout le monde.
     */
    public void broadcastUserList() {
        for (ClientHandler handler : connectedUsers.values()) {
            handler.handleGetUsers();
        }
    }

    /**
     * Notifie tous les clients qu'un utilisateur vient de changer de statut (RG4).
     */
    public void broadcastStatusChange(String username, String status) {
        String notification = "USER_STATUS:" + username + ":" + status;
        for (ClientHandler handler : connectedUsers.values()) {
            handler.sendMessage(notification);
        }
    }

    /**
     * Envoie une annonce à l'ensemble du serveur.
     */
    public void broadcastMessage(String message) {
        for (ClientHandler handler : connectedUsers.values()) {
            handler.sendMessage(message);
        }
    }
}
