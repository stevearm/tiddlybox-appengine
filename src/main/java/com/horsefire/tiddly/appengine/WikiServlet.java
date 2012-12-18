package com.horsefire.tiddly.appengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;

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
import com.horsefire.tiddly.QueryString;
import com.horsefire.tiddly.SingleFileService;
import com.horsefire.tiddly.StatelessWikiService;
import com.horsefire.tiddly.Tiddler;
import com.horsefire.tiddly.TiddlerRenderer;
import com.horsefire.tiddly.TiddlerService;
import com.horsefire.tiddly.Wiki;
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

	private static SingleFileService getFileService(final String path,
			DatastoreService datastore, UserInfoService prefs) {
		final FileService service = new FileService(datastore, prefs,
				new DropboxService(prefs));
		return new SingleFileService() {
			@Override
			public byte[] get() {
				try {
					return service.getFile(path);
				} catch (OAuthMessageSignerException e) {
					throw new RuntimeException(e);
				} catch (OAuthExpectationFailedException e) {
					throw new RuntimeException(e);
				} catch (OAuthCommunicationException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				} catch (ParseException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void put(byte[] file) {
				try {
					service.putFile(path, file);
				} catch (OAuthMessageSignerException e) {
					throw new RuntimeException(e);
				} catch (OAuthExpectationFailedException e) {
					throw new RuntimeException(e);
				} catch (OAuthCommunicationException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				} catch (ParseException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
			DatastoreService datastore, UserInfoService userService)
			throws ServletException, IOException {
		final String path = getPath(req);
		if (userService.needsAuthorization()) {
			resp.sendRedirect(ServletMapper.HANDSHAKE_ONE + "?path="
					+ URLEncoder.encode(path, "UTF-8"));
			return;
		}

		SingleFileService fileService = getFileService(path, datastore,
				userService);
		com.horsefire.tiddly.WikiService wikiService = new StatelessWikiService(
				fileService);
		PrintWriter out = resp.getWriter();
		String queryString = req.getQueryString();
		String tiddlerName = null;
		if (queryString != null && !queryString.isEmpty()) {
			tiddlerName = new QueryString(queryString).getParameter("tiddler");
		}
		if (tiddlerName != null && !tiddlerName.isEmpty()) {
			TiddlerService tiddlerService = new TiddlerService(wikiService);
			Tiddler tiddler = tiddlerService.get(tiddlerName);
			if (tiddler == null) {
				out.print("Tiddler " + tiddlerName + " does not exist");
			} else {
				TiddlerRenderer renderer = new TiddlerRenderer(
						ServletMapper.WIKI + path + "?tiddler=");
				out.print(renderer.render(tiddler));
			}
		} else {
			Wiki wiki = wikiService.get();

			out.print(wiki.getHeader());
			out.print(wiki.getStore());
			out.print(wiki.getPostStore());
			out.print("<script type=\"text/javascript\" src=\"/tiddlybox.js\"></script>");
			out.print(wiki.getPostScript());
		}
	}

	private static String read(BufferedReader in) throws IOException {
		try {
			StringBuilder result = new StringBuilder();
			result.append("\n<div id=\"storeArea\">\n");
			String line;
			while ((line = in.readLine()) != null) {
				result.append(line).append('\n');
			}
			result.append("</div>").append('\n');
			return result.toString();
		} finally {
			in.close();
		}
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

		SingleFileService fileService = getFileService(path, datastore,
				userService);
		
		String queryString = req.getQueryString();
		String tiddlerName = null;
		if (queryString != null && !queryString.isEmpty()) {
			tiddlerName = new QueryString(queryString).getParameter("tiddler");
		}
		if (tiddlerName != null && !tiddlerName.isEmpty()) {
			resp.getWriter()
					.print("{\"success\":false,\"message\":\"Saving tiddlers not yet supported\"}");
		} else {
			StatelessWikiService statelessWikiService = new StatelessWikiService(
					fileService);

			Wiki wiki = statelessWikiService.get();
			String store = read(req.getReader());
			LOG.debug("Read store: {}", store);
			wiki = wiki.setStore(store);
			statelessWikiService.put(wiki);

			resp.getWriter().print("{\"success\":true}");
		}
	}
}