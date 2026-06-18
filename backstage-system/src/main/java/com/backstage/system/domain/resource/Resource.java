package com.backstage.system.domain.resource;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/**
 * 内部资源实体
 *
 * @author backstage
 */
@TableName("osh_resource")
public class Resource extends OSHBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 资源ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 资源编号
     */
    @TableField("no")
    private String no;

    /**
     * 资源名称
     */
    @TableField("name")
    private String name;

    /**
     * 资源类型（doc/video/image/code/other）
     */
    @TableField("type")
    private String type;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    @TableField("file_path")
    private String filePath;

    @TableField("file_platform")
    private String filePlatform;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }

    public String getFilePlatform() {
        return filePlatform;
    }

    public void setFilePlatform(String filePlatform) {
        this.filePlatform = filePlatform;
    }
}
