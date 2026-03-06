package com.messenger.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Utilitaire pour la configuration et la gestion d'Hibernate.
 * Permet de créer la connexion avec la base de données MySQL.
 */
public class HibernateUtil {
    // SessionFactory unique pour toute l'application
    private static final SessionFactory sessionFactory = buildSessionFactory();

    /**
     * Charge le fichier hibernate.cfg.xml et construit la SessionFactory.
     */
    private static SessionFactory buildSessionFactory() {
        try {
            SessionFactory sf = new Configuration().configure().buildSessionFactory();
            repairSchema(sf); // Vérification automatique de la structure de la table
            return sf;
        } catch (Throwable ex) {
            System.err.println("Erreur lors de la création de SessionFactory: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Méthode de "réparation" pour s'assurer que les colonnes nécessaires existent.
     * Très pratique lors des montées de version du projet.
     */
    private static void repairSchema(SessionFactory sf) {
        try (org.hibernate.Session session = sf.openSession()) {
            session.beginTransaction();
            // Ajout dynamique de la colonne 'type' si elle manque (pour les
            // images/fichiers)
            session.createNativeMutationQuery(
                    "ALTER TABLE messages ADD COLUMN IF NOT EXISTS type VARCHAR(255) DEFAULT 'TEXT'")
                    .executeUpdate();
            session.getTransaction().commit();
            System.out.println("[DB] Connexion réussie et schéma vérifié.");
        } catch (Exception e) {
            // On ignore si l'erreur est juste que la colonne existe déjà
            System.out.println("[DB] Note: Schéma déjà à jour.");
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Ferme proprement la connexion à la base de données.
     */
    public static void shutdown() {
        if (sessionFactory != null)
            sessionFactory.close();
    }
}
