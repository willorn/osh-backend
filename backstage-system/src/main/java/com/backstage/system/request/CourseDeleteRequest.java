package com.backstage.system.request;

import com.backstage.common.annotation.OshResourceId;

import java.util.List;

public class CourseDeleteRequest {
    @OshResourceId
    private List<Long> ids;
    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
}
