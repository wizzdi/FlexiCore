package com.wizzdi.flexicore.init;

import com.oembedler.moon.graphql.boot.SchemaStringProvider;
import com.wizzdi.flexicore.boot.base.init.FlexiCorePluginManager;
import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.pf4j.Extension;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
public class FlexiCoreStringProviderImpl implements SchemaStringProvider {

	@Lazy
	@Autowired
	private FlexiCorePluginManager pluginManager;
	@Value("${graphql.tools.schemaLocationPattern:**/*.graphqls}")
	private String schemaLocationPattern;

	@Override
	public List<String> schemaStrings() throws IOException {
		return pluginManager.getStartedPlugins().stream()
				.map(f->pluginManager.getApplicationContext(f))
				.map(this::getResourcesOrNull).filter(Objects::nonNull)
				.flatMap(Arrays::stream)
				.map(this::readSchema)
				.collect(Collectors.toList());

	}

	private Resource[] getResourcesOrNull(org.springframework.context.ApplicationContext f) {
		try {
			return f.getResources("classpath*:" + schemaLocationPattern);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String readSchema(Resource resource) {
		StringWriter writer = new StringWriter();
		try (InputStream inputStream = resource.getInputStream()) {
			IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read graphql schema from resource " + resource, e);
		}
		return writer.toString();
	}
}
