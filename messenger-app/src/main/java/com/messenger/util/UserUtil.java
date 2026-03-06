package com.messenger.util;

import com.messenger.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

/**
 * Abstraction pour les opérations de base de données liées aux Utilisateurs.
 */
public class UserUtil {
    private SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    /**
     * Sauvegarde ou met à jour un utilisateur en base (RG1, RG4).
     */
    public void save(User user) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.merge(user); // Fusionne les modifications si l'objet existe déjà
            session.getTransaction().commit();
        }
    }

    /**
     * Recherche un utilisateur par son identifiant unique.
     */
    public User findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(User.class, id);
        }
    }

    /**
     * Recherche un utilisateur par son pseudo (utilisé pour le login - RG2).
     */
    public User findByUsername(String username) {
        try (Session session = sessionFactory.openSession()) {
            Query<User> query = session.createQuery(
                    "FROM User WHERE username = :username", User.class);
            query.setParameter("username", username);
            return query.uniqueResult(); // Renvoie null si non trouvé
        }
    }

    /**
     * Récupère la liste de tous les utilisateurs enregistrés (RG5).
     */
    public List<User> findAll() {
        try (Session session = sessionFactory.openSession()) {
            Query<User> query = session.createQuery("FROM User", User.class);
            return query.list();
        }
    }

    /**
     * Supprime un utilisateur définitivement.
     */
    public void delete(Long id) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            User user = session.get(User.class, id);
            if (user != null) {
                session.remove(user);
            }
            session.getTransaction().commit();
        }
    }
}
