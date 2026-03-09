package com.messenger.util;

import com.messenger.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.util.List;

public class DatabaseCleanup {
    public static void main(String[] args) {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            
            List<User> users = session.createQuery("FROM User", User.class).list();
            int count = 0;
            for (User user : users) {
                // BCrypt hashes start with $2a$ or $2b$ or $2y$
                if (user.getPassword() == null || !user.getPassword().startsWith("$2")) {
                    System.out.println("Deleting user with non-hashed password: " + user.getUsername());
                    session.remove(user);
                    count++;
                }
            }
            
            session.getTransaction().commit();
            System.out.println("Cleanup finished. Deleted " + count + " users.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sessionFactory.close();
            System.exit(0);
        }
    }
}
