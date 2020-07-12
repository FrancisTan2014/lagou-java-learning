package com.lagou.utils;

import com.alibaba.druid.pool.DruidDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DruidUtils {

    private static DruidDataSource dataSource = new DruidDataSource();

    private static DruidUtils instance = new DruidUtils();

    private DruidUtils() { }

    static {
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/test");
        dataSource.setUsername("root");
        dataSource.setPassword("123456");
    }

    public static  DruidUtils getInstance() {
        return instance;
    }

    public Connection getSqlConnection() throws SQLException {
        return dataSource.getConnection();
    }

}
