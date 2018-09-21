package com.feitai.backend.base.model;

import lombok.Data;
import lombok.ToString;

import javax.persistence.Id;
import javax.persistence.Table;

@Data
@ToString(callSuper = true)
@Table(name = "t_test_multipy")
public class TestMultipty {


    @Id
    private Long id;

    private Long testId;

    private String value;

}
