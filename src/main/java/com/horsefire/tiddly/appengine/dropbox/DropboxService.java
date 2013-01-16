package com.horsefire.tiddly.appengine.dropbox;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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

import com.google.inject.Inject;

public class DropboxService {

	public static final String URL_REQUEST_TOKEN = "http://api.getdropbox.com/0/oauth/request_token";
	public static final String URL_ACCESS_TOKEN = "http://api.getdropbox.com/0/oauth/access_token";
	public static final String URL_AUTHORIZATION = "http://api.getdropbox.com/0/oauth/authorize";

	public static final String META_KEY_REV = "rev";
	public static final String META_KEY_BYTES = "bytes";

	@SuppressWarnings("serial")
	public static class UnauthorizedException extends IOException {
		public UnauthorizedException() {
			super("Not authorized");
		}
	}

	private static final Logger LOG = LoggerFactory
			.getLogger(DropboxService.class);

	private final AppCredentials m_appCredentials;

	@Inject
	public DropboxService(AppCredentials appCredentials) {
		m_appCredentials = appCredentials;
	}

	public byte[] getBytes(String key, String secret, String path)
			throws IOException, UnauthorizedException {
		DefaultOAuthConsumer consumer = new DefaultOAuthConsumer(
				m_appCredentials.getKey(), m_appCredentials.getSecret());
		consumer.setTokenWithSecret(key, secret);

		LOG.debug("Getting dropbox file {}", path);
		final URL url = new URL(
				"https://api-content.dropbox.com/0/files/dropbox" + path);
		final HttpURLConnection connection = (HttpURLConnection) url
				.openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("GET");

		try {
			consumer.sign(connection);
		} catch (OAuthMessageSignerException e) {
			throw new IOException("Security problem signing request", e);
		} catch (OAuthExpectationFailedException e) {
			throw new IOException("Security problem signing request", e);
		} catch (OAuthCommunicationException e) {
			throw new IOException("Security problem signing request", e);
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			return IOUtils.toByteArray(connection.getInputStream());
		}

		LOG.error(
				"Getting {} returned {}: {}",
				new Object[] {
						url,
						responseCode,
						new String(IOUtils.toByteArray(connection
								.getInputStream())) });
		if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
			throw new UnauthorizedException();
		}
		throw new IOException();
	}

	public JSONObject putBytes(String key, String secret, String path,
			byte[] contents) throws IOException, UnauthorizedException {
		LOG.debug("Saving dropbox file {}", path);
		final URL url = new URL(
				"https://api-content.dropbox.com/1/files_put/dropbox" + path);
		final HttpURLConnection connection = (HttpURLConnection) url
				.openConnection();
		connection.setRequestMethod("PUT");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestProperty("Content-Length", "" + contents.length);

		try {
			DefaultOAuthConsumer consumer = new DefaultOAuthConsumer(
					m_appCredentials.getKey(), m_appCredentials.getSecret());
			consumer.setTokenWithSecret(key, secret);
			consumer.sign(connection);
		} catch (OAuthMessageSignerException e) {
			throw new IOException("Security problem signing request", e);
		} catch (OAuthExpectationFailedException e) {
			throw new IOException("Security problem signing request", e);
		} catch (OAuthCommunicationException e) {
			throw new IOException("Security problem signing request", e);
		}

		OutputStream out = connection.getOutputStream();
		out.write(contents);
		out.close();

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try {
				Object object = new JSONParser().parse(new InputStreamReader(
						connection.getInputStream()));
				if (!(object instanceof JSONObject)) {
					throw new IOException(
							"Metadata response must be a json object");
				}
				return (JSONObject) object;
			} catch (ParseException e) {
				throw new IOException("Response is not parsable json", e);
			}
		}
		LOG.error(
				"Saving to {} returned {}: {}",
				new Object[] {
						url,
						responseCode,
						new String(IOUtils.toByteArray(connection
								.getInputStream())) });
		if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
			throw new UnauthorizedException();
		}
		throw new IOException();
	}

	public JSONObject getMetadata(String key, String secret, String path)
			throws IOException, UnauthorizedException {
		LOG.debug("Getting dropbox metadata for {}", path);
		final URL url = new URL("https://api.dropbox.com/1/metadata/dropbox"
				+ path);
		final HttpURLConnection connection = (HttpURLConnection) url
				.openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("GET");

		try {
			DefaultOAuthConsumer consumer = new DefaultOAuthConsumer(
					m_appCredentials.getKey(), m_appCredentials.getSecret());
			consumer.setTokenWithSecret(key, secret);
			consumer.sign(connection);
		} catch (OAuthMessageSignerException e) {
			throw new IOException("Security problem signing request", e);
		} catch (OAuthExpectationFailedException e) {
			throw new IOException("Security problem signing request", e);
		} catch (OAuthCommunicationException e) {
			throw new IOException("Security problem signing request", e);
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try {
				Object object = new JSONParser().parse(new InputStreamReader(
						connection.getInputStream()));
				if (!(object instanceof JSONObject)) {
					throw new IOException(
							"Metadata response must be a json object");
				}
				return (JSONObject) object;
			} catch (ParseException e) {
				throw new IOException("Response is not parsable json", e);
			}
		}
		LOG.error(
				"Getting metadata for {} returned {}: {}",
				new Object[] {
						url,
						responseCode,
						new String(IOUtils.toByteArray(connection
								.getInputStream())) });
		if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
			throw new UnauthorizedException();
		}
		throw new IOException();
	}
}
