package com.lagou.ioc;

public interface BeanFactory {

    Object getBean(String name);

    Object getBean(Class<?> type);

}
