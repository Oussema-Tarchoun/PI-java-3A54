module pi_java_aiva {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;
    requires itextpdf;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.naming;

    // Open packages for reflection
    opens Controllers to javafx.fxml;
    opens utils to javafx.fxml;
    opens Test to javafx.fxml;
    opens Models to org.hibernate.orm.core, javafx.fxml;

    // Export packages for access
    exports utils;
    exports Controllers;
    exports Models;
    exports Services;
}
