module com.hitchhikerprod.dragonjars {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.graphics;
    requires javafx.base;

    exports com.hitchhikerprod.dragonjars;
    opens com.hitchhikerprod.dragonjars to javafx.fxml;
    exports com.hitchhikerprod.dragonjars.data;
    exports com.hitchhikerprod.dragonjars.ui;
    opens com.hitchhikerprod.dragonjars.ui to javafx.fxml;
}