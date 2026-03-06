package com.messenger.network;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Objet de transfert de données (DTO) pour la communication Réseau.
 * Permet d'encapsuler toutes les commandes client-serveur (RG12).
 */
public class NetworkMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Liste des commandes possibles dans le protocole de l'application.
     */
    public enum Type {
        // Authentification (RG2, RG3)
        REGISTER,
        REGISTER_RESPONSE,
        LOGIN,
        LOGIN_RESPONSE,
        LOGOUT,

        // Messagerie (RG5, RG6, RG7)
        SEND_MESSAGE,
        MESSAGE_RECEIVED, // Notification de réception (Double coche)

        // Gestion des utilisateurs et statuts (RG4)
        GET_USERS,
        USERS_LIST,
        USER_STATUS_CHANGED,

        // Erreurs système
        ERROR
    }

    /**
     * Status de réponse pour les opérations.
     */
    public enum Status {
        SUCCESS, // Opération réussie
        FAILED, // Opération échouée (Ex : Identifiant incorrect)
        ONLINE, // Statut Connecté (RG4)
        OFFLINE, // Statut Déconnecté (RG4)
        SENT, // Message envoyé au serveur
        RECEIVED, // Message arrivé chez le destinataire
        READ // Message vu par le destinataire
    }

    // Champs du message réseau
    private Type type; // Type de commande
    private String sender; // Expéditeur
    private String receiver; // Destinataire
    private String content; // Corps du message (RG7)
    private Status status; // Statut de l'opération
    private LocalDateTime timestamp; // Horodatage (RG8)

    // =========================
    // CONSTRUCTEURS
    // =========================

    public NetworkMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public NetworkMessage(Type type) {
        this();
        this.type = type;
    }

    public NetworkMessage(Type type, String sender, String receiver, String content) {
        this();
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    // =========================
    // GETTERS / SETTERS
    // =========================

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "NetworkMessage{" +
                "type=" + type +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", content='" + content + '\'' +
                ", status=" + status +
                ", timestamp=" + timestamp +
                '}';
    }
}