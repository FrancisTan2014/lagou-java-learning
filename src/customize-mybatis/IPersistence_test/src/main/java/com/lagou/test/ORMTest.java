package com.lagou.test;

import com.lagou.dao.IUserRepository;
import com.lagou.io.Resources;
import com.lagou.pojo.User;
import com.lagou.pojo.UserQuery;
import com.lagou.sql.session.SqlSession;
import com.lagou.sql.session.SqlSessionFactory;
import com.lagou.sql.session.SqlSessionFactoryBuilder;
import org.dom4j.DocumentException;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.io.InputStream;

public class ORMTest {

    private IUserRepository getUserRepository() throws PropertyVetoException, DocumentException {
        InputStream stream = Resources.getResourceAsStream("sqlMapConfig.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(stream);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        return sqlSession.getMapper(IUserRepository.class);
    }

    @Test
    public void test()
            throws PropertyVetoException, DocumentException {

        UserQuery query = new UserQuery();
        query.setId(1);
        query.setUsername("francis");

        IUserRepository repository = getUserRepository();
        User user = repository.querySingle(query);
        System.out.println(user);
    }

    @Test
    public void testInsert() throws PropertyVetoException, DocumentException {
        User user = new User();
        user.setUsername("test_insert");

        IUserRepository repository = getUserRepository();
        int rowCount = repository.insert(user);
        System.out.println("Insert success, affectedRows: " + rowCount);
    }

    @Test
    public void testUpdate() throws PropertyVetoException, DocumentException {
        User user = new User();
        user.setId(3);
        user.setUsername("update_test_insert");

        IUserRepository repository = getUserRepository();
        int rowCount = repository.update(user);
        System.out.println("Update success, affectedRows: " + rowCount);

        UserQuery query = new UserQuery();
        query.setId(3);
        query.setUsername("update_test_insert");
        User updatedUser = repository.querySingle(query);
        System.out.println(updatedUser);
    }

    @Test
    public void testDelete() throws PropertyVetoException, DocumentException, ClassNotFoundException {
        IUserRepository repository = getUserRepository();
        boolean success = repository.delete(3);
        System.out.println("Delete result: " + success);
    }

}
