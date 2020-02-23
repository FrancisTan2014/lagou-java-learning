package com.lagou.test;

import com.lagou.io.Resources;
import com.lagou.pojo.User;
import com.lagou.pojo.UserQuery;
import com.lagou.sql.session.SqlSession;
import com.lagou.sql.session.SqlSessionFactory;
import com.lagou.sql.session.SqlSessionFactoryBuilder;
import org.dom4j.DocumentException;

import java.beans.IntrospectionException;
import java.beans.PropertyVetoException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class ORMTest {

    public static void main(String[] args)
            throws PropertyVetoException, DocumentException, IllegalAccessException, ClassNotFoundException, IntrospectionException, InstantiationException, SQLException, InvocationTargetException, NoSuchFieldException {
        InputStream stream = Resources.getResourceAsStream("sqlMapConfig.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(stream);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        UserQuery query = new UserQuery();
        query.setId(1);
        query.setUsername("francis");
        User user = sqlSession.querySingle("User.querySingle", query);
        System.out.println(user);
    }

}
