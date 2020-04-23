package per.nonlone.common.service;

import lombok.Data;

import java.util.Date;

@Data
public abstract class BaseModel {

    /**
     * 审计字段 创建时间
     */
    private Date createTime;

    /**
     * 审计字段 更新时间，insert 时， createTime 和 updateTime 一致
     */
    private Date updateTime;

}
