package iut.sae.controleur;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class TabGanttProcesseursControleur implements Initializable{
    private VBox BoiteConteneurGantt;

    private List<String> listeProcesseurs;
    private int mkspan;
    private GridPane diagrammeG;

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'initialize'");
    }

    public void setMakespan(int mkspan){
        this.mkspan = mkspan;
    }

    public void setProcesseurs(List<String> listeProcesseurs){
        this.listeProcesseurs = listeProcesseurs;
    }

    public TabGanttProcesseursControleur(){
        this.diagrammeG = new GridPane(30, 30);
    }

    private void setGantt(){
        for(int j = 0; j < this.listeProcesseurs.size(); j++){
            this.diagrammeG.addRow(j, new Label(listeProcesseurs.get(j)));
        }

        for(int i = 1; i < this.mkspan; i++){
            this.diagrammeG.addColumn(i, null);
        }
    }
}
