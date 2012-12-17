package com.horsefire.tiddly.appengine.dropbox;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.LoggerFactory;

public class AppCredentials {

	public static final AppCredentials INSTANCE = new AppCredentials();

	private final String m_key;
	private final String m_secret;

	private AppCredentials() {
		String key  = "9vc7omt80m63p6q";
		String secret = "27riv02ia4c92hf";
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("appcredentials.properties");
		if (in != null) {
			try {
			Properties overrides = new Properties();
				overrides.load(in);
				in.close();
				
				key = overrides.getProperty("key");
				secret = overrides.getProperty("secret");
			} catch (IOException e) {
				LoggerFactory.getLogger(AppCredentials.class).error("Found override file but error reading from it", e);
			}
		}
		m_key = key;
		m_secret = secret;
	}

	public String getKey() {
		return m_key;
	}

	public String getSecret() {
		return m_secret;
	}
}
