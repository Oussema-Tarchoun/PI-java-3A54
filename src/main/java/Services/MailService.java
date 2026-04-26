package Services;

import Models.Energie;
import Models.Recommandation;
import Models.User;
import io.mailtrap.client.MailtrapClient;
import io.mailtrap.config.MailtrapConfig;
import io.mailtrap.factory.MailtrapClientFactory;
import io.mailtrap.model.request.emails.Address;
import io.mailtrap.model.request.emails.MailtrapMail;

import java.util.Collections;

public class MailService {

    // --- CONFIGURATION MAILTRAP SDK ---
    private final String TOKEN = "1316fd3e9d8a2468d642ac913aceef5b"; // Correct API Token
    private final long INBOX_ID = 3648673L;

    private final MailtrapClient client;

    public MailService() {
        MailtrapConfig config = new MailtrapConfig.Builder()
            .sandbox(true)
            .inboxId(INBOX_ID)
            .token(TOKEN)
            .build();
        this.client = MailtrapClientFactory.createMailtrapClient(config);
    }

    public void sendEmail(String to, String subject, String htmlContent) {
        if (to == null || to.isEmpty()) {
            System.err.println("Destinataire manquant, email non envoyé.");
            return;
        }

        final MailtrapMail mail = MailtrapMail.builder()
            .from(new Address("hello@aiva.energy", "AIVA Energy Advisor"))
            .to(Collections.singletonList(new Address(to)))
            .subject(subject)
            .html(htmlContent)
            .build();

        try {
            client.send(mail);
            System.out.println("Email envoyé avec succès via Mailtrap SDK à : " + to);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email (Mailtrap SDK) : " + e.getMessage());
        }
    }

    public void sendEnergyAdded(User user, Energie energie, Recommandation reco) {
        String subject = "➕ Nouvelle donnée énergétique ajoutée - AIVA";
        String content = "<h1>Bonjour " + user.getNom() + "</h1>" +
                "<p>Une nouvelle donnée de consommation a été enregistrée :</p>" +
                "<ul>" +
                "<li><b>Type :</b> " + energie.getType_energie() + "</li>" +
                "<li><b>Valeur :</b> " + energie.getValeur() + " unités</li>" +
                "<li><b>Période :</b> " + energie.getPeriode() + " jours</li>" +
                "</ul>" +
                "<hr>" +
                "<h3>🤖 Recommandation IA :</h3>" +
                "<p><i>" + (reco != null ? reco.getDescription() : "Aucune recommandation pour le moment.") + "</i></p>" +
                "<br>" +
                "<p>Merci d'utiliser AIVA pour votre gestion d'énergie !</p>";

        sendEmail(user.getEmail(), subject, content);
    }

    public void sendEnergyModified(User user, Energie oldE, Energie newE, Recommandation reco) {
        String subject = "✏️ Modification de votre donnée énergétique - AIVA";
        String content = "<h1>Bonjour " + user.getNom() + "</h1>" +
                "<p>Votre donnée énergétique (ID: " + newE.getId() + ") a été mise à jour.</p>" +
                "<table border='1' cellpadding='10' style='border-collapse: collapse;'>" +
                "<tr><th>Champ</th><th>Ancienne Valeur</th><th>Nouvelle Valeur</th></tr>" +
                "<tr><td>Type</td><td>" + oldE.getType_energie() + "</td><td>" + newE.getType_energie() + "</td></tr>" +
                "<tr><td>Valeur</td><td>" + oldE.getValeur() + "</td><td>" + newE.getValeur() + "</td></tr>" +
                "<tr><td>Période</td><td>" + oldE.getPeriode() + "</td><td>" + newE.getPeriode() + "</td></tr>" +
                "</table>" +
                "<hr>" +
                "<h3>🤖 Nouvelle Recommandation IA :</h3>" +
                "<p><i>" + (reco != null ? reco.getDescription() : "Mise à jour en cours...") + "</i></p>" +
                "<br>" +
                "<p>AIVA - Votre assistant intelligent.</p>";

        sendEmail(user.getEmail(), subject, content);
    }

    public void sendAlert(User user, String type, String message) {
        String subject = "⚠️ ALERTE ÉNERGÉTIQUE - " + type;
        String content = "<h1>Attention " + user.getNom() + "</h1>" +
                "<p style='color: red; font-weight: bold;'>" + message + "</p>" +
                "<p>Nous vous conseillons de vérifier vos équipements ou de suivre les recommandations de l'IA pour optimiser votre consommation.</p>";

        sendEmail(user.getEmail(), subject, content);
    }
}
