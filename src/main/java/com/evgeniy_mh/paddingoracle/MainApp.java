package com.evgeniy_mh.paddingoracle;

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/fxml/mainOverview.fxml"));
        AnchorPane rootOverview = (AnchorPane) loader.load();

        Scene scene = new Scene(rootOverview);
        stage.setTitle("Padding Oracle");
        stage.setScene(scene);
        stage.show();

        FXMLController mc = loader.getController();
        mc.setMainApp(this);  
    }

    public static void main(String[] args) {
        launch(args);
    }
}
