package com.horsefire.tiddly.appengine.dropbox;

public class AppCredentials {

	public static final AppCredentials INSTANCE = new AppCredentials();

	// Insecure-app
	private final String m_key = "9vc7omt80m63p6q";
	private final String m_secret = "27riv02ia4c92hf";

	// Real keys
	// private final String m_key = "0kjz7fwqtpyra1g";
	// private final String m_secret = "vyiwj7zgaj7huqm";

	private AppCredentials() {
	}

	public String getKey() {
		return m_key;
	}

	public String getSecret() {
		return m_secret;
	}
}
