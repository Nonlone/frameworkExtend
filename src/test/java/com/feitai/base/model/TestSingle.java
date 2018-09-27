package com.feitai.base.model;

import lombok.Data;
import lombok.ToString;

import javax.persistence.Id;
import javax.persistence.Table;

@Data
@ToString(callSuper = true)
@Table(name = "t_test_single")
public class TestSingle {

    @Id
    private Long id;

    private Long testId;

    private String value;

}
