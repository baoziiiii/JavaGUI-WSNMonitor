package com.qq452651705.Utils;

import java.util.List;
import java.util.Map;

public interface Bean {
    List<String> getFieldNames();
    void setFields(Map<String,Object> map);
    Map<String,Object> toTable();
    Bean getCopy();
}
