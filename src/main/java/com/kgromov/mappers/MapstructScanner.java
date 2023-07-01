package com.kgromov.mappers;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Set;

@Component
@ConditionalOnClass({org.mapstruct.Mapper.class})
public class MapstructScanner implements MapperScaner {

    @Override
    public MappingSettings scan() {
        // TODO: implement
        Reflections reflections = new Reflections("com.kgromov",
                Scanners.TypesAnnotated,
                Scanners.SubTypes,
                Scanners.MethodsAnnotated,
                Scanners.MethodsSignature
        );
//        ClassUtils.getInterfaceMethodIfPossible()
//        ClassUtils.getMostSpecificMethod()
        Set<Method> methods = reflections.getMethodsAnnotatedWith(org.mapstruct.Mapping.class);
        return null;
    }
}
