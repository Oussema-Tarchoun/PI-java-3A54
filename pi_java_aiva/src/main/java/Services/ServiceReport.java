package Services;

import Models.Report;
import Models.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.JPAUtil;

import java.util.List;

public class ServiceReport {

    /**
     * Adds a new report and links it to the user.
     */
    public void addReport(User user, Report report) {
        Transaction transaction = null;
        try (Session session = JPAUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            // Explicitly link the report to the user
            report.setUser(user);
            
            // Save the report
            session.persist(report);
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all reports for a specific user.
     */
    public List<Report> getReportsByUser(User user) {
        try (Session session = JPAUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Report where user.id = :userId", Report.class)
                    .setParameter("userId", user.getId())
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves all reports for all users.
     */
    public List<Report> recupererTout() {
        try (Session session = JPAUtil.getSessionFactory().openSession()) {
            // join fetch user to load associated user data efficiently
            return session.createQuery("select r from Report r join fetch r.user", Report.class).list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deletes a specific report.
     */
    public void deleteReport(Report report) {
        Transaction transaction = null;
        try (Session session = JPAUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.remove(session.contains(report) ? report : session.merge(report));
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }
}
