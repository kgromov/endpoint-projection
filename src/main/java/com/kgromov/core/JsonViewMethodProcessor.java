package com.kgromov.core;

import com.kgromov.mappers.MapperScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.stream.Collectors.toSet;

//@Component
@Aspect
@Slf4j
@RequiredArgsConstructor
public class JsonViewMethodProcessor {
    private final JsonViewScanner jsonViewScanner;
    private final List<MapperScanner> mapperScanners;

    @Pointcut("within(com.kgromov.core.Mapper+)")
    public void mapperMethods() {
    }

    @Around("mapperMethods()")
    public Object jsonFieldsFiltering(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> returnType = method.getReturnType();
        Object source = joinPoint.getArgs()[0];
        if (this.isProxyClass(source.getClass())) {
            return joinPoint.proceed();
        }
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("jsonFieldsFiltering");
        List<StackTraceElement> stackTrace = List.of(Thread.currentThread().getStackTrace());
        Map<String, Set<String>> methodToFieldsProjection = jsonViewScanner.getMethodToFieldsProjection();
        Set<String> fieldsProjection = stackTrace.stream()
                .filter(trace -> methodToFieldsProjection.containsKey(trace.getClassName() + '.' + trace.getMethodName()))
                .findFirst()
                .map(trace -> methodToFieldsProjection.get(trace.getClassName() + '.' + trace.getMethodName()))
                .orElseGet(Collections::emptySet);
        if (!fieldsProjection.isEmpty()) {
            Map<String, Method> fieldsToGetters = this.analyzeType(source.getClass());
            // fetch getters my field names and leave exclusions since it's a bit risky just to mock all others including some business logic
            Set<String> excludedGetters = fieldsToGetters.entrySet()
                    .stream()
                    .filter(entry -> !fieldsProjection.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .map(Method::getName)
                    .collect(toSet());
            Object proxy = this.createProxy(source, excludedGetters);
            stopWatch.stop();
            log.info("Processing project by json view took = {} ms", stopWatch.getLastTaskTimeMillis());
            return joinPoint.proceed(new Object[]{proxy});
        }
        stopWatch.stop();
        return joinPoint.proceed();
    }

    private Map<String, Method> analyzeType(Class<?> type) {
        try {
            Map<String, Method> fieldToGetter = new HashMap<>();
            BeanInfo beanInfo = Introspector.getBeanInfo(type, Object.class);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                String propertyName = propertyDescriptor.getName();
                Method getter = propertyDescriptor.getReadMethod();
                Method setter = propertyDescriptor.getWriteMethod();
//                Field field = type.getDeclaredField(propertyName);
                log.trace("Property " + propertyName);
                log.trace("Getter " + getter);
                log.trace("Setter " + setter);
                fieldToGetter.put(propertyName, getter);
            }
            return fieldToGetter;
        } catch (Exception e) {
            log.error("Can't introspect class = {}", type, e);
            throw new RuntimeException(e);
        }
    }

    private Object createProxy(Object source, Set<String> mockedMethodNames) {
       /* Enhancer enhancer = new Enhancer();
        enhancer.setContextClass(source.getClass());
        enhancer.setSuperclass(source.getClass().getSuperclass());
        enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
            if (mockedMethodNames.contains(method.getName())) {
                return null;
            } else {
                return proxy.invokeSuper(source, args);
            }
        });
        return enhancer.create();*/
        MethodInterceptor handler = (obj, method, args, proxy) -> {
            if (mockedMethodNames.contains(method.getName())) {
                return null;
            }
            return proxy.invoke(source, args);
        };
        return Enhancer.create(source.getClass(), handler);
    }

    private boolean isProxyClass(Class<?> clazz) {
        String classSimpleName = clazz.getSimpleName();
        return classSimpleName.contains("$") || classSimpleName.contains("@");
    }
}
