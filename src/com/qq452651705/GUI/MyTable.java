package com.qq452651705.GUI;

import com.qq452651705.Utils.Bean;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyTable<T extends Bean> extends AbstractTableModel {
    private static final long serialVersionUID = 1L;


    private List<T> list=new ArrayList<>();
    private List<String> title ;

    public MyTable(){
        super();
    }

    public MyTable(List<String> title,List<T> list){
        super();
        this.list=list;
        this.title=title;
    }

    public void setString(List<String> n){
        this.title =n;
    }
    public void setList(List<T> list) {
        this.list = list;
    }

    public void addBean(T t){
        if(t==null) return;
        title=t.getFieldNames();
        list.add((T)t.getCopy());
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
        return title.size();
    }

    @Override
    public Object getValueAt(int row, int col) {
        return list.get(row).toTable().get(title.get(col));
    }

    @Override
    public String getColumnName(int column) {
        return title.get(column);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        Bean bean=list.get(rowIndex);
        Map<String,Object> map=bean.toTable();
        map.put(title.get(columnIndex),value);
        bean.setFields(map);
        fireTableCellUpdated(rowIndex, columnIndex);
    }
}

