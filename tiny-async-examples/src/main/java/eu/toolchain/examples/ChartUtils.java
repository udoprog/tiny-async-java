package eu.toolchain.examples;

import com.google.common.collect.ImmutableList;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.RefineryUtilities;

public class ChartUtils {
  private static final Stroke LINE = new BasicStroke(2.0f);

  private static final Stroke DASHED =
      new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
          new float[]{6.0f, 6.0f}, 0.0f);

  private static final List<Color> COLORS =
      ImmutableList.of(Color.RED, Color.PINK, Color.ORANGE, Color.YELLOW, Color.GREEN,
          Color.MAGENTA, Color.CYAN, Color.BLUE, Color.WHITE, Color.BLACK);

  /**
   * Creates a chart.
   *
   * @return A chart.
   */
  private static JFreeChart createChart(
      String title, String xAxis, String yAxis1, XYDataset dataset1, String yAxis2,
      XYDataset dataset2
  ) {
    JFreeChart chart =
        ChartFactory.createXYLineChart(title, xAxis, yAxis1, dataset1, PlotOrientation.VERTICAL,
            true, true, false);

    final XYPlot plot = (XYPlot) chart.getPlot();

    plot.setBackgroundPaint(Color.lightGray);
    plot.setDomainGridlinePaint(Color.white);
    plot.setRangeGridlinePaint(Color.white);
    plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);

    final XYItemRenderer r = plot.getRenderer();

    int count = Math.min(dataset1.getSeriesCount(), dataset2.getSeriesCount());

    if (r instanceof XYLineAndShapeRenderer) {
      XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;

      renderer.setBaseStroke(LINE);
      renderer.setAutoPopulateSeriesPaint(false);
      renderer.setBaseShapesVisible(false);
      renderer.setBaseShapesFilled(false);
      renderer.setDrawSeriesLineAsPath(true);

      for (int i = 0; i < count; i++) {
        renderer.setSeriesPaint(i, COLORS.get(i % COLORS.size()));
      }
    }

    chart.setBackgroundPaint(Color.white);

    // chart two
    {
      final NumberAxis axis2 = new NumberAxis(yAxis2);
      axis2.setAutoRangeIncludesZero(false);
      plot.setRangeAxis(1, axis2);
      plot.setDataset(1, dataset2);
      plot.mapDatasetToRangeAxis(1, 1);

      final StandardXYItemRenderer renderer = new StandardXYItemRenderer();
      renderer.setAutoPopulateSeriesPaint(false);
      renderer.setAutoPopulateSeriesStroke(false);

      renderer.setBaseShapesVisible(false);
      renderer.setBaseShapesFilled(false);
      renderer.setDrawSeriesLineAsPath(true);
      renderer.setBaseStroke(DASHED);

      for (int i = 0; i < count; i++) {
        renderer.setSeriesPaint(i, COLORS.get(i % COLORS.size()));
      }

      plot.setRenderer(1, renderer);
    }

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
  public static void showChart(
      String name, String xAxis, String yAxis1, XYDataset dataset1, String yAxis2,
      XYDataset dataset2
  ) {
    final JFreeChart chart = createChart(name, xAxis, yAxis1, dataset1, yAxis2, dataset2);
    final ChartPanel chartPanel = createPanel(chart);

    final ApplicationFrame app = new ApplicationFrame(name);

    chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
    app.setContentPane(chartPanel);
    app.pack();

    RefineryUtilities.centerFrameOnScreen(app);

    app.setVisible(true);
  }
}
