package Test;

import Models.Objectif;
import Models.Activite;
import Services.ServiceObjectif;
import Services.ServiceActivite;

import java.sql.SQLDataException;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) throws SQLDataException {

        ServiceObjectif serviceObjectif = new ServiceObjectif();
        ServiceActivite serviceActivite = new ServiceActivite();

        // ── Test Objectif ──────────────────────────────
        Objectif obj = new Objectif(
                "Perdre du poids",
                "Perte de poids",
                10,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 6, 1),
                "En cours",
                1
        );

        serviceObjectif.ajouter(obj);                                    // ← was addObjectif()
        System.out.println("=== Liste des Objectifs ===");
        serviceObjectif.recuperer().forEach(System.out::println);        // ← was displayAll()

        // ── Test Activite ──────────────────────────────
        Activite act = new Activite(
                "Course",
                30,
                250.0,
                LocalDate.now(),
                "Modérée",
                1
        );

        serviceActivite.ajouter(act);                                    // ← was addActivite()
        System.out.println("=== Liste des Activités ===");
        serviceActivite.recuperer().forEach(System.out::println);        // ← was displayAll()
    }
}