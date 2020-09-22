package com.lagou.ioc;

public class IocConfiguration {

    private String[] packages;
    private BeanDefinition[] beanDefinitions;

    public String[] getPackages() {
        return packages;
    }

    public void setPackages(String[] packages) {
        this.packages = packages;
    }

    public BeanDefinition[] getBeanDefinitions() {
        return beanDefinitions;
    }

    public void setBeanDefinitions(BeanDefinition[] beanDefinitions) {
        this.beanDefinitions = beanDefinitions;
    }

}
