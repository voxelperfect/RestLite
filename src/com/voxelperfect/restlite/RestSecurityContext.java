package com.voxelperfect.restlite;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

public class RestSecurityContext implements SecurityContext {

	private static List<String> adminRoles = new ArrayList<String>(10);

	public static void addAdminRole(String role) {
		adminRoles.add(role);
	}

	public static void removeAdminRole(String role) {
		adminRoles.remove(role);
	}

	private static final ThreadLocal<RestSecurityContext> context = new ThreadLocal<RestSecurityContext>();

	public static RestSecurityContext getCurrent() {
		return context.get();
	}

	public static RestSecurityContext initialize(HttpServletRequest request) {
		RestSecurityContext sc = new RestSecurityContext(request);
		context.set(sc);
		return sc;
	}

	private HttpServletRequest request;
	private Principal principal;
	private String userName;
	private String userIP;
	private boolean isAdmin;

	private RestSecurityContext(HttpServletRequest request) {

		this.request = request;

		principal = request.getUserPrincipal();
		userName = (principal != null) ? principal.getName() : null;
		userIP = request.getRemoteAddr();

		isAdmin = false;
		for (String role : adminRoles) {
			if (request.isUserInRole(role)) {
				isAdmin = true;
				break;
			}
		}
	}

	public final void checkSameIdentity(String userID, String action) {

		if (!sameIdentity(userID)) {
			throw new RestException(Status.FORBIDDEN, "Action '" + action
					+ "' allowed only to a user with the same user-id");
		}
	}

	public final void checkSameIdentityOrAdmin(String userID, String action) {

		if (!sameIdentity(userID) && !isAdmin) {
			throw new RestException(
					Status.FORBIDDEN,
					"Action '"
							+ action
							+ "' allowed only to a user with the same user-id or an administrator");
		}
	}

	public String getUserName() {

		return (userName != null) ? userName : "unauthenticated";
	}

	public void checkAdministrator(String action) {

		if (!isAdmin) {
			throw new RestException(Status.FORBIDDEN, "Action '" + action
					+ "' allowed only to administrators");
		}
	}

	public boolean isAdministrator() {

		return isAdmin;
	}

	private final boolean sameIdentity(String userID) {

		return this.userName.equals(userID);
	}

	public String getUserIP() {

		return userIP != null ? userIP : "unknown";
	}

	@Override
	public String getAuthenticationScheme() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Principal getUserPrincipal() {

		return principal;
	}

	@Override
	public boolean isSecure() {

		return (principal != null);
	}

	@Override
	public boolean isUserInRole(String role) {

		return (request != null) ? request.isUserInRole(role) : false;
	}

}
