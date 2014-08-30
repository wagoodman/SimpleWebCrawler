import java.awt.Color;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class DynamicDataChart extends JFreeChart {

   final static NumberAxis domainAxis = new NumberAxis("DEFAULT");
   final static CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domainAxis);
   private XYSeriesCollection datasets;

   public DynamicDataChart(String title, String xAxisTitle, String yAxisTitle) {
      super(plot);

      plot.getDomainAxis().setLabel(xAxisTitle);
      plot.getDomainAxis().setStandardTickUnits(
            NumberAxis.createIntegerTickUnits());
      // plot.getDomainAxis().setRange(minX, maxX);
      this.datasets = new XYSeriesCollection();

      final XYSeries series = new XYSeries("Random " + 0);
      this.datasets = new XYSeriesCollection(series);

      final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
      // renderer.setSeriesShapesVisible(0, false);
      // renderer.setSeriesLinesVisible(0, false);

      final NumberAxis rangeAxis = new NumberAxis(yAxisTitle);
      rangeAxis.setAutoRangeIncludesZero(false);
      rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

      final XYPlot subplot = new XYPlot(this.datasets, null, rangeAxis,
            renderer);
      subplot.setBackgroundPaint(Color.lightGray);
      subplot.setDomainGridlinePaint(Color.white);
      subplot.setRangeGridlinePaint(Color.white);
      plot.add(subplot);

   }

   public void addPoint(double x, double y) {
      this.datasets.getSeries(0).add(x, y);
   }

}