package com.backstage.system.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

/**
 * 章/节拖拽排序请求：一次性提交受影响节点的新 parentId 与 sort，由后端在一个事务内更新。
 * 仅更新 parent_id 与 sort，不触碰视频地址、文档内容等字段，避免重排时误伤已有数据。
 */
@ApiModel("章节拖拽排序请求")
public class CourseSectionReorderRequest {

    @ApiModelProperty(value = "课程ID", required = true, example = "100")
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @ApiModelProperty(value = "受影响的章/节排序项列表", required = true)
    @NotEmpty(message = "排序项不能为空")
    @Valid
    private List<Item> items;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    @ApiModel("章节排序项")
    public static class Item {

        @ApiModelProperty(value = "章/节ID", required = true, example = "200")
        @NotNull(message = "节点ID不能为空")
        private Long id;

        @ApiModelProperty(value = "父级ID（0=章，>0=所属章ID）", required = true, example = "0")
        @NotNull(message = "父级ID不能为空")
        @PositiveOrZero(message = "父级ID不能小于0")
        private Long parentId;

        @ApiModelProperty(value = "排序值，越小越靠前", required = true, example = "1")
        @NotNull(message = "排序不能为空")
        @PositiveOrZero(message = "排序不能小于0")
        private Integer sort;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }
    }
}
