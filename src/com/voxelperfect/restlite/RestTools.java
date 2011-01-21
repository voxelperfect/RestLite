package com.voxelperfect.restlite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.ws.rs.core.Response.Status;

public class RestTools {

	public static String readString(InputStream is) throws IOException {

		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		StringBuffer str = new StringBuffer();
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			str.append(inputLine).append(' ');
		}
		in.close();

		return str.toString();
	}

	public static String errorResultToJson(Status status, String reason) {

		return "{  \"error\": \"" + status + "\"," + " \"reason\": \"" + reason
				+ "\"}";
	}
}
