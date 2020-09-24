package com.lagou.ioc.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.lagou.ioc.*;
import com.lagou.ioc.aop.TransactionInterceptor;
import com.lagou.ioc.aop.Transactional;
import com.lagou.utils.CollectionUtils;
import net.sf.cglib.proxy.Enhancer;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppContextBeanFactory implements BeanFactory {

    private final ConcurrentHashMap<String, Object> beans = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, String> beanTypeAndNamesMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CreationState> objectCreationStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Unfilled>> unfilledFields = new ConcurrentHashMap<>();

    private final long PARALLELISM_THRESHOLD = 1000L;

    private IocConfiguration configuration;
    private Properties properties;

    @Override
    public Object getBean(String name) {
        return beans.get(name);
    }

    @Override
    public Object getBean(Class<?> type) {
        String beanName = getOrAddTypeAndBeanNameMap(type);
        return beans.get(beanName);
    }

    public AppContextBeanFactory() throws IOException, DocumentException,
            ClassNotFoundException, IllegalAccessException, InstantiationException {
        this("app-context.xml", "settings.properties");
    }

    public AppContextBeanFactory(String configXmlFile, String... propertyFiles)
            throws IOException, DocumentException, ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        // 1. read configuration files
        // 2. scan annotated services(@Service)
        resolveConfiguration(configXmlFile, propertyFiles);

        // 3. create beans by bean definition
        createBeans();
    }

    private void resolveConfiguration(String configXmlFile, String... propertyFiles)
            throws IOException, DocumentException {
        resolveProperties(propertyFiles);
        resolveAppContext(configXmlFile);
    }

    private void resolveAppContext(String configXmlFile) throws DocumentException, FileNotFoundException {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(configXmlFile);
        if (stream == null) {
            throw new FileNotFoundException(configXmlFile);
        }

        SAXReader reader = new SAXReader();
        Document document = reader.read(stream);
        Element root = document.getRootElement();
        IocConfiguration configuration = new IocConfiguration();

        String[] packages = resolvePackages(root);
        configuration.setPackages(packages);

        BeanDefinition[] annotatedBeanDefinitions = scanForAnnotatedBeans(packages);
        BeanDefinition[] beanDefinitions = resolveBeanDefinitions(root);
        BeanDefinition[] allDefinitions = mergeBeanDefinitions(beanDefinitions, annotatedBeanDefinitions);
        configuration.setBeanDefinitions(allDefinitions);

        this.configuration = configuration;
    }

    private String[] resolvePackages(Element root) {
        List<Node> packageNodes = root.selectNodes("packages/package");
        ArrayList<String> packages = new ArrayList<>();
        for (Node node: packageNodes) {
            packages.add(node.getStringValue());
        }

        return Iterables.toArray(packages, String.class);
    }

    private BeanDefinition[] mergeBeanDefinitions(BeanDefinition[] xmlBeanDefinitions
            , BeanDefinition[] annotatedBeanDefinitions) {
        ArrayList<BeanDefinition> result = new ArrayList<>();
        Collections.addAll(result, xmlBeanDefinitions);

        for (BeanDefinition definition: annotatedBeanDefinitions) {
            // If encounters the same bean definition(judge by the class name),
            // we take the bean definition from the xml configuration.
            boolean exists = CollectionUtils.contains(result, d -> d.equals(definition));
            if (exists == false) {
                result.add(definition);
            }
        }

        return Iterables.toArray(result, BeanDefinition.class);
    }

    private BeanDefinition[] scanForAnnotatedBeans(String[] packages) {
        ArrayList<BeanDefinition> result = new ArrayList<>();
        for (String item: packages) {
            Reflections reflections = new Reflections(item);
            Set<Class<?>> services = reflections.getTypesAnnotatedWith(Service.class);
            for (Class<?> klass: services) {
                Service annotation = klass.getAnnotation(Service.class);
                String id = Strings.isNullOrEmpty(annotation.name())
                        ? klass.getTypeName()
                        : annotation.name();

                BeanDefinition definition = new BeanDefinition();
                definition.setId(id);
                definition.setLifetime(annotation.lifetime());
                definition.setKlass(klass.getTypeName());

                result.add(definition);
            }
        }

        return Iterables.toArray(result, BeanDefinition.class);
    }

    private BeanDefinition[] resolveBeanDefinitions(Element root) {
        List<Node> beanNodes = root.selectNodes("beans/bean");
        ArrayList<BeanDefinition> definitions = new ArrayList<>();
        for (Node node: beanNodes) {
            BeanDefinition definition = resolveBeanDefinition(node);
            definitions.add(definition);
        }

        return Iterables.toArray(definitions, BeanDefinition.class);
    }

    private BeanDefinition resolveBeanDefinition(Node beanNode) {
        String id = beanNode.valueOf("@id");
        String klass = beanNode.valueOf("@class");
        String initMethod = beanNode.valueOf("@init-method");
        String destroyMethod = beanNode.valueOf("@destroy-method");
        Property[] properties = resolveBeanProperties(beanNode);
        String lifetime = beanNode.valueOf("@lifetime");
        if (lifetime == null) {
            lifetime = "singleton";
        }

        BeanDefinition definition = new BeanDefinition();
        definition.setId(id);
        definition.setKlass(klass);
        definition.setInitMethod(initMethod);
        definition.setDestroyMethod(destroyMethod);
        definition.setProperties(properties);
        switch (lifetime.toLowerCase()) {
            case "transient": definition.setLifetime(Lifetime.Transient); break;
            case "scoped": definition.setLifetime(Lifetime.Scoped); break;
            case "singleton":
            default:
                definition.setLifetime(Lifetime.Singleton);
                break;
        }

        return definition;
    }

    private String replacePropertyValue(String value) {
        String regex = "\\$\\{(\\w+)\\}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        if (false == matcher.find()) {
            return value;
        } else {
            String propertyName = matcher.group(1);
            return this.properties.getProperty(propertyName);
        }
    }

    private Property[] resolveBeanProperties(Node beanNode) {
        List<Node> nodes = beanNode.selectNodes("property");
        ArrayList<Property> list = new ArrayList<>();
        for (Node node: nodes) {
            String name = node.valueOf("@name");
            String value = replacePropertyValue(node.valueOf("@value"));
            String ref = node.valueOf("@ref");

            Property property = new Property();
            property.setName(name);
            property.setValue(value);
            property.setRef(ref);

            list.add(property);
        }

        return Iterables.toArray(list, Property.class);
    }

    private void resolveProperties(String... propertyFiles) throws IOException {
        Properties properties = new Properties();
        for (String file: propertyFiles) {
            InputStream stream = this.getClass().getClassLoader().getResourceAsStream(file);
            if (stream == null) {
                continue;
            }
            properties.load(stream);
        }

        this.properties = properties;
    }

    private void createBeans()
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        // create type name and bean name map for querying in the following steps
        createTypeNameAndBeanNameMap();

        // Create instances for every bean definition and put to the table
        for (BeanDefinition definition: this.configuration.getBeanDefinitions()) {
            createBean(definition);
        }

        // A simple way to avoid the cycle-reference
        dealWithUnfilledFields();
    }

    private void dealWithUnfilledFields() {
        unfilledFields.forEachValue(PARALLELISM_THRESHOLD, set -> {
            for (Unfilled unfilled: set) {
                try {
                    Object bean = this.getBeanInternal(unfilled.field.getType());
                    FieldUtils.writeField(unfilled.field, unfilled.instance, bean, true);
                } catch (Exception e) {
                    // ignored
                }
            }
        });
    }

    private void createBean(BeanDefinition definition)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        setBeanCreationState(definition.getId(), CreationState.Creating);

        Class<?> type = Class.forName(definition.getKlass());
        // To make the sample easier, we don't consider the situation
        // that the constructor has parameters. And also we don't
        // consider the cycle-reference situations.
        Object instance = createInstance(type);

        // put the bean to the map
        beans.put(definition.getId(), instance);

        // set properties
        setProperties(type, instance, definition.getProperties());

        // set Autowired members
        setAutowiredMembers(type, instance);

        // invoke the init-method if exists
        invokeNoParamsMethod(type, instance, definition.getInitMethod());

        setBeanCreationState(definition.getId(), CreationState.Created);
    }

    private void setBeanCreationState(String beanName, CreationState state) {
        CreationState currentState = objectCreationStates.get(beanName);
        if (false == objectCreationStates.containsKey(beanName)) {
            objectCreationStates.putIfAbsent(beanName, state);
        } else {
            objectCreationStates.put(beanName, state);
        }
    }

    private boolean beanIsCreating(String beanName) {
        CreationState creationState = objectCreationStates.get(beanName);
        if (creationState == null) {
            setBeanCreationState(beanName, CreationState.NotCreated);
            return false;
        }
        return creationState == CreationState.Creating;
    }

    private void addUnfilledFiled(String beanName, Object instance, Field field) {
        Set<Unfilled> fields = unfilledFields.get(beanName);
        if (fields == null) {
            fields = new HashSet<>();
        }
        boolean exists = CollectionUtils.contains(fields, f -> f.instance == instance);
        if (false == exists) {
            fields.add(new Unfilled(instance, field));
        }
    }

    private Object createInstance(Class<?> type) throws IllegalAccessException,
            ClassNotFoundException, InstantiationException {
        if (false == typeNeedIntercepter(type)) {
            // To make the sample easier, we just consider the default constructor.
            return type.newInstance();
        }

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(type);

        // Prepare callbacks
        // To make the sample easier, we don't consider customized
        // interceptors here.
        TransactionInterceptor interceptor = (TransactionInterceptor)
                this.getBeanInternal(TransactionInterceptor.class);
        enhancer.setCallback(interceptor);

        return enhancer.create();
    }

    private boolean typeNeedIntercepter(Class<?> type) {
        List<Annotation> annotations = new ArrayList<>();
        annotations.addAll(Arrays.asList(type.getAnnotations()));
        for (Method method: type.getDeclaredMethods()) {
            annotations.addAll(Arrays.asList(method.getAnnotations()));
        }

        // To make the sample easier
        for (Annotation annotation: annotations) {
            if (annotation instanceof Transactional) {
                return true;
            }
        }

        return false;
    }

    private Object getBeanInternal(Class<?> type) throws IllegalAccessException,
            InstantiationException, ClassNotFoundException {
        String beanName = getOrAddTypeAndBeanNameMap(type);
        return getBeanInternal(beanName);
    }

    private Object getBeanInternal(String beanName) throws IllegalAccessException,
            InstantiationException, ClassNotFoundException {
        Object bean = beans.get(beanName);
        if (bean == null) {
            BeanDefinition definition = CollectionUtils.getSingleOrDefault(
                    Arrays.asList(this.configuration.getBeanDefinitions().clone()),
                    d -> d.getId().equals(beanName)
            );
            // TODO: add the new bean definition to the list

            this.createBean(definition);
            bean = beans.get(beanName);
        }

        return bean;
    }

    private void createTypeNameAndBeanNameMap() throws ClassNotFoundException {
        for (BeanDefinition definition: this.configuration.getBeanDefinitions()) {
            Class<?> type = Class.forName(definition.getKlass());
            String beanName = definition.getId();

            // dealing with repeated type
            if (false == beanTypeAndNamesMap.containsKey(type)) {
                beanTypeAndNamesMap.put(type, beanName);
            }
        }
    }

    private String getOrAddTypeAndBeanNameMap(Class<?> type) {
        AtomicReference<String> beanName = new AtomicReference<>(beanTypeAndNamesMap.get(type));
        if (beanName.get() != null) {
            return beanName.get();
        }

        // Iterate all the keys of beanTypeAndNamesMap to find
        // out if there is a type which inherited from current type.
        beanTypeAndNamesMap.forEachKey(PARALLELISM_THRESHOLD, k -> {
            if (type.isAssignableFrom(k)) {
                String name = beanTypeAndNamesMap.get(k);
                beanName.set(name);
                // put the new type to the map for reducing the next search
                beanTypeAndNamesMap.put(type, name);
            }
        });

        if (Strings.isNullOrEmpty(beanName.get())) {
            // TODO: dealing with the situation that sub type cannot be found
        }

        return beanName.get();
    }

    private void setAutowiredMembers(Class<?> type, Object instance)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        Set<Field> autowiredFields = ReflectionUtils.getAllFields(
                type, f -> f.isAnnotationPresent(Autowired.class));

        // To make the sample easier, we just handle the private fields here.
        for (Field field: autowiredFields) {
            String beanName = getOrAddTypeAndBeanNameMap(field.getType());
            if (beanIsCreating(beanName)) {
                // cycle-reference detected
                addUnfilledFiled(beanName, instance, field);
                continue;
            }

            Object bean = getBeanInternal(beanName);
            FieldUtils.writeField(field, instance, bean, true);
        }
    }

    private void invokeNoParamsMethod(Class<?> type, Object instance, String methodName) {
        // invoke the init-method if exists
        if (false == Strings.isNullOrEmpty(methodName)) {
            try {
                Method method = type.getMethod(methodName);
                method.invoke(instance);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                // To make the sample easier, just ignore the exceptions here.
            }
        }
    }

    private void setProperties(Class<?> type, Object instance, Property[] properties)
            throws InstantiationException, ClassNotFoundException {
        if (properties != null) {
            for (Property prop: properties) {
                try {
                    Field field = FieldUtils.getField(type, prop.getName(), true);
                    if (field == null) {
                        continue;
                    }

                    Object value;
                    String ref = prop.getRef();
                    if (false == Strings.isNullOrEmpty(ref)) {
                        if (beanIsCreating(ref)) {
                            // cycle-reference detected
                            addUnfilledFiled(ref, instance, field);
                            continue;
                        }
                        value = getBeanInternal(ref);
                    } else {
                        value = parse(prop.getValue(), field.getType());
                    }

                    FieldUtils.writeField(field, instance, value, true);
                } catch (IllegalAccessException e) {
                    // To make the sample easier, we ignored the exceptions here.
                }
            }
        }
    }

    private Object parse(String value, Class<?> type) {
        Object result = null;
        if (type.equals(String.class)) {
            result = value;
        } else if (type.equals(int.class) || type.equals(Integer.class)) {
            result = Integer.parseInt(value);
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            result = Long.parseLong(value);
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            result = Boolean.parseBoolean(value);
        } else {
            // To make the sample easier, we ignore the other situations.
        }

        return result;
    }

    private enum CreationState {
        NotCreated, Creating, Created
    }

    private class Unfilled {
        private Object instance;
        private Field field;

        public Unfilled(Object instance, Field field) {
            this.instance = instance;
            this.field = field;
        }

        public Object getInstance() {
            return instance;
        }

        public Field getField() {
            return field;
        }
    }

}
