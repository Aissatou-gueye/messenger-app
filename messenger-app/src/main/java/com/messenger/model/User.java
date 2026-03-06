package com.messenger.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant un Utilisateur dans le système.
 * Mappe la table "users" de la base de données via Hibernate.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Identifiant unique auto-généré

    @Column(unique = true, nullable = false, length = 50)
    private String username; // Nom d'utilisateur unique (RG1)

    @Column(nullable = false)
    private String password; // Mot de passe (Haché avec BCrypt côté serveur - RG9)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status; // Statut de l'utilisateur (ONLINE/OFFLINE - RG4)

    @Column(nullable = false)
    private LocalDateTime dateCreation; // Date de création du compte

    @Column(columnDefinition = "TEXT")
    private String profilePicture; // Image encodée en Base64

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    /**
     * Enumération des statuts possibles pour un utilisateur.
     */
    public enum Status {
        ONLINE, // L'utilisateur est connecté au serveur
        OFFLINE // L'utilisateur est déconnecté ou a perdu le réseau
    }

    // =========================
    // Constructeurs
    // =========================

    // Constructeur vide obligatoire pour que JPA puisse instancier l'objet
    public User() {
        this.dateCreation = LocalDateTime.now();
        this.status = Status.OFFLINE;
    }

    /**
     * Constructeur pour créer un nouvel utilisateur.
     * 
     * @param username Le pseudo
     * @param password Le mot de passe (sera haché avant stockage)
     */
    public User(String username, String password) {
        this();
        this.username = username;
        this.password = password;
    }

    // =========================
    // Getters et Setters
    // =========================
    // (Poursuite des getters/setters standards...)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    // =========================
    // Méthode toString
    // =========================

    @Override
    public String toString() {
        return username + " (" + status + ")";
    }
}