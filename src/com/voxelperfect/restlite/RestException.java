package com.voxelperfect.restlite;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestException extends RuntimeException {
	private static final long serialVersionUID = 5236460039968687796L;

	private static final Logger log = LoggerFactory
			.getLogger(RestException.class);

	Status status;
	String userName;

	public RestException(Status status, String message) {

		super(message);

		this.status = status;

		extractUserName();

		log.error(getUserContext() + status + ": " + message);
	}

	public RestException(Status status, Throwable ex) {

		this(status, (ex.getMessage() != null) ? ex.getMessage() : "", ex);
	}

	public RestException(Status status, String message, Throwable ex) {

		super(message, ex);

		this.status = status;

		extractUserName();

		log.error(getUserContext() + status + ": " + message, ex);
	}

	private void extractUserName() {
	
		SecurityContext sc = RestSecurityContext.getCurrent();
		Principal user = (sc != null) ? sc.getUserPrincipal() : null;
		this.userName = (user != null) ? user.getName() : null;
	}
	
	private String getUserContext() {

		if (userName == null || userName.equals(""))
			return "[<unauthenticated>]: ";
		return "[" + userName + "]: ";
	}

	public Status getStatus() {
		return status;
	}

	public void toResponse(HttpServletResponse resp) throws IOException {

		String message = getMessage();
		if (message == null) {
			message = "";
		}

		String entity = RestTools.errorResultToJson(status, message);

		resp.setStatus(status.getStatusCode());
		resp.setContentType("application/json");
		PrintWriter writer = resp.getWriter();
		writer.print(entity);
		writer.close();
	}
}
