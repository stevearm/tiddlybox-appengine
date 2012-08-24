package com.horsefire.tiddly.appengine;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

@SuppressWarnings("serial")
public abstract class PreferencedServlet extends HttpServlet {

	private Key getPrefsKey(HttpServletRequest req, HttpServletResponse resp) {
		final String cookieKey = "tiddlyboxkey";
		String value = null;
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookieKey.equals(cookie.getName())) {
					value = cookie.getValue();
					break;
				}
			}
		}
		if (value == null) {
			value = UUID.randomUUID().toString();
			Cookie cookie = new Cookie(cookieKey, value);
			cookie.setPath("/");
			resp.addCookie(cookie);
		}
		return KeyFactory.createKey("Prefs", value);
	}

	private UserPreferences getPrefs(HttpServletRequest req,
			HttpServletResponse resp, DatastoreService service) {
		Key key = getPrefsKey(req, resp);
		Entity entity;
		try {
			entity = service.get(key);
		} catch (EntityNotFoundException e) {
			entity = new Entity(key);
		}
		return new UserPreferences(entity);
	}

	private void save(UserPreferences prefs, DatastoreService service) {
		service.put(prefs.getEntity());
	}

	@Override
	protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		UserPreferences prefs = getPrefs(req, resp, datastore);
		if (prefs != null) {
			doGet(req, resp, prefs);
			save(prefs, datastore);
		}
	}

	@Override
	protected final void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		UserPreferences prefs = getPrefs(req, resp, datastore);
		if (prefs != null) {
			doPost(req, resp, prefs);
			save(prefs, datastore);
		}
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
			UserPreferences prefs) throws ServletException, IOException {
		throw new UnsupportedOperationException();
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp,
			UserPreferences prefs) throws ServletException, IOException {
		throw new UnsupportedOperationException();
	}
}
