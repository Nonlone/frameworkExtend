package com.feitai.base.mybatis.interceptor;

import com.alibaba.fastjson.JSON;
import com.feitai.utils.StringUtils;
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

    public ConnectionSignature(String url,String userName){
        this.url = url;
        this.userName = userName;
    }

    public ConnectionSignature(Connection connection) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        this.url = databaseMetaData.getURL();
        this.userName = databaseMetaData.getUserName();
    }

    /**
     * 比较 ConnectionSignature 是否相等
     * @param connectionSignature
     * @return
     */
    public boolean checkEquals(ConnectionSignature connectionSignature){
        if(Objects.isNull(connectionSignature)){
            return false;
        }
        return checkEquals(connectionSignature.getUrl(),connectionSignature.getUserName());
    }

    /**
     * 比较 ConnectionSignature 是否相等
     * @param url
     * @param userName
     * @return
     */
    public boolean checkEquals(String url,String userName) {
       if(StringUtils.isAnyBlank(url,userName)){
           return false;
       }
        return Objects.equals(this.url, url) && Objects.equals(this.userName,userName);
    }


    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
