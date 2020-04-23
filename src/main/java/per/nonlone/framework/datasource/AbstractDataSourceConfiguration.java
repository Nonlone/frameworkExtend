package per.nonlone.framework.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.datasource.DataSourceException;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import per.nonlone.utils.StringUtils;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 抽象数据配置
 */
public abstract class AbstractDataSourceConfiguration implements EnvironmentAware {

    /**
     * Base
     * 默认Druid 数据库配置
     */
    private static final String DEFAULT_DRUID_PROPERTIES_PREFIX = "druid";

    /**
     * 默认Druid 数据库连接配置
     */
    private static final String DEFAULT_DRUID_CONNECTION_PROPERTIES = "druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000;druid.keepAlive=true";

    /**
     * Hikari 数据库配置
     */
    private static final String DEFAULT_HIKARI_PROPERTIES_PREFIX = "hikari";

    private Environment environment;
    private Properties defaultDataSourceProperties = new Properties() {{
        this.put("cachePrepStmts", "true");
        this.put("prepStmtCacheSize", "250");
        this.put("prepStmtCacheSqlLimit", "2048");
        this.put("useServerPrepStmts", "true");
        this.put("useLocalSessionState", "true");
        this.put("rewriteBatchedStatements", "true");
        this.put("cacheResultSetMetadata", "true");
        this.put("cacheServerConfiguration", "true");
        this.put("elideSetAutoCommits", "true");
        this.put("maintainTimeStats", "false");
    }};

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    protected Properties buildPropertiesFromEnvironment(Properties properties, String propertiesPrefix, String propertiesKey) {
        String value = environment.getProperty(propertiesPrefix + "." + propertiesKey);
        if (StringUtils.isNotBlank(value)) {
            properties.put(propertiesKey, value);
        }
        return properties;
    }

    /**
     * 获取数据源
     *
     * @param propertiesPrefix
     * @param connectionProperties
     * @return
     * @throws Exception
     */
    protected DataSource getDruidDataSource(String propertiesPrefix, String connectionProperties) throws DataSourceException {
        String druidPropertiesPrefix = DEFAULT_DRUID_PROPERTIES_PREFIX;
        try {
            Properties props = new Properties();

            // 构建基础信息
            buildPropertiesFromEnvironment(props, propertiesPrefix, DruidDataSourceFactory.PROP_URL);
            buildPropertiesFromEnvironment(props, propertiesPrefix, DruidDataSourceFactory.PROP_USERNAME);
            buildPropertiesFromEnvironment(props, propertiesPrefix, DruidDataSourceFactory.PROP_PASSWORD);

            // 构建默认信息
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_DRIVERCLASSNAME);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_INITIALSIZE);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_MINIDLE);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_MAXACTIVE);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_MAXWAIT);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_POOLPREPAREDSTATEMENTS);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_MAXOPENPREPAREDSTATEMENTS);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_TIMEBETWEENEVICTIONRUNSMILLIS);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_MINEVICTABLEIDLETIMEMILLIS);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_VALIDATIONQUERY);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_VALIDATIONQUERY_TIMEOUT);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_TESTWHILEIDLE);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_TESTONBORROW);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_TESTONRETURN);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_FILTERS);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_REMOVEABANDONED);
            buildPropertiesFromEnvironment(props, druidPropertiesPrefix, DruidDataSourceFactory.PROP_LOGABANDONED);
            props.put(DruidDataSourceFactory.PROP_CONNECTIONPROPERTIES, connectionProperties);

            DruidDataSource druidDataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(props);
            druidDataSource.setName(propertiesPrefix);
            druidDataSource.init();
            return druidDataSource;
        } catch (Exception e) {
            throw new DataSourceException(String.format("getDruidDataSource error %s propertiesPrefix<%s> defaultPropertiesPrefix<%s> connectionProperties<%s>", e.getMessage(), propertiesPrefix, druidPropertiesPrefix, connectionProperties), e);
        }
    }

    protected DataSource getHikariDataSource(String propertiesPrefix) throws DataSourceException {
        String hikariPropertiesPrefix = DEFAULT_HIKARI_PROPERTIES_PREFIX;
        try {
            // 构建基础信息
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(environment.getProperty(propertiesPrefix + "." + HikariConfigureProperties.URL));
            hikariConfig.setUsername(environment.getProperty(propertiesPrefix + "." + HikariConfigureProperties.USERNAME));
            hikariConfig.setPassword(environment.getProperty(propertiesPrefix + "." + HikariConfigureProperties.PASSWORD));
            if (StringUtils.isNotBlank(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.CONNECTION_TEST_QUERY))) {
                hikariConfig.setConnectionTestQuery(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.CONNECTION_TEST_QUERY).replace("\"", "").replace("'", ""));
            }
            if (StringUtils.isNotBlank(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.AUTO_COMMIT))) {
                hikariConfig.setAutoCommit(Boolean.parseBoolean(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.AUTO_COMMIT)));
            }
            if (StringUtils.isNotBlank(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.MIN_IDLE))) {
                hikariConfig.setMinimumIdle(Integer.parseInt(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.MIN_IDLE)));
            }
            if (StringUtils.isNotBlank(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.MAX_POOL_SIZE))) {
                hikariConfig.setMaximumPoolSize(Integer.parseInt(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.MAX_POOL_SIZE)));
            }
            if (StringUtils.isNotBlank(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.MAX_LIFETIME))) {
                hikariConfig.setMaxLifetime(Long.parseLong(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.MAX_LIFETIME)));
            }
            if (StringUtils.isNotBlank(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.IDLE_TIMEOUT))) {
                hikariConfig.setIdleTimeout(Integer.parseInt(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.IDLE_TIMEOUT)));
            }
            if (StringUtils.isNotBlank(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.CONNECTION_TIMEOUT))) {
                hikariConfig.setConnectionTimeout(Long.parseLong(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.CONNECTION_TIMEOUT)));
            }
            if (StringUtils.isNotBlank(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.CONNECTION_INIT_SQL))) {
                hikariConfig.setConnectionInitSql(environment.getProperty(hikariPropertiesPrefix + "." + HikariConfigureProperties.CONNECTION_INIT_SQL));
            }
            hikariConfig.setPoolName(propertiesPrefix);
            hikariConfig.setDataSourceProperties(defaultDataSourceProperties);
            HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
            return hikariDataSource;
        } catch (Exception e) {
            throw new DataSourceException(String.format("getHikariDataSource error %s propertiesPrefix<%s> defaultPropertiesPrefix<%s>", e.getMessage(), propertiesPrefix, hikariPropertiesPrefix), e);
        }
    }


}
