package org.adorsys.forge.plugins.description;

import java.util.ArrayList;
import java.util.List;

import org.adorsys.javaext.description.Description;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;

public class JavaClassOrInterface {
    
	private final JavaClass javaClass;
	private final JavaInterface javaInterface;
	public JavaClassOrInterface(JavaClass javaClass) {
		this.javaClass = javaClass;
		this.javaInterface = null;
	}
	public JavaClassOrInterface(JavaInterface javaInterface) {
		this.javaClass = null;
		this.javaInterface = javaInterface;
	}
	public JavaClass getJavaClass() {
		return javaClass;
	}
	public JavaInterface getJavaInterface() {
		return javaInterface;
	}

	public boolean isClass(){return javaClass!=null;}
	public boolean isInterface(){return javaInterface!=null;}

	public String getDescriptionKey(){
		String descriptionKey = "" ;
		if(isClass())descriptionKey = new StringBuilder(getJavaClass().getCanonicalName()).append(".description").toString();
		if(isInterface()) descriptionKey = new StringBuilder(getJavaInterface().getCanonicalName()).append(".description").toString();
		return descriptionKey ;
	}

	public String getDescription(){
		String descritption = "" ;
		if(isClass()){
			if(getJavaClass().hasAnnotation(Description.class)) descritption = getJavaClass().getAnnotation(Description.class).getStringValue();
		}
		if(isInterface()){
			if(getJavaInterface().hasAnnotation(Description.class)) descritption = getJavaClass().getAnnotation(Description.class).getStringValue();
		}
		return descritption ;
	}

	public List<Field<?>> getField(){
		List<Field<?>> fields = new ArrayList<Field<?>>();
		if(isClass()) fields.addAll(getJavaClass().getFields());
		if(isInterface()) fields.addAll(getJavaInterface().getFields());
		return fields ;
	}
}
