package app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Debug : lister ce qui est disponible
            URL test1 = getClass().getResource("/view/Main.fxml");
            URL test2 = getClass().getResource("/view/AlimentView.fxml");
            URL test3 = getClass().getResource("/view/aliments/ListAliment.fxml");
            System.out.println("Main.fxml        : " + test1);
            System.out.println("AlimentView.fxml : " + test2);
            System.out.println("ListAliment.fxml : " + test3);

            // ✅ Essayer dans l'ordre jusqu'à trouver un FXML valide
            URL fxml = test1 != null ? test1
                    : test2 != null ? test2
                    : test3;

            if (fxml == null) {
                throw new RuntimeException("Aucun FXML trouvé — vérifiez que resources est marqué comme Resources Root");
            }

            System.out.println("✅ Chargement : " + fxml);

            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();

            Scene scene = new Scene(root, 1280, 800);

            URL aivaTheme = getClass().getResource("/styles/aiva-theme.css");
            if (aivaTheme != null) {
                scene.getStylesheets().add(aivaTheme.toExternalForm());
                System.out.println("✅ CSS chargé : " + aivaTheme);
            } else {
                System.err.println("⚠️ aiva-theme.css introuvable");
            }

            primaryStage.setTitle("AIVA");
            primaryStage.setScene(scene);
            primaryStage.setWidth(1280);
            primaryStage.setHeight(800);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(e.getMessage());
            TextArea textArea = new TextArea(sw.toString());
            textArea.setEditable(false);
            alert.getDialogPane().setContent(textArea);
            alert.showAndWait();
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}