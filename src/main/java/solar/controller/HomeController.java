package solar.controller;

import com.fazecast.jSerialComm.SerialPort;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.ScatterChart;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import solar.data.PreferencesManager;
import solar.data.SerialManager;
import solar.data.model.DeviceState;
import solar.gui.WindowManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.*;

@Singleton
public class HomeController {

    private ObservableList<Double> targets = FXCollections.observableArrayList();

    @FXML
    public Text estimatedTemperatureText;

    @FXML
    public Text thermocoupleTemperatureText;

    @FXML
    public Text rtd2TemperatureText;

    @FXML
    public Text rtd1TemperatureText;

    @FXML
    public ScatterChart ivChart;

    @FXML
    public TextArea consoleTextArea;

    @FXML
    private JFXButton runButton;

    @FXML
    private JFXButton addButton;

    @FXML
    private JFXTextField targetInputLayout;

    @FXML
    private JFXListView<Double> targetsListView;

    @Inject
    WindowManager windowManager;

    @Inject
    private PreferencesManager preferencesManager;

    @Inject
    private SerialManager serialManager;

    @FXML
    private void initialize() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                appendText(String.valueOf((char) b));
            }
        };
        System.setOut(new PrintStream(out, true));
        setupBindings();
        setupListeners();
        consoleTextArea.setEditable(false);
    }

    private void appendText(String str) {
        Platform.runLater(() -> consoleTextArea.appendText(str));
    }

    private void setupBindings() {
        updateTargetListView(preferencesManager.getTemperatureTargets());

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
        System.out.println("Discovering device");

        serialManager.getStateRx()
                .subscribeOn(Schedulers.io())
                .subscribe(this::onNewState, Throwable::printStackTrace);
    }

    private void onNewState(DeviceState deviceState) {
        thermocoupleTemperatureText.setText(String.format("%4.2f °C", deviceState.getThermTemp()));
        rtd1TemperatureText.setText(String.format("%4.2f °C", deviceState.getRtd1Temp()));
        rtd2TemperatureText.setText(String.format("%4.2f °C", deviceState.getRtd2Temp()));
    }

    private void updateTargetListView(ArrayList<Double> items) {
        targets.addAll(items);
        targets.sort((o1, o2) -> {
            Double result = o1-o2;

            if (result < 0) return -1;
            if (result == 0) return 0;
            if (result > 0) return 1;

            return 0;
        });
    }

    @SuppressWarnings("unchecked")
    private void setupListeners() {
        runButton.setOnAction(this::onRunClicked);
        addButton.setOnAction(this::onAddTargetClicked);
        targetsListView.setOnMouseClicked(this::onListViewItemClick);
    }

    private void onAddTargetClicked(ActionEvent event) {
        String text = targetInputLayout.getText();
        if (text.length() > 0) {
            double target = Double.parseDouble(targetInputLayout.getText());

            targets.add(target);
            targets.sort((o1, o2) -> {
                Double result = o1 - o2;

                if (result < 0) return -1;
                if (result == 0) return 0;
                if (result > 0) return 1;

                return 0;
            });

            preferencesManager.appendTemperatureTarget(target);
            targetInputLayout.clear();
        }
    }

    private void onRunClicked(ActionEvent event) {
        // TODO: Wait for temperature targets, then send Serial to runSweep
    }

    private void onListViewItemClick(MouseEvent event) {
        Double selected = targetsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            windowManager.showDialog(
                    "Confirm Delete",
                    "Remove Temperature Target",
                    "Are you sure you want to delete " + selected + "°C?",
                    new WindowManager.ConfirmationCallback() {
                        @Override
                        public void onOk() {
                            targets.remove(selected);
                            preferencesManager.removeTemperatureTarget(selected);
                        }

                        @Override
                        public void onCancel() {
                            // Do nothing
                        }
                    });
        }
    }

}
