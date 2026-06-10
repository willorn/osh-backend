package com.backstage.system.domain.site;

import java.util.List;

/**
 * 级联选择器专用下拉实体
 * 前端 naive-ui n-cascader 直接识别
 */
public class SelectOption {
    /** 显示文本 */
    private String label;
    
    /** 值（唯一） */
    private Object value;
    
    /** 子级列表 */
    private List<SelectOption> children;

    public List<SelectOption> getChildren() {
        return children;
    }

    public void setChildren(List<SelectOption> children) {
        this.children = children;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}