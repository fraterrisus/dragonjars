module com.hitchhikerprod.dragonwars.dragonjars {
    requires javafx.controls;
    requires javafx.fxml;

    exports com.hitchhikerprod.dragonjars;
    opens com.hitchhikerprod.dragonjars to javafx.fxml;
}