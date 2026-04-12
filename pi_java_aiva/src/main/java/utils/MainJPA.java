package utils;

import Models.Report;
import Models.User;
import Services.ServiceReport;
import Services.ServiceUser;

import java.sql.SQLException;
import java.util.List;

public class MainJPA {
    public static void main(String[] args) {
        ServiceUser serviceUser = new ServiceUser();
        ServiceReport serviceReport = new ServiceReport();

        try {
            // 1. Retrieve an existing user (assuming user with ID 1 exists)
            // If not, you might need to create one first
            List<User> users = serviceUser.recuperer();
            if (users.isEmpty()) {
                System.out.println("No users found. Please create a user first.");
                return;
            }
            User testUser = users.get(0);
            System.out.println("Processing Reports for User: " + testUser.getName());

            // 2. Create a new Report
            Report newReport = new Report("AIVA System Analysis", 
                    "This is an automatically generated report via Hibernate JPA.", testUser);
            
            // 3. Save using Service
            serviceReport.addReport(testUser, newReport);
            System.out.println("✅ Report created successfully!");

            // 4. Retrieve reports for user
            List<Report> userReports = serviceReport.getReportsByUser(testUser);
            System.out.println("Found " + userReports.size() + " reports for " + testUser.getName());
            for (Report r : userReports) {
                System.out.println(" - [" + r.getCreatedAt() + "] " + r.getTitle());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            JPAUtil.shutdown();
        }
    }
}
