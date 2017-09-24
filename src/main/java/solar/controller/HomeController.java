package solar.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import io.reactivex.MaybeObserver;
import io.reactivex.disposables.Disposable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextFormatter;
import solar.database.DatabaseManager;
import solar.database.TargetService;
import solar.gui.WindowManager;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.ArrayList;

@Singleton
public class HomeController {

    private static final ObservableList<Double> targets = FXCollections.observableArrayList();

    @FXML
    private JFXButton runButton;

    @FXML
    private JFXButton addButton;

    @FXML
    private JFXTextField targetInputLayout;

    @FXML
    private JFXListView<Double> targetsListView;

    @Inject
    private TargetService targetService;

    @Inject
    WindowManager windowManager;

    @Inject
    DatabaseManager databaseManager;

    @FXML
    private void initialize() {
        setupBindings();
        setupListeners();
    }

    private void setupBindings() {
        System.out.println("setupBindings");
        databaseManager.getTargets()
                .subscribe(this::updateTargetListView, System.out::println);

        DecimalFormat format = new DecimalFormat( "#.0" );

        targetInputLayout.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().isEmpty()) {
                return change;
            }

            ParsePosition parsePosition = new ParsePosition(0);
            Object object = format.parse(change.getControlNewText(), parsePosition);

            if (object == null || parsePosition.getIndex() < change.getControlNewText().length()) {
                return null;
            } else {
                return change;
            }
        }));

        targetsListView.setItems(targets);
    }

    private void updateTargetListView(ArrayList<Double> items) {
        targets.addAll(items);
    }

    @SuppressWarnings("unchecked")
    private void setupListeners() {
        System.out.println("setupListeners");
        runButton.setOnAction(this::onRunClicked);
        addButton.setOnAction(this::onAddTargetClicked);
    }

    private void onAddTargetClicked(ActionEvent event) {
        double target = Double.parseDouble(targetInputLayout.getText());
        targets.add(target);
        targetInputLayout.clear();
    }

    private void onRunClicked(ActionEvent event) {

    }

}
