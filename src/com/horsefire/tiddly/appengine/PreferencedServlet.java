package com.horsefire.tiddly.appengine;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
		UserInfoService prefs = new UserInfoService();
		prefs.init(req, resp);
		if (get) {
			doGet(req, resp, prefs);
		} else {
			doPost(req, resp, prefs);
		}
		prefs.save();
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
			UserInfoService prefs) throws ServletException, IOException {
		throw new UnsupportedOperationException();
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp,
			UserInfoService prefs) throws ServletException, IOException {
		throw new UnsupportedOperationException();
	}
}
