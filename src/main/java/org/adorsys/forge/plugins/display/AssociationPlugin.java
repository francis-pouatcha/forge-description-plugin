package org.adorsys.forge.plugins.display;

import java.io.FileNotFoundException;
import java.math.BigDecimal;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.adorsys.forge.plugins.description.DescriptionFacet;
import org.adorsys.forge.plugins.description.DescriptionPluginUtils;
import org.adorsys.forge.plugins.description.JavaClassOrInterface;
import org.adorsys.javaext.display.Association;
import org.adorsys.javaext.display.SelectionMode;
import org.apache.commons.lang3.StringUtils;
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
 * Add display hint to entities. This is generally the case when an entity references another 
 * entity in an aggregation or composition relationship.
 * 
 * Generally only identifying information will be displayed. We can use the @Asssociation annotation
 * to tell the processor what to display on the page of the referencing relationship.
 * 
 * Beside the list of fields to display, we can also tell the processor if the selection of the 
 * referenced entity will be mapped with a combo box or if the application will refer the user
 * to the pane of the referenced entity for the purpose of this selection.
 * 
 * @author francis pouatcha
 * 
 */
@Alias("association")
@RequiresProject
@Help("This plugin will help you add display hints on association fields.")
@RequiresFacet({ DescriptionFacet.class })
public class AssociationPlugin implements Plugin {

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

	@Command(value = "set-selection-mode", help = "Defines the way the referencing end selects an instance of the referenced end.")
	public void setSelctionMode(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "selectionMode", completer=SelectionModeCompleter.class, required = true) SelectionMode selectionMode,
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
		Annotation<JavaClass> numberFormatTypeAnnotation = field.getAnnotation(Association.class);
		if(numberFormatTypeAnnotation==null){
			numberFormatTypeAnnotation = field.addAnnotation(Association.class);
		}
		numberFormatTypeAnnotation.setEnumArrayValue("selectionMode",selectionMode);
		saveAndFire(javaClass);
	}
	
	@Command(value = "display-field", help = "Add the selected field to the list of fields of the referenced entity"
			+ "that will be displayed on the view of the referencing entity.")
	public void displayField(
			@Option(name = "field", completer = FieldTypePropertyCompleter.class, required = true) String fieldName,
			final PipeOut out) {
		final Resource<?> currentResource = shell.getCurrentResource();
			
		JavaClassOrInterface javaClassOrInterface = DescriptionPluginUtils
				.inspectResource(currentResource);

		if (!javaClassOrInterface.isClass()) {
			throw new IllegalStateException(
					"The current resource is not a class.");
		}

		JavaClass javaClass = javaClassOrInterface.getJavaClass();
		
		String property = StringUtils.substringBeforeLast(fieldName, ".");
		String nestedFieldName = StringUtils.substringAfterLast(fieldName, ".");
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
		Annotation<JavaClass> displayAnnotation = null;
		if(field.hasAnnotation(Association.class)){
			displayAnnotation = field.getAnnotation(Association.class);
		} else {
			displayAnnotation = field.addAnnotation(Association.class);
		}
		String fieldList= displayAnnotation.getLiteralValue("fields");
		String literanFieldName = "\""+nestedFieldName+"\"";
		if(StringUtils.isBlank(fieldList)){
			displayAnnotation.setLiteralValue("fields", literanFieldName);
		} else if(!fieldList.startsWith("{")){
			fieldList = "{"+fieldList+","+literanFieldName+"}";
			displayAnnotation.setLiteralValue("fields", fieldList);
		} else {
			fieldList=StringUtils.substringBeforeLast(fieldList, "}");
			fieldList = fieldList+","+literanFieldName+"}";
			displayAnnotation.setLiteralValue("fields", fieldList);
		}
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
