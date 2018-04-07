package com.qq452651705.GUI;

import com.qq452651705.Utils.Bean;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyTable extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    private Bean bean;

    private List<Object[]> list=new ArrayList<>();

    String[] title =new String[]{"空"};

    public void setBean(Bean t){
        if(t==null) return;
        bean=t;
        Map<String,Object> map=t.toTable();
        Integer count=map.size();
        title =new String[count];
        Object[] val=new Object[count];
        int i=0;
        for(Map.Entry<String,Object> entry:map.entrySet()){
            title[i]=entry.getKey();
            val[i++]=entry.getValue();
        }
        list.add(val);
    }



    public void setString(String[] n){
        this.title =n;
    }

    public void setList(List<Object[]> list) {
        this.list = list;
    }

    public void clearList(){
        list=new ArrayList<>();
    }

    @Override
    public int getRowCount() {
        return list.size();
    }

    @Override
    public int getColumnCount() {
        return title.length;
    }

    @Override
    public Object getValueAt(int row, int col) {
        return list.get(row)[col];
    }

    @Override
    public String getColumnName(int column) {
        return title[column];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        list.get(rowIndex)[columnIndex] = value;
        fireTableCellUpdated(rowIndex, columnIndex);
    }
}

