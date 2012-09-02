package com.horsefire.tiddly.appengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DropboxService {

	private static final Logger LOG = LoggerFactory
			.getLogger(DropboxService.class);

	private final OAuthConsumer m_consumer;

	public DropboxService(UserInfoService userService) {
		m_consumer = new DefaultOAuthConsumer(AppCredentials.INSTANCE.getKey(),
				AppCredentials.INSTANCE.getSecret());
		m_consumer.setTokenWithSecret(userService.getOauthTokenKey(),
				userService.getOauthTokenSecret());
	}

	private InputStream getBytes(String path) throws IOException,
			OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException {
		final URL url = new URL(DropboxWikiClient.FILE_BASE_URL + path);
		final HttpURLConnection connection = (HttpURLConnection) url
				.openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("GET");

		m_consumer.sign(connection);
		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			return connection.getInputStream();
		}
		LOG.error("Error getting wiki with path {}. Got response {}", url,
				connection.getResponseCode());
		throw new IOException();
	}

	private static String getString(InputStream in2) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(in2));
		try {
			StringBuilder wikiContents = new StringBuilder();
			String line = in.readLine();
			while (line != null) {
				wikiContents.append(line).append('\n');
				line = in.readLine();
			}
			return wikiContents.toString();
		} finally {
			in.close();
		}
	}

	public String getText(String path) throws OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException,
			IOException {
		return getString(getBytes(path));
	}

	public void putBytes(String path, byte[] contents) throws IOException,
			OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException {
		final URL url = new URL(
				"https://api-content.dropbox.com/1/files_put/dropbox" + path);
		final HttpURLConnection connection = (HttpURLConnection) url
				.openConnection();
		connection.setRequestMethod("PUT");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestProperty("Content-Length", "" + contents.length);

		m_consumer.sign(connection);

		OutputStream out = connection.getOutputStream();
		out.write(contents);
		out.close();
		int responseCode = connection.getResponseCode();
		if (responseCode != HttpServletResponse.SC_OK) {
			LOG.warn("Failed to save {} to {}: {}", new Object[] {
					responseCode, url, getString(connection.getInputStream()) });
			throw new IOException("Error saving file to " + url);
		}
	}

	public void putBytes(String path, InputStream stream) {

	}

	public void putText(String path, String contents) throws IOException,
			OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException {
		putBytes(path, contents.getBytes());
	}
}
