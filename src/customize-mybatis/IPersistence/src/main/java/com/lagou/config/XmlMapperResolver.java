package com.lagou.config;

import com.lagou.pojo.Configuration;
import com.lagou.pojo.MappedStatement;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.List;

public class XmlMapperResolver {

    private final Configuration configuration;

    public XmlMapperResolver(Configuration configuration) {
        this.configuration = configuration;
    }

    public void resolve(InputStream stream) throws DocumentException {
        Document document = new SAXReader().read(stream);
        Element root = document.getRootElement();
        String namespace = root.attributeValue("namespace");
        List<Element> mapperNodes = root.selectNodes("//statement");
        for (Element node: mapperNodes) {
            MappedStatement statement = resolveStatement(node);
            String statementId = namespace + "." + statement.getId();
            configuration.getMappedStatementMap().put(statementId, statement);
        }
    }

    private MappedStatement resolveStatement(Element node) {
        String id = node.attributeValue("id");
        String resultType = node.attributeValue("resultType");
        String parameterType = node.attributeValue("parameterType");
        String sql = node.getTextTrim();

        MappedStatement statement = new MappedStatement();
        statement.setId(id);
        statement.setResultType(resultType);
        statement.setParameterType(parameterType);
        statement.setSql(sql);

        return statement;
    }

}
