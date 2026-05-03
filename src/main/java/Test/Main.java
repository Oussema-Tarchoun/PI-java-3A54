package Test;

import java.sql.Date;
import java.sql.SQLDataException;
import java.sql.Time;

import Models.ActivitePhysique;
import Services.ServiceActivitePhysique;

import Models.Aliment;
import Services.ServiceAliment;

import Models.Badge;
import Services.ServiceBadge;

import Models.Categorie;
import Services.ServiceCategorie;

import Models.Chapitre;
import Services.ServiceChapitre;

import Models.Cours;
import Services.ServiceCours;

import Models.Depense;
import Services.ServiceDepense;

import Models.Energie;
import Services.ServiceEnergie;

import Models.Objectif;
import Services.ServiceObjectif;

import Models.Recommandation;
import Services.ServiceRecommandation;

import Models.Repas;
import Services.ServiceRepas;

import Models.ResetPasswordRequest;
import Services.ServiceResetPasswordRequest;

import Models.User;
import Services.ServiceUser;
import utils.MyDatabase;

public class Main {
    public static void main(String[] args) {
        ServiceRepas serviceRepas = new ServiceRepas();

        try {
            // Adding a new meal (user_id, nom, heure, calories, description, type, date)
            // Note: Time.valueOf("HH:mm:ss") and Date.valueOf("YYYY-MM-DD") are very handy
            serviceRepas.ajouter(new Repas(
                    1,
                    "Petit Déjeuner",
                    Time.valueOf("08:00:00"),
                    350,
                    "Omelette et café",
                    "dejeuner",
                    Date.valueOf("2024-05-20")
            ));

            serviceRepas.ajouter(new Repas(
                    1,
                    "Déjeuner",
                    Time.valueOf("13:30:00"),
                    700,
                    "Couscous Tunisien",
                    "dejeuner",
                    Date.valueOf("2024-05-20")
            ));

            // Modifying a meal (id is passed as the last parameter in this example)
            // Assuming ID 1 exists in your database:
            serviceRepas.modifier(new Repas(
                    1,
                    1,
                    "Dîner Léger",
                    Time.valueOf("20:00:00"),
                    200,
                    "Salade verte",
                    "diner",
                    Date.valueOf("2024-05-20")
            ));

            // Displaying all meals
            System.out.println("--- Liste des Repas ---");
            System.out.println(serviceRepas.recuperer());

        } catch (SQLDataException e) {
            throw new RuntimeException(e);
        }
    }
}

