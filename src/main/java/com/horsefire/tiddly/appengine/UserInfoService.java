package com.horsefire.tiddly.appengine;

import java.util.Date;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class UserInfoService {

	public static final String COOKIE_KEY = "tiddlyboxkey";
	public static final String DB_KEY_OAUTH_TOKEN_KEY = "oauthTokenKey";
	public static final String DB_KEY_OAUTH_TOKEN_SECRET = "oauthTokenSecret";

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

	private static final String KEY_COOKIE_EXPIRY = "cookieExpires";

	private final DatastoreService m_datastore;
	private Entity m_entity;


	public UserInfoService(DatastoreService datastore) {
		m_datastore = datastore;
	}

	private String getSessionKey(HttpServletRequest req,
			HttpServletResponse resp) {
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (COOKIE_KEY.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		String value = UUID.randomUUID().toString();
		Cookie cookie = new Cookie(COOKIE_KEY, value);
		cookie.setPath("/");
		cookie.setMaxAge(EXPIRE_SECONDS);
		resp.addCookie(cookie);
		return value;
	}

	public void init(HttpServletRequest req, HttpServletResponse resp) {
		String sessionKey = getSessionKey(req, resp);
		Key dbKey = KeyFactory.createKey("Prefs", sessionKey);
		try {
			m_entity = m_datastore.get(dbKey);
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
		m_datastore.put(m_entity);
	}

	public boolean needsAuthorization() {
		return getOauthTokenKey().isEmpty() || getOauthTokenSecret().isEmpty();
	}

	public String getOauthTokenKey() {
		String oauthTokenKey = (String) m_entity
				.getProperty(UserInfoService.DB_KEY_OAUTH_TOKEN_KEY);
		if (oauthTokenKey == null) {
			return "";
		}
		return oauthTokenKey;
	}

	public void setOauthTokenKey(String oauthTokenKey) {
		assertNotEmpty(oauthTokenKey, "Key");
		m_entity.setProperty(UserInfoService.DB_KEY_OAUTH_TOKEN_KEY, oauthTokenKey);
	}

	public String getOauthTokenSecret() {
		String oauthTokenSecret = (String) m_entity
				.getProperty(UserInfoService.DB_KEY_OAUTH_TOKEN_SECRET);
		if (oauthTokenSecret == null) {
			return "";
		}
		return oauthTokenSecret;
	}

	public void setOauthTokenSecret(String oauthTokenSecret) {
		assertNotEmpty(oauthTokenSecret, "Secret");
		m_entity.setProperty(UserInfoService.DB_KEY_OAUTH_TOKEN_SECRET,
				oauthTokenSecret);
	}

	public Key getDbKey() {
		return m_entity.getKey();
	}
}
