package org.adorsys.forge.plugins.description;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Future;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.adorsys.javaext.description.Description;
import org.jboss.forge.parser.java.Annotation;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.java.JavaType;
import org.jboss.forge.parser.java.Member;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.PropertiesFileResource;
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
@Help("This plugin will hel you add description on entity or field  .")
@RequiresFacet({ DescriptionFacet.class })
public class DescriptionPlugin implements Plugin {


	private static final String GET_PREFIX = "get";
	private static final String IS_PREFIX = "is";	
	public static final String DESCRIPTION_CONSTANT = "description";
	public static final String VALIDATION_CONSTANT = "validation";
	/*
	 * Description title will be used in user interfaces as field descriptors, form titles.
	 */
	public static final String TITLE_SUFFIX = "title";
	/*
	 * Text will be used as help messages, popup illustrations of fields and forms
	 */
	public static final String TEXT_SUFFIX = "text";
	public static final String DOT_CONSTANT = ".";
	public static final String UNDERSCORE_CONSTANT = "_";

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

	@Command(value = "add-class-description", help = "Adds a description annotation to the current resource class, interface")
	public void addDescription( 
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,			
			final PipeOut out){
		final Resource<?> currentResource = shell.getCurrentResource();

			JavaClassOrInterface javaClassOrInterface = DescriptionPluginUtils
					.inspectResource(currentResource);

			if (javaClassOrInterface.isClass()) {
				JavaClass clazz = javaClassOrInterface.getJavaClass();
				String descriptionKey = getDescriptionKey(clazz);	
				updateResourceBundleFiles(clazz.getPackage(), clazz.getName(), locale, descriptionKey, title, text);
				
				Annotation<JavaClass> annotation = null;
				if (!clazz.hasAnnotation(Description.class)) {
					annotation = clazz
							.addAnnotation(Description.class);
					annotation.setStringValue(descriptionKey);
					saveAndFire(clazz);
				} else {
					annotation = clazz.getAnnotation(Description.class);
					if(!descriptionKey.equals(annotation.getStringValue())){
						annotation.setStringValue(descriptionKey);
						saveAndFire(clazz);
					}
				}
			} else if (javaClassOrInterface.isInterface()) {
				JavaInterface javaInterface = javaClassOrInterface
						.getJavaInterface();
				String descriptionKey = getDescriptionKey(javaInterface);	
				updateResourceBundleFiles(javaInterface.getPackage(), javaInterface.getName(),locale, descriptionKey, title, text);

				Annotation<JavaInterface> annotation = null;
				if (!javaInterface.hasAnnotation(Description.class)) {
					annotation = javaInterface
							.addAnnotation(Description.class);
					annotation.setStringValue(descriptionKey);
					saveAndFire(javaInterface);
				} else {
					annotation = javaInterface.getAnnotation(Description.class);
					if(!descriptionKey.equals(annotation.getStringValue())){
						annotation.setStringValue(descriptionKey);
						saveAndFire(javaInterface);
					}
				}
			}
	}

	@Command(value = "add-field-description", help = "Adds a description annotation to the field of a class")
	public void setFieldDescription(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
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

		String descriptionKey = getDescriptionKey(field);
		updateResourceBundleFiles(field.getOrigin().getPackage(), field.getOrigin().getName(), locale, descriptionKey, title, text);
		Annotation<JavaClass> annotation = null;
		if(!field.hasAnnotation(Description.class)){
			annotation = field.addAnnotation(Description.class);
			annotation.setStringValue(descriptionKey);
			saveAndFire(javaClass);
		} else {
			annotation = field.getAnnotation(Description.class);
			if(!descriptionKey.equals(annotation.getStringValue())){
				annotation.setStringValue(descriptionKey);
				saveAndFire(javaClass);
			}
		}
	}
	
	@Command(value = "add-size-message", help = "Adds a message key to the @Size constraint and corresponding message in the properties files.")
	public void addSizeMessage(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
			final PipeOut out) {
		handleValidationCommand(property, title, text, locale, Size.class);
	}

	@Command(value = "add-notNull-message", help = "Adds a message key to the @NotNull constraint and corresponding message in the properties files.")
	public void addNotNullMessage(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
			final PipeOut out) {
		handleValidationCommand(property, title, text, locale, NotNull.class);
	}

	@Command(value = "add-null-message", help = "Adds a message key to the @Null constraint and corresponding message in the properties files.")
	public void addNullMessage(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
			final PipeOut out) {
		handleValidationCommand(property, title, text, locale, Null.class);
	}

	@Command(value = "add-pattern-message", help = "Adds a message key to the @Pattern constraint and corresponding message in the properties files.")
	public void addPatternMessage(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
			final PipeOut out) {
		handleValidationCommand(property, title, text, locale, Pattern.class);
	}
	
	@Command(value = "add-past-message", help = "Adds a message key to the @Past constraint and corresponding message in the properties files.")
	public void addPastMessage(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
			final PipeOut out) {
		handleValidationCommand(property, title, text, locale, Past.class);
	}

	@Command(value = "add-future-message", help = "Adds a message key to the @Future constraint and corresponding message in the properties files.")
	public void addFutureMessage(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
			final PipeOut out) {
		handleValidationCommand(property, title, text, locale, Future.class);
	}

	@Command(value = "add-min-message", help = "Adds a message key to the @Min constraint and corresponding message in the properties files.")
	public void addMinMessage(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
			final PipeOut out) {
		handleValidationCommand(property, title, text, locale, Min.class);
	}

	@Command(value = "add-max-message", help = "Adds a message key to the @Max constraint and corresponding message in the properties files.")
	public void addMaxMessage(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
			final PipeOut out) {
		handleValidationCommand(property, title, text, locale, Max.class);
	}

	@Command(value = "add-digits-message", help = "Adds a message key to the @Digits constraint and corresponding message in the properties files.")
	public void addDigitsMessage(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
			final PipeOut out) {
		handleValidationCommand(property, title, text, locale, Digits.class);
	}

	@Command(value = "add-decimalMin-message", help = "Adds a message key to the @DecimalMin constraint and corresponding message in the properties files.")
	public void addDecimalMinMessage(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
			final PipeOut out) {
		handleValidationCommand(property, title, text, locale, DecimalMin.class);
	}

	@Command(value = "add-decimalMax-message", help = "Adds a message key to the @DecimalMax constraint and corresponding message in the properties files.")
	public void addDecimalMaxMessage(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
			final PipeOut out) {
		handleValidationCommand(property, title, text, locale, DecimalMax.class);
	}

	@Command(value = "add-assertTrue-message", help = "Adds a message key to the @AssertTrue constraint and corresponding message in the properties files.")
	public void addAssertTrueMessage(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
			final PipeOut out) {
		handleValidationCommand(property, title, text, locale, AssertTrue.class);
	}

	@Command(value = "add-assertFalse-message", help = "Adds a message key to the @AssertFalse constraint and corresponding message in the properties files.")
	public void addAssertFalseMessage(
			@Option(name = "onProperty", completer = PropertyCompleter.class, required = true) String property,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
			final PipeOut out) {
		handleValidationCommand(property, title, text, locale, AssertFalse.class);
	}
	
	private void handleValidationCommand(String property, String title, String text, String locale, Class<? extends java.lang.annotation.Annotation> annotationKlass){
		final Resource<?> currentResource = shell.getCurrentResource();
		JavaClassOrInterface javaClassOrInterface = DescriptionPluginUtils.inspectResource(currentResource);

		if (!javaClassOrInterface.isClass()) {
			throw new IllegalStateException("The current resource is not a class.");
		}

		JavaClass javaClass = javaClassOrInterface.getJavaClass();
		
		final Field<JavaClass> field = javaClass.getField(property);
		if (field == null)
			throw new IllegalStateException("The current class has no property named '" + property + "'");
		
		handleValidationMessage(javaClass, field, annotationKlass, locale, title, text);
	}
	
	private void handleValidationMessage(JavaClass javaClass, Field<JavaClass> field, 
			Class<? extends java.lang.annotation.Annotation> annotationKlass, 
			String locale, String title, String text){
		String validationKey = getValidationKey(field, annotationKlass.getSimpleName());
		updateResourceBundleFiles(field.getOrigin().getPackage(), field.getOrigin().getName(), locale, validationKey, title, text);
		Annotation<JavaClass> annotation = null;
		if(!field.hasAnnotation(annotationKlass)){
			annotation = field.addAnnotation(annotationKlass);
			annotation.setStringValue("message", validationKey);
			saveAndFire(javaClass);
		} else {
			annotation = field.getAnnotation(annotationKlass);
			if(!validationKey.equals(annotation.getStringValue("message"))){
				annotation.setStringValue("message", validationKey);
				saveAndFire(javaClass);
			}
		}
	}

	@Command(value = "add-accessor-description", help = "Adds a description annotation to a class or interface accessor method")
	public void addAccessorDescription(
			@Option(name = "onAccessor", completer = AccessorCompleter.class, required = true) String methodName,
			@Option(name = "title") String title, 
			@Option(name = "text") String text, 
            @Option(name = "locale") String locale,
			final PipeOut out) {
		final Resource<?> currentResource = shell.getCurrentResource();

		JavaClassOrInterface javaClassOrInterface = DescriptionPluginUtils
				.inspectResource(currentResource);

		if (javaClassOrInterface.isClass()) {
			JavaClass javaClass = javaClassOrInterface.getJavaClass();
			final Method<JavaClass> method = javaClass.getMethod(methodName);
			if (method == null)
				throw new IllegalStateException(
						"The current class has no property accessor named '"
								+ method + "'");
			
			String descriptionKey = getDescriptionKey(method);	
			updateResourceBundleFiles(javaClass.getPackage(), javaClass.getName(), locale, descriptionKey, title, text);
			Annotation<JavaClass> annotation = null;
			
			if (method.hasAnnotation(Description.class)) {
				throw new IllegalStateException("The element '"
						+ method.getName() + "' is already annotated with @"
						+ Description.class.getSimpleName());
			}
			if(!method.hasAnnotation(Description.class)){
				annotation = method.addAnnotation(Description.class);
				annotation.setStringValue(descriptionKey);
				saveAndFire(javaClass);
			} else {
				annotation = method.getAnnotation(Description.class);
				if(!descriptionKey.equals(annotation.getStringValue())){
					annotation.setStringValue(descriptionKey);
					saveAndFire(javaClass);
				}
			}
		} else if (javaClassOrInterface.isInterface()) {
			JavaInterface javaInterface = javaClassOrInterface
					.getJavaInterface();
			final Method<JavaInterface> method = javaInterface
					.getMethod(methodName);
			if (method == null)
				throw new IllegalStateException(
						"The current interface has no property accessor named '"
								+ method + "'");

			String descriptionKey = getDescriptionKey(method);	
			updateResourceBundleFiles(javaInterface.getPackage(), javaInterface.getName(), locale, descriptionKey, title, text);
			Annotation<JavaInterface> annotation = null;
			
			if(!method.hasAnnotation(Description.class)){
				annotation = method.addAnnotation(Description.class);
				annotation.setStringValue(descriptionKey);
				saveAndFire(javaInterface);
			} else {
				annotation = method.getAnnotation(Description.class);
				if(!descriptionKey.equals(annotation.getStringValue())){
					annotation.setStringValue(descriptionKey);
					saveAndFire(javaInterface);
				}
			}
		}
	}

	@Command(value = "generate-description-keys", help = "Add description keys and annotation to class, fields and/or method as specified by hte caller.")
	public void generateDescriptionKeys(
			@Option(name = "onAllProperties", flagOnly = true, required=false) boolean onProperties,
			@Option(name = "onAllAccessors", flagOnly = true, required=false) boolean onAccessors,
			final PipeOut out){
		final Resource<?> currentResource = shell.getCurrentResource();
		
		if(currentResource instanceof JavaFieldResource){
			JavaFieldResource jfr =  (JavaFieldResource) currentResource;
			Field<? extends JavaSource<?>> field = jfr.getUnderlyingResourceObject();
			Annotation<? extends JavaSource<?>> annotation = field.addAnnotation(Description.class);
			JavaSource<?> origin = field.getOrigin();
			String klassQualifiedName = origin.getQualifiedName();
			String fieldName = field.getName();
			String descriptionKey = klassQualifiedName + "." + fieldName + "." + DESCRIPTION_CONSTANT;
			annotation.setStringValue(descriptionKey);
			updateResourceBundleFiles(origin.getPackage(), origin.getName(), null, descriptionKey, null, null);
			saveAndFire(origin);
		
		}else if (currentResource instanceof JavaMethodResource){
			JavaMethodResource jmr =  (JavaMethodResource) currentResource;
			Method<? extends JavaSource<?>> method = jmr.getUnderlyingResourceObject();
			Annotation<? extends JavaSource<?>> annotation = method.addAnnotation(Description.class);
			JavaSource<?> origin = method.getOrigin();
			String klassQualifiedName = origin.getQualifiedName();
			String fieldName = method.getName();
			String descriptionKey = klassQualifiedName + "." + fieldName + "." + DESCRIPTION_CONSTANT;
			annotation.setStringValue(descriptionKey);
			updateResourceBundleFiles(origin.getPackage(), origin.getName(), null, descriptionKey, null, null);
			saveAndFire(origin);
		} else if(currentResource instanceof JavaResource){
			JavaClassOrInterface javaClassOrInterface = DescriptionPluginUtils
					.inspectResource(currentResource);

			if (javaClassOrInterface.isClass()) {
				JavaClass clazz = javaClassOrInterface.getJavaClass();
				
				if (!clazz.hasAnnotation(Description.class)) {
					Annotation<JavaClass> annotation = clazz
							.addAnnotation(Description.class);
					String descriptionKey = clazz.getQualifiedName() + "."+DESCRIPTION_CONSTANT;
					annotation.setStringValue(descriptionKey);
					updateResourceBundleFiles(clazz.getPackage(), clazz.getName(), null, descriptionKey, null, null);
				}
				if(onAccessors)
					addDescriptionOnAccessors(clazz);
				if(onProperties)
					addPropertiesOnProperties(clazz);
				
				saveAndFire(clazz);

			} else if (javaClassOrInterface.isInterface()) {
				JavaInterface javaInterface = javaClassOrInterface.getJavaInterface();
				
				if (!javaInterface.hasAnnotation(Description.class)) {
					Annotation<JavaInterface> annotation = javaInterface
							.addAnnotation(Description.class);
					String descriptionKey = javaInterface.getQualifiedName() + "."+DESCRIPTION_CONSTANT;
					annotation.setStringValue(descriptionKey);
					updateResourceBundleFiles(javaInterface.getPackage(), javaInterface.getName(), null, descriptionKey, null, null);
				}
				if(onAccessors)
					addDescriptionOnAccessors(javaInterface);

				saveAndFire(javaInterface);
			}
		}
	}	

	private void addDescriptionOnAccessors(JavaInterface javaInterface) {
		List<Method<JavaInterface>> methods = javaInterface.getMethods();
		for (Method<JavaInterface> method : methods) {
			String methodName = method.getName();
			if (methodName == null)
				continue;
			if (methodName.startsWith(IS_PREFIX)
					|| methodName.startsWith(GET_PREFIX)){
				if(method.hasAnnotation(Description.class))continue;
				Annotation<JavaInterface> annotation = method.addAnnotation(Description.class);
				String descriptionKey = javaInterface.getQualifiedName() + "." + methodName + "." + DESCRIPTION_CONSTANT;
				annotation.setStringValue(descriptionKey);
				updateResourceBundleFiles(javaInterface.getPackage(), javaInterface.getName(), null, descriptionKey, null, null);
			}
		}
	}

	private void addPropertiesOnProperties(JavaClass javaClass) {
		List<Field<JavaClass>> fields = javaClass.getFields();
		for (Field<JavaClass> field : fields) {
			if(field.isStatic()) continue;
			if(field.hasAnnotation(Description.class)) continue;
			Annotation<JavaClass> annotation = field.addAnnotation(Description.class);
			String descriptionKey = javaClass.getQualifiedName() + "." + field.getName() + "." + DESCRIPTION_CONSTANT;
			annotation.setStringValue(descriptionKey);
			updateResourceBundleFiles(javaClass.getPackage(), javaClass.getName(), null, descriptionKey, null, null);
		}
		
	}

	private void addDescriptionOnAccessors(JavaClass javaClass) {
		List<Method<JavaClass>> methods = javaClass.getMethods();
		for (Method<JavaClass> method : methods) {
			String methodName = method.getName();
			if (methodName == null)
				continue;
			if (methodName.startsWith(IS_PREFIX)
					|| methodName.startsWith(GET_PREFIX)){
				if(method.hasAnnotation(Description.class))continue;
				Annotation<JavaClass> annotation = method.addAnnotation(Description.class);
				String descriptionKey = javaClass.getQualifiedName() + "." + methodName + "." + DESCRIPTION_CONSTANT;
				annotation.setStringValue(descriptionKey);
				updateResourceBundleFiles(javaClass.getPackage(), javaClass.getName(), null, descriptionKey, null, null);
			}
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

	/*
	 * Will update the resource bundle file. We will us a single file for each package.
	 */
	private void updateResourceBundleFiles(String packageName, String klassSimpleName, String locale, String key, String title, String text){
		String bundleName = klassSimpleName +(locale!=null?"_"+locale:"")+".properties";
		
		PropertiesFileResource propertiesFileResource = getOrCreate(packageName, bundleName);
		String keyFormated = key.replace(DOT_CONSTANT, UNDERSCORE_CONSTANT);
		String titleKey = keyFormated + DOT_CONSTANT + TITLE_SUFFIX;
		String textKey = keyFormated + DOT_CONSTANT + TEXT_SUFFIX;
		propertiesFileResource.putProperty(titleKey, title);
		propertiesFileResource.putProperty(textKey, text);
	}

   /**
    * Gets another file resource. Creates a file in case it does not exist
    * 
    * @param bundleName
    * @return
    */
   protected PropertiesFileResource getOrCreate(final String packageName, final String bundleName)
   {
      final ResourceFacet resourceFacet = project.getFacet(ResourceFacet.class);
      String bundleDirectoryName = packageName.replace(".", File.separator);

      PropertiesFileResource bundleFile = resourceFacet.getResourceFolder().getChildOfType(PropertiesFileResource.class, bundleDirectoryName + File.separator + bundleName);
      if(!bundleFile.exists())bundleFile.createNewFile();
      return bundleFile;
   }   
   
   private String getDescriptionKey(Member<?, ?> member){
	  return member.getOrigin().getName() + UNDERSCORE_CONSTANT + member.getName() + UNDERSCORE_CONSTANT + DESCRIPTION_CONSTANT;
   }

   private String getValidationKey(Member<?, ?> member, String constraintClass){
	  return member.getOrigin().getName() + UNDERSCORE_CONSTANT + member.getName() + UNDERSCORE_CONSTANT + constraintClass + UNDERSCORE_CONSTANT + VALIDATION_CONSTANT;
   }
   
	private String getDescriptionKey(JavaType<?> javaType) {
		return javaType.getName() + UNDERSCORE_CONSTANT + DESCRIPTION_CONSTANT;
	}
   
}
