package com.qq452651705.GUI;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.util.Date;

public class RealTimeChart {
    private final Integer maximumItemCount=100;
    private TimeSeries timeSeries;
    private long value = 0;
    private String chartContent;
    private String title;
    private String yaxisName;

    private TimeSeriesCollection timeSeriesCollection;
    private ChartPanel chartPanel;
    private JFreeChart jfreechart;

    public RealTimeChart(String chartContent, String title, String yaxisName,TimeSeries ts) {
        this.chartContent=chartContent;
        this.title=title;
        this.yaxisName=yaxisName;
        //创建时序图对象
        timeSeries=(ts==null)?new TimeSeries(chartContent):ts;
        timeSeries.setMaximumItemCount(maximumItemCount);
        timeSeriesCollection = new TimeSeriesCollection(timeSeries);
        jfreechart = ChartFactory.createTimeSeriesChart(title, "time(s)", yaxisName, timeSeriesCollection, true, true, false);
        XYPlot xyplot = jfreechart.getXYPlot();
        //纵坐标设定
        ValueAxis valueaxis = xyplot.getDomainAxis();
        //自动设置数据轴数据范围
        valueaxis.setAutoRange(true);
        //数据轴固定数据范围 30s
        valueaxis.setFixedAutoRange(30000D);
        valueaxis = xyplot.getRangeAxis();
        chartPanel = new ChartPanel(jfreechart);
    }

    public void setXRange(double lower,double upper){
        XYPlot xyPlot=chartPanel.getChart().getXYPlot();
        ValueAxis valueAxis=xyPlot.getDomainAxis();
        valueAxis.setAutoRange(false);
        valueAxis.setRange(new Range(lower,upper));
        SwingUtilities.invokeLater(()->chartPanel.updateUI());
    }

    public void resetXRange(){
        XYPlot xyPlot=chartPanel.getChart().getXYPlot();
        ValueAxis valueAxis=xyPlot.getDomainAxis();
        valueAxis.setAutoRange(true);
        valueAxis.setFixedAutoRange(30000D);
        SwingUtilities.invokeLater(()->chartPanel.updateUI());
    }

    public void addNewData(Date date, Float value){
        timeSeries.add(new Millisecond(date),value);
    }

    public TimeSeries getTimeSeries() {
        return timeSeries;
    }

    public ChartPanel getChartPanel() {
        return chartPanel;
    }

    public JFreeChart getJfreechart() {
        return jfreechart;
    }
}