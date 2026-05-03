package utils;

import javafx.application.Application;

/**
 * Main wrapper class to bypass JavaFX module system issues 
 * when running from certain IDEs or command line.
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(MainFX.class, args);
    }
}
