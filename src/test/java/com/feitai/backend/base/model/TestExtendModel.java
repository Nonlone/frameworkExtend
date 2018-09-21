package com.feitai.backend.base.model;

import com.feitai.backend.base.mapper.TestMultipyMapper;
import com.feitai.backend.base.mybatis.Many;
import com.feitai.backend.base.mybatis.One;
import com.feitai.backend.base.mapper.TestSingleMapper;
import lombok.Data;
import lombok.ToString;

import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.List;

@Data
@ToString(callSuper = true)
@Table(name = "t_test")
public class TestExtendModel extends TestModel {

    @Transient
    @One(classOfMapper = TestSingleMapper.class, targetField = "testId")
    private TestSingle testSingle;

    @Transient
    @Many(classOfMapper = TestMultipyMapper.class, classOfEntity = TestMultipty.class, targetField = "testId")
    private List<TestMultipty> testMultiptyList;

}
