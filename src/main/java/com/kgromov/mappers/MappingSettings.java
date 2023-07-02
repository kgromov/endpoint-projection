package com.kgromov.mappers;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MappingSettings {
    private Class<?> targetType;
    private List<Class<?>> sourceTypes;
    private String fieldName;
    private String sourceExpression;
}
