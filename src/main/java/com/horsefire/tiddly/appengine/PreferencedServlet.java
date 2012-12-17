package com.horsefire.tiddly.appengine;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;

@SuppressWarnings("serial")
public abstract class PreferencedServlet extends HttpServlet {

	@Override
	protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		service(req, resp, true);
	}

	@Override
	protected final void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		service(req, resp, false);
	}

	private void service(HttpServletRequest req, HttpServletResponse resp,
			boolean get) throws ServletException, IOException {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		UserInfoService userService = new UserInfoService(datastore);
		userService.init(req, resp);
		if (get) {
			doGet(req, resp, datastore, userService);
		} else {
			doPost(req, resp, datastore, userService);
		}
		userService.save();
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
			DatastoreService datastore, UserInfoService prefs)
			throws ServletException, IOException {
		doGet(req, resp, prefs);
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
			UserInfoService prefs) throws ServletException, IOException {
		throw new UnsupportedOperationException();
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp,
			DatastoreService datastore, UserInfoService prefs)
			throws ServletException, IOException {
		doPost(req, resp, prefs);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp,
			UserInfoService prefs) throws ServletException, IOException {
		throw new UnsupportedOperationException();
	}
}
