package com.flexicore.rest.swagger;

import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.core.model.ApiDescription;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.*;
import java.util.stream.Collectors;

public class TagFilter extends AbstractSpecFilter {

    private Set<String> accessibleTags;

    public TagFilter(Set<String> accessibleTags) {
        this.accessibleTags = accessibleTags;
    }

    @Override
    public Optional<Operation> filterOperation(Operation operation, ApiDescription api, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        List<String> names=operation.getTags()!=null?operation.getTags().parallelStream().filter(f->accessibleTags.contains(f)).collect(Collectors.toList()):new ArrayList<>();
        return names.isEmpty()?Optional.empty():Optional.of(operation);
    }

    @Override
    public Optional<ApiResponse> filterResponse(ApiResponse response, Operation operation, ApiDescription api, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        return super.filterResponse(response, operation, api, params, cookies, headers);
    }

    @Override
    public Optional<Schema> filterSchemaProperty(Schema property, Schema schema, String propName, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        return super.filterSchemaProperty(property, schema, propName, params, cookies, headers);
    }

    @Override
    public Optional<OpenAPI> filterOpenAPI(OpenAPI openAPI, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {

        return openAPI.getTags().isEmpty()?Optional.empty(): super.filterOpenAPI(openAPI, params, cookies, headers);
    }

    @Override
    public Optional<PathItem> filterPathItem(PathItem pathItem, ApiDescription api, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        return super.filterPathItem(pathItem, api, params, cookies, headers);
    }

    @Override
    public Optional<Parameter> filterParameter(Parameter parameter, Operation operation, ApiDescription api, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        return super.filterParameter(parameter, operation, api, params, cookies, headers);
    }

    @Override
    public Optional<RequestBody> filterRequestBody(RequestBody requestBody, Operation operation, ApiDescription api, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        return super.filterRequestBody(requestBody, operation, api, params, cookies, headers);
    }

    @Override
    public Optional<Schema> filterSchema(Schema schema, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        return super.filterSchema(schema, params, cookies, headers);
    }


}
