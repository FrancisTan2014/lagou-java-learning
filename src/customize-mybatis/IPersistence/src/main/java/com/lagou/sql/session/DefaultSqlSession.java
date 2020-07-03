package com.lagou.sql.session;

import com.lagou.pojo.Configuration;
import com.lagou.pojo.MappedStatement;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public class DefaultSqlSession implements SqlSession {

    private final Configuration configuration;
    private final SqlExecutor executor;

    public DefaultSqlSession(Configuration configuration) {
        this.configuration = configuration;
        this.executor = new DefaultSqlExecutor();
    }

    public <E> List<E> queryList(String statementId, Object... params) throws IllegalAccessException,
            IntrospectionException, InstantiationException, NoSuchFieldException, SQLException,
            InvocationTargetException, ClassNotFoundException {
        MappedStatement statement = configuration.getMappedStatementMap().get(statementId);
        return this.executor.queryList(configuration.getDataSource(), statement, params);
    }

    @Override
    public <E> E querySingle(String statementId, Object... params)
             throws IllegalAccessException, IntrospectionException, InstantiationException, NoSuchFieldException,
            SQLException, InvocationTargetException, ClassNotFoundException {
        MappedStatement statement = configuration.getMappedStatementMap().get(statementId);
        return this.executor.querySingle(configuration.getDataSource(), statement, params);
    }

    @Override
    public <T> T getMapper(Class<?> mapperClass) {
        Object proxyInstance = Proxy.newProxyInstance(DefaultSqlSession.class.getClassLoader(),
                new Class[]{mapperClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String statementId = method.getDeclaringClass().getName() + "." + method.getName();
                MappedStatement statement = configuration.getMappedStatementMap().get(statementId);
                switch (statement.getSqlCommandType()) {
                    case INSERT:
                        return rowCountResult(executor.insert(configuration.getDataSource(),
                            statement, args), method);
                    case UPDATE:
                        return rowCountResult(executor.update(configuration.getDataSource(),
                            statement, args), method);
                    case DELETE:
                        return rowCountResult(executor.delete(configuration.getDataSource(),
                            statement, args), method);
                    case SELECT:
                        return selectResult(statement, args, method);
                    default:
                        throw new Exception("Not supported sql command type.");
                }
            }
        });

        return (T) proxyInstance;
    }

    private Object rowCountResult(int rowCount, Method method) throws Exception {
        final Object result;
        if (returnsVoid(method)) {
            result = null;
        } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
            result = rowCount;
        } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
            result = (long) rowCount;
        } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
            result = rowCount > 0;
        } else {
            throw new Exception("Mapper method '" + method.getName() + "' has an unsupported return type: " + method.getReturnType());
        }
        return result;
    }

    private Object selectResult(MappedStatement statement, Object[] args, Method method) throws IllegalAccessException, IntrospectionException, InstantiationException, NoSuchFieldException, SQLException, InvocationTargetException, ClassNotFoundException {
        if (returnsVoid(method)) {
            return null;
        } else if (returnsMany(method)) {
            return executor.queryList(configuration.getDataSource(), statement, args);
        } else {
            return executor.querySingle(configuration.getDataSource(), statement, args);
        }
    }

    private boolean returnsVoid(Method method) {
        return void.class.equals(method.getReturnType().getClass());
    }

    private boolean returnsMany(Method method) {
        return Collection.class.isAssignableFrom(method.getReturnType());
    }

}
