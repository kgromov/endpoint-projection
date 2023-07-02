package com.kgromov.core;

import com.fasterxml.jackson.annotation.JsonView;
//import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Slf4j
//@Component
public class JsonViewScanner {
    private final Map<String, Set<String>> methodToFieldsProjection = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("JsonViewPostProcessor");
        Package[] packages = Thread.currentThread().getContextClassLoader().getDefinedPackages();
        String basePackage = Stream.of(packages).map(Package::getName).sorted(String::compareToIgnoreCase).collect(toList()).get(0);
        log.info("Base package = {}", basePackage);
        Reflections reflections = new Reflections(basePackage,
                Scanners.TypesAnnotated,
                Scanners.SubTypes,
                Scanners.MethodsAnnotated,
                Scanners.MethodsSignature
        );
        Set<Method> methods = reflections.getMethodsAnnotatedWith(com.fasterxml.jackson.annotation.JsonView.class);
        methods.stream()
                .filter(method -> !method.getReturnType().equals(Void.class))
                .forEach(method -> {
                    Class<?> returnType = method.getReturnType();
                    if (returnType.isArray()) {
                        returnType = returnType.getComponentType();
                    } else if (Collection.class.isAssignableFrom(returnType)) {
                        returnType = (Class<?>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
                    }
                    JsonView targetView = method.getAnnotation(JsonView.class);
                    Set<Class<?>> targetViewClasses = Set.of(targetView.value());

                    Set<Class<?>> returnTypeViewClasses = this.resolveViewClassesForType(returnType);
                    // as 1st step only declared fields; later on - add recursive bypass
                    Map<String, Set<Class<?>>> fieldViewClasses = this.getAllFields(returnType)
                            .stream()
                            .map(field -> Pair.of(field.getName(), this.resolveViewClassesFromField(field)))
                            .map(fieldViews -> Pair.of(fieldViews.getKey(), fieldViews.getValue().isEmpty() ? returnTypeViewClasses : fieldViews.getValue()))
                            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
                    Set<String> fieldNamesToReturn = fieldViewClasses.entrySet()
                            .stream()
                            .filter(entry -> entry.getValue().stream().anyMatch(targetViewClasses::contains))
                            .map(Map.Entry::getKey)
                            .collect(toSet());
                    String methodKey = method.getDeclaringClass().getName() + '.' + method.getName();
                    this.methodToFieldsProjection.put(methodKey, fieldNamesToReturn);
                    log.debug("Class = {}, method = {}: collected fields for projection = {}",
                            method.getClass(), method.getName(), fieldNamesToReturn);
                });
        stopWatch.stop();
        log.info("Processing JsonView methods took = {} ms", stopWatch.getLastTaskTimeMillis());
    }

    private <T> Set<Class<?>> resolveViewClassesForType(Class<T> type) {
        if (type.equals(Object.class)) {
            return emptySet();
        }
        JsonView fieldView = type.getAnnotation(JsonView.class);
        return fieldView != null ? Set.of(fieldView.value()) : this.resolveViewClassesForType(type.getSuperclass());
    }

    private Set<Class<?>> resolveViewClassesFromField(Field field) {
        JsonView fieldView = field.getAnnotation(JsonView.class);
        return fieldView == null ? emptySet() : Set.of(fieldView.value());
    }

    private Set<Field> getAllFields(Class<?> type) {
        Set<Field> fields = new HashSet<>(Set.of(type.getDeclaredFields()));
        Class<?> superclass = type.getSuperclass();
        while (!superclass.equals(Object.class)) {
            fields.addAll(Set.of(superclass.getDeclaredFields()));
            superclass = superclass.getSuperclass();
        }
        return fields;
    }

    public Map<String, Set<String>> getMethodToFieldsProjection() {
        return unmodifiableMap(methodToFieldsProjection);
    }
}
