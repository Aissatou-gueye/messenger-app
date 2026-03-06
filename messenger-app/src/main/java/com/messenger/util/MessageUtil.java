package com.messenger.util;

import com.messenger.model.Message;
import com.messenger.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

/**
 * Abstraction pour les opérations de base de données liées aux Messages.
 */
public class MessageUtil {
    private SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    /**
     * Enregistre un message en base de données (RG6: Livraison différée).
     */
    public void save(Message message) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.merge(message);
            session.getTransaction().commit();
        }
    }

    public Message findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Message.class, id);
        }
    }

    /**
     * Récupère la conversation complète entre deux personnes (RG8: Ordre
     * chronologique ASC).
     */
    public List<Message> findConversation(User user1, User user2) {
        try (Session session = sessionFactory.openSession()) {
            Query<Message> query = session.createQuery(
                    "FROM Message WHERE (sender = :user1 AND receiver = :user2) " +
                            "OR (sender = :user2 AND receiver = :user1) " +
                            "ORDER BY dateEnvoi ASC",
                    Message.class);
            query.setParameter("user1", user1);
            query.setParameter("user2", user2);
            return query.list();
        }
    }

    /**
     * Met à jour le statut des messages reçus pour passer à "LU" (Double coche
     * bleue).
     */
    public void markAsRead(User sender, User receiver) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.createMutationQuery(
                    "UPDATE Message SET status = 'LU' WHERE sender = :sender AND receiver = :receiver AND status != 'LU'")
                    .setParameter("sender", sender)
                    .setParameter("receiver", receiver)
                    .executeUpdate();
            session.getTransaction().commit();
        }
    }

    /**
     * Récupère uniquement le tout dernier message échangé (pour l'aperçu dans la
     * liste).
     */
    public Message findLastMessage(User user1, User user2) {
        try (Session session = sessionFactory.openSession()) {
            Query<Message> query = session.createQuery(
                    "FROM Message WHERE (sender = :user1 AND receiver = :user2) " +
                            "OR (sender = :user2 AND receiver = :user1) " +
                            "ORDER BY dateEnvoi DESC",
                    Message.class);
            query.setParameter("user1", user1);
            query.setParameter("user2", user2);
            query.setMaxResults(1);
            return query.uniqueResult();
        }
    }

    /**
     * Compte le nombre de messages non lus envoyés par un contact précis.
     */
    public long countUnread(User sender, User receiver) {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(m) FROM Message m WHERE m.sender = :sender AND m.receiver = :receiver AND m.status != 'LU'",
                    Long.class);
            query.setParameter("sender", sender);
            query.setParameter("receiver", receiver);
            return query.uniqueResult();
        }
    }

    /**
     * Récupère l'intégralité des messages concernant un utilisateur.
     */
    public List<Message> findAllConversations(User user) {
        try (Session session = sessionFactory.openSession()) {
            Query<Message> query = session.createQuery(
                    "FROM Message WHERE sender = :user OR receiver = :user " +
                            "ORDER BY dateEnvoi ASC",
                    Message.class);
            query.setParameter("user", user);
            return query.list();
        }
    }
}
