package com.kgromov.mappers;

import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

@Component
@ConditionalOnClass({org.mapstruct.Mapper.class})
public class MapstructScanner implements MapperScanner {
    private final Map<Class<?>,  PropertyDescriptor[]> beanInfoCache = new HashMap<>();

    @Override
    public MappingSettings scan(String basePackage) {
        // TODO: implement
        Reflections reflections = new Reflections(basePackage,
                Scanners.TypesAnnotated,
                Scanners.SubTypes,
                Scanners.MethodsAnnotated,
                Scanners.MethodsSignature
        );
//        ClassUtils.getInterfaceMethodIfPossible()
//        ClassUtils.getMostSpecificMethod()
        Set<Method> mappingMethods = reflections.getMethodsAnnotatedWith(Mapping.class);
        Set<Method> mappingsMethods = reflections.getMethodsAnnotatedWith(Mappings.class);
        Stream.concat(mappingMethods.stream(), mappingsMethods.stream())
                .distinct()
                .filter(MapstructScanner::isSingularType)
                .forEach(method -> {
                    Class<?> sourceType = method.getParameterTypes()[0];
                    Class<?> targetType = method.getReturnType();
                    List<Mapping> mappings = Optional.ofNullable(method.getAnnotation(Mappings.class))
                            .map(Mappings::value)
                            .map(List::of)
                            .orElse(new ArrayList<>());
                    Optional.ofNullable(method.getAnnotation(Mapping.class)).ifPresent(mappings::add);
                    mappings.stream()
                            .filter(mapping -> Objects.nonNull(mapping.source()) && Objects.nonNull(mapping.target()))
                            .forEach(mapping -> {
                                String sourceExpression = mapping.source();
                                String targetProperty = mapping.target();


                            });
                });
        return null;
    }

    private static boolean isSingularType(Method method) {
        Class<?> sourceType = method.getParameterTypes()[0];
        return !sourceType.isArray() && Collection.class.isAssignableFrom(sourceType);
    }


}
