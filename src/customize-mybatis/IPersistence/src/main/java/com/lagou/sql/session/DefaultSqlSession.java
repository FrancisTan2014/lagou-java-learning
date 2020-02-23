package com.lagou.sql.session;

import com.lagou.pojo.Configuration;
import com.lagou.pojo.MappedStatement;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;

public class DefaultSqlSession implements SqlSession {

    private final Configuration configuration;
    private final SqlExecutor executor;

    public DefaultSqlSession(Configuration configuration) {
        this.configuration = configuration;
        this.executor = new DefaultSqlExecutor();
    }

    public <E> List<E> queryList(String statementId, Object... params) {
        MappedStatement statement = configuration.getMappedStatementMap().get(statementId);
        return this.executor.queryList(configuration.getDataSource(), statement, params);
    }

    @Override
    public <E> E querySingle(String statementId, Object... params)
            throws IllegalAccessException, IntrospectionException, InstantiationException, NoSuchFieldException, SQLException, InvocationTargetException, ClassNotFoundException {
        MappedStatement statement = configuration.getMappedStatementMap().get(statementId);
        return this.executor.querySingle(configuration.getDataSource(), statement, params);
    }

}
