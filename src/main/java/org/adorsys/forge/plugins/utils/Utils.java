package org.adorsys.forge.plugins.utils;

import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.resources.PropertiesFileResource;

public class Utils {

	/**
	 * Gets another file resource. Creates a file in case it does not exist
	 * 
	 * @param bundleName
	 * @return
	 */
	public static PropertiesFileResource getOrCreate(final String bundleName, Project project) {
		final ResourceFacet resourceFacet = project.getFacet(ResourceFacet.class);
		PropertiesFileResource bundleFile = resourceFacet.getResourceFolder()
				.getChildOfType(PropertiesFileResource.class,bundleName);
		if (!bundleFile.exists())
			bundleFile.createNewFile();
		return bundleFile;
	}

	public static void updatePropertiesFile(String key, String value,String bundleName, boolean override, Project project) {
		PropertiesFileResource propertiesFileResource = getOrCreate(bundleName, project);
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
}
