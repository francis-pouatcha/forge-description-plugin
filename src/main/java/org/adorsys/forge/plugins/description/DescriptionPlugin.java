package org.adorsys.forge.plugins.description;

import java.io.FileNotFoundException;
import java.util.List;

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
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.PropertiesFileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.ResourceFilter;
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

	private static final String GET_PREFIX = "get";
	private static final String IS_PREFIX = "is";	
	public static final String DESCRIPTION_CONSTANT = "description";
	@Command(value = "add-keyed-description", help = "Add description keys and annotation to class, fields and/or method as specified by hte caller.")
	public void addKeyedDescription(
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
			updateResourceBundleFiles(origin.getPackage(), descriptionKey);
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
			updateResourceBundleFiles(origin.getPackage(), descriptionKey);
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
					updateResourceBundleFiles(clazz.getPackage(), descriptionKey);
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
					updateResourceBundleFiles(javaInterface.getPackage(), descriptionKey);
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
				updateResourceBundleFiles(javaInterface.getPackage(), descriptionKey);
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
			updateResourceBundleFiles(javaClass.getPackage(), descriptionKey);
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
				updateResourceBundleFiles(javaClass.getPackage(), descriptionKey);
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
	
	private void saveAndFire(JavaResource resource) {
		try {
			saveAndFire(resource.getJavaSource());
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(
					"The current resource '"
							+ resource.getName() + "' was deleted from the file system.");
		}
	}
	
	
	/*
	 * Will update the resource bundle file. We will us a single file for each package.
	 */
	private void updateResourceBundleFiles(String packageName, String key){
		String bundleName = packageName + ".descriptions.messages.properties";
		PropertiesFileResource propertiesFileResource = getOrCreate(bundleName);
		propertiesFileResource.putProperty(key, key);
		
	}

   private class BundleBaseNameResourceFilter implements ResourceFilter
   {
      private String fileName;

      public BundleBaseNameResourceFilter(String fileName)
      {
         this.fileName = fileName;
      }

      @Override
      public boolean accept(Resource<?> resource)
      {
         return (resource instanceof PropertiesFileResource && resource.getName().startsWith(fileName));
      }
   }
   
   /**
    * Gets another file resource. Creates a file in case it does not exist
    * 
    * @param bundleName
    * @return
    */
   protected PropertiesFileResource getOrCreate(final String bundleName)
   {
      final ResourceFacet resourceFacet = project.getFacet(ResourceFacet.class);
      final BundleBaseNameResourceFilter filter = new BundleBaseNameResourceFilter(bundleName);
      PropertiesFileResource newFileResource = null;
      for (DirectoryResource directoryResource : resourceFacet.getResourceFolders())
      {
         for (Resource<?> resource : directoryResource.listResources(filter))
         {
            newFileResource = (PropertiesFileResource) resource;
            // Using the first resource found
            break;
         }
      }
      if (newFileResource == null)
      {
         newFileResource = resourceFacet.getResourceFolder().getChildOfType(PropertiesFileResource.class,
                  bundleName);
         if (!newFileResource.exists())
         {
            newFileResource.createNewFile();
         }
      }
      return newFileResource;
   }   
}
