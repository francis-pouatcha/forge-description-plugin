package org.adorsys.forge.plugins.display;

import java.io.FileNotFoundException;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.adorsys.forge.plugins.description.DescriptionFacet;
import org.adorsys.forge.plugins.description.DescriptionPluginUtils;
import org.adorsys.forge.plugins.description.JavaClassOrInterface;
import org.adorsys.javaext.display.ToStringField;
import org.adorsys.javaext.list.ListField;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.parser.java.Annotation;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
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
 * Help provide hints for the display of an entity.
 * 
 * @author francis pouatcha
 * 
 */
@Alias("display")
@RequiresProject
@Help("This plugin will help you add display hints on entity fields.")
@RequiresFacet({ DescriptionFacet.class })
public class DisplayPlugin implements Plugin {

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

	@Command(value = "add-toString-field", help = "Add a field to the list of fields used in the string representation of this entity on the user interface.")
	public void addToStringField(
			@Option(name = "field", completer = NestedPropertyCompleter.class, required = true) String fieldName,
			final PipeOut out) {
		final Resource<?> currentResource = shell.getCurrentResource();
		JavaClassOrInterface javaClassOrInterface = DescriptionPluginUtils
				.inspectResource(currentResource);

		if (!javaClassOrInterface.isClass()) {
			throw new IllegalStateException(
					"The current resource is not a class.");
		}

		JavaClass javaClass = javaClassOrInterface.getJavaClass();

		Annotation<JavaClass> tostringFieldAnnotation;
		if(javaClass.hasAnnotation(ToStringField.class)){
			tostringFieldAnnotation = javaClass.getAnnotation(ToStringField.class);
		}else {
			tostringFieldAnnotation = javaClass.addAnnotation(ToStringField.class);
		}
		String fieldList= tostringFieldAnnotation.getLiteralValue();
		String literanFieldName = "\""+fieldName+"\"";
		if(StringUtils.isBlank(fieldList)){
			tostringFieldAnnotation.setLiteralValue(literanFieldName);
		} else if(!fieldList.startsWith("{")){
			fieldList = "{"+fieldList+","+literanFieldName+"}";
			tostringFieldAnnotation.setLiteralValue(fieldList);
		} else {
			fieldList=StringUtils.substringBeforeLast(fieldList, "}");
			fieldList = fieldList+","+literanFieldName+"}";
			tostringFieldAnnotation.setLiteralValue(fieldList);
		}
		saveAndFire(javaClass);
	}
	
	@Command(value = "add-list-field", help = "Add a field to the list of fields that will displayed.")
	public void setFieldDescription(
			@Option(name = "field", completer = NestedPropertyCompleter.class, required = true) String fieldName,
			final PipeOut out) {
		final Resource<?> currentResource = shell.getCurrentResource();
		JavaClassOrInterface javaClassOrInterface = DescriptionPluginUtils
				.inspectResource(currentResource);

		if (!javaClassOrInterface.isClass()) {
			throw new IllegalStateException(
					"The current resource is not a class.");
		}

		JavaClass javaClass = javaClassOrInterface.getJavaClass();

		Annotation<JavaClass> listFieldAnnotation;
		if(javaClass.hasAnnotation(ListField.class)){
			listFieldAnnotation = javaClass.getAnnotation(ListField.class);
		}else {
			listFieldAnnotation = javaClass.addAnnotation(ListField.class);
		}
		String fieldList= listFieldAnnotation.getLiteralValue();
		String literanFieldName = "\""+fieldName+"\"";
		if(StringUtils.isBlank(fieldList)){
			listFieldAnnotation.setLiteralValue(literanFieldName);
		} else if(!fieldList.startsWith("{")){
			fieldList = "{"+fieldList+","+literanFieldName+"}";
			listFieldAnnotation.setLiteralValue(fieldList);
		} else {
			fieldList=StringUtils.substringBeforeLast(fieldList, "}");
			fieldList = fieldList+","+literanFieldName+"}";
			listFieldAnnotation.setLiteralValue(fieldList);
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
