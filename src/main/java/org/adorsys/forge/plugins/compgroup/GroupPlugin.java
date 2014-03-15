package org.adorsys.forge.plugins.compgroup;

import java.io.FileNotFoundException;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.adorsys.forge.plugins.description.DescriptionFacet;
import org.adorsys.forge.plugins.description.DescriptionPluginUtils;
import org.adorsys.forge.plugins.description.JavaClassOrInterface;
import org.adorsys.forge.plugins.utils.Utils;
import org.adorsys.javaext.compgroup.Grouper;
import org.jboss.forge.parser.java.EnumConstant;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaEnum;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
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
import org.jboss.forge.spec.javaee.PersistenceFacet;

/**
 * 
 * @author francis pouatcha
 * 
 */
@Alias("group")
@RequiresProject
@Help("This plugin will help you add CompGroup adn CompGrouper annotations to entities.")
@RequiresFacet({ DescriptionFacet.class })
public class GroupPlugin implements Plugin {

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

	@Command(value = "grouper", help = "Makes the current compoent a grouper.")
	public void newGrouper(final PipeOut out) {
		final Resource<?> currentResource = shell.getCurrentResource();
		JavaSource<?> javaSource = DescriptionPluginUtils.readCurrentResource(currentResource);
		
		if (!javaSource.isEnum()) {
			throw new IllegalStateException("The current resource is not an enum resource");
		}

		JavaEnum javaEnum = (JavaEnum) javaSource;
		if(!javaEnum.hasAnnotation(Grouper.class)){
			javaEnum.addAnnotation(Grouper.class);
		}
		saveAndFire(javaEnum);
	}

	@Command(value = "add", help = "Defines the way the referencing end selects an instance of the referenced end.")
	public void addGroup(@Option(name = "grouper", required = true,
            description = "The @Entity type to which this field is a relationship",
            type = PromptType.JAVA_CLASS) final String grouper,
            @Option(name = "named", required = true,description = "The group name",
            type = PromptType.JAVA_VARIABLE_NAME) final String groupName,
			final PipeOut out) {
		final Resource<?> currentResource = shell.getCurrentResource();

		JavaClassOrInterface javaClassOrInterface = DescriptionPluginUtils
				.inspectResource(currentResource);

		if (!javaClassOrInterface.isClass()) 
			throw new IllegalStateException("The current resource is not a class.");

		JavaEnum javaEnum;
		try {
			javaEnum = findEnum(grouper);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}
		EnumConstant<JavaEnum> enumConstant = javaEnum.getEnumConstant(groupName);
		if(enumConstant==null)
			throw new IllegalStateException(groupName + " is not an enum constant fo the enum type " + grouper);

		JavaClass javaClass = javaClassOrInterface.getJavaClass();
		String bundleName = javaEnum.getQualifiedName() + ".properties";
		Utils.updatePropertiesFile(enumConstant.getName(), javaClass.getQualifiedName(), bundleName, false, project);
//		
//		Annotation<JavaClass> groupAnnotation = javaClass.getAnnotation(Group.class);
//		if(groupAnnotation==null)groupAnnotation=javaClass.addAnnotation(Group.class);
//		
//		String nameList= groupAnnotation.getLiteralValue();
//		String literalFieldName = "\""+groupName+"\"";
//		if(StringUtils.isBlank(nameList)){
//			groupAnnotation.setLiteralValue(literalFieldName);
//		} else if(!nameList.startsWith("{")){
//			nameList = "{"+nameList+","+literalFieldName+"}";
//			groupAnnotation.setLiteralValue(nameList);
//		} else {
//			nameList=StringUtils.substringBeforeLast(nameList, "}");
//			nameList = nameList+","+literalFieldName+"}";
//			groupAnnotation.setLiteralValue(nameList);
//		}
//
//		saveAndFire(javaClass);
	}

	private void saveAndFire(JavaSource<?> source) {
		final JavaSourceFacet javaSourceFacet = project
				.getFacet(JavaSourceFacet.class);
		try {
			javaSourceFacet.saveJavaSource(source);
			pickup.fire(new PickupResource(javaSourceFacet
					.getJavaResource(source)));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("The current resource '"
					+ source.getName() + "' was deleted from the file system.");
		}

	}

	private JavaEnum findEnum(final String enumType)
			throws FileNotFoundException {
		PersistenceFacet scaffold = project.getFacet(PersistenceFacet.class);
		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		JavaResource javaResource = java.getJavaResource(enumType);
		if(javaResource==null){
			javaResource = java.getJavaResource(scaffold.getEntityPackage() + "." + enumType);
		}

		if (javaResource == null) {
			throw new FileNotFoundException("Could not locate java enum for input: " + enumType);
		}
		
		JavaSource<?> javaSource = javaResource.getJavaSource();
		if(javaSource.isEnum())return (JavaEnum) javaSource;
		throw new IllegalStateException(enumType + " is not a java enum type");
	}
}
