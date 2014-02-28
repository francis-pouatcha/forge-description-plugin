package org.adorsys.forge.plugins.access;

import java.io.FileNotFoundException;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.adorsys.forge.plugins.description.DescriptionFacet;
import org.adorsys.forge.plugins.description.DescriptionPluginUtils;
import org.adorsys.javaext.admin.LoginRole;
import org.adorsys.javaext.admin.LoginTable;
import org.adorsys.javaext.admin.PermissionTable;
import org.adorsys.javaext.admin.RoleTable;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.parser.java.Annotation;
import org.jboss.forge.parser.java.EnumConstant;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaEnum;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.PropertiesFileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
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
import org.jboss.forge.spec.javaee.PersistenceFacet;

/**
 * Add
 * 
 * @author francis pouatcha
 * 
 */
@Alias("access")
@RequiresProject
@Help("This plugin will hel you add access control informations on entity fields.")
@RequiresFacet({ DescriptionFacet.class })
public class AccessPlugin implements Plugin {

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

	@Command(value = "login-role", help = "Adds the login role annotation to an enumeration")
	public void loginRole(
			@Option(name = "onConstant", completer =EnumConstantCompleter.class, required = true) String constant,			
			final PipeOut out) {
		final Resource<?> currentResource = shell.getCurrentResource();
		JavaSource<?> javaSource = DescriptionPluginUtils.readCurrentResource(currentResource);
		
		if (!javaSource.isEnum()) {
			throw new IllegalStateException("The current resource is not an enum resource");
		}

		JavaEnum javaEnum = (JavaEnum) javaSource;
		EnumConstant<JavaEnum> enumConstant = javaEnum.getEnumConstant(constant);
		if (enumConstant == null)
			throw new IllegalStateException(
					"The current Enum has no constant named '" + constant
					+ "'");
		
		if(!enumConstant.hasAnnotation(LoginRole.class)){
			enumConstant.addAnnotation(LoginRole.class);
		}
		saveAndFire(javaEnum);
	}
	
	@Command(value = "role-name-field", help = "Designates the role name field in the role table")
	public void roleNameField(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			final PipeOut out) {
		addToAnnotation(RoleTable.class, "roleNameField", property);
	}

	@Command(value = "login-name-field", help = "Designates the login name field in the login table")
	public void loginNameField(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			final PipeOut out) {
		addToAnnotation(LoginTable.class, "loginNameField", property);
	}

	@Command(value = "full-name-field", help = "Designates the full name field in the login table")
	public void fullNameField(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			final PipeOut out) {
		addToAnnotation(LoginTable.class, "fullNameField", property);
	}
	
	@Command(value = "password-field", help = "Designates the password field in the login table")
	public void passwordField(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			final PipeOut out) {
		addToAnnotation(LoginTable.class, "passwordField", property);
	}

	@Command(value = "permission-action-field", help = "Designates the permissionAction field in the permission table")
	public void permissionActionField(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			final PipeOut out) {
		addToAnnotation(PermissionTable.class, "permissionActionField", property);
	}

	@Command(value = "permission-name-field", help = "Designates the permissionName field in the permission table")
	public void permissionNameField(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			final PipeOut out) {
		addToAnnotation(PermissionTable.class, "permissionNameField", property);
	}
	
	public void addToAnnotation(final Class<? extends java.lang.annotation.Annotation> annotationClass,
			final String propertyName, final String propertyValue) {
		final Resource<?> currentResource = shell.getCurrentResource();
		JavaSource<?> javaSource = DescriptionPluginUtils
				.readCurrentResource(currentResource);

		if (!javaSource.isClass()) {
			throw new IllegalStateException(
					"The current resource is not a java class resource.");
		}
		JavaClass javaClass = (JavaClass) javaSource;
		Annotation<JavaClass> ann = javaClass.getAnnotation(annotationClass);
		if(ann==null)
			ann = javaClass.addAnnotation(annotationClass);
		ann.setStringValue(propertyName, propertyValue);

		saveAndFire(javaClass);
	}

	@Command(value = "permission-table", help = "Adds the permission table annotation to an entity")
	public void permissionTable(final PipeOut out,
			@Option(name = "actionEnumClass", type = PromptType.JAVA_CLASS, required = true) final String actionEnumClass) throws FileNotFoundException {
		final Resource<?> currentResource = shell.getCurrentResource();
		JavaSource<?> javaSource = DescriptionPluginUtils
				.readCurrentResource(currentResource);

		if (!javaSource.isClass()) {
			throw new IllegalStateException(
					"The current resource is not a class resource");
		}

		JavaClass javaClass = (JavaClass) javaSource;
		Annotation<JavaClass> permTableAnnotation = javaClass.getAnnotation(PermissionTable.class);
		if (permTableAnnotation==null) {
			permTableAnnotation = javaClass.addAnnotation(PermissionTable.class);
		}
		JavaEnum enumClass = findEnum(actionEnumClass);
		permTableAnnotation.setLiteralValue("actionEnumClass", enumClass.getName()+".class");
		javaClass.addImport(enumClass.getQualifiedName());
		saveAndFire(javaClass);
	}

	@Command(value = "login-table", help = "Adds the login table annotation to an entity")
	public void loginTable(final PipeOut out) {
		final Resource<?> currentResource = shell.getCurrentResource();
		JavaSource<?> javaSource = DescriptionPluginUtils
				.readCurrentResource(currentResource);

		if (!javaSource.isClass()) {
			throw new IllegalStateException(
					"The current resource is not a class resource");
		}

		JavaClass javaClass = (JavaClass) javaSource;
		if (!javaClass.hasAnnotation(LoginTable.class)) {
			javaClass.addAnnotation(LoginTable.class);
		}
		saveAndFire(javaClass);
	}

	@Command(value = "role-table", help = "Adds the permission table annotation to an entity")
	public void roleTable(
			final PipeOut out,
			@Option(name = "enumClass", type = PromptType.JAVA_CLASS, required = true) final String enumClass) {
		final Resource<?> currentResource = shell.getCurrentResource();
		JavaSource<?> javaSource = DescriptionPluginUtils
				.readCurrentResource(currentResource);

		if (!javaSource.isClass()) {
			throw new IllegalStateException(
					"The current resource is not a class resource");
		}

		JavaEnum javaEnum;
		try {
			javaEnum = findEnum(enumClass);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}

		JavaClass javaClass = (JavaClass) javaSource;
		Annotation<JavaClass> roleTableAnnotation = javaClass
				.getAnnotation(RoleTable.class);
		if (roleTableAnnotation == null) {
			roleTableAnnotation = javaClass.addAnnotation(RoleTable.class);
		}
		roleTableAnnotation.setStringValue("enumClass", javaEnum.getName()
				+ ".class");
		if (javaClass.hasImport(javaEnum.getQualifiedName())) {
			javaClass.addImport(javaEnum.getQualifiedName());
		}
		saveAndFire(javaClass);
	}

	@Command(value = "login-entry", help = "Prepare data for the creation of an initial user")
	public void loginEntry(final PipeOut out,
			@Option(name = "userName", required = true) String userName,
			@Option(name = "password", required = true) String password) 
	{
		
		/*
		 * Override property here set to true
		 */
		updatePropertiesFile(userName, password, "logins.properties", true);
	}

	@Command(value = "role-entry", help = "Prepare data for the creation of a role record")
	public void roleEntry(final PipeOut out,
			@Option(name = "userName", required = true) String userName,
			@Option(name = "role", required = true) String role) {
		/*
		 * Override property here set to true
		 */
		updatePropertiesFile(userName, role, "roles.properties", false);
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

	private JavaEnum findEnum(final String entity) throws FileNotFoundException {
		JavaEnum result = null;

		PersistenceFacet scaffold = project.getFacet(PersistenceFacet.class);
		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);

		if (entity != null) {
			result = getJavaEnumFrom(java.getJavaResource(entity));
			if (result == null) {
				result = getJavaEnumFrom(java.getJavaResource(scaffold
						.getEntityPackage() + "." + entity));
			}
		}

		if (result == null) {
			throw new FileNotFoundException(
					"Could not locate JavaEnum with name: " + entity);
		}

		return result;
	}

	private JavaEnum getJavaEnumFrom(final Resource<?> resource)
			throws FileNotFoundException {
		JavaSource<?> source = ((JavaResource) resource).getJavaSource();
		if (!source.isEnum()) {
			throw new IllegalStateException(
					"Current resource is not a JavaEnum!");
		}
		return (JavaEnum) source;
	}

	/*
	 * Will update the resource bundle file. We will us a single file for each
	 * package.
	 */
	private void updatePropertiesFile(String key, String value,String bundleName, boolean override) {
		PropertiesFileResource propertiesFileResource = getOrCreate(bundleName);
		String property = propertiesFileResource.getProperty(key);
		if(override){
			property=value;
		} else {
			if(StringUtils.isNotBlank(property)){
				if(StringUtils.equalsIgnoreCase(value, property)) return;// This value is the only one existing for this key.
				if(StringUtils.endsWithIgnoreCase(property, ","+value)) return;
				if(StringUtils.startsWithIgnoreCase(property, value+",")) return;
				if(StringUtils.containsIgnoreCase(property, ","+value+","))return;
				/*
				 * Either prefixed or sufixed with a comma
				 */
				property+=","+value;
			} else {
				property=value;
			}
		}
		propertiesFileResource.putProperty(key, property);
	}

	/**
	 * Gets another file resource. Creates a file in case it does not exist
	 * 
	 * @param bundleName
	 * @return
	 */
	protected PropertiesFileResource getOrCreate(final String bundleName) {
		final ResourceFacet resourceFacet = project.getFacet(ResourceFacet.class);

		PropertiesFileResource bundleFile = resourceFacet.getResourceFolder()
				.getChildOfType(PropertiesFileResource.class,bundleName);
		if (!bundleFile.exists())
			bundleFile.createNewFile();
		return bundleFile;
	}
}
