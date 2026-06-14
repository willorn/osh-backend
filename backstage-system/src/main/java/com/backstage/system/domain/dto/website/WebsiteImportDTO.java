package com.backstage.system.domain.dto.website;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;

import java.io.Serializable;

/**
 * 实用网站批量导入 DTO（Excel 行映射）
 */
@ColumnWidth(20)
public class WebsiteImportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 网站名称（必填） */
    @ExcelProperty("网站名称")
    private String name;

    /** 网站链接（必填，需以 http:// 或 https:// 开头） */
    @ExcelProperty("网站链接")
    private String url;

    /** 网站描述（可选） */
    @ExcelProperty("网站描述")
    private String description;

    /** 网站 Logo 地址（可选） */
    @ExcelProperty("Logo地址")
    private String logoUrl;

    /** 标签，多个用英文逗号分隔，如：后端,AI工具（可选） */
    @ExcelProperty("标签(多个用英文逗号分隔)")
    private String tags;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "WebsiteImportDTO{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", description='" + description + '\'' +
                ", logoUrl='" + logoUrl + '\'' +
                ", tags='" + tags + '\'' +
                '}';
    }
}
