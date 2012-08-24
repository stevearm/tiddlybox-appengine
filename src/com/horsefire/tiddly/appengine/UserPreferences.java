package com.horsefire.tiddly.appengine;

import com.google.appengine.api.datastore.Entity;

public class UserPreferences {

	private static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}

	private static void assertNotEmpty(String string, String title) {
		if (isEmpty(string)) {
			throw new IllegalArgumentException(title + " can't be null");
		}
	}

	public static final String KEY_WIKI_PATH = "wikiPath";
	public static final String KEY_OAUTH_TOKEN_KEY = "oauthTokenKey";
	public static final String KEY_OAUTH_TOKEN_SECRET = "oauthTokenSecret";

	private Entity m_entity;
	private String m_wikiPath;
	private String m_oauthTokenKey;
	private String m_oauthTokenSecret;

	public UserPreferences(Entity entity) {
		m_entity = entity;
		m_wikiPath = (String) entity.getProperty(UserPreferences.KEY_WIKI_PATH);
		m_oauthTokenKey = (String) entity
				.getProperty(UserPreferences.KEY_OAUTH_TOKEN_KEY);
		m_oauthTokenSecret = (String) entity
				.getProperty(UserPreferences.KEY_OAUTH_TOKEN_SECRET);
	}

	public Entity getEntity() {
		m_entity.setProperty(UserPreferences.KEY_WIKI_PATH, m_wikiPath);
		m_entity.setProperty(UserPreferences.KEY_OAUTH_TOKEN_KEY,
				m_oauthTokenKey);
		m_entity.setProperty(UserPreferences.KEY_OAUTH_TOKEN_SECRET,
				m_oauthTokenSecret);
		return m_entity;
	}

	public boolean needsAuthorization() {
		return isEmpty(m_oauthTokenKey) || isEmpty(m_oauthTokenSecret);
	}

	public String getWikiPath() {
		return m_wikiPath;
	}

	public String getFullWikiPath() {
		final String WIKI_URL = "/wiki/";
		if (isEmpty(m_wikiPath)) {
			return WIKI_URL;
		}
		if (m_wikiPath.charAt(0) == '/') {
			return WIKI_URL + m_wikiPath.substring(1);
		}
		return WIKI_URL + m_wikiPath;
	}

	public void setWikiPath(String wikiPath) {
		assertNotEmpty(wikiPath, "Path");
		if (!wikiPath.endsWith(".html") && !wikiPath.endsWith(".htm")) {
			throw new IllegalArgumentException(
					"Path must be an htm or html file");
		}
		m_wikiPath = wikiPath;
	}

	public String getOauthTokenKey() {
		return m_oauthTokenKey;
	}

	public void setOauthTokenKey(String oauthTokenKey) {
		assertNotEmpty(oauthTokenKey, "Key");
		m_oauthTokenKey = oauthTokenKey;
	}

	public String getOauthTokenSecret() {
		return m_oauthTokenSecret;
	}

	public void setOauthTokenSecret(String oauthTokenSecret) {
		assertNotEmpty(oauthTokenSecret, "Secret");
		m_oauthTokenSecret = oauthTokenSecret;
	}
}
