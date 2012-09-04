package com.horsefire.tiddly.appengine.dropbox;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.horsefire.tiddly.appengine.PreferencedServlet;
import com.horsefire.tiddly.appengine.ServletMapper;
import com.horsefire.tiddly.appengine.UserInfoService;

@SuppressWarnings("serial")
public class HandshakeOneServlet extends PreferencedServlet {

	private static final Logger LOG = LoggerFactory
			.getLogger(HandshakeOneServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
			UserInfoService prefs) throws ServletException, IOException {
		final OAuthConsumer consumer = new DefaultOAuthConsumer(
				AppCredentials.INSTANCE.getKey(),
				AppCredentials.INSTANCE.getSecret());
		final OAuthProvider provider = new DefaultOAuthProvider(
				DropboxWikiClient.VALUE_REQUEST_TOKEN_URL,
				DropboxWikiClient.VALUE_ACCESS_TOKEN_URL,
				DropboxWikiClient.VALUE_AUTHORIZATION_URL);

		try {
			String host = "http://" + req.getServerName();
			if (req.getServerPort() != 80) {
				host = host + ':' + req.getLocalPort();
			}
			final String url = provider
					.retrieveRequestToken(consumer,
							(host + req.getContextPath())
									+ ServletMapper.HANDSHAKE_TWO);
			prefs.setOauthTokenKey(consumer.getToken());
			prefs.setOauthTokenSecret(consumer.getTokenSecret());
			resp.sendRedirect(url);
		} catch (OAuthMessageSignerException e) {
			error(resp, e);
		} catch (OAuthNotAuthorizedException e) {
			error(resp, e);
		} catch (OAuthExpectationFailedException e) {
			error(resp, e);
		} catch (OAuthCommunicationException e) {
			error(resp, e);
		}
	}

	private void error(HttpServletResponse resp, Exception e)
			throws IOException {
		String message = "Failure during part one of handshake";
		resp.getWriter().print(message);
		LOG.error(message, e);
	}
}