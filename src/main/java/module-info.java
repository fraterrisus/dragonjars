module com.hitchhikerprod.dragonjars {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    exports com.hitchhikerprod.dragonjars;
    opens com.hitchhikerprod.dragonjars to javafx.fxml;
    exports com.hitchhikerprod.dragonjars.ui;
    opens com.hitchhikerprod.dragonjars.ui to javafx.fxml;
}