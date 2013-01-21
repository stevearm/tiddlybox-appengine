package com.horsefire.tiddly.appengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.horsefire.tiddly.QueryString;
import com.horsefire.tiddly.SingleFileService;
import com.horsefire.tiddly.StatelessWikiService;
import com.horsefire.tiddly.Tiddler;
import com.horsefire.tiddly.TiddlerRenderer;
import com.horsefire.tiddly.TiddlerService;
import com.horsefire.tiddly.Wiki;
import com.horsefire.tiddly.appengine.dropbox.DropboxService;
import com.horsefire.tiddly.appengine.dropbox.DropboxService.UnauthorizedException;

@SuppressWarnings("serial")
@Singleton
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
			DatastoreService datastore, UserInfoService prefs,
			DropboxService dropboxService) {
		final FileService service = new FileService(datastore, prefs,
				dropboxService);
		return new SingleFileService() {
			@Override
			public byte[] get() {
				try {
					return service.getFile(path);
				} catch (UnauthorizedException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void put(byte[] file) {
				try {
					service.putFile(path, file);
				} catch (UnauthorizedException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	private final DropboxService m_dropboxService;

	@Inject
	public WikiServlet(DropboxService dropboxService) {
		m_dropboxService = dropboxService;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
			DatastoreService datastore, UserInfoService userService)
			throws ServletException, IOException {
		final String path = getPath(req);
		if (userService.needsAuthorization()) {
			resp.sendRedirect(BootstrapListener.HANDSHAKE_ONE_URL + "?path="
					+ URLEncoder.encode(path, "UTF-8"));
			return;
		}

		SingleFileService fileService = getFileService(path, datastore,
				userService, m_dropboxService);
		com.horsefire.tiddly.WikiService wikiService = new StatelessWikiService(
				fileService);
		PrintWriter out = resp.getWriter();
		String queryString = req.getQueryString();
		String tiddlerName = null;
		QueryString queryStringParsed = null;
		if (queryString != null && !queryString.isEmpty()) {
			queryStringParsed = new QueryString(queryString);
			tiddlerName = queryStringParsed.getParameter("tiddler");
		}
		if (tiddlerName != null && !tiddlerName.isEmpty()) {
			TiddlerService tiddlerService = new TiddlerService(wikiService);
			Tiddler tiddler = tiddlerService.get(tiddlerName);
			if (tiddler == null) {
				out.print("Tiddler " + tiddlerName + " does not exist");
			} else {
				if ("true".equals(queryStringParsed.getParameter("edit"))) {
					out.print("<html>");
					out.print("<head><style>textarea{width:600px;height:200px}</style></head>");
					out.print("<body><form method=\"post\" action=\"");
					out.print(BootstrapListener.WIKI_URL + path + "?"
							+ queryString);
					out.print("\"><textarea name=\"content\">");
					out.print(tiddler.content);
					out.print("</textarea><input type=\"submit\"/></body></html>");
				} else {
					TiddlerRenderer renderer = new TiddlerRenderer(
							BootstrapListener.WIKI_URL + path + "?tiddler=");
					out.print(renderer.render(tiddler));
				}
			}
		} else {
			WikiServletNode node = new WikiServletNode(out, wikiService);
			node.render();
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
				userService, m_dropboxService);

		String queryString = req.getQueryString();
		String tiddlerName = null;
		if (queryString != null && !queryString.isEmpty()) {
			tiddlerName = new QueryString(queryString).getParameter("tiddler");
		}
		if (tiddlerName != null && !tiddlerName.isEmpty()) {
			String content = req.getParameter("content");
			TiddlerService tiddlerService = new TiddlerService(
					new StatelessWikiService(fileService));
			Tiddler tiddler = tiddlerService.get(tiddlerName);
			int changeCount = Integer.parseInt(tiddler.changecount);
			changeCount++;
			tiddler.changecount = Integer.toString(changeCount);
			tiddler.content = content.replaceAll("\r\n", "\n");
			tiddlerService.put(tiddler);
			resp.getWriter().print("Saved");
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
