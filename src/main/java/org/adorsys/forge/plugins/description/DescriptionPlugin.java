package org.adorsys.forge.plugins.description;

import java.io.FileNotFoundException;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.adorsys.javaext.description.Description;
import org.jboss.forge.parser.java.Annotation;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.java.Member;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaFieldResource;
import org.jboss.forge.resources.java.JavaMethodResource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.completer.PropertyCompleter;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;

/**
 * Add
 * 
 * @author francis pouatcha
 * 
 */
@Alias("description")
@RequiresProject
@Help("This plugin will help you setting up Envers .")
@RequiresFacet({ DescriptionFacet.class })
public class DescriptionPlugin implements Plugin {

	@Inject
	private Project project;

	@Inject
	private Event<InstallFacets> request;
	
	@Inject
	private Event<PickupResource> pickup;

	@Inject
	private Shell shell;

	@SetupCommand
	public void setup(final PipeOut out) {
		if (!project.hasFacet(DescriptionFacet.class)) {
			request.fire(new InstallFacets(DescriptionFacet.class));
		}
		if (project.hasFacet(DescriptionFacet.class)) {
			ShellMessages.success(out, "Description service installed.");
		} else {
			ShellMessages.error(out,
					"Description service could not be installed.");
		}
	}
	
	@Command(value = "add-description", help = "Adds a description annotation to the current resource (class, interface, field, method")
	public void addDescription(@Option String description, final PipeOut out){
		final Resource<?> currentResource = shell.getCurrentResource();
		if(currentResource instanceof JavaFieldResource){
			JavaFieldResource jfr =  (JavaFieldResource) currentResource;
			Field<? extends JavaSource<?>> field = jfr.getUnderlyingResourceObject();
			Annotation<? extends JavaSource<?>> annotation = field.addAnnotation(Description.class);
			annotation.setStringValue(description);
			JavaSource<?> origin = field.getOrigin();
//			JavaResource parent = (JavaResource) jfr.getParent();
			saveAndFire(origin);
		}else if (currentResource instanceof JavaMethodResource){
			JavaMethodResource jmr =  (JavaMethodResource) currentResource;
			Method<? extends JavaSource<?>> method = jmr.getUnderlyingResourceObject();
			Annotation<? extends JavaSource<?>> annotation = method.addAnnotation(Description.class);
			annotation.setStringValue(description);
			JavaSource<?> origin = method.getOrigin();
//			JavaResource parent = (JavaResource) jmr.getParent();
			saveAndFire(origin);
		} else if(currentResource instanceof JavaResource){
			JavaClassOrInterface javaClassOrInterface = DescriptionPluginUtils
					.inspectResource(currentResource);

			if (javaClassOrInterface.isClass()) {
				JavaClass clazz = javaClassOrInterface.getJavaClass();
				if (clazz.hasAnnotation(Description.class)) {
					throw new IllegalStateException("The element '"
							+ clazz.getName() + "' is already annotated with @"
							+ Description.class.getSimpleName());
				}
				Annotation<JavaClass> annotation = clazz
						.addAnnotation(Description.class);
				annotation.setStringValue(description);
				saveAndFire(clazz);

			} else if (javaClassOrInterface.isInterface()) {
				JavaInterface javaInterface = javaClassOrInterface
						.getJavaInterface();
				if (javaInterface == null)
					throw new IllegalStateException(
							"The current interface has no property accessor named '"
									+ javaInterface + "'");
				if (javaInterface.hasAnnotation(Description.class)) {
					throw new IllegalStateException("The element '"
							+ javaInterface.getName()
							+ "' is already annotated with @"
							+ Description.class.getSimpleName());
				}
				Annotation<JavaInterface> annotation = javaInterface
						.addAnnotation(Description.class);
				annotation.setStringValue(description);
				saveAndFire(javaInterface);
			}
		}
	}
	

	@Command(value = "add-field-description", help = "Adds a description annotation to a class field")
	public void addFieldDescription(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option String description, final PipeOut out) {
		final Resource<?> currentResource = shell.getCurrentResource();
		
		if(currentResource instanceof JavaFieldResource){
			JavaFieldResource jfr =  (JavaFieldResource) currentResource;
			Field<? extends JavaSource<?>> field = jfr.getUnderlyingResourceObject();
			Annotation<? extends JavaSource<?>> annotation = field.addAnnotation(Description.class);
			annotation.setStringValue(description);
			JavaResource parent = (JavaResource) jfr.getParent();
			saveAndFire(parent);
		}else {
			JavaClassOrInterface javaClassOrInterface = DescriptionPluginUtils
					.inspectResource(currentResource);
	
			if (!javaClassOrInterface.isClass()) {
				throw new IllegalStateException(
						"The current resource is not a class.");
			}
	
			JavaClass clazz = javaClassOrInterface.getJavaClass();
			final Field<JavaClass> field = clazz.getField(property);
			if (field == null)
				throw new IllegalStateException(
						"The current class has no property named '" + property
								+ "'");
	
			Member<JavaClass, ?> member = field;
			if (member.hasAnnotation(Description.class)) {
				throw new IllegalStateException("The element '" + member.getName()
						+ "' is already annotated with @"
						+ Description.class.getSimpleName());
			}
			Annotation<JavaClass> annotation = member
					.addAnnotation(Description.class);
			annotation.setStringValue(description);
			saveAndFire(clazz);
		}
	}


	@Command(value = "add-accessor-description", help = "Adds a description annotation to a class or interface accessor method")
	public void addAccessorDescription(
			@Option(name = "onAccessor", completer = AccessorCompleter.class, required = true) String methodName,
			@Option String description, final PipeOut out) {
		final Resource<?> currentResource = shell.getCurrentResource();
		JavaClassOrInterface javaClassOrInterface = DescriptionPluginUtils
				.inspectResource(currentResource);

		if (javaClassOrInterface.isClass()) {
			JavaClass clazz = javaClassOrInterface.getJavaClass();
			final Method<JavaClass> method = clazz.getMethod(methodName);
			if (method == null)
				throw new IllegalStateException(
						"The current class has no property accessor named '"
								+ method + "'");
			if (method.hasAnnotation(Description.class)) {
				throw new IllegalStateException("The element '"
						+ method.getName() + "' is already annotated with @"
						+ Description.class.getSimpleName());
			}
			Annotation<JavaClass> annotation = method
					.addAnnotation(Description.class);
			annotation.setStringValue(description);
			saveAndFire(clazz);
		} else if (javaClassOrInterface.isInterface()) {
			JavaInterface javaInterface = javaClassOrInterface
					.getJavaInterface();
			final Method<JavaInterface> method = javaInterface
					.getMethod(methodName);
			if (method == null)
				throw new IllegalStateException(
						"The current interface has no property accessor named '"
								+ method + "'");
			if (method.hasAnnotation(Description.class)) {
				throw new IllegalStateException("The element '"
						+ method.getName() + "' is already annotated with @"
						+ Description.class.getSimpleName());
			}
			Annotation<JavaInterface> annotation = method
					.addAnnotation(Description.class);
			annotation.setStringValue(description);
			saveAndFire(javaInterface);
		}
	}
	
	private void saveAndFire(JavaSource<?> source){
		final JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
		try {
			javaSourceFacet.saveJavaSource(source);
			pickup.fire(new PickupResource(javaSourceFacet.getJavaResource(source)));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(
					"The current resource '"
							+ source.getName() + "' was deleted from the file system.");
		}
		
	}
	
	private void saveAndFire(JavaResource resource) {
		try {
			saveAndFire(resource.getJavaSource());
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(
					"The current resource '"
							+ resource.getName() + "' was deleted from the file system.");
		}
	}

}
