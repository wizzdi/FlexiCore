/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.flexicore.annotations.OperationsInside;
import com.flexicore.annotations.ProtectedREST;
import com.flexicore.data.jsoncontainers.ObjectMapperContextResolver;
import com.flexicore.data.jsoncontainers.Views;
import com.flexicore.init.FlexiCorePluginManager;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.DocumentationTag;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.rest.swagger.CustomResolver;
import com.flexicore.rest.swagger.FlexiCoreOpenApiReader;
import com.flexicore.rest.swagger.TagFilter;
import com.flexicore.security.MD5Calculator;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.BaseclassService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.filter.OpenAPISpecFilter;
import io.swagger.v3.core.filter.SpecFilter;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.api.AbstractOpenApiResource;
import org.springdoc.core.OpenAPIService;
import org.springdoc.core.SpringDocUtils;
import org.springdoc.webmvc.api.OpenApiResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.enterprise.context.RequestScoped;
import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static io.swagger.v3.jaxrs2.integration.ServletConfigContextUtils.getContextIdFromServletConfig;

@Path("/openapi.json")
@ProtectedREST
@RequestScoped
@Component
@OperationsInside
public class SwaggerAPIRESTService extends BaseOpenApiResource implements RESTService {
	@Context
	ServletConfig config;

	@Context
	Application app;

	@Autowired
	private BaseclassService baseclassService;

	@Autowired
	private OpenAPIService openAPIService;
	@Autowired
	@Lazy
	private FlexiCorePluginManager pluginManager;
	@Autowired
	private OpenApiResource openApiResource;

	private static final Logger logger = LoggerFactory.getLogger(SwaggerAPIRESTService.class);

	private static AtomicBoolean init = new AtomicBoolean(false);
	private static Cache<String, String> swaggerCache = CacheBuilder.newBuilder().maximumSize(5).build();

	@GET
	@Path("{authenticationkey}")
	@Produces({MediaType.APPLICATION_JSON})
	@Operation(hidden = true)
	@JsonView(Views.Unrefined.class)
	public Response getOpenApi(@PathParam("authenticationkey") String authKey,
							   @Context HttpHeaders headers,
							   @Context UriInfo uriInfo,
							   @Context SecurityContext securityContext) throws Exception {
		long start = System.currentTimeMillis();
		//  logger.info("Open api request received");
		Set<String> tags = baseclassService.getAllByKeyWordAndCategory(new QueryInformationHolder<>(DocumentationTag.class, securityContext)).parallelStream().map(f -> f.getName()).collect(Collectors.toSet());
		//  logger.info("time taken to get tags " + (System.currentTimeMillis() - start));
		//start = System.currentTimeMillis();
		//String md5 = MD5Calculator.getMD5(tags.parallelStream().collect(Collectors.joining()));
		// logger.info("time taken to calculate Md5 " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		String tagNamesConcat = tags.stream().collect(Collectors.joining(","));
		String md5 = tagNamesConcat.isEmpty() ? "all" : MD5Calculator.getMD5(tagNamesConcat);
		String openApiString = swaggerCache.get(md5, () -> getSecureOpenApi(headers, config, app, uriInfo, tags));
		logger.info("time taken to get openapi json " + (System.currentTimeMillis() - start));

		if (openApiString != null) {
			return buildOpenApiResponse(openApiString);
		}
		return Response.status(404).build();
	}

	protected String getSecureOpenApi(HttpHeaders headers,
									  ServletConfig config,
									  Application app,
									  UriInfo uriInfo, Set<String> tags) throws Exception {
		logger.info("open api read started ");

		long started = System.currentTimeMillis();
		if (init.compareAndSet(false, true)) {
			ObjectMapperContextResolver.configureToDefault(Json.mapper());
			ModelConverters.getInstance().addConverter(new CustomResolver(Json.mapper(), Views.Unrefined.class));


		}


		String ctxId = getContextIdFromServletConfig(config);
		OpenApiContext ctx = new JaxrsOpenApiContextBuilder()
				.servletConfig(config)
				.application(app)
				.resourcePackages(resourcePackages)
				.configLocation(configLocation)
				.openApiConfiguration(openApiConfiguration)
				.ctxId(ctxId)
				.buildContext(true);
		OpenAPI oas = ctx.read();

		SpringDocUtils.getConfig().removeSimpleTypesForParameterObject(SecurityContext.class);

		Method getOpenApi = AbstractOpenApiResource.class.getDeclaredMethod("getOpenApi");
		getOpenApi.setAccessible(true);
		OpenAPI springOpenApi = (OpenAPI) getOpenApi.invoke(openApiResource);
		springOpenApi.getServers().get(0).setUrl("/");
		FlexiCoreOpenApiReader reader = new FlexiCoreOpenApiReader(oas);
		reader.setConfiguration(ctx.getOpenApiConfiguration());
		reader.read(JaxRsActivator.getAll());

		oas = reader.getOpenAPI();
		mergeOas(oas, springOpenApi);
		logger.info("open api read taken " + (System.currentTimeMillis() - started));
		started = System.currentTimeMillis();

		boolean pretty = false;
		if (ctx.getOpenApiConfiguration() != null && Boolean.TRUE.equals(ctx.getOpenApiConfiguration().isPrettyPrint())) {
			pretty = true;
		}

		if (oas != null) {
			try {
				OpenAPISpecFilter filterImpl = new TagFilter(tags);
				SpecFilter f = new SpecFilter();
				oas = f.filter(oas, filterImpl, getQueryParams(uriInfo.getQueryParameters()), getCookies(headers),
						getHeaders(headers));
			} catch (Exception e) {
				logger.error("failed to load filter", e);
			}

		}

		logger.info("open api filter read taken " + (System.currentTimeMillis() - started));

		if (oas == null) {
			return null;
		}
		Map<String, Schema> schemas = oas.getComponents().getSchemas();
		for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
			Schema value = entry.getValue();
			String superName = com.flexicore.service.BaseclassService.getSuperSimple(entry.getKey());
			if (superName != null) {
				superName = getClosestParentSchema(superName, schemas);
				if (superName != null) {
					Map<String, ?> properties = value.getProperties();
					Set<String> childProps = properties != null ? properties.keySet() : new HashSet<>();
					Set<String> parentProps = getAllParentsProperties(superName, schemas);
					childProps.removeAll(parentProps);
					ComposedSchema composedSchema = new ComposedSchema()
							.addAllOfItem(new Schema().$ref(superName))
							.addAllOfItem(value);
					schemas.put(entry.getKey(), composedSchema);

				}
			}


		}
		logger.info("open api Schema read taken " + (System.currentTimeMillis() - started));
		if (!oas.getServers().isEmpty()) {
			Server server = oas.getServers().get(0);
			String url = server.getUrl();
			if (!url.contains("rest")) {
				server.setUrl(url + "/rest");
			}
		}
		return pretty ? Json.pretty(oas) : Json.mapper().writeValueAsString(oas);

	}

	private void mergeOas(OpenAPI main, OpenAPI secondary) {
		for (Map.Entry<String, PathItem> stringPathItemEntry : secondary.getPaths().entrySet()) {
			PathItem value = stringPathItemEntry.getValue();
			if(main.getPaths().putIfAbsent(stringPathItemEntry.getKey(), value)==null){
				value.setServers(secondary.getServers());

			}
		}
		Components components = secondary.getComponents();
		Components mainComponents = main.getComponents();
		main.setComponents(mergeComponents(mainComponents, components));

		Map<String, Tag> existingTags = main.getTags().stream().collect(Collectors.toMap(f -> f.getName(), f -> f, (a, b) -> a));
		for (Tag tag : secondary.getTags()) {
			if (!existingTags.containsKey(tag.getName())) {
				main.getTags().add(tag);
			}
		}
		Map<String, Server> servers = main.getServers().stream().collect(Collectors.toMap(f -> f.getUrl(), f -> f, (a, b) -> a));
		for (Server server : secondary.getServers()) {
			if(!servers.containsKey(server.getUrl())){
				main.getServers().add(server);
			}
		}

	}

	private Components mergeComponents(Components mainComponents, Components components) {
		if (components == null) {
			return mainComponents;
		}
		if (mainComponents == null) {
			return components;
		}

		mainComponents.setResponses(mergeMaps(mainComponents.getResponses(), components.getResponses()));
		mainComponents.setCallbacks(mergeMaps(mainComponents.getCallbacks(), components.getCallbacks()));
		mainComponents.setExamples(mergeMaps(mainComponents.getExamples(), components.getExamples()));
		mainComponents.setSchemas(mergeMaps(mainComponents.getSchemas(), components.getSchemas()));
		mainComponents.setExtensions(mergeMaps(mainComponents.getExtensions(), components.getExtensions()));
		mainComponents.setHeaders(mergeMaps(mainComponents.getHeaders(), components.getHeaders()));
		mainComponents.setLinks(mergeMaps(mainComponents.getLinks(), components.getLinks()));
		mainComponents.setParameters(mergeMaps(mainComponents.getParameters(), components.getParameters()));
		mainComponents.setSecuritySchemes(mergeMaps(mainComponents.getSecuritySchemes(), components.getSecuritySchemes()));
		mainComponents.setRequestBodies(mergeMaps(mainComponents.getRequestBodies(), components.getRequestBodies()));


		return mainComponents;
	}

	private <T> Map<String, T> mergeMaps(Map<String, T> main, Map<String, T> secondary) {
		if (secondary == null) {
			return main;
		}
		if (main == null) {
			return secondary;
		}
		for (Map.Entry<String, T> stringTEntry : secondary.entrySet()) {
			main.putIfAbsent(stringTEntry.getKey(), stringTEntry.getValue());
		}
		return main;

	}

	private String getClosestParentSchema(String superName, Map<String, Schema> schemas) {
		for (String current = superName; current != null; current = com.flexicore.service.BaseclassService.getSuperSimple(current)) {
			Schema schema = schemas.get(current);
			if (schema != null) {
				return current;
			}
		}
		return null;

	}

	private Set<String> getAllParentsProperties(String simpleName, Map<String, Schema> schemaMap) {
		Set<String> properties = new HashSet<>();
		for (String current = simpleName; current != null; current = com.flexicore.service.BaseclassService.getSuperSimple(current)) {
			Schema schema = schemaMap.get(current);
			if (schema != null) {
				properties.addAll(getSchemaProperties(schema));

			}
		}
		return properties;
	}

	private Set<String> getSchemaProperties(Schema parent) {
		return parent instanceof ComposedSchema ? ((ComposedSchema) parent).getAllOf().parallelStream().filter(f -> f.getProperties() != null).map(f -> (Set<String>) f.getProperties().keySet()).flatMap(Set::stream).collect(Collectors.toSet()) : parent != null && parent.getProperties() != null ? parent.getProperties().keySet() : new HashSet<>();
	}

	private Response buildOpenApiResponse(String openApiString) {
		return Response.status(Response.Status.OK)
				.entity(openApiString)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.build();
	}

	private static Map<String, List<String>> getQueryParams(MultivaluedMap<String, String> params) {
		Map<String, List<String>> output = new HashMap<String, List<String>>();
		if (params != null) {
			for (String key : params.keySet()) {
				List<String> values = params.get(key);
				output.put(key, values);
			}
		}
		return output;
	}

	private static Map<String, String> getCookies(HttpHeaders headers) {
		Map<String, String> output = new HashMap<String, String>();
		if (headers != null) {
			for (String key : headers.getCookies().keySet()) {
				Cookie cookie = headers.getCookies().get(key);
				output.put(key, cookie.getValue());
			}
		}
		return output;
	}

	private static Map<String, List<String>> getHeaders(HttpHeaders headers) {
		Map<String, List<String>> output = new HashMap<String, List<String>>();
		if (headers != null) {
			for (String key : headers.getRequestHeaders().keySet()) {
				List<String> values = headers.getRequestHeaders().get(key);
				output.put(key, values);
			}
		}
		return output;
	}
}
