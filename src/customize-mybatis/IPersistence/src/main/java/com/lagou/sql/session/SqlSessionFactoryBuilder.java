package com.lagou.sql.session;

import com.lagou.config.XmlConfigResolver;
import com.lagou.pojo.Configuration;
import org.dom4j.DocumentException;

import java.beans.PropertyVetoException;
import java.io.InputStream;

public class SqlSessionFactoryBuilder {

    public SqlSessionFactory build(InputStream in) throws PropertyVetoException, DocumentException {
        XmlConfigResolver resolver = new XmlConfigResolver();
        Configuration configuration = resolver.resolve(in);
        return new DefaultSqlSessionFactory(configuration);
    }

}
