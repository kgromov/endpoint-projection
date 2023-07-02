package com.kgromov;

import com.kgromov.core.ProjectionConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ProjectionConfig.class)
public @interface EnableProjectionProcessing {
}
