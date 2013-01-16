package com.horsefire.tiddly.appengine.dropbox;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class AppCredentials {

	private static final Logger LOG = LoggerFactory
			.getLogger(AppCredentials.class);
	private static final String FILENAME = "appcredentials.properties";

	private final String m_key;
	private final String m_secret;

	public AppCredentials() {
		String key = "9vc7omt80m63p6q";
		String secret = "27riv02ia4c92hf";
		InputStream in = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(FILENAME);
		if (in == null) {
			LOG.info(FILENAME + " not found. Using default credentials");
		} else {
			try {
				Properties overrides = new Properties();
				overrides.load(in);
				in.close();

				key = overrides.getProperty("key");
				secret = overrides.getProperty("secret");
				LOG.debug("Read app credentials from " + FILENAME);
			} catch (IOException e) {
				LOG.error("Found " + FILENAME + " but error reading from it", e);
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
