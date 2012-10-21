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

import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.datastore.DatastoreService;
import com.horsefire.tiddly.appengine.dropbox.DropboxService;

@SuppressWarnings("serial")
public class WikiServlet extends PreferencedServlet {

	private static final Logger LOG = LoggerFactory
			.getLogger(WikiServlet.class);

	private static String getPath(HttpServletRequest req) {
		String wikiPath = req.getPathInfo();
		if (wikiPath != null && !wikiPath.endsWith(".html")
				&& !wikiPath.endsWith(".htm")) {
			throw new IllegalArgumentException(
					"Path must be an htm or html file");
		}
		return wikiPath;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
			DatastoreService datastore, UserInfoService prefs)
			throws ServletException, IOException {
		String path = getPath(req);
		if (prefs.needsAuthorization()) {
			resp.sendRedirect(ServletMapper.HANDSHAKE_ONE);
			return;
		}

		PrintWriter out = resp.getWriter();
		try {
			FileService service = new FileService(datastore, prefs,
					new DropboxService(prefs));
			WikiService wikiService = new WikiService(service);
			out.println(wikiService.prepareToServe(path));
		} catch (OAuthMessageSignerException e) {
			getError(out, e);
		} catch (OAuthExpectationFailedException e) {
			getError(out, e);
		} catch (OAuthCommunicationException e) {
			getError(out, e);
		} catch (ParseException e) {
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
			DatastoreService datastore, UserInfoService userService)
			throws ServletException, IOException {
		String path = getPath(req);
		if (userService.needsAuthorization()) {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
					"{\"success\":false,\"message\":\"Have not logged in yet. Please refresh\"}");
			return;
		}
		final PrintWriter out = resp.getWriter();

		try {
			FileService fileService = new FileService(datastore, userService,
					new DropboxService(userService));
			WikiService service = new WikiService(fileService);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					req.getInputStream()));
			service.saveNewStore(reader, path);
			reader.close();

			resp.getWriter().print("{\"success\":true}");
		} catch (OAuthMessageSignerException e) {
			postError(out, e);
		} catch (OAuthExpectationFailedException e) {
			postError(out, e);
		} catch (OAuthCommunicationException e) {
			postError(out, e);
		} catch (ParseException e) {
			postError(out, e);
		}
	}

	private void postError(PrintWriter out, Exception e) throws IOException {
		out.print("{\"success\":false,\"message\":\"Error during save. See server logs\"}");
		LOG.error("Error during save", e);
	}
}
