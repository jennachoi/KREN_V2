package com.kren.common;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the React build from {@code classpath:/static} (copied there by the frontend build) on the
 * same origin as the API, so the session cookie and CSRF cookie just work with no CORS.
 * Paths that aren't a real file fall back to index.html so client-side routes like /inbox survive a
 * page refresh. Controllers are matched before this handler, and /api paths never fall back, so an
 * unknown API URL still gets a JSON 404 rather than HTML.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
			.addResourceLocations("classpath:/static/")
			.resourceChain(true)
			.addResolver(new PathResourceResolver() {
				@Override
				protected Resource getResource(String resourcePath, Resource location) throws IOException {
					Resource requested = location.createRelative(resourcePath);
					if (requested.exists() && requested.isReadable()) {
						return requested;
					}
					if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")) {
						return null;
					}
					Resource index = new ClassPathResource("/static/index.html");
					return index.exists() ? index : null;
				}
			});
	}

}
