package com.feitai.base.model;

import lombok.Data;
import lombok.ToString;

import javax.persistence.Id;
import javax.persistence.Table;

@Data
@ToString(callSuper = true)
@Table(name = "t_test")
public class TestModel extends BaseModel {

    @Id
    private Long id;

    private String value;

}
