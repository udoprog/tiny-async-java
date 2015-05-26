package eu.toolchain.perftests;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import com.google.common.collect.ImmutableList;

public class Charts {
    private static final Stroke LINE = new BasicStroke(2.0f);

    private static final Stroke DASHED = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
            new float[] { 6.0f, 6.0f }, 0.0f);

    private static final List<Color> COLORS = ImmutableList.of(Color.RED, Color.PINK, Color.ORANGE, Color.YELLOW,
            Color.GREEN, Color.MAGENTA, Color.CYAN, Color.BLUE, Color.WHITE, Color.BLACK);

    private static ChartTheme currentTheme = new StandardChartTheme("JFree");

    @RequiredArgsConstructor
    public static final class Dataset {
        private final String yAxis;
        private final String xAxis;
        private final XYDataset dataset;
    }

    /**
     * Creates a chart.
     *
     * @param dataset a dataset.
     *
     * @return A chart.
     */
    public static JFreeChart createChart(String title, Collection<Dataset> datasets) {
        final XYPlot plot = new XYPlot();

        plot.setOrientation(PlotOrientation.VERTICAL);

        int index = 0;

        for (final Dataset dataset : datasets) {
            final NumberAxis xAxis = new NumberAxis(dataset.xAxis);
            xAxis.setAutoRangeIncludesZero(false);

            final NumberAxis yAxis = new NumberAxis(dataset.yAxis);

            final StandardXYItemRenderer renderer = new StandardXYItemRenderer();
            renderer.setAutoPopulateSeriesPaint(false);
            renderer.setAutoPopulateSeriesStroke(false);

            renderer.setBaseShapesVisible(false);
            renderer.setBaseShapesFilled(false);
            renderer.setDrawSeriesLineAsPath(true);
            renderer.setBaseStroke(LINE);

            for (int i = 0; i < dataset.dataset.getSeriesCount(); i++)
                renderer.setSeriesPaint(i, COLORS.get(i % COLORS.size()));

            plot.setRangeAxis(index, xAxis);
            plot.setDomainAxis(index, yAxis);
            plot.setDataset(index, dataset.dataset);
            plot.setRenderer(index, renderer);

            ++index;
        }

        final JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);

        currentTheme.apply(chart);

        return chart;

    }

    /**
     * Creates a panel for the demo (used by SuperDemo.java).
     *
     * @return A panel.
     */
    public static ChartPanel createPanel(JFreeChart chart) {
        ChartPanel panel = new ChartPanel(chart);
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }

    /**
     * Starting point for the demonstration application.
     */
    public static void showChart(final String name, final JFreeChart chart) {
        final ChartPanel chartPanel = createPanel(chart);

        final ApplicationFrame app = new ApplicationFrame(name);

        chartPanel.setPreferredSize(new java.awt.Dimension(1000, 600));
        app.setContentPane(chartPanel);
        app.pack();

        RefineryUtilities.centerFrameOnScreen(app);

        app.setVisible(true);
    }
}