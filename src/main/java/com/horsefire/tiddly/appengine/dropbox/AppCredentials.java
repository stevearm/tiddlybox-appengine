package com.horsefire.tiddly.appengine.dropbox;

public class AppCredentials {

	public static final AppCredentials INSTANCE = new AppCredentials();

	private final String m_key = "9vc7omt80m63p6q";
	private final String m_secret = "27riv02ia4c92hf";

	private AppCredentials() {
	}

	public String getKey() {
		return m_key;
	}

	public String getSecret() {
		return m_secret;
	}
}
