package com.qq452651705.Utils;

import java.util.List;
import java.util.Map;

/**
 * The interface TBean.  可输出表格数据接口，TBean实现类的对象可通过BeanTableModel向JTable表格组件输出，以及通过ExcelUtils输出Excel文件
 */
public interface TBean {
    /**
     * Gets field names.   输出表格列名，对应每个属性
     *
     * @return the field names
     */
    List<String> getFieldNames();

    /**
     * Sets fields.       列名作为索引初始化一个对象
     *
     * @param map the map
     */
    void setFields(Map<String,Object> map);

    /**
     * To table map.     输出列名-属性值的map
     *
     * @return the map
     */
    Map<String,Object> toTable();

    /**
     * Gets copy.       输出一个自身的拷贝
     *
     * @return the copy
     */
    TBean getCopy();
}
