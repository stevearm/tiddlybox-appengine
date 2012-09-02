package com.horsefire.tiddly.appengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class WikiServlet extends PreferencedServlet {

	private static final Logger LOG = LoggerFactory
			.getLogger(WikiServlet.class);

	private final WikiMorpher m_morpher;

	public WikiServlet() {
		m_morpher = new WikiMorpher();
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
			UserInfoService prefs) throws ServletException, IOException {
		String path = req.getParameter("path");
		if (path != null && !path.isEmpty()) {
			prefs.setWikiPath(path);
		}
		if (prefs.needsAuthorization()) {
			resp.sendRedirect(ServletMapper.HANDSHAKE_ONE);
			return;
		}

		PrintWriter out = resp.getWriter();
		try {
			DropboxService service = new DropboxService(prefs);
			final String contents = service.getText(prefs.getWikiPath());
			out.println(m_morpher.prepareToServe(contents, prefs));
		} catch (OAuthMessageSignerException e) {
			getError(out, e);
		} catch (OAuthExpectationFailedException e) {
			getError(out, e);
		} catch (OAuthCommunicationException e) {
			getError(out, e);
		}
	}

	private void getError(PrintWriter out, Exception e) throws IOException {
		String message = "Problem displaying wiki";
		out.print(message);
		LOG.error(message, e);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp,
			UserInfoService prefs) throws ServletException, IOException {
		if (prefs.needsAuthorization()) {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
					"{\"success\":false,\"message\":\"Have not logged in yet. Please refresh\"}");
			return;
		}
		final PrintWriter out = resp.getWriter();

		try {
			DropboxService service = new DropboxService(prefs);
			String oldStore = service.getText(prefs.getWikiPath());
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					req.getInputStream()));
			String newFile = m_morpher.prepareToSave(oldStore, reader);
			reader.close();
			service.putText(prefs.getWikiPath(), newFile);

			resp.getWriter().print("{\"success\":true}");
		} catch (OAuthMessageSignerException e) {
			postError(out, e);
		} catch (OAuthExpectationFailedException e) {
			postError(out, e);
		} catch (OAuthCommunicationException e) {
			postError(out, e);
		}
	}

	private void postError(PrintWriter out, Exception e) throws IOException {
		out.print("{\"success\":false,\"message\":\"Error during save. See server logs\"}");
		LOG.error("Error during save", e);
	}
}
