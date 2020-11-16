package de.gsi.chart.axes.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.XYChart;

@ExtendWith(ApplicationExtension.class)
class TimeAxisTests {
    private DefaultNumericAxis xAxis;
    private DefaultNumericAxis yAxis;
    private XYChart chart;

    @Start
    public void start(Stage stage) {
        xAxis = new DefaultNumericAxis("time");
        yAxis = new DefaultNumericAxis("y");
        chart = new XYChart(xAxis, yAxis);

        Scene scene = new Scene(new Pane(), 100, 100);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void test() {
        assertDoesNotThrow(() -> xAxis.setTimeAxis(true));
        xAxis.setAutoRangePadding(0.1);
        xAxis.setAutoRangeRounding(false);
        assertDoesNotThrow(() -> xAxis.recomputeTickMarks());
    }
}
