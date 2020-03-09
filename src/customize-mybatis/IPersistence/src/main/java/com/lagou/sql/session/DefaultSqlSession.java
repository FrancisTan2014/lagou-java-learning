package com.lagou.sql.session;

import com.lagou.pojo.Configuration;
import com.lagou.pojo.MappedStatement;

import java.beans.IntrospectionException;
import java.lang.reflect.*;
import java.sql.SQLException;
import java.util.ArrayList;
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

    @Override
    public <T> T getMapper(Class<?> mapperClass) {
        Object proxyInstance = Proxy.newProxyInstance(DefaultSqlSession.class.getClassLoader(), new Class[]{mapperClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String className = method.getDeclaringClass().getName();
                String methodName = method.getName();
                String statementId = className + "." + methodName;

                List<Class<?>> list = new ArrayList<>();
                list.add(String.class);
                for (Class<?> parameterType : method.getParameterTypes()) {
                    list.add(parameterType);
                }
                Class<?>[] parameterTypes = list.toArray(new Class<?>[0]);
                Method targetMethod = this.getClass().getDeclaredMethod(methodName, parameterTypes);
                return targetMethod.invoke(this, statementId, args);
            }
        });

        return (T) proxyInstance;
    }

}
