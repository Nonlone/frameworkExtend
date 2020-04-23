package per.nonlone.framework.mybatis.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;
import tk.mybatis.mapper.entity.EntityTable;
import tk.mybatis.mapper.entity.IDynamicTableName;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;


@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DynamicTableInterceptor extends StatementHandlerInterceptor {

    private static final String CREATE_TEMPLATE = "CREATE TABLE IF NOT EXISTS %s LIKE %s ; ";

    private static final String ALTER_TEMPLATE = "ALTER TABLE %s ENGINE=InnoDB ;";

    public DynamicTableInterceptor() {
        super(t -> {
            Connection connection = (Connection) t.getArgs()[0];
            StatementHandler handler = getStatementHandlerFromInvocationOfStatementHandler(t);
            BoundSql boundSql = handler.getBoundSql();
            Object object = boundSql.getParameterObject();
            if (object instanceof IDynamicTableName) {
                String dynamicTableName = ((IDynamicTableName) object).getDynamicTableName();
                try {
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getTables(null, null, dynamicTableName, null);
                    if (resultSet.next()) {
                        // 对应表存在
                        return null;
                    }
                    // 建表操作
                    EntityTable entityTable = EntityHelper.getEntityTable(object.getClass());
                    connection.createStatement().execute(String.format(CREATE_TEMPLATE, dynamicTableName, entityTable.getName()));
                    connection.createStatement().execute(String.format(ALTER_TEMPLATE, dynamicTableName));
                } catch (SQLException sqle) {
                    log.error(String.format("dynamicTable<%s> create error %s", dynamicTableName, sqle.getMessage()), sqle);
                }
            }
            return null;
        });
    }


}
