module com.os.cpuscheduling.cpuscheduling {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.os.cpuscheduling.cpuscheduling to javafx.fxml;
    exports com.os.cpuscheduling.cpuscheduling;
    exports com.os.cpuscheduling.cpuscheduling.model;
    exports com.os.cpuscheduling.cpuscheduling.sim;
}