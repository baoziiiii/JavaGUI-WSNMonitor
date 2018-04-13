package com.qq452651705.GUI;

import com.qq452651705.Utils.TBean;
import com.sun.org.apache.xpath.internal.operations.Bool;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The type BeanTableModel.    将由TBean实现类对象组成的记录链表包装成可向JTable输出的模型
 *
 * @param <T> the type parameter    T必须是TBean的实现类
 */
public class BeanTableModel<T extends TBean> extends AbstractTableModel {

    /**
     *  表格的列名,由TBean的实现类提供
     */
    private List<String> titles;
    /**
     *  TBean数据记录,每一个TBean代表表格的一行记录
     */
    private List<T> list=new ArrayList<>();

    private Boolean isCellEditable;

    /**
     * Instantiates a new TBean table.
     */
    public BeanTableModel(Boolean isCellEditable){
        super();
        this.isCellEditable=isCellEditable;
    }

    /**
     * Instantiates a new TBean table.
     *
     * @param titles the titles
     * @param list  the list
     * @param isCellEditable true:表格可编辑; false:不可编辑
     */
    public BeanTableModel(List<String> titles, List<T> list,Boolean isCellEditable){
        super();
        this.list=list;
        this.titles = titles;
        this.isCellEditable=isCellEditable;
    }

    /**
     * Set Titles.
     *
     * @param n the n
     */
    public void setTitles(List<String> n){
        this.titles =n;
    }

    /**
     * Sets list.
     *
     * @param list the list
     */
    public void setList(List<T> list) {
        this.list = list;
    }

    /**
     * Add bean.        添加一行记录,使用TBean的拷贝以使表格的编辑与数据的保存分离
     *
     * @param t the t
     */
    public void addBean(T t){
        if(t==null) return;
        titles =t.getFieldNames();
        list.add((T)t.getCopy());
    }

    /**
     * Get titles col int.   获取指定列名对应的列序号
     *
     * @param t the t
     * @return the int
     */
    public int getTitleCol(String t){
        return titles.indexOf(t);
    }


    /**
     * Clear list.    清空链表
     */
    public void clearList(){
        list=new ArrayList<>();
    }

    /**
     * Get row count.    获取记录条数
     */
    @Override
    public int getRowCount() {
        return list.size();
    }

    /**
     * Get column count.    获取列数
     */
    @Override
    public int getColumnCount() {
        return titles.size();
    }


    /**
     * Get value at
     * @param row
     * @param col
     * @return    获取指定行与列的值
     */
    @Override
    public Object getValueAt(int row, int col) {
        return list.get(row).toTable().get(titles.get(col));
    }


    /**
     * Get column name.
     * @param column
     * @return            获取指定列号的列标题.
     */
    @Override
    public String getColumnName(int column) {
        return titles.get(column);
    }

    /**
     * @param rowIndex
     * @param columnIndex
     * @return             返回true可编辑,返回false不可编辑
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return isCellEditable;
    }

    /**
     * Set value.        设置rowIndex行与colIndex列的值为value
     * @param value
     * @param rowIndex
     * @param columnIndex
     */
    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        TBean tBean =list.get(rowIndex);
        Map<String,Object> map= tBean.toTable();
        map.put(titles.get(columnIndex),value);
        tBean.setFields(map);
        fireTableCellUpdated(rowIndex, columnIndex);
    }
}

