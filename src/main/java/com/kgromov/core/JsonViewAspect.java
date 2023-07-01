package com.kgromov.core;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toSet;

//@Component
//@Aspect
@Slf4j
public class JsonViewAspect {
    private Map<String, Set<String>> methodToFieldsProjection = new ConcurrentHashMap<>();

    @Pointcut("@annotation(com.fasterxml.jackson.annotation.JsonView)")
    public void jsonViewEndpoints() {
    }

    @Around("jsonViewEndpoints()")
    public Object jsonFieldsFiltering(ProceedingJoinPoint joinPoint) throws Throwable {
        Object target = joinPoint.getTarget();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> returnType = signature.getReturnType();
        if (returnType.isAssignableFrom(Void.class)) {
            return joinPoint.proceed();
        }
        if (returnType.isArray()) {
            returnType = returnType.getComponentType();
        } else if (Collection.class.isAssignableFrom(returnType)) {
            Method method = signature.getMethod();
            returnType = (Class<?>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        }
        JsonView targetView = signature.getMethod().getAnnotation(JsonView.class);
        Set<Class<?>> targetViewClasses = Set.of(targetView.value());

//        AnnotatedType annotatedSuperclass = returnType.getAnnotatedSuperclass();
//        Class<?> declaringType = signature.getDeclaringType();
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
        String methodKey = joinPoint.getTarget().getClass().getName() + '.' + signature.getMethod().getName();
        this.methodToFieldsProjection.put(methodKey, fieldNamesToReturn);
        log.info("Class = {}, method = {}: collected fields for projection = {}",
                joinPoint.getTarget().getClass(), signature.getMethod().getName(), fieldNamesToReturn);
        return joinPoint.proceed();
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
