package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jboss.tools.common.validation.ValidationErrorManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.AbstractJaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Xavier Coulon The class name says it all.
 */
public class ValidationUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ValidationUtils.class);
	
	/**
	 * Converts the given {@link IResource} elements into a set of {@link IFile}s
	 * 
	 * @param elements
	 * @return the set containing the given elements
	 */
	public static Set<IFile> toSet(final IResource... elements) {
		final Set<IFile> result = new HashSet<IFile>();
		for (IResource element : elements) {
			result.add((IFile)element);
		}
		return result;
	}
	
	/**
	 * find JAX-RS Markers on the given resources.
	 * 
	 * @param element
	 * @return
	 * @throws CoreException
	 */
	public static IMarker[] findJaxrsMarkers(final IJaxrsElement... elements) throws CoreException {
		final List<IMarker> markers = new ArrayList<IMarker>();
		for (IJaxrsElement element : elements) {
 			final IMarker[] elementMarkers = element.getResource().findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, true,
					IResource.DEPTH_INFINITE);
			switch (element.getElementKind().getCategory()) {
			case APPLICATION:
			case HTTP_METHOD:
			case NAME_BINDING:
			case PROVIDER:
			case RESOURCE:
				for(IMarker marker : elementMarkers) {
					markers.add(marker);
				}
				break;
			case RESOURCE_METHOD:
				final IMarker[] resourceMarkers = elementMarkers;
				final ISourceRange methodSourceRange = ((JaxrsResourceMethod) element).getJavaElement()
						.getSourceRange();
				for (IMarker marker : resourceMarkers) {
					final int markerCharStart = marker.getAttribute(IMarker.CHAR_START, -1);
					if (markerCharStart >= methodSourceRange.getOffset()
							&& markerCharStart <= (methodSourceRange.getOffset() + methodSourceRange.getLength())) {
						markers.add(marker);
					}
				}
				break;
			default:
				break;
			}
		}
		printMarkers(markers);
		return markers.toArray(new IMarker[markers.size()]);
	}

	/**
	 * Finds JAX-RS Markers <strong>at the project level only</strong>, not on
	 * the children resources of this project !
	 * 
	 * @param project
	 * @return
	 * @throws CoreException
	 */
	public static IMarker[] findJaxrsMarkers(IProject project) throws CoreException {
		return project.findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, false, 0);
	}

	/**
	 * @param element
	 * @throws CoreException
	 */
	public static void deleteJaxrsMarkers(final AbstractJaxrsBaseElement element) throws CoreException {
		element.getResource().deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, false,
				IResource.DEPTH_INFINITE);
	}

	/**
	 * @param element
	 * @throws CoreException
	 */
	public static void deleteJaxrsMarkers(final IResource resource) throws CoreException {
		resource.deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, false, IResource.DEPTH_INFINITE);
	}

	/**
	 * Reset JAX-RS Markers on the given {@link IJaxrsMetamodel} and all its children elements.
	 * @param metamodel the metamodel to clean
	 * @throws CoreException
	 */
	public static void deleteJaxrsMarkers(final JaxrsMetamodel metamodel) throws CoreException {
		metamodel.getProject().deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, false, IResource.DEPTH_INFINITE);
		metamodel.resetProblemLevel();
		final List<IJaxrsElement> allElements = metamodel.getAllElements();
		for(IJaxrsElement element : allElements) {
			((AbstractJaxrsBaseElement)element).resetProblemLevel();
		}
	}

	public static Matcher<IMarker[]> hasPreferenceKey(String javaApplicationInvalidTypeHierarchy) {
		return new MarkerPreferenceKeyMatcher(javaApplicationInvalidTypeHierarchy);
	}

	static class MarkerPreferenceKeyMatcher extends BaseMatcher<IMarker[]> {

		final String expectedProblemType;

		MarkerPreferenceKeyMatcher(final String expectedProblemType) {
			this.expectedProblemType = expectedProblemType;
		}

		@Override
		public boolean matches(Object item) {
			if (item instanceof IMarker[]) {
				for (IMarker marker : (IMarker[]) item) {
					final String preferenceKey = marker.getAttribute(
							ValidationErrorManager.PREFERENCE_KEY_ATTRIBUTE_NAME, "");
					if (preferenceKey.equals(expectedProblemType)) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("marker contains preference_key (\"" + expectedProblemType + "\" problem type)");
		}

	}
	
	public static void printMarkers(final List<IMarker> markers) {
		for(IMarker marker: markers) {
			LOGGER.debug(" Marker with severity={}: {}", marker.getAttribute(IMarker.SEVERITY, 0), marker.getAttribute(IMarker.MESSAGE, ""));
		}
	}

}