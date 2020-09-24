package com.lagou.ioc;

public interface BeanFactory {

    Object getBean(String name);

    <T> T getBean(Class<T> type);

}
