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
            System.out.println("Loading FXML...");

            URL fxml = getClass().getResource("/view/MainLayout.fxml");
            System.out.println("FXML path: " + fxml);

            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();

            Scene scene = new Scene(root, 1280, 800);
            URL css = getClass().getResource("/style.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());

            primaryStage.setTitle("AIVA");
            primaryStage.setScene(scene);
            primaryStage.setX(100);
            primaryStage.setY(50);
            primaryStage.setWidth(1280);
            primaryStage.setHeight(800);
            primaryStage.show();
            primaryStage.toFront();
            primaryStage.requestFocus();

        } catch (Exception e) {
            System.err.println("=== ERROR ===");
            e.printStackTrace();

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Loading Application");
            alert.setHeaderText(e.getMessage());

            TextArea textArea = new TextArea(sw.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            alert.getDialogPane().setContent(textArea);

            alert.showAndWait();
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}