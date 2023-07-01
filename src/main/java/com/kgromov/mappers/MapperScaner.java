package com.kgromov.mappers;

@FunctionalInterface
public interface MapperScaner {
    // TODO: rename/define mapping settings as return type
    MappingSettings scan();
}
