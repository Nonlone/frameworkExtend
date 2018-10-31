package com.feitai.base.mybatis.interceptor;

import lombok.Data;
import lombok.Getter;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Objects;

/**
 * 连接签名，判断Connection是否相同
 */
@Data
@Getter
public class ConnectionSignature {

    private String url;

    private String userName;

    public ConnectionSignature(Connection connection) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        this.url = databaseMetaData.getURL();
        this.userName = databaseMetaData.getUserName();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConnectionSignature that = (ConnectionSignature) o;
        return Objects.equals(url, that.url) &&
                Objects.equals(userName, that.userName);
    }

}
