package app;

import Models.Energie;
import Models.Recommandation;
import Models.User;
import Services.MailService;
import java.util.Date;

public class TestMailing {
    public static void main(String[] args) {
        System.out.println("--- Démarrage du test de mailing AIVA ---");
        
        MailService mailService = new MailService();
        
        // 1. Création d'un utilisateur fictif
        User testUser = new User("1", "Hamza Test", "kefihamza23.hamza@gmail.com");
        
        // 2. Création d'une donnée énergétique fictive
        Energie testEnergie = new Energie(1, "Électricité", 1250.5f, 30.0f, new Date(), "Compteur Intelligent", "1");
        
        // 3. Création d'une recommandation fictive
        Recommandation testReco = new Recommandation(1, "Optimisation Chauffage", 
            "Votre consommation électrique est élevée. Pensez à baisser le thermostat de 1°C pour économiser 7%.", 
            "Élevé", new Date(), 1);

        try {
            // Test 1: Ajout d'énergie
            System.out.println("Envoi de l'email 'Ajout d'énergie'...");
            mailService.sendEnergyAdded(testUser, testEnergie, testReco);
            
            // Test 2: Alerte consommation
            System.out.println("Envoi de l'alerte 'Forte Consommation'...");
            mailService.sendAlert(testUser, "Forte Consommation", "Attention, votre consommation dépasse 1000 unités !");
            
            System.out.println("--- Test terminé. Vérifiez votre boîte Mailtrap ! ---");
        } catch (Exception e) {
            System.err.println("Erreur durant le test : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
