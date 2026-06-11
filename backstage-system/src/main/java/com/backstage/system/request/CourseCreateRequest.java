package com.backstage.system.request;

import com.backstage.common.annotation.OshResourceId;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 课程创建请求
 */
public class CourseCreateRequest {

    // 新增：有值时走更新，null 时走新增
    @OshResourceId
    private Long id;

    @NotBlank(message = "课程标题不能为空")
    private String title;

    private String cover;

    private String intro;

    private String serviceContent;

    @NotNull(message = "课程价格不能为空")
    @DecimalMin(value = "0.00", message = "课程价格不能小于0")
    private BigDecimal price;

    @NotNull(message = "课程原价不能为空")
    @DecimalMin(value = "0.00", message = "课程原价不能小于0")
    @JsonProperty("tPrice")
    private BigDecimal tPrice;

    @NotBlank(message = "课程类型不能为空")
    private String type;

    @PositiveOrZero(message = "免费类型不能小于0")
    private Integer freeType;

    @PositiveOrZero(message = "售后答疑天数不能小于0")
    private Integer afterServiceDays;

    private Integer examId;

    private String remark;

    private String resourceType;

    private Integer level;

    private Integer servicePeriod;

    /**
     * 课程标签
     */
    private List<String> tags;

    /**
     * 课程资料
     */
    @Valid
    private CourseMaterialCreateRequest material;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = StringUtils.trimToNull(title);
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = StringUtils.trimToNull(cover);
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = StringUtils.trimToNull(intro);
    }

    public String getServiceContent() {
        return serviceContent;
    }

    public void setServiceContent(String serviceContent) {
        this.serviceContent = StringUtils.trimToNull(serviceContent);
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTPrice() {
        return tPrice;
    }

    public void setTPrice(BigDecimal tPrice) {
        this.tPrice = tPrice;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = StringUtils.trimToNull(type);
    }

    public Integer getFreeType() {
        return freeType;
    }

    public void setFreeType(Integer freeType) {
        this.freeType = freeType;
    }

    public Integer getAfterServiceDays() {
        return afterServiceDays;
    }

    public void setAfterServiceDays(Integer afterServiceDays) {
        this.afterServiceDays = afterServiceDays;
    }

    public Integer getExamId() {
        return examId;
    }

    public void setExamId(Integer examId) {
        this.examId = examId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = StringUtils.trimToNull(remark);
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        if (tags == null) {
            this.tags = null;
            return;
        }
        List<String> normalizedTags = new ArrayList<>(tags.size());
        for (String tag : tags) {
            normalizedTags.add(tag);
        }
        this.tags = normalizedTags;
    }

    public CourseMaterialCreateRequest getMaterial() {
        return material;
    }

    public void setMaterial(CourseMaterialCreateRequest material) {
        this.material = material;
    }


    public BigDecimal gettPrice() {
        return tPrice;
    }

    public void settPrice(BigDecimal tPrice) {
        this.tPrice = tPrice;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = StringUtils.trimToNull(resourceType);
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getServicePeriod() {
        return servicePeriod;
    }

    public void setServicePeriod(Integer servicePeriod) {
        this.servicePeriod = servicePeriod;
    }
}
