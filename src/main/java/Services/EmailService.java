package Services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static final String FROM = "aiva.nutrition.app@gmail.com";
    private static final String APP_PWD = "//"; // ← ton app password

    public void sendRapportNutrition(String toEmail, String userName,
            com.fasterxml.jackson.databind.JsonNode data) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM, APP_PWD);
            }
        });

        int score = data.get("score").asInt();
        String resume = data.get("resume").asText();
        String today = java.time.LocalDate.now().toString();

        // Build HTML
        StringBuilder html = new StringBuilder();
        html.append(
                "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;background:#0a0e1a;color:#fff;border-radius:12px;overflow:hidden;'>")
                .append("<div style='background:#0f766e;padding:24px;text-align:center;'>")
                .append("<h1 style='margin:0;font-size:24px;'>AIVA</h1>")
                .append("<p style='margin:4px 0 0;color:#a7f3d0;'>Votre assistant nutrition intelligent</p>")
                .append("</div>")
                .append("<div style='padding:24px;'>")
                .append("<p style='color:#94a3b8;'>Rapport du ").append(today).append("</p>")
                .append("<p>Bonjour <strong>").append(userName)
                .append("</strong>, voici votre rapport nutritionnel hebdomadaire.</p>")

                // Score
                .append("<div style='text-align:center;padding:20px;background:#161b2e;border-radius:10px;margin:16px 0;'>")
                .append("<div style='font-size:48px;font-weight:bold;color:#f59e0b;'>").append(score).append("</div>")
                .append("<div style='color:#94a3b8;'>").append(resume).append("</div>")
                .append("</div>");

        // Stats
        if (data.has("stats")) {
            var stats = data.get("stats");
            html.append("<div style='display:flex;gap:12px;margin:16px 0;'>")
                    .append(statBox(stats.get("total_repas").asText(), "Repas"))
                    .append(statBox(stats.get("total_jours").asText(), "Jours"))
                    .append(statBox(stats.get("kcal_moy_par_jour").asText() + " kcal", "kcal/jour moy."))
                    .append("</div>");
        }

        // Points positifs
        html.append("<h3 style='color:#86efac;'>✅ Points positifs</h3><ul>");
        data.get("points_positifs").forEach(
                p -> html.append("<li style='color:#cbd5e1;margin:4px 0;'>").append(p.asText()).append("</li>"));
        html.append("</ul>");

        // À améliorer
        html.append("<h3 style='color:#fbbf24;'>⚠️ À améliorer</h3><ul>");
        data.get("a_ameliorer").forEach(
                p -> html.append("<li style='color:#cbd5e1;margin:4px 0;'>").append(p.asText()).append("</li>"));
        html.append("</ul>");

        // Analyse par jour
        html.append("<h3 style='color:#93c5fd;'>📅 Analyse par jour</h3>");
        data.get("analyse_par_jour").forEach(j -> html
                .append("<div style='background:#161b2e;padding:10px 14px;border-radius:6px;margin:6px 0;'>")
                .append("<strong style='color:#94a3b8;'>").append(j.get("date").asText()).append("</strong>")
                .append("<p style='margin:4px 0;color:#cbd5e1;'>").append(j.get("commentaire").asText()).append("</p>")
                .append("</div>"));

        // Conseils
        html.append("<h3 style='color:#c4b5fd;'>💡 Conseils personnalisés</h3><ul>");
        data.get("conseils").forEach(
                p -> html.append("<li style='color:#cbd5e1;margin:4px 0;'>").append(p.asText()).append("</li>"));
        html.append("</ul>")
                .append("</div>")
                .append("<div style='background:#161b2e;padding:16px;text-align:center;color:#64748b;font-size:12px;'>")
                .append("AIVA Nutrition — Rapport généré automatiquement")
                .append("</div></div>");

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject("[AIVA] Votre rapport nutrition — Score " + score + "/100 — " + today);
        msg.setContent(html.toString(), "text/html; charset=utf-8");

        Transport.send(msg);
    }

    private String statBox(String val, String label) {
        return "<div style='flex:1;background:#161b2e;padding:12px;border-radius:8px;text-align:center;'>" +
                "<div style='font-size:22px;font-weight:bold;color:#fff;'>" + val + "</div>" +
                "<div style='color:#64748b;font-size:11px;'>" + label + "</div></div>";
    }
}