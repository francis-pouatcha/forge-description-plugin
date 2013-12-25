package org.adorsys.forge.plugins.description;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.adorsys.javaext.description.Description;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.resources.PropertiesFileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaResource;

public final class DescriptionPluginUtils {

	public static final JavaClassOrInterface inspectResource(
			Resource<?> resource) {
		if (resource == null) {
			throw new IllegalArgumentException(
					"The current resource can not be null");
		}
		if (!(resource instanceof JavaResource)) {
			throw new IllegalArgumentException("The given resource '"
					+ resource.getName() + "' is not a Java resource");
		}

		final JavaResource javaResource = (JavaResource) resource;

		JavaSource<?> javaSource;
		try {
			javaSource = javaResource.getJavaSource();
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("The given resource '"
					+ resource.getName()
					+ "' has be deleted from the file system.");
		}

		if (javaSource.isClass()) {
			return new JavaClassOrInterface((JavaClass) javaSource);
		} else if (javaSource.isInterface()) {
			return new JavaClassOrInterface((JavaInterface) javaSource);
		} else {
			throw new IllegalArgumentException("The given resource '"
					+ resource.getName()
					+ "' is not a class or an interface");
		}
	}

	public static PropertiesFileResource loadDescriptionPropertiesFileResource(Project project) throws IOException{
		ResourceFacet resourceFacet = project.getFacet(ResourceFacet.class);
		PropertiesFileResource descriptionProperties = (PropertiesFileResource) resourceFacet.getResource("description.properties");
		if(!descriptionProperties.exists()) descriptionProperties.createNewFile();
		return descriptionProperties ;


	}

}
