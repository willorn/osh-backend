package com.backstage.system.domain.dto.info_gap;

import com.backstage.common.annotation.OshResourceId;

public class InfoGapVoteReqDTO {

    @OshResourceId
    private Long id;
    private Integer type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
