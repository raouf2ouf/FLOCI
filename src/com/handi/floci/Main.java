package com.handi.floci;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class Main extends Application {
	
	private Stage m_primaryStage;
	private BorderPane m_rootLayout;
	
	@Override
	public void start(Stage primaryStage) {
		m_primaryStage = primaryStage;
		m_primaryStage.setTitle("FLOCI: Visualisation et Classification des individus dans une Ontologie floue");
		
		initRootLayout();
	}
	
	
	public void initRootLayout() {
        try {
            // Load root layout.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Main.class.getResource("view/main_layout.fxml"));
            m_rootLayout = (BorderPane) loader.load();

            // Show the scene containing the root layout.
            Scene scene = new Scene(m_rootLayout);
            m_primaryStage.setScene(scene);
            m_primaryStage.setMaximized(true);
            //m_primaryStage.initStyle(StageStyle.UNDECORATED);
            m_primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
