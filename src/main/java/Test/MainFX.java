package Test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("views/ActiviteFront.fxml"));

        Scene scene = new Scene(root);

        // Optionnel : CSS
        try {
            scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS non trouvé, on continue sans");
        }

        primaryStage.setTitle("AIVA - Gestion des Activités");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}