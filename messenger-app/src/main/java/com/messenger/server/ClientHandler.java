package com.messenger.server;

import com.messenger.model.*;
import com.messenger.util.MessageUtil;
import com.messenger.util.UserUtil;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

/**
 * Gestionnaire de connexion pour chaque client.
 * S'occupe de l'authentification, messagerie et notifications.
 */
/**
 * ClientHandler : Gère la communication individuelle entre le serveur et un
 * client.
 * Implémente Runnable pour être exécuté dans un thread séparé (RG11).
 */
public class ClientHandler implements Runnable {

    private final Socket socket; // Socket pour la communication réseau
    private BufferedReader input; // Flux de lecture (reçoit du client)
    private PrintWriter output; // Flux d'écriture (envoie au client)
    private String currentUsername; // Nom de l'utilisateur actuellement connecté via ce thread

    // Utilitaires pour l'accès à la base de données (JPA/Hibernate)
    private final UserUtil userUtil;
    private final MessageUtil messageUtil;
    private final com.messenger.util.GroupUtil groupUtil;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.userUtil = new UserUtil();
        this.messageUtil = new MessageUtil();
        this.groupUtil = new com.messenger.util.GroupUtil();

        try {
            // Initialisation des flux de communication
            this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.output = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Boucle principale d'écoute du client.
     */
    @Override
    public void run() {
        try {
            String messageFromClient;
            // Lit chaque ligne envoyée par le client jusqu'à la déconnexion
            while ((messageFromClient = input.readLine()) != null) {
                processMessage(messageFromClient);
            }
        } catch (IOException e) {
            System.out.println("[SERVER] Erreur de communication: " + e.getMessage());
        } finally {
            // Nettoyage lors de la déconnexion (RG4)
            disconnect();
        }
    }

    /**
     * Analyse la commande reçue et appelle la méthode correspondante.
     */
    private void processMessage(String message) {
        // Format typique : COMMANDE:arg1:arg2:arg3
        String[] parts = message.split(":", 4);
        String commandType = parts[0];

        try {
            switch (commandType) {
                case "REGISTER":
                    handleRegister(parts[1], parts[2]);
                    break;
                case "LOGIN":
                    handleLogin(parts[1], parts[2]);
                    break;
                case "LOGOUT":
                    handleLogout();
                    break;
                case "SEND_MESSAGE":
                    handleSendMessage(parts[1], parts[2]);
                    break;
                case "GET_USERS":
                    handleGetUsers();
                    break;
                case "GET_ALL_USERS":
                    handleGetAllUsers();
                    break;
                case "GET_HISTORY":
                    handleGetHistory(parts[1]);
                    break;
                case "GET_ALL_HISTORY":
                    handleGetAllHistory();
                    break;
                case "CREATE_GROUP":
                    handleCreateGroup(parts[1], parts[2]);
                    break;
                case "CALL":
                    handleCall(parts[1], parts[2]);
                    break;
                case "MESSAGE_READ":
                    handleMessageRead(parts[1]);
                    break;
                case "UPDATE_PIC":
                    handleUpdatePic(parts[1]);
                    break;
                default:
                    output.println("ERROR:Commande inconnue");
            }
        } catch (Exception e) {
            output.println("ERROR:" + e.getMessage());
        }
    }

    /**
     * Met à jour la photo de profil et notifie les autres.
     */
    private void handleUpdatePic(String b64) {
        User user = userUtil.findByUsername(currentUsername);
        if (user != null) {
            user.setProfilePicture(b64);
            userUtil.save(user);
        }
        // Informe tous les clients connectés du changement
        UserConnectionManager.getInstance().broadcastMessage("PIC_UPDATE:" + currentUsername + ":" + b64);
    }

    /**
     * Marque les messages comme lus dans la DB et notifie l'envoyeur.
     */
    private void handleMessageRead(String senderUsername) {
        User sender = userUtil.findByUsername(senderUsername);
        User receiver = userUtil.findByUsername(currentUsername);
        if (sender != null && receiver != null) {
            messageUtil.markAsRead(sender, receiver);
        }
        // Envoie une notification "Double check bleu" à l'envoyeur
        UserConnectionManager.getInstance().sendMessageToUser(
                senderUsername,
                "MESSAGE_READ_UPDATE:" + currentUsername);
    }

    /**
     * Renvoie la liste de tous les utilisateurs (pour le popup "Nouveau message").
     */
    private void handleGetAllUsers() {
        java.util.List<User> allUsers = userUtil.findAll();
        StringBuilder userList = new StringBuilder();
        for (User u : allUsers) {
            if (!u.getUsername().equals(currentUsername)) {
                userList.append(u.getUsername()).append(",");
            }
        }
        String result = userList.length() > 0
                ? userList.deleteCharAt(userList.length() - 1).toString()
                : "";
        output.println("ALL_USERS_LIST:" + result);
    }

    /**
     * Envoie l'historique de TOUTES les conversations de l'utilisateur (utilisé au
     * login).
     */
    private void handleGetAllHistory() {
        User sender = userUtil.findByUsername(currentUsername);
        if (sender != null) {
            java.util.List<Message> history = messageUtil.findAllConversations(sender);
            StringBuilder sb = new StringBuilder("ALL_HISTORY:");
            for (Message msg : history) {
                sb.append(msg.getSender().getUsername())
                        .append("=")
                        .append(msg.getReceiver().getUsername())
                        .append("=")
                        .append(msg.getContent().replace(":", "[[COLON]]").replace("|", "[[PIPE]]"))
                        .append("=")
                        .append(msg.getDateEnvoi().toString())
                        .append("|");
            }
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '|') {
                sb.deleteCharAt(sb.length() - 1);
            }
            output.println(sb.toString());
        }
    }

    // ---------------------------------------------------------
    // Gestion des utilisateurs (Inscription / Connexion)
    // ---------------------------------------------------------

    /**
     * Enregistre un nouvel utilisateur (RG1, RG9).
     */
    private void handleRegister(String username, String password) {
        System.out.println("[SERVER] Tentative d'inscription: " + username);
        User existingUser = userUtil.findByUsername(username);

        if (existingUser != null) {
            output.println("REGISTER_RESPONSE:FAILED:Utilisateur existe déjà");
            return;
        }

        // RG9: Hachage du mot de passe avec BCrypt
        String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());
        User newUser = new User(username, hashedPassword);
        userUtil.save(newUser);

        System.out.println("[SERVER] Inscription réussie: " + username);
        output.println("REGISTER_RESPONSE:SUCCESS:Inscription réussie");
    }

    /**
     * Authentifie un utilisateur (RG2, RG3, RG4, RG9).
     */
    private void handleLogin(String username, String password) {
        System.out.println("[SERVER] Tentative de connexion: " + username);
        User user = userUtil.findByUsername(username);

        // RG9: Vérification du mot de passe haché
        boolean passwordMatch = false;
        try {
            passwordMatch = user != null && org.mindrot.jbcrypt.BCrypt.checkpw(password, user.getPassword());
        } catch (Exception e) {
            System.out.println("[SERVER] Erreur format hash pour " + username);
        }

        if (!passwordMatch) {
            System.out.println("[SERVER] Échec de connexion: " + username);
            output.println("LOGIN_RESPONSE:FAILED:Identifiants incorrects");
            return;
        }

        // RG3: Empêche la double connexion
        if (UserConnectionManager.getInstance().isUserOnline(username)) {
            output.println("LOGIN_RESPONSE:FAILED:Déjà connecté");
            return;
        }

        this.currentUsername = username;
        // RG4: Passe en ONLINE
        user.setStatus(User.Status.ONLINE);
        userUtil.save(user);

        // Enregistre le client dans la liste globale des connectés
        UserConnectionManager.getInstance().addUser(username, this);

        String pic = user.getProfilePicture() != null ? user.getProfilePicture() : "";
        output.println("LOGIN_RESPONSE:SUCCESS:" + username + ":" + pic);

        // Notifie tout le monde du changement de statut
        UserConnectionManager.getInstance().broadcastStatusChange(username, "ONLINE");
        UserConnectionManager.getInstance().broadcastUserList();
    }

    /**
     * Déconnecte l'utilisateur et met à jour son statut (RG4).
     */
    private void handleLogout() {
        if (currentUsername != null) {
            System.out.println("[SERVER] Déconnexion de: " + currentUsername);
            User user = userUtil.findByUsername(currentUsername);
            if (user != null) {
                user.setStatus(User.Status.OFFLINE);
                userUtil.save(user); // RG4
            }

            UserConnectionManager.getInstance().removeUser(currentUsername);
            UserConnectionManager.getInstance().broadcastStatusChange(currentUsername, "OFFLINE");
            UserConnectionManager.getInstance().broadcastUserList();

            output.println("LOGOUT_RESPONSE:SUCCESS");
        }
    }

    // ---------------------------------------------------------
    // Gestion des messages (RG5, RG6, RG7, RG8)
    // ---------------------------------------------------------

    /**
     * Traite l'envoi d'un message entre deux utilisateurs.
     */
    private void handleSendMessage(String receiverUsername, String content) {
        // RG2 & RG5: Vérification de l'authentification
        if (currentUsername == null) {
            output.println("MESSAGE_RESPONSE:FAILED:Non authentifié");
            return;
        }

        // RG7: Longueur du message (1-1000 caractères)
        if (content == null || content.trim().isEmpty() || content.length() > 1000) {
            output.println("MESSAGE_RESPONSE:FAILED:Message invalide");
            return;
        }

        try {
            User sender = userUtil.findByUsername(currentUsername);
            User receiver = userUtil.findByUsername(receiverUsername);

            if (receiver == null) {
                output.println("MESSAGE_RESPONSE:FAILED:Destinataire introuvable");
                return;
            }

            // Création et sauvegarde du message en DB (RG8: Historique)
            Message message = new Message(sender, receiver, content);
            message.setDateEnvoi(LocalDateTime.now());
            message.setStatus(Message.Status.ENVOYE);
            messageUtil.save(message);

            // RG6: Si le destinataire est connecté, on lui envoie immédiatement
            boolean delivered = UserConnectionManager.getInstance().sendMessageToUser(
                    receiverUsername,
                    "MESSAGE_RECEIVED:" + currentUsername + ":" + content);

            if (delivered) {
                message.setStatus(Message.Status.RECU);
                messageUtil.save(message); // Mise à jour du statut "Reçu" (double coche)
            }

            output.println("MESSAGE_RESPONSE:SUCCESS");
        } catch (Exception e) {
            output.println("MESSAGE_RESPONSE:FAILED:Erreur serveur");
        }
    }

    /**
     * Envoie la liste des contacts avec leurs derniers messages et statuts.
     */
    public void handleGetUsers() {
        StringBuilder userList = new StringBuilder();
        java.util.List<User> allUsers = userUtil.findAll();
        User currentUserObj = userUtil.findByUsername(currentUsername);

        for (User u : allUsers) {
            if (!u.getUsername().equals(currentUsername)) {
                String status = UserConnectionManager.getInstance().isUserOnline(u.getUsername()) ? "ONLINE"
                        : "OFFLINE";

                // Récupère l'aperçu du dernier message pour l'affichage dans la liste
                Message last = messageUtil.findLastMessage(currentUserObj, u);
                String lastMsg = "Pas de message";
                String time = "";
                if (last != null) {
                    lastMsg = (last.getSender().getUsername().equals(currentUsername) ? "Moi: " : "")
                            + last.getContent();
                    time = last.getDateEnvoi().toString();
                }

                long unread = messageUtil.countUnread(u, currentUserObj);
                String pic = u.getProfilePicture() != null ? u.getProfilePicture() : "";

                userList.append(u.getUsername()).append(":")
                        .append(status).append(":")
                        .append(lastMsg.replace(":", "[[COLON]]").replace(",", "[[COMMA]]")).append(":")
                        .append(time).append(":")
                        .append(unread).append(":")
                        .append(pic).append(",");
            }
        }
        String result = userList.length() > 0 ? userList.substring(0, userList.length() - 1) : "";
        output.println("USERS_LIST:" + result);
    }

    /**
     * Notifie un utilisateur d'un appel entrant (audio ou vidéo).
     */
    private void handleCall(String type, String receiverUsername) {
        UserConnectionManager.getInstance().sendMessageToUser(
                receiverUsername,
                "CALL_INCOMING:" + type + ":" + currentUsername);
    }

    /**
     * Crée un groupe de discussion et notifie les membres (RG5).
     */
    private void handleCreateGroup(String groupName, String members) {
        Group group = new Group(groupName);
        String[] memberList = members.split(",");
        java.util.Set<User> memberSet = new java.util.HashSet<>();

        for (String memberName : memberList) {
            User u = userUtil.findByUsername(memberName);
            if (u != null)
                memberSet.add(u);
        }

        group.setMembers(memberSet);
        groupUtil.save(group);

        // Notifie chaque membre de la création du groupe
        for (String member : memberList) {
            UserConnectionManager.getInstance().sendMessageToUser(
                    member,
                    "GROUP_CREATED:" + groupName + ":" + members);
        }
    }

    /**
     * Renvoie l'historique complet entre deux utilisateurs (RG8).
     */
    private void handleGetHistory(String contactUsername) {
        User sender = userUtil.findByUsername(currentUsername);
        User contact = userUtil.findByUsername(contactUsername);

        if (sender != null && contact != null) {
            java.util.List<Message> history = messageUtil.findConversation(sender, contact);
            StringBuilder sb = new StringBuilder("HISTORY:" + contactUsername + ":");
            for (Message msg : history) {
                sb.append(msg.getSender().getUsername())
                        .append("=")
                        .append(msg.getContent())
                        .append("|");
            }
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '|') {
                sb.deleteCharAt(sb.length() - 1);
            }
            output.println(sb.toString());
        }
    }

    /**
     * Envoie une chaîne brute au client.
     */
    public void sendMessage(String message) {
        output.println(message);
    }

    /**
     * Ferme la connexion proprement.
     */
    private void disconnect() {
        try {
            handleLogout();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}