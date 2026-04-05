package com.caresync;

import com.caresync.services.ReminderService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        stage.setTitle("CareSync Clinic Management");
        URL logo = Main.class.getResource("/images/caresync-logo-mark.png");
        if (logo != null) {
            stage.getIcons().add(new Image(logo.toExternalForm()));
        }
        setRoot("startup_splash", 1080, 720);
        stage.show();
        new ReminderService().startBackgroundDispatcher();
    }

    public static void setRoot(String fxml, double width, double height) throws IOException {
        Scene scene = new Scene(loadFXML(fxml), width, height);
        scene.getStylesheets().add(Main.class.getResource("/css/app.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static Parent loadFXML(String fxml) throws IOException {
        URL resource = Main.class.getResource("/fxml/" + fxml + ".fxml");
        if (resource == null) {
            throw new IOException("Missing FXML: " + fxml);
        }
        return FXMLLoader.load(resource);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
