package utils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Sends emails via Gmail SMTP.
 * Used to deliver generated roadmaps to the user.
 */
public class EmailService {

    // ── Gmail SMTP config ──────────────────────────────────────────────────────
    // Replace with your Gmail address and App Password
    // To get an App Password: Google Account → Security → 2-Step Verification → App Passwords
    private static final String SMTP_HOST       = "smtp.gmail.com";
    private static final int    SMTP_PORT       = 587;
    private static final String SENDER_EMAIL    = "houssemlemjid02@gmail.com";      // ← your Gmail
    private static final String SENDER_PASSWORD = "zgis ymnb ntmd pajp";      // ← 16-char App Password
    public static final  String TEST_RECIPIENT  = "houssem.lemjid@esprit.tn"; // ← your real email  // ← any email, Mailtrap catches it  // ← replace for testing

    /**
     * Send a roadmap by email.
     *
     * @param toEmail      recipient email address
     * @param coursTitre   course title
     * @param level        course level
     * @param roadmapText  the generated roadmap content
     * @return true if sent successfully, false otherwise
     */
    public boolean sendRoadmap(String toEmail, String coursTitre, String level, String roadmapText) {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "AIVA Learning Platform"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("🎯 Votre Roadmap — " + coursTitre);
            message.setContent(buildEmailBody(coursTitre, level, roadmapText), "text/html; charset=utf-8");

            Transport.send(message);
            return true;

        } catch (Exception e) {
            System.err.println("EmailService error: " + e.getMessage());
            return false;
        }
    }

    /** Build a clean HTML email body for the roadmap */
    private String buildEmailBody(String coursTitre, String level, String roadmapText) {
        // Convert plain text line breaks to HTML
        String formattedRoadmap = roadmapText
                .replace("\n", "<br>")
                .replace("•", "&#8226;");

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Arial, sans-serif; background: #0a0e1a; color: #ffffff; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background: #161b2e; border-radius: 16px; overflow: hidden; border: 1px solid #2a3142; }
                        .header { background: linear-gradient(135deg, #8b5cf6, #00d4ff); padding: 32px; text-align: center; }
                        .header h1 { margin: 0; font-size: 28px; color: #ffffff; }
                        .header p { margin: 8px 0 0; color: rgba(255,255,255,0.8); font-size: 14px; }
                        .badge { display: inline-block; background: rgba(255,255,255,0.2); color: #ffffff; padding: 4px 14px; border-radius: 20px; font-size: 12px; font-weight: bold; margin-top: 12px; }
                        .content { padding: 32px; }
                        .content h2 { color: #00d4ff; font-size: 18px; margin-bottom: 16px; }
                        .roadmap { background: #111827; border-radius: 12px; padding: 24px; line-height: 1.8; color: #c0c8e8; font-size: 14px; border-left: 4px solid #8b5cf6; }
                        .footer { padding: 20px 32px; text-align: center; color: #64748b; font-size: 12px; border-top: 1px solid #2a3142; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🎯 Votre Roadmap d'Apprentissage</h1>
                            <p>%s</p>
                            <span class="badge">%s</span>
                        </div>
                        <div class="content">
                            <h2>📋 Votre plan personnalisé :</h2>
                            <div class="roadmap">%s</div>
                        </div>
                        <div class="footer">
                            Généré par AIVA Learning Platform • Bonne chance dans votre apprentissage ! 🚀
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(coursTitre, level.toUpperCase(), formattedRoadmap);
    }
}