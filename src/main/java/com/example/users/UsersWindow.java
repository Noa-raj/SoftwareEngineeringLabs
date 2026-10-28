package com.example.users;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class UsersWindow extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        URL layout = Objects.requireNonNull(
                UsersWindow.class.getResource("/com/example/users/users-view.fxml"),
                "Missing users-view.fxml on the classpath");
        FXMLLoader loader = new FXMLLoader(layout);
        stage.setScene(new Scene(loader.load()));
        stage.setTitle("Software Engineering Labs");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
