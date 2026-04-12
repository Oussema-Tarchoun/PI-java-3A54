package utils;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the Login screen initially
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();

            // Create Scene with a fixed size and nice styling
            Scene scene = new Scene(root, 1080, 720);
            
            // Premium Window setup
            primaryStage.setTitle("AIVA — Système de Gestion Administrative");
            primaryStage.setScene(scene);
            
            // Minimum window size
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(620);
            
            primaryStage.show();
            
        } catch (Exception e) {
            System.err.println("Fatal Error: Could not load the interface.");
            e.printStackTrace();
            // Show a simple alert if it fails
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erreur de lancement");
            alert.setHeaderText("Fichier FXML introuvable");
            alert.setContentText("L'interface 'Dashboard.fxml' n'a pas pu être chargée. Vérifiez l'emplacement du fichier.");
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
