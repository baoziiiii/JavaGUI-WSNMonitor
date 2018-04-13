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

/**
 * The type Real time chart.  图表类
 */
public class RealTimeChart {

    private JFreeChart jfreechart;    //图表GUI组件
    private TimeSeriesCollection timeSeriesCollection; //数据集包装
    private TimeSeries timeSeries;    //数据集
    private ChartPanel chartPanel;    //图表显示容器

    private long value = 0;
    private String chartContent;
    private String title;
    private String yaxisName;


    private final Integer maximumItemCount=100; //数据集最大缓存数量,防止内存溢出



    /**
     * Instantiates a new Real time chart. 初始化图表
     *
     * @param chartContent the chart content  图例
     * @param title        the title          图表标题
     * @param yaxisName    the yaxis name     y轴名称
     * @param ts           the ts             数据集
     */
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

    /**
     * Set x range.  设定X轴显示范围
     *
     * @param lower the lower  下限
     * @param upper the upper  上限
     */
    public void setXRange(double lower,double upper){
        XYPlot xyPlot=chartPanel.getChart().getXYPlot();
        ValueAxis valueAxis=xyPlot.getDomainAxis();
        valueAxis.setAutoRange(false);
        valueAxis.setRange(new Range(lower,upper));
        SwingUtilities.invokeLater(()->chartPanel.updateUI());
    }

    /**
     * Reset x range.   重置X轴显示范围为30s
     */
    public void resetXRange(){
        XYPlot xyPlot=chartPanel.getChart().getXYPlot();
        ValueAxis valueAxis=xyPlot.getDomainAxis();
        valueAxis.setAutoRange(true);
        valueAxis.setFixedAutoRange(30000D);
        SwingUtilities.invokeLater(()->chartPanel.updateUI());
    }

    /**
     * Add new data.     向数据集中添加新数据
     *
     * @param date  the date   x轴时间
     * @param value the value  y轴值
     */
    public void addNewData(Date date, Float value){
        timeSeries.add(new Millisecond(date),value);
    }

    /**
     * Gets time series.   获取图表数据集
     *
     * @return the time series
     */
    public TimeSeries getTimeSeries() {
        return timeSeries;
    }

    /**
     * Gets chart panel.   获取图表显示容器
     *
     * @return the chart panel
     */
    public ChartPanel getChartPanel() {
        return chartPanel;
    }

    /**
     * Gets jfreechart.   获取图表组件
     *
     * @return the jfreechart
     */
    public JFreeChart getJfreechart() {
        return jfreechart;
    }
}