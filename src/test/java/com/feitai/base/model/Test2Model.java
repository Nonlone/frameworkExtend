package com.feitai.base.model;

import com.feitai.base.mybatis.genid.UUIDGenId;
import lombok.Data;
import lombok.ToString;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;

@Data
@ToString(callSuper = true)
@Table(name = "t_test2")
public class Test2Model extends BaseModel {

    @Id
    @KeySql(genId = UUIDGenId.class)
    private String id;

    private String value;

}
