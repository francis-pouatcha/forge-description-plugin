package org.adorsys.forge.plugins.display;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Version;

import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.completer.SimpleTokenCompleter;
import org.jboss.forge.shell.util.ResourceUtil;

public class NestedPropertyCompleter extends SimpleTokenCompleter {
	private final Shell shell;
	   @Inject 
	   private Project project;

	@Inject
	public NestedPropertyCompleter(Shell shell) {
		this.shell = shell;
	}

	@Override
	public List<String> getCompletionTokens() {
		final List<String> tokens = new ArrayList<String>();
		final Resource<?> currentResource = shell.getCurrentResource();

		try {

			final JavaClass javaClass = ResourceUtil
					.getJavaClassFromResource(currentResource);
			for (Field<JavaClass> field : javaClass.getFields()) {
				if(field.hasAnnotation(Id.class))continue;
				if(field.hasAnnotation(Version.class)) continue;
				if(field.hasAnnotation(OneToOne.class) || field.hasAnnotation(ManyToOne.class)){
					/*
					 * For relationship fields, show nested fields.
					 */
					String qualifiedType = field.getQualifiedType();
					JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
					String packagetName = StringUtils.substringBeforeLast(qualifiedType, ".");
					String resourcePath = packagetName.replace(".", File.separator) + File.separator + field.getType() + ".java";
					Resource<?> child = java.getSourceFolder().getChild(resourcePath);
					if(!child.exists()) continue;
					JavaResource javaResource = (JavaResource) child;
					JavaSource<?> javaSource = javaResource.getJavaSource();
					if(!javaSource.isClass()) continue;
					JavaClass nestedClass = (JavaClass) javaSource;
					List<Field<JavaClass>> nestedFields = nestedClass.getFields();
					for (Field<JavaClass> nestedField : nestedFields) {
						if(nestedField.hasAnnotation(Id.class))continue;
						if(nestedField.hasAnnotation(Version.class)) continue;
						if(nestedField.hasAnnotation(OneToOne.class) || nestedField.hasAnnotation(ManyToOne.class))
							continue;
						tokens.add(field.getName()+"."+nestedField.getName());
					}
					
				} else {
					tokens.add(field.getName());
				}
			}

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		return tokens;
	}
}
