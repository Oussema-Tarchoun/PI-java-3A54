package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/ObjectifView.fxml"));
        stage.setTitle("AIVA - Objectifs");
        stage.setScene(new Scene(root, 1200, 700));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}