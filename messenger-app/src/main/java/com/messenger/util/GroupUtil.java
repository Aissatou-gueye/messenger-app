package com.messenger.util;

import com.messenger.model.Group;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

/**
 * Abstraction pour les opérations de base de données liées aux Groupes.
 */
public class GroupUtil {
    private SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    /**
     * Crée ou met à jour un groupe de discussion (RG5).
     */
    public void save(Group group) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.merge(group);
            session.getTransaction().commit();
        }
    }

    /**
     * Liste tous les groupes existants.
     */
    public List<Group> findAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Group", Group.class).list();
        }
    }

    /**
     * Recherche un groupe précis par son nom.
     */
    public Group findByName(String name) {
        try (Session session = sessionFactory.openSession()) {
            Query<Group> query = session.createQuery("FROM Group WHERE name = :name", Group.class);
            query.setParameter("name", name);
            return query.uniqueResult();
        }
    }
}
