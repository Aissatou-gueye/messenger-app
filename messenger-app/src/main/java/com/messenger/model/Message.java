package com.messenger.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant un Message échangé.
 * Permet de stocker l'historique des conversations (RG8).
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Utilisateur expéditeur (RG5: Doit être connecté au moment de l'envoi)
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // Utilisateur destinataire (RG5: Doit exister)
    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = true)
    private User receiver;

    // Groupe destinataire (Optionnel, si le message est envoyé dans un groupe)
    @ManyToOne
    @JoinColumn(name = "group_id", nullable = true)
    private Group group;

    // Contenu textuel du message (RG7: Max 1000 caractères)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime dateEnvoi; // Date et heure de l'envoi pour le tri chronologique (RG8)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status; // État de livraison (RG6)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type = Type.TEXT;

    /**
     * Type de contenu (Texte, Image ou Fichier).
     */
    public enum Type {
        TEXT, IMAGE, FILE
    }

    /**
     * Statuts de livraison du message.
     */
    public enum Status {
        ENVOYE, // Message stocké en base mais pas encore reçu par le destinataire (RG6)
        RECU, // Message reçu par l'application du destinataire (Une coche)
        LU // Message affiché par le destinataire (Double coche bleue)
    }

    // Constructeur par défaut JPA
    public Message() {
        this.dateEnvoi = LocalDateTime.now();
        this.status = Status.ENVOYE;
    }

    /**
     * Création d'un message standard entre deux utilisateurs.
     */
    public Message(User sender, User receiver, String content) {
        this();
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}