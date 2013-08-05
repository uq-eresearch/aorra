package charts.builder;

import charts.Dimensions;

public class Chart {

    private ChartDescription description;

    private Dimensions chart;

    public Chart(ChartDescription description, Dimensions chart) {
        super();
        this.description = description;
        this.chart = chart;
    }

    public ChartDescription getDescription() {
        return description;
    }

    public Dimensions getChart() {
        return chart;
    }

}
