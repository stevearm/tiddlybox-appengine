package com.horsefire.tiddly.appengine;

import java.util.Date;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class UserInfoService {

	private static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}

	private static void assertNotEmpty(String string, String title) {
		if (isEmpty(string)) {
			throw new IllegalArgumentException(title + " can't be null");
		}
	}

	private static final int EXPIRE_SECONDS = 60 /* sec/min */
	* 60 /* min/hour */
	* 24 /* hour/day */
	* 60 /* days */;

	private static final String KEY_WIKI_PATH = "wikiPath";
	private static final String KEY_OAUTH_TOKEN_KEY = "oauthTokenKey";
	private static final String KEY_OAUTH_TOKEN_SECRET = "oauthTokenSecret";
	private static final String KEY_COOKIE_EXPIRY = "cookieExpires";

	private final DatastoreService m_service;
	private Entity m_entity;

	public UserInfoService() {
		m_service = DatastoreServiceFactory.getDatastoreService();
	}

	private String getSessionKey(HttpServletRequest req,
			HttpServletResponse resp) {
		final String cookieKey = "tiddlyboxkey";
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookieKey.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		String value = UUID.randomUUID().toString();
		Cookie cookie = new Cookie(cookieKey, value);
		cookie.setPath("/");
		cookie.setMaxAge(EXPIRE_SECONDS);
		resp.addCookie(cookie);
		return value;
	}

	public void init(HttpServletRequest req, HttpServletResponse resp) {
		String sessionKey = getSessionKey(req, resp);
		Key dbKey = KeyFactory.createKey("Prefs", sessionKey);
		try {
			m_entity = m_service.get(dbKey);
		} catch (EntityNotFoundException e) {
			m_entity = new Entity(dbKey);
		}
		if (m_entity.getProperty(KEY_COOKIE_EXPIRY) == null) {
			Date expiry = new Date(System.currentTimeMillis()
					+ (EXPIRE_SECONDS * 1000));
			m_entity.setProperty(KEY_COOKIE_EXPIRY, expiry);
		}
	}

	public void save() {
		m_service.put(m_entity);
	}

	public boolean needsAuthorization() {
		return getOauthTokenKey().isEmpty() || getOauthTokenSecret().isEmpty();
	}

	public String getWikiPath() {
		String wikiPath = (String) m_entity
				.getProperty(UserInfoService.KEY_WIKI_PATH);
		if (wikiPath == null) {
			return "";
		}
		return wikiPath;
	}

	public void setWikiPath(String wikiPath) {
		assertNotEmpty(wikiPath, "Path");
		if (!wikiPath.endsWith(".html") && !wikiPath.endsWith(".htm")) {
			throw new IllegalArgumentException(
					"Path must be an htm or html file");
		}
		m_entity.setProperty(UserInfoService.KEY_WIKI_PATH, wikiPath);
	}

	public String getOauthTokenKey() {
		String oauthTokenKey = (String) m_entity
				.getProperty(UserInfoService.KEY_OAUTH_TOKEN_KEY);
		if (oauthTokenKey == null) {
			return "";
		}
		return oauthTokenKey;
	}

	public void setOauthTokenKey(String oauthTokenKey) {
		assertNotEmpty(oauthTokenKey, "Key");
		m_entity.setProperty(UserInfoService.KEY_OAUTH_TOKEN_KEY, oauthTokenKey);
	}

	public String getOauthTokenSecret() {
		String oauthTokenSecret = (String) m_entity
				.getProperty(UserInfoService.KEY_OAUTH_TOKEN_SECRET);
		if (oauthTokenSecret == null) {
			return "";
		}
		return oauthTokenSecret;
	}

	public void setOauthTokenSecret(String oauthTokenSecret) {
		assertNotEmpty(oauthTokenSecret, "Secret");
		m_entity.setProperty(UserInfoService.KEY_OAUTH_TOKEN_SECRET,
				oauthTokenSecret);
	}
}
