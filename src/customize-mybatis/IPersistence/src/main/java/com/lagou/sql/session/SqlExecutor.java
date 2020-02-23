package com.lagou.sql.session;

import com.lagou.pojo.MappedStatement;

import javax.sql.DataSource;
import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;

public interface SqlExecutor {

    public <E> List<E> queryList(DataSource dataSource, MappedStatement statement, Object... params);

    public <E> E querySingle(DataSource dataSource, MappedStatement statement, Object... params) throws SQLException,
            ClassNotFoundException, NoSuchFieldException, IllegalAccessException, InstantiationException, IntrospectionException, InvocationTargetException;

}
