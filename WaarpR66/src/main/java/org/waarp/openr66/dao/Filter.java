package org.waarp.openr66.dao;

public class Filter {
    public String key;
    public String operand;
    public Object value;

    public Filter(String key, String operand, Object value) {
        this.key = key;
        this.operand = operand;
        this.value = value;
    }
}
