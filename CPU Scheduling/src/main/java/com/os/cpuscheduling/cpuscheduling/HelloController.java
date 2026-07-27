package com.os.cpuscheduling.cpuscheduling;

import com.os.cpuscheduling.cpuscheduling.model.Process;
import com.os.cpuscheduling.cpuscheduling.model.QueueConfig;
import com.os.cpuscheduling.cpuscheduling.sim.MultiLevelScheduler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Comparator;

public class HelloController {
    @FXML private TableView<QueueConfig> queuesTable;
    @FXML private TableView<Process> processTable;
    @FXML private Label timeLabel;
    @FXML private Label currentLabel;
    @FXML private Label utilLabel;
    @FXML private Label throughputLabel;
    @FXML private Label tatLabel;
    @FXML private Label waitLabel;
    @FXML private Label respLabel;
    @FXML private CheckBox preemptiveCheck;

    private final ObservableList<QueueConfig> queueConfigs = FXCollections.observableArrayList();
    private final ObservableList<Process> processes = FXCollections.observableArrayList();
    private MultiLevelScheduler scheduler;

    @FXML
    public void initialize() {
        queuesTable.setItems(queueConfigs);
        processTable.setItems(processes);
        resetScheduler();
    }

    private void resetScheduler() {
        scheduler = new MultiLevelScheduler(preemptiveCheck != null && preemptiveCheck.isSelected());
        // add queues sorted by priority level
        queueConfigs.stream()
                .sorted(Comparator.comparingInt(QueueConfig::getPriorityLevel))
                .forEach(scheduler::addQueue);
        processes.forEach(scheduler::addProcess);
        updateMetricsAndStatus();
    }

    private void updateMetricsAndStatus() {
        timeLabel.setText(String.valueOf(scheduler.getCurrentTime()));
        MultiLevelScheduler.Metrics m = scheduler.computeMetrics();
        utilLabel.setText(String.format("%.1f%%", m.cpuUtilizationPercent()));
        throughputLabel.setText(String.format("%.3f", m.throughputPerTimeUnit()));
        tatLabel.setText(String.format("%.2f", m.avgTurnaroundTime()));
        waitLabel.setText(String.format("%.2f", m.avgWaitingTime()));
        respLabel.setText(String.format("%.2f", m.avgResponseTime()));
    }

    @FXML
    protected void onAddQueue() {
        Dialog<QueueConfig> dialog = new Dialog<>();
        dialog.setTitle("Add Queue");
        DialogPane pane = dialog.getDialogPane();
        TextField name = new TextField("Q" + (queueConfigs.size() + 1));
        Spinner<Integer> level = new Spinner<>(0, 10, queueConfigs.size());
        ComboBox<QueueConfig.Algorithm> algo = new ComboBox<>();
        algo.getItems().setAll(QueueConfig.Algorithm.values());
        algo.getSelectionModel().select(QueueConfig.Algorithm.ROUND_ROBIN);
        Spinner<Integer> quantum = new Spinner<>(1, 100, 2);
        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(8);
        gp.addRow(0, new Label("Name"), name);
        gp.addRow(1, new Label("Priority Level"), level);
        gp.addRow(2, new Label("Algorithm"), algo);
        gp.addRow(3, new Label("Time Quantum"), quantum);
        pane.setContent(gp);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> bt == ButtonType.OK ?
                new QueueConfig(name.getText(), level.getValue(), algo.getValue(), quantum.getValue()) : null);
        dialog.showAndWait().ifPresent(q -> { queueConfigs.add(q); resetScheduler(); });
    }

    @FXML
    protected void onAddProcess() {
        Dialog<Process> dialog = new Dialog<>();
        dialog.setTitle("Add Process");
        DialogPane pane = dialog.getDialogPane();
        TextField pid = new TextField("P" + (processes.size() + 1));
        Spinner<Integer> arrival = new Spinner<>(0, 1000, 0);
        Spinner<Integer> burst = new Spinner<>(1, 1000, 5);
        Spinner<Integer> prio = new Spinner<>(0, Math.max(0, queueConfigs.size() - 1), 0);
        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(8);
        gp.addRow(0, new Label("PID"), pid);
        gp.addRow(1, new Label("Arrival"), arrival);
        gp.addRow(2, new Label("Burst"), burst);
        gp.addRow(3, new Label("Priority (queue index)"), prio);
        pane.setContent(gp);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> bt == ButtonType.OK ?
                new Process(pid.getText(), arrival.getValue(), burst.getValue(), prio.getValue()) : null);
        dialog.showAndWait().ifPresent(p -> { processes.add(p); resetScheduler(); });
    }

    @FXML
    protected void onStep() {
        if (scheduler.isAllCompleted()) {
            return;
        }
        if (!scheduler.hasReadyNow()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "No ready processes in any queue at T=" + scheduler.getCurrentTime(), ButtonType.OK);
            a.setHeaderText("All queues empty");
            a.setTitle("No Work");
            a.showAndWait();
            return;
        }
        MultiLevelScheduler.StepResult r = scheduler.step();
        currentLabel.setText(r.idle ? "Idle" : (r.process != null ? r.process.getId() + " (" + r.executedTime + ")" : "Idle"));
        updateMetricsAndStatus();
    }

    @FXML
    protected void onRunTen() {
        if (scheduler.isAllCompleted()) return;
        if (!scheduler.hasReadyNow()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "No ready processes in any queue at T=" + scheduler.getCurrentTime(), ButtonType.OK);
            a.setHeaderText("All queues empty");
            a.setTitle("No Work");
            a.showAndWait();
            return;
        }
        for (int i = 0; i < 10; i++) {
            if (scheduler.isAllCompleted()) break;
            scheduler.step();
        }
        currentLabel.setText("...");
        updateMetricsAndStatus();
    }

    @FXML
    protected void onReset() {
        resetScheduler();
        currentLabel.setText("Idle");
    }
}
