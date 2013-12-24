package org.adorsys.forge.plugins.description;

import java.io.FileNotFoundException;

import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.JavaSource;
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
}
