package org.adorsys.forge.plugins.format;

import java.io.FileNotFoundException;
import java.math.BigDecimal;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.adorsys.forge.plugins.description.DescriptionFacet;
import org.adorsys.forge.plugins.description.DescriptionPluginUtils;
import org.adorsys.forge.plugins.description.JavaClassOrInterface;
import org.adorsys.javaext.format.NumberFormatType;
import org.adorsys.javaext.format.NumberType;
import org.jboss.forge.parser.java.Annotation;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.Resource;
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
@Alias("format")
@RequiresProject
@Help("This plugin will hel you add format description on entity fields.")
@RequiresFacet({ DescriptionFacet.class })
public class FormatPlugin implements Plugin {

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

	@Command(value = "add-number-type", help = "Adds a number type to the format.")
	public void setFieldDescription(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "type", completer=NumberTypeCompleter.class, required = true) NumberType numberType,
			final PipeOut out) {
		final Resource<?> currentResource = shell.getCurrentResource();
			
		JavaClassOrInterface javaClassOrInterface = DescriptionPluginUtils
				.inspectResource(currentResource);

		if (!javaClassOrInterface.isClass()) {
			throw new IllegalStateException(
					"The current resource is not a class.");
		}

		JavaClass javaClass = javaClassOrInterface.getJavaClass();
		
		final Field<JavaClass> field = javaClass.getField(property);
		if (field == null)
			throw new IllegalStateException(
					"The current class has no property named '" + property
					+ "'");
		if(field.getType().equals(BigDecimal.class.getName())){
			throw new IllegalStateException(
					"The property '" + property
					+ "' is not a BigDecimal" );
		}
		Annotation<JavaClass> numberFormatTypeAnnotation = field.getAnnotation(NumberFormatType.class);
		if(numberFormatTypeAnnotation==null){
			numberFormatTypeAnnotation = field.addAnnotation(NumberFormatType.class);
		}
		numberFormatTypeAnnotation.setEnumArrayValue(numberType);
		saveAndFire(javaClass);
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
}
