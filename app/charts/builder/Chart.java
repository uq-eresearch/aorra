package charts.builder;

import charts.Drawable;

public class Chart {

    private ChartDescription description;

    private Drawable chart;

    public Chart(ChartDescription description, Drawable chart) {
        super();
        this.description = description;
        this.chart = chart;
    }

    public ChartDescription getDescription() {
        return description;
    }

    public Drawable getChart() {
        return chart;
    }

}
