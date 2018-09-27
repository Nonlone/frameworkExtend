package com.feitai.base;

import com.alibaba.fastjson.JSON;
import com.feitai.base.mapper.*;
import com.feitai.base.mybatis.ManyAnnotationFieldWalkProcessor;
import com.feitai.backend.base.mapper.*;
import com.feitai.base.model.TestExtendModel;
import com.feitai.base.model.TestModel;
import com.feitai.base.model.TestMultipty;
import com.feitai.base.model.TestSingle;
import com.feitai.base.mybatis.OneAnnotationFieldWalkProcessor;
import com.feitai.utils.ObjectUtils;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.lang3.RandomUtils;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.util.Sqls;
import tk.mybatis.mapper.weekend.Fn;
import tk.mybatis.mapper.weekend.WeekendSqls;
import tk.mybatis.mapper.weekend.reflection.Reflections;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BackendBaseApplication.class)
public class TestMyBatisPlugin implements ApplicationContextAware {

    @Autowired
    private SqlSession sqlSession;

    @Autowired
    private TestMapper testMapper;

    @Autowired
    private TestSingleMapper testSingleMapper;

    @Autowired
    private TestMultipyMapper testMultipyMapper;

    @Autowired
    private TestExtendMapper testExtendMapper;

    @Autowired
    private ApplicationContext applicationContext;


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
    public void testSingleInsert() {
        long testId = 6661471052564218567L;
        TestSingle testSingle = new TestSingle();
        testSingle.setId(Math.abs(UUID.randomUUID().getLeastSignificantBits()));
        testSingle.setValue(RandomString.make(50));
        testSingle.setTestId(testId);
        testSingleMapper.insert(testSingle);
    }


    @Test
    public void testMultipyInsert() {
        long testId = 6661471052564218567L;
        int count = RandomUtils.nextInt(0, 10);
        for (int i = 0; i < count; i++) {
            TestMultipty testMultipty = new TestMultipty();
            testMultipty.setId(Math.abs(UUID.randomUUID().getLeastSignificantBits()));
            testMultipty.setValue(RandomString.make(50));
            testMultipty.setTestId(testId);
            testMultipyMapper.insert(testMultipty);
        }
    }

    @Test
    public void testUpdate() {
        TestModel testModel = testMapper.selectByPrimaryKey("6942390892258768637");
        testModel.setValue(RandomString.make(10));
        testMapper.updateByPrimaryKey(testModel);
    }

    @Test
    public void testSelect() {
        List<TestModel> testModelList = testMapper.selectByExample(Example.builder(TestModel.class).where(WeekendSqls.<TestModel>custom().andEqualTo(TestModel::getId, "xxx")).orderByDesc("createdTime").build());
        System.out.println(testModelList);

    }

    @Test
    public void testDynmaticSelect() {
        String sql = "select * from t_test tt left join t_test_single tts on tt.id=tts.test_id";
        SqlMapper sqlMapper = new SqlMapper(sqlSession);
        List<TestModel> testModelList = sqlMapper.selectList(sql, TestModel.class);
        System.out.println(JSON.toJSONString(testModelList));
    }

    @Test
    public void testFunction() throws NoSuchMethodException {
        Fn<TestModel, Object> fn = TestModel::getId;
        Fn<TestModel, Object> fn1 = (t) -> {
            return t.getId();
        };

        Fn<Object, Object> fn2;

        System.out.println(Reflections.fnToFieldName(fn));
        Function<TestModel, Object> function = TestModel::getId;
        function.getClass();
        Method method = function.getClass().getDeclaredMethod("writeReplace");
    }

    @Test
    public void testFn() {
        StringBuffer sb = new StringBuffer(null);
        Object object = null;
        sb.append(object);
        System.out.println(sb.toString());
    }

    @Test
    public void testExample() {
        Sqls sqls = Sqls.custom().andEqualTo("id","xxx");
        System.out.println(sqls.toString());
    }



    @Test
    public void testWrap() {
        List<TestExtendModel> testExtendModelList = testExtendMapper.selectAll();
        OneAnnotationFieldWalkProcessor oneAnnotationFieldWalkProcessor = new OneAnnotationFieldWalkProcessor(applicationContext);
        ManyAnnotationFieldWalkProcessor manyAnnotationFieldWalkProcessor = new ManyAnnotationFieldWalkProcessor(applicationContext);
        for (TestExtendModel testExtendModel : testExtendModelList) {
            ObjectUtils.fieldWalkProcess(testExtendModel, oneAnnotationFieldWalkProcessor);
            ObjectUtils.fieldWalkProcess(testExtendModel,manyAnnotationFieldWalkProcessor);
            System.out.println(JSON.toJSONString(testExtendModel));
        }
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
