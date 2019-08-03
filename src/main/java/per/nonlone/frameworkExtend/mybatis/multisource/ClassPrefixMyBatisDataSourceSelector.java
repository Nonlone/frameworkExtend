package per.nonlone.frameworkExtend.mybatis.multisource;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import per.nonlone.frameworkExtend.datasource.MultipleDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ClassPrefixMyBatisDataSourceSelector extends BaseMyBatisDataSourceSelector {

    /**
     * 类前缀数据池Key映射
     */
    private Map<String, String> classPrefixKeyMap;

    private Map<Class<?>, String> classKeyMap = new ConcurrentHashMap<>();

    public ClassPrefixMyBatisDataSourceSelector(MultipleDataSource multipleDataSource, Map<String, String> classPrefixKeyMap) {
        super(multipleDataSource);
        this.classPrefixKeyMap = classPrefixKeyMap;
    }


    /**
     * 根据类获取Key
     *
     * @param mapperClass
     * @return
     */
    @Override
    public String getKey(Class<?> mapperClass) {
        String key = null;
        // 查询缓存类映射
        if (classKeyMap.containsKey(mapperClass)) {
            key = classKeyMap.get(mapperClass);
        }else {
        // 查询前缀映射
            ClassPrefixMatcher classPrefixMatcher = null;
            String mapperClassString = mapperClass.getName();
            for (Map.Entry<String, String> entry : classPrefixKeyMap.entrySet()) {
                if (mapperClassString.startsWith(entry.getKey())) {
                    if(log.isDebugEnabled()){
                        log.debug(String.format("check matcher mapperClass<%s> classPrefix<%s>",mapperClassString,entry.getKey()));
                    }
                    ClassPrefixMatcher tempMatcher = new ClassPrefixMatcher(entry.getKey());
                    if (tempMatcher.matchThan(classPrefixMatcher)) {
                        classPrefixMatcher = tempMatcher;
                        key = entry.getValue();
                        // 缓存到映射
                        classKeyMap.put(mapperClass, entry.getValue());
                    }
                }
            }
        }
        return key;
    }


    /**
     * 获取指定类数据源
     *
     * @param classOfT
     * @return
     */
    @Override
    public DataSource getDataSource(Class<?> classOfT) {
        String key = getKey(classOfT);
        if (StringUtils.isNotBlank(key) && multipleDataSource.hasDataSourceKey(key)) {
            DataSource dataSource = multipleDataSource.getDataSouce(key);
            return dataSource;
        }
        return null;
    }


    /**
     * 类前缀比较类
     */
    private class ClassPrefixMatcher {
        /**
         * ClassName点数
         */
        private int dotsOfClassName;

        /**
         * ClassName长度数
         */
        private int lengthOfCalssName;


        private String className;

        public ClassPrefixMatcher(String className) {
            this.className = className;
            this.dotsOfClassName = StringUtils.countMatches(className, ".");
            this.lengthOfCalssName = className.length();
        }

        /*
         * 比较两个Matcher
         */
        public boolean matchThan(ClassPrefixMatcher matcher) {
            if (matcher == null) {
                return true;
            }

            if (this.dotsOfClassName > matcher.dotsOfClassName) {
                return true;
            }
            if (this.lengthOfCalssName > matcher.lengthOfCalssName) {
                return true;
            }
            return false;
        }
    }
}
