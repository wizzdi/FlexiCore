package com.flexicore.rest.swagger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexicore.data.jsoncontainers.Views;
import com.flexicore.model.Baseclass;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.AUTO;

public class CustomResolver extends ModelResolver {
    private Class<? extends Views.Unrefined> view;

    public CustomResolver(ObjectMapper mapper, Class<? extends Views.Unrefined> view) {
        super(mapper);
        this.view = view;
    }

    @Override
    public Schema resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> next) {

        Annotation[] ctxAnnotations = annotatedType.getCtxAnnotations();
        if (ctxAnnotations != null) {
            boolean ignore = Arrays.stream(ctxAnnotations).anyMatch(f -> jsonViewIgnore(f) || f instanceof JsonIgnore);
            if (ignore) {
                return null;
            }


        }
        annotatedType.skipJsonIdentity(true);
        return super.resolve(annotatedType, context, next);

    }







    private boolean jsonViewIgnore(Annotation f) {
        if (f instanceof JsonView) {
            for (Class<?> aClass : ((JsonView) f).value()) {
                if (!view.equals(aClass) && view.isAssignableFrom(aClass)) {
                    return true;
                }
            }
        }
        return false;
    }






}
