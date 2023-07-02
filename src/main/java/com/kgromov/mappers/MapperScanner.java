package com.kgromov.mappers;

@FunctionalInterface
public interface MapperScanner {
    // TODO: rename/define mapping settings as return type
    MappingSettings scan(String basePackage);
}
