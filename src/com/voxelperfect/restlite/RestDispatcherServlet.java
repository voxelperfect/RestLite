package com.voxelperfect.restlite;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import com.voxelperfect.restlite.PathTreeNode.DataRef;
import com.voxelperfect.restlite.RestRegistry.RequestHandler;

public class RestDispatcherServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	static Map<Class<?>, Class<?>> primitiveTypeMap;

	static {
		primitiveTypeMap = new HashMap<Class<?>, Class<?>>(10);
		primitiveTypeMap.put(Integer.TYPE, Integer.class);
		primitiveTypeMap.put(Float.TYPE, Float.class);
		primitiveTypeMap.put(Double.TYPE, Double.class);
	}

	@Override
	public void init() throws ServletException {
		super.init();

		try {
			ServletConfig cfg = getServletConfig();

			String urlPrefix = cfg
					.getInitParameter("com.evorad.aurora.services.rest.UrlPrefix");

			String appClass = cfg.getInitParameter("javax.ws.rs.Application");
			Application app = (Application) Class.forName(appClass)
					.newInstance();
			Set<Class<?>> classes = app.getClasses();

			RestRegistry registry = RestRegistry.getInstance();
			for (Class<?> clazz : classes) {
				registry.registerHandler(urlPrefix, clazz);
			}

		} catch (Exception ex) {
			throw new ServletException(ex);
		}
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		RestRegistry registry = RestRegistry.getInstance();
		String uri = req.getRequestURI().substring(
				req.getContextPath().length());
		DataRef<RequestHandler> ref = registry.getHandler(req.getMethod(), uri);
		if (ref != null) {
			try {
				SecurityContext sc = RestSecurityContext.initialize(req);

				Object result = callHandler(ref, req, sc, resp);

				RequestHandler handler = ref.getData();
				if (handler.handlerMethod.getReturnType().equals(Void.TYPE)) {
					if (resp.getStatus() == HttpServletResponse.SC_OK) {
						resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
					}
				} else if (String.class.isAssignableFrom(result.getClass())) {
					String contentType = "plain/text";
					Produces produces = handler.handlerMethod
							.getAnnotation(Produces.class);
					if (produces != null) {
						contentType = produces.value()[0];
					}

					resp.setContentType(contentType);
					PrintWriter writer = resp.getWriter();
					writer.print((String) result);
					writer.close();
				}
			} catch (RestException rex) {
				rex.toResponse(resp);
			} catch (InvocationTargetException iex) {
				Throwable ex = iex.getTargetException();
				if (RestException.class.isAssignableFrom(ex.getClass())) {
					((RestException) ex).toResponse(resp);
				} else {
					RestException rex = new RestException(
							Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
					rex.toResponse(resp);
				}
			} catch (Exception e) {
				RestException rex = new RestException(
						Status.INTERNAL_SERVER_ERROR, e.getMessage(), e);
				rex.toResponse(resp);
			}
		} else {
			super.service(req, resp);
		}
	}

	protected Object callHandler(DataRef<RequestHandler> ref,
			HttpServletRequest req, SecurityContext sc, HttpServletResponse resp)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, IOException, SecurityException,
			InstantiationException {

		Map<String, String[]> queryParams = req.getParameterMap();
		Map<String, String> pathParams = ref.getPathParams();

		RequestHandler handler = ref.getData();
		Method method = handler.handlerMethod;

		Class<?>[] paramTypes = handler.handlerMethodParamTypes;
		Annotation[][] paramAnnotations = handler.handlerMethodParamAnnotations;

		int count = paramTypes.length;
		Object[] params = new Object[count];
		Annotation a, d;
		String defaultValue, value;
		String[] values;
		for (int p = 0; p < count; p++) {
			a = getAnnotation(QueryParam.class, paramAnnotations[p]);
			if (a != null) {
				values = queryParams.get(((QueryParam) a).value());
				value = (values != null && values.length > 0) ? values[0]
						: null;
				d = getAnnotation(DefaultValue.class, paramAnnotations[p]);
				defaultValue = (d != null) ? ((DefaultValue) d).value() : null;
				params[p] = parseValue(paramTypes[p], value, defaultValue);
				continue;
			}

			a = getAnnotation(PathParam.class, paramAnnotations[p]);
			if (a != null) {
				value = pathParams.get(((PathParam) a).value());
				d = getAnnotation(DefaultValue.class, paramAnnotations[p]);
				defaultValue = (d != null) ? ((DefaultValue) d).value() : null;
				params[p] = parseValue(paramTypes[p], value, defaultValue);
				continue;
			}

			a = getAnnotation(Context.class, paramAnnotations[p]);
			if (a != null) {
				if (paramTypes[p].equals(HttpServletRequest.class)) {
					params[p] = req;
					continue;
				} else if (paramTypes[p].equals(HttpServletResponse.class)) {
					params[p] = resp;
					continue;
				} else if (paramTypes[p].equals(SecurityContext.class)) {
					params[p] = sc;
					continue;
				}
			}

			if (paramTypes[p].equals(String.class)) {
				params[p] = RestTools.readString(req.getInputStream());
			}
		}

		return method.invoke(handler.handlerInstance, params);
	}

	protected Object parseValue(Class<?> type, String stringValue,
			String defaultValue) throws SecurityException,
			IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException {

		if (stringValue == null) {
			stringValue = defaultValue;
		}

		Class<?> mappedType = primitiveTypeMap.get(type);
		if (mappedType != null) {
			type = mappedType;
		}

		try {
			Constructor<?> c = type.getConstructor(String.class);
			return c.newInstance(stringValue);
		} catch (NoSuchMethodException ex) {
		}

		try {
			Method m = type.getMethod("valueOf", String.class);
			return m.invoke(null, stringValue);
		} catch (NoSuchMethodException ex) {
		}

		return null;
	}

	protected Annotation getAnnotation(Class<? extends Annotation> clazz,
			Annotation[] annotations) {

		if (annotations == null) {
			return null;
		}

		for (Annotation a : annotations) {
			if (a.annotationType().equals(clazz)) {
				return a;
			}
		}
		return null;
	}
}
