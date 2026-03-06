package com.messenger.client;

import com.messenger.model.User;

/**
 * Gère la session utilisateur actuelle côté client.
 * Utilise le pattern Singleton pour être accessible partout dans l'application.
 */
public class SessionManager {
    private static SessionManager instance;
    private ClientSocket clientSocket; // Socket partagé pour toute l'application
    private User currentUser; // Informations de l'utilisateur connecté

    private SessionManager() {
        // Initialisation du socket sur l'IP locale et le port 5000 (RG11)
        clientSocket = new ClientSocket("127.0.0.1", 5000);
    }

    /**
     * @return L'instance unique du SessionManager.
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public ClientSocket getClientSocket() {
        return clientSocket;
    }

    /**
     * @return L'utilisateur actuellement authentifié (RG2).
     */
    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}
