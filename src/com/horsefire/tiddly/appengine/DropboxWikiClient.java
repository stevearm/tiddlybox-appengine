package com.horsefire.tiddly.appengine;

import java.io.IOException;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DropboxWikiClient {

	public static final String VALUE_REQUEST_TOKEN_URL = "http://api.getdropbox.com/0/oauth/request_token";
	public static final String VALUE_ACCESS_TOKEN_URL = "http://api.getdropbox.com/0/oauth/access_token";
	public static final String VALUE_AUTHORIZATION_URL = "http://api.getdropbox.com/0/oauth/authorize";
	public static final String FILE_BASE_URL = "https://api-content.dropbox.com/0/files/dropbox";

	private static final Logger LOG = LoggerFactory
			.getLogger(DropboxWikiClient.class);

	private final OAuthConsumer m_consumer;
	private final String m_wikiPath;
	private final String m_wikiFileName;

	public DropboxWikiClient(String oauthKey, String oauthSecret,
			String wikiPath) {
		m_consumer = new CommonsHttpOAuthConsumer(
				AppCredentials.INSTANCE.getKey(),
				AppCredentials.INSTANCE.getSecret());
		m_consumer.setTokenWithSecret(oauthKey, oauthSecret);

		int index = wikiPath.lastIndexOf("/");
		if (index == -1) {
			throw new IllegalArgumentException("Path must have a / in it");
		}
		m_wikiPath = wikiPath.substring(0, index);
		m_wikiFileName = wikiPath.substring(index + 1);
	}

	public void pushWiki(String wiki) throws IOException {
		throw new UnsupportedOperationException();
	}
}
