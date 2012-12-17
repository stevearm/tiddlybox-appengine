package com.horsefire.tiddly.appengine.dropbox;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.horsefire.tiddly.appengine.UserInfoService;

public class DropboxService {

	public static final String META_KEY_REV = "rev";
	public static final String META_KEY_BYTES = "bytes";

	private static final Logger LOG = LoggerFactory
			.getLogger(DropboxService.class);

	private final OAuthConsumer m_consumer;

	public DropboxService(UserInfoService userService) {
		m_consumer = new DefaultOAuthConsumer(AppCredentials.INSTANCE.getKey(),
				AppCredentials.INSTANCE.getSecret());
		m_consumer.setTokenWithSecret(userService.getOauthTokenKey(),
				userService.getOauthTokenSecret());
	}

	public byte[] getBytes(String path) throws IOException,
			OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException {
		LOG.debug("Getting dropbox file {}", path);
		final URL url = new URL(
				"https://api-content.dropbox.com/0/files/dropbox" + path);
		final HttpURLConnection connection = (HttpURLConnection) url
				.openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("GET");

		m_consumer.sign(connection);
		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			return IOUtils.toByteArray(connection.getInputStream());
		}
		LOG.error("Error getting file with path {}. Got response {}", url,
				connection.getResponseCode());
		throw new IOException();
	}

	public JSONObject putBytes(String path, byte[] contents)
			throws IOException, OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException,
			ParseException {
		LOG.debug("Saving dropbox file {}", path);
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

		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			Object object = new JSONParser().parse(new InputStreamReader(
					connection.getInputStream()));
			if (!(object instanceof JSONObject)) {
				throw new IOException("Metadata response must be a json object");
			}
			return (JSONObject) object;
		}
		LOG.error(
				"Failed to save {} to {}: {}",
				new Object[] { connection.getResponseCode(), url,
						new String(IOUtils.toByteArray(connection.getInputStream())) });
		throw new IOException();
	}

	public JSONObject getMetadata(String path) throws IOException,
			OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException, ParseException {
		LOG.debug("Getting dropbox metadata for {}", path);
		final URL url = new URL("https://api.dropbox.com/1/metadata/dropbox"
				+ path);
		final HttpURLConnection connection = (HttpURLConnection) url
				.openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("GET");

		m_consumer.sign(connection);
		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			Object object = new JSONParser().parse(new InputStreamReader(
					connection.getInputStream()));
			if (!(object instanceof JSONObject)) {
				throw new IOException("Metadata response must be a json object");
			}
			return (JSONObject) object;
		}
		LOG.error("Error getting metadata for {}. Got response {}", url,
				connection.getResponseCode());
		throw new IOException();
	}
}
