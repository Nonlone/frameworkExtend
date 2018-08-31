package com.feitai.backend.base;

import com.feitai.backend.BackendBaseApplication;
import com.feitai.backend.base.mapper.TestMapper;
import com.feitai.backend.base.model.TestModel;
import net.bytebuddy.utility.RandomString;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BackendBaseApplication.class)
public class TestMyBatisPlugin {

    @Autowired
    private TestMapper testMapper;

    @Test
    public void testInsert() {
        TestModel testModel = new TestModel();
        testModel.setId(Math.abs(UUID.randomUUID().getLeastSignificantBits()));
        testModel.setValue(RandomString.make(50));
//        testModel.setCreatedTime(new Date());
//        testModel.setUpdateTime(new Date());
        testMapper.insert(testModel);
    }

    @Test
    public void testUpdate() {
        TestModel testModel = testMapper.selectByPrimaryKey("6942390892258768637");
        testModel.setValue(RandomString.make(10));
        testMapper.updateByPrimaryKey(testModel);
    }
}
