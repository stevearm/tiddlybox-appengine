package com.horsefire.tiddly.appengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class WikiServlet extends PreferencedServlet {

	private static final Logger LOG = LoggerFactory
			.getLogger(WikiServlet.class);

	public static String extractString(InputStream in2) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(in2));

		StringBuilder wikiContents = new StringBuilder();
		String line = in.readLine();
		while (line != null) {
			wikiContents.append(line).append('\n');
			line = in.readLine();
		}
		in.close();
		return wikiContents.toString();
	}

	private static String getUpdatedWikiContents(UserInfoService prefs)
			throws IOException, OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException {
		final OAuthConsumer consumer = new DefaultOAuthConsumer(
				AppCredentials.INSTANCE.getKey(),
				AppCredentials.INSTANCE.getSecret());
		consumer.setTokenWithSecret(prefs.getOauthTokenKey(),
				prefs.getOauthTokenSecret());

		final URL url = new URL(DropboxWikiClient.FILE_BASE_URL
				+ prefs.getWikiPath());
		final HttpURLConnection connection = (HttpURLConnection) url
				.openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("GET");

		consumer.sign(connection);
		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			String contents = extractString(connection.getInputStream());
			return contents;
		}
		LOG.error("Error getting wiki. Got response {}",
				connection.getResponseCode());
		throw new IOException();
	}

	private final WikiMorpher m_morpher;

	public WikiServlet() {
		m_morpher = new WikiMorpher();
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
			UserInfoService prefs) throws ServletException, IOException {
		prefs.setWikiPath(req.getPathInfo());
		if (prefs.needsAuthorization()) {
			resp.sendRedirect("/handshake/one");
			return;
		}

		PrintWriter out = resp.getWriter();
		try {
			final String contents = getUpdatedWikiContents(prefs);
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

	private void pushWikiContents(UserInfoService prefs, String content)
			throws IOException, OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException {
		OAuthConsumer consumer = new DefaultOAuthConsumer(
				AppCredentials.INSTANCE.getKey(),
				AppCredentials.INSTANCE.getSecret());
		consumer.setTokenWithSecret(prefs.getOauthTokenKey(),
				prefs.getOauthTokenSecret());

		String filename = prefs.getWikiPath(); // "text.txt";

		final URL url = new URL(
				"https://api-content.dropbox.com/1/files_put/dropbox"
						+ filename);
		final HttpURLConnection connection = (HttpURLConnection) url
				.openConnection();
		connection.setRequestMethod("PUT");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestProperty("Content-Length", ""
				+ content.getBytes().length);

		consumer.sign(connection);

		Writer out = new OutputStreamWriter(connection.getOutputStream());
		out.write(content);
		out.close();
		int responseCode = connection.getResponseCode();
		if (responseCode != HttpServletResponse.SC_OK) {
			LOG.warn("Failed to save {}: {}", responseCode,
					WikiServlet.extractString(connection.getInputStream()));
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp,
			UserInfoService prefs) throws ServletException, IOException {
		prefs.setWikiPath(req.getPathInfo());
		if (prefs.needsAuthorization()) {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
					"{\"success\":false,\"message\":\"Have not logged in yet. Please refresh\"}");
			return;
		}
		final PrintWriter out = resp.getWriter();

		final String store = extractString(req.getInputStream());

		try {
			String newFile = m_morpher.prepareToSave(
					getUpdatedWikiContents(prefs), store);
			out.println(newFile);
			pushWikiContents(prefs, newFile);
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
