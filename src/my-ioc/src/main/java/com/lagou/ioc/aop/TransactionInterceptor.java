package com.lagou.ioc.aop;

import com.lagou.ioc.Autowired;
import com.lagou.ioc.Service;
import com.lagou.utils.ConnectionUtils;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.sql.Connection;

@Service
public class TransactionInterceptor implements MethodInterceptor {

    @Autowired
    private ConnectionUtils connectionUtils;

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        // Make sure that only methods annotated with Transactional
        // will be proxied by this interceptor. Of course it makes
        // this class coupled with the Transactional annotation,
        // but we just attempt to make the sample easier.
        Transactional annotation = method.getAnnotation(Transactional.class);
        if (annotation == null) {
            annotation = o.getClass().getAnnotation(Transactional.class);
        }
        if (annotation == null) {
            return method.invoke(o, objects);
        }

        Connection connection = connectionUtils.getSqlConnection();
        boolean autoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            Object result = method.invoke(o, objects);
            connection.commit();
            return result;
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

}
