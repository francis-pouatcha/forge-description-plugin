package org.adorsys.forge.plugins.description;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.completer.SimpleTokenCompleter;

public class AccessorCompleter extends SimpleTokenCompleter {
	private static final String GET_PREFIX = "get";
	private static final String IS_PREFIX = "is";

	private final Shell shell;

	@Inject
	public AccessorCompleter(Shell shell) {
		this.shell = shell;
	}

	@Override
	public List<String> getCompletionTokens() {
		final List<String> tokens = new ArrayList<String>();
		final Resource<?> currentResource = shell.getCurrentResource();

		JavaClassOrInterface javaClassOrInterface = DescriptionPluginUtils
				.inspectResource(currentResource);
		if (javaClassOrInterface.isClass()) {
			JavaClass javaClass = javaClassOrInterface.getJavaClass();
			List<Method<JavaClass>> methods = javaClass.getMethods();
			for (Method<JavaClass> method : methods) {
				String methodName = method.getName();
				if (methodName == null)
					continue;
				if (methodName.startsWith(IS_PREFIX)
						|| methodName.startsWith(GET_PREFIX))
					tokens.add(methodName);
			}
		} else if (javaClassOrInterface.isInterface()) {
			JavaInterface javaInterface = javaClassOrInterface
					.getJavaInterface();
			List<Method<JavaInterface>> methods = javaInterface.getMethods();
			for (Method<JavaInterface> method : methods) {
				String methodName = method.getName();
				if (methodName == null)
					continue;
				if (methodName.startsWith(IS_PREFIX)
						|| methodName.startsWith(GET_PREFIX))
					tokens.add(methodName);
			}
		}
		return tokens;
	}
}
