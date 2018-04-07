package com.qq452651705.Utils;

import com.qq452651705.GUI.MainActivity;
import com.qq452651705.GUI.RealTimeChart;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExcelUtils {

    private String path;
    private HSSFWorkbook wb;

    public ExcelUtils(String path){
        this.path=path;
         wb = new HSSFWorkbook();
    }

    public void setPath(String path) {
        this.path = path;
    }

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

    public void beansToExcel(List<Bean> beans)throws IOException{
        if(beans==null||beans.size()==0)return;

        List<String> fieldNames=beans.get(0).getFieldNames();

//创建HSSFSheet对象
        HSSFSheet sheet = wb.createSheet("sheet0");
//创建HSSFRow对象
        HSSFRow titles = sheet.createRow(0);
        int columnIndex=0;
        for (String field:fieldNames) {
            titles.createCell(columnIndex++).setCellValue(field);
        }

        int rowIndex=1;
        for (Bean bean:beans) {
             HSSFRow row=sheet.createRow(rowIndex);
            Map<String,Object> fieldMap=bean.toTable();
            columnIndex=0;
            for (String field:fieldNames) {
                 row.createCell(columnIndex).setCellValue(fieldMap.get(field).toString());
            }
        }
        writeToFile();
    }

    private void writeToFile()throws IOException{
        FileOutputStream output;
        output = new FileOutputStream(path);
        wb.write(output);
        output.flush();
    }
}
