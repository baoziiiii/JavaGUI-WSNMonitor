package com.qq452651705.Utils;

import com.qq452651705.GUI.RealTimeChart;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The type Excel utils.   Excel输出类.将TBean对象的记录链表或RealTimeChart图表数据输出成.xls文件
 *
 * @param <T> the type parameter   TBean的实现类
 */
public class ExcelUtils<T extends TBean> {


    private String path;
    private HSSFWorkbook wb;

    /**
     * Instantiates a new Excel utils.
     *
     * @param path the path   保存路径
     */
    public ExcelUtils(String path){
        this.path=path;
         wb = new HSSFWorkbook();
    }

    /**
     * Sets path.   设置保存路径
     *
     * @param path the path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Chart to excel.   图表数据输出,提取RealTimeChart中的timeSeries数据链表后输出
     *
     * @param realTimeChart the real time chart
     * @throws IOException the io exception
     */
    public void chartToExcel(RealTimeChart realTimeChart)throws IOException{
        JFreeChart jFreeChart=realTimeChart.getJfreechart();
        XYPlot xyPlot=jFreeChart.getXYPlot();
        String domain=xyPlot.getDomainAxis().getLabel();
        String range=xyPlot.getRangeAxis().getLabel();
        HSSFSheet sheet = wb.createSheet(jFreeChart.getTitle().getText().toString());
        HSSFRow titles=sheet.createRow(0);
        titles.createCell(0).setCellValue(domain);
        titles.createCell(1).setCellValue(range);

        TimeSeries timeSeries=realTimeChart.getTimeSeries();
        for(int i=0;i<timeSeries.getItemCount();i++){
            HSSFRow row=sheet.createRow(i+1);
            TimeSeriesDataItem tsdi=timeSeries.getDataItem(i);
            row.createCell(0).setCellValue(tsdi.getPeriod().toString());
            row.createCell(1).setCellValue(tsdi.getValue().toString());
        }
        writeToFile();
    }

    /**
     * Beans to excel.   输出TBeans记录.可同时输出多种TBean记录,保存在同一个xls文件的不同sheet中
     *
     * @param map the map   key:String记录名称,用作sheetname   value:List<T>对应的记录链表
     * @throws IOException the io exception
     */
    public void beansToExcel(Map<String,List<T>> map)throws IOException{
        for(Map.Entry<String,List<T>> entry:map.entrySet()) {
            String sheetName=entry.getKey(); //表单名
            List<T> beans=entry.getValue();  //表单数据
            if (beans == null || beans.size() == 0) continue;

            List<String> fieldNames = beans.get(0).getFieldNames(); //获取列标题

            HSSFSheet sheet = wb.createSheet(sheetName);//sheet

            HSSFRow titles = sheet.createRow(0);//第一行为列标题
            int columnIndex = 0;
            for (String field : fieldNames) {
                titles.createCell(columnIndex++).setCellValue(field);
            }

            int rowIndex = 1; //第二行开始输出数据
            for (TBean tBean : beans) {
                HSSFRow row = sheet.createRow(rowIndex++);
                Map<String, Object> fieldMap = tBean.toTable();
                columnIndex = 0;
                for (String field : fieldNames) {
                    row.createCell(columnIndex++).setCellValue(fieldMap.get(field).toString());
                }
            }
        }
        writeToFile();
    }

    /**
    *   Write to file. 输出
    */
    private void writeToFile()throws IOException{
        FileOutputStream output;
        output = new FileOutputStream(path);
        wb.write(output);
        output.flush();
    }
}
