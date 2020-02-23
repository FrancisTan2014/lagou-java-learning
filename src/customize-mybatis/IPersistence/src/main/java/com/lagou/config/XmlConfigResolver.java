package com.lagou.config;

import com.lagou.io.Resources;
import com.lagou.pojo.Configuration;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class XmlConfigResolver {

    private Configuration configuration;

    public XmlConfigResolver() {
        this.configuration = new Configuration();
    }

    public Configuration resolve(InputStream in) throws DocumentException, PropertyVetoException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(in);
        Element root = document.getRootElement();
        DataSource dataSource = resolveDataSource(root);
        configuration.setDataSource(dataSource);
        resolveMappers(root);
        return configuration;
    }

    private void resolveMappers(Element root) throws DocumentException {
        List<Element> mapperNodes = root.selectNodes("//mapper");
        for (Element node: mapperNodes) {
            String resource = node.attributeValue("resource");
            InputStream stream = Resources.getResourceAsStream(resource);
            XmlMapperResolver resolver = new XmlMapperResolver(configuration);
            resolver.resolve(stream);
        }
    }

    private DataSource resolveDataSource(Element root) throws PropertyVetoException {
        List<Element> nodes = root.selectNodes("//property");
        Properties properties = new Properties();
        for (Element node: nodes) {
            String name = node.attributeValue("name");
            String value = node.attributeValue("value");
            properties.setProperty(name, value);
        }

        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass(properties.getProperty("driverClass"));
        dataSource.setJdbcUrl(properties.getProperty("jdbcUrl"));
        dataSource.setUser(properties.getProperty("username"));
        dataSource.setPassword(properties.getProperty("password"));
        return dataSource;
    }
}
