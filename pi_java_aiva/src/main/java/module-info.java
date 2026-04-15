module pi_java_aiva {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;
    requires itextpdf;

    requires java.naming;
    requires jakarta.mail;
    requires org.eclipse.angus.mail;

    // Open packages for reflection
    opens Controllers to javafx.fxml;
    opens utils to javafx.fxml;
    opens Test to javafx.fxml;
    opens Models to javafx.fxml;

    // Export packages for access
    exports utils;
    exports Controllers;
    exports Models;
    exports Services;
}
