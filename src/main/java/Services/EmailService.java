package Services;

import Models.Depense;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST     = "smtp.gmail.com";
    private static final int    SMTP_PORT     = 587;
    private static final String FROM_EMAIL    = "houssemlemjid02@gmail.com";
    private static final String FROM_PASSWORD = "zgis ymnb ntmd pajp";

    public void envoyerRappelDepensesImpayees(
            String destinataire,
            List<Depense> depensesImpayees)
            throws MessagingException, UnsupportedEncodingException {  // ← déclarer les deux exceptions

        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);

        // ── Correction ligne 38 : ajouter le charset UTF-8 ──
        message.setFrom(new InternetAddress(FROM_EMAIL, "AIVA - Gestion Financiere", "UTF-8"));

        message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(destinataire));

        message.setSubject("=?UTF-8?B?" +
                java.util.Base64.getEncoder().encodeToString(
                        ("⚠️ Rappel : " + depensesImpayees.size()
                                + " depense(s) non reglee(s) - AIVA")
                                .getBytes("UTF-8")) + "?=");

        message.setContent(
                construireCorpsHTML(depensesImpayees),
                "text/html; charset=UTF-8");

        Transport.send(message);
    }

    private String construireCorpsHTML(List<Depense> depenses) {
        double totalImpaye = depenses.stream()
                .mapToDouble(Depense::getMontant).sum();

        StringBuilder lignes = new StringBuilder();
        for (Depense d : depenses) {
            String couleurStatut = normaliserStatut(d.getStatut()).equals("En attente")
                    ? "#f59e0b" : "#ef4444";
            String bgStatut = normaliserStatut(d.getStatut()).equals("En attente")
                    ? "rgba(245,158,11,0.15)" : "rgba(239,68,68,0.15)";

            lignes.append("<tr>")
                    .append("<td style='padding:12px 16px; color:#ffffff; border-bottom:1px solid #2a3142;'>")
                    .append(d.getDescription()).append("</td>")
                    .append("<td style='padding:12px 16px; color:#00d4ff; font-weight:bold;"
                            + " border-bottom:1px solid #2a3142; text-align:right;'>")
                    .append(String.format("%.2f TND", d.getMontant())).append("</td>")
                    .append("<td style='padding:12px 16px; color:#94a3b8;"
                            + " border-bottom:1px solid #2a3142; text-align:center;'>")
                    .append(d.getDateDepense() != null
                            ? d.getDateDepense().toString() : "-").append("</td>")
                    .append("<td style='padding:12px 16px; border-bottom:1px solid #2a3142;"
                            + " text-align:center;'>")
                    .append("<span style='background:").append(bgStatut)
                    .append("; color:").append(couleurStatut)
                    .append("; padding:4px 12px; border-radius:20px;"
                            + " font-size:12px; font-weight:bold;'>")
                    .append(normaliserStatut(d.getStatut()))
                    .append("</span></td>")
                    .append("</tr>");
        }

        return "<!DOCTYPE html><html><body style='margin:0; padding:0;"
                + " background:#0a0e1a; font-family:Arial,sans-serif;'>"
                + "<div style='max-width:680px; margin:40px auto; background:#0a0e1a;'>"

                // ── Header ──
                + "<div style='background:#161b2e; border-radius:16px 16px 0 0; padding:32px;"
                + " border:1px solid #2a3142; border-bottom:none;'>"
                + "<div style='width:5px; height:100%; background:#00d4ff;'></div>"
                + "<h1 style='color:#ffffff; font-size:26px; margin:0 0 8px 0;'>"
                + "Rappel de paiement</h1>"
                + "<p style='color:#64748b; margin:0; font-size:14px;'>Genere le "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " - AIVA Gestion Financiere</p>"
                + "</div>"

                // ── Alerte ──
                + "<div style='background:rgba(239,68,68,0.1);"
                + " border:1px solid rgba(239,68,68,0.3);"
                + " border-top:none; border-bottom:none; padding:20px 32px;'>"
                + "<p style='color:#ef4444; font-size:15px; margin:0; font-weight:bold;'>"
                + "Vous avez <strong>" + depenses.size() + " depense(s)</strong>"
                + " non reglee(s) pour un montant total de "
                + "<strong style='color:#ef4444;'>"
                + String.format("%.2f TND", totalImpaye) + "</strong></p>"
                + "</div>"

                // ── Tableau ──
                + "<div style='background:#161b2e; border:1px solid #2a3142;"
                + " border-top:none; border-bottom:none; padding:24px;'>"
                + "<table style='width:100%; border-collapse:collapse;'>"
                + "<thead><tr style='background:#0a0e1a;'>"
                + "<th style='padding:12px 16px; color:#00d4ff; font-size:12px;"
                + " text-align:left; border-bottom:2px solid #00d4ff;"
                + " text-transform:uppercase; letter-spacing:1px;'>Description</th>"
                + "<th style='padding:12px 16px; color:#00d4ff; font-size:12px;"
                + " text-align:right; border-bottom:2px solid #00d4ff;"
                + " text-transform:uppercase; letter-spacing:1px;'>Montant</th>"
                + "<th style='padding:12px 16px; color:#00d4ff; font-size:12px;"
                + " text-align:center; border-bottom:2px solid #00d4ff;"
                + " text-transform:uppercase; letter-spacing:1px;'>Date</th>"
                + "<th style='padding:12px 16px; color:#00d4ff; font-size:12px;"
                + " text-align:center; border-bottom:2px solid #00d4ff;"
                + " text-transform:uppercase; letter-spacing:1px;'>Statut</th>"
                + "</tr></thead>"
                + "<tbody>" + lignes + "</tbody>"
                + "</table>"
                + "</div>"

                // ── Total ──
                + "<div style='background:#161b2e; border:1px solid #2a3142;"
                + " border-top:none; padding:20px 32px;'>"
                + "<table style='width:100%;'><tr>"
                + "<td style='color:#94a3b8; font-size:14px;'>Total a regler</td>"
                + "<td style='color:#ef4444; font-size:22px; font-weight:bold;"
                + " text-align:right;'>"
                + String.format("%.2f TND", totalImpaye) + "</td>"
                + "</tr></table>"
                + "</div>"

                // ── Footer ──
                + "<div style='background:#0d1220; border-radius:0 0 16px 16px; padding:24px 32px;"
                + " border:1px solid #2a3142; border-top:none; text-align:center;'>"
                + "<p style='color:#4a5568; font-size:12px; margin:0;'>"
                + "Ce message a ete envoye automatiquement par AIVA - Gestion Financiere</p>"
                + "</div>"

                + "</div></body></html>";
    }

    private String normaliserStatut(String statut) {
        if (statut == null) return "Non paye";
        String s = statut.trim().toLowerCase()
                .replace("e\u0301", "e")
                .replace("\u00e9", "e")
                .replace("\u00e8", "e")
                .replace("\u00ea", "e");
        if (s.equals("paye") || s.equals("pay")) return "Paye";
        if (s.contains("attente") || s.contains("cours")) return "En attente";
        return "Non paye";
    }
}