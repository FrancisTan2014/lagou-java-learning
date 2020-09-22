package com.lagou.ioc;

import java.util.Arrays;
import java.util.Objects;

public class BeanDefinition {

    private String id;
    private String klass;
    private String initMethod;
    private String destroyMethod;
    private Property[] properties;
    private Lifetime lifetime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKlass() {
        return klass;
    }

    public void setKlass(String klass) {
        this.klass = klass;
    }

    public String getInitMethod() {
        return initMethod;
    }

    public void setInitMethod(String initMethod) {
        this.initMethod = initMethod;
    }

    public String getDestroyMethod() {
        return destroyMethod;
    }

    public void setDestroyMethod(String destroyMethod) {
        this.destroyMethod = destroyMethod;
    }

    public Property[] getProperties() {
        return properties;
    }

    public void setProperties(Property[] properties) {
        this.properties = properties;
    }

    public Lifetime getLifetime() {
        return lifetime;
    }

    public void setLifetime(Lifetime lifetime) {
        this.lifetime = lifetime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeanDefinition that = (BeanDefinition) o;
        return Objects.equals(klass, that.klass);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, klass, initMethod, destroyMethod, lifetime);
        result = 31 * result + Arrays.hashCode(properties);
        return result;
    }
}
