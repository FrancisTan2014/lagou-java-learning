package com.lagou.utils;

import com.alibaba.druid.pool.DruidDataSource;
import com.lagou.ioc.Autowired;
import com.lagou.ioc.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Service
public class ConnectionUtils {

    @Autowired
    private DataSource dataSource;

    private ThreadLocal<Connection> threadLocal = new InheritableThreadLocal<>();

    public Connection getSqlConnection() throws SQLException {
        Connection connection = threadLocal.get();
        if (connection == null) {
            connection = dataSource.getConnection();
            threadLocal.set(connection);
        }
        return connection;
    }

}
