package com.backstage.system.domain.dto.info_gap;

import com.backstage.common.annotation.OshResourceId;

import java.util.List;

public class InfoGapUpdateReqDTO {
    @OshResourceId
    private Long id;
    private String title;
    private String content;
    private String tag;
    private List<Long> tagIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }
}
