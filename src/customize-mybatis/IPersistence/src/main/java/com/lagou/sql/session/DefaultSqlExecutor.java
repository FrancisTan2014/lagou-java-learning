package com.lagou.sql.session;

import com.lagou.pojo.MappedStatement;
import com.lagou.utils.GenericTokenParser;
import com.lagou.utils.ParameterMapping;
import com.lagou.utils.ParameterMappingTokenHandler;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DefaultSqlExecutor implements SqlExecutor {

    public <E> List<E> queryList(DataSource dataSource, MappedStatement statement, Object... params) {
        return null;
    }

    public <E> E querySingle(DataSource dataSource, MappedStatement statement, Object... params)
            throws SQLException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, InstantiationException, IntrospectionException, InvocationTargetException {
        // prepare sql statement
        ParsedContent parsedContent = resolvePlaceholders(statement);
        Connection connection = dataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(parsedContent.getParsedSql());
        List<ParameterMapping> parameterMappings = parsedContent.getParameterMappings();
        Class<?> parameterType = getType(statement.getParameterType());
        setParameters(preparedStatement, parameterMappings, parameterType, params[0]);

        // execute sql
        ResultSet resultSet = preparedStatement.executeQuery();
        Class<?> resultType = getType(statement.getResultType());
        List<E> list = mapEntities(resultSet, resultType);
        if (list.size() == 0) {
            return null;
        }
        return (E) list.get(0);
    }

    @NotNull
    private <E> List<E> mapEntities(ResultSet resultSet, Class<?> resultType)
            throws SQLException, InstantiationException, IllegalAccessException, IntrospectionException, InvocationTargetException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        ArrayList<E> list = new ArrayList<>();
        while (resultSet.next()) {
            Object o = resultType.newInstance();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i);
                Object value = resultSet.getObject(columnName);
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(columnName, resultType);
                Method writeMethod = propertyDescriptor.getWriteMethod();
                writeMethod.invoke(o, value);
            }
        }
        return list;
    }

    private void setParameters(PreparedStatement preparedStatement,
                               List<ParameterMapping> parameterMappings, Class<?> parameterType, Object param)
            throws NoSuchFieldException, IllegalAccessException, SQLException {
        for (int i = 0; i < parameterMappings.size(); i++) {
            ParameterMapping mapping = parameterMappings.get(i);
            String content = mapping.getContent();
            // reflection
            Object value = getFiledValue(parameterType, param, content);
            preparedStatement.setObject(i+1,  value);
        }
    }

    private Object getFiledValue(Class<?> type, Object target, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private ParsedContent resolvePlaceholders(MappedStatement statement) {
        ParameterMappingTokenHandler tokenHandler = new ParameterMappingTokenHandler();
        GenericTokenParser parser = new GenericTokenParser("#{", "}", tokenHandler);
        String parsedSql = parser.parse(statement.getSql());
        return new ParsedContent(parsedSql, tokenHandler.getParameterMappings());
    }

    private Class<?> getType(String fullName) throws ClassNotFoundException {
        if (fullName != null) {
            return ClassLoader.class.forName(fullName);
        }
        return null;
    }

    private class ParsedContent {

        private String parsedSql;

        private List<ParameterMapping> parameterMappings;

        public ParsedContent(String parsedSql, List<ParameterMapping> parameterMappings) {
            this.parsedSql = parsedSql;
            this.parameterMappings = parameterMappings;
        }

        public String getParsedSql() {
            return parsedSql;
        }

        public void setParsedSql(String parsedSql) {
            this.parsedSql = parsedSql;
        }

        public List<ParameterMapping> getParameterMappings() {
            return parameterMappings;
        }

        public void setParameterMappings(List<ParameterMapping> parameterMappings) {
            this.parameterMappings = parameterMappings;
        }
    }

}
