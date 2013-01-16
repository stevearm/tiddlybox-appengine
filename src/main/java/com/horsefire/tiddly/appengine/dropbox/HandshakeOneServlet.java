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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.horsefire.tiddly.appengine.BootstrapListener;
import com.horsefire.tiddly.appengine.PreferencedServlet;
import com.horsefire.tiddly.appengine.UserInfoService;

@SuppressWarnings("serial")
@Singleton
public class HandshakeOneServlet extends PreferencedServlet {

	private static final Logger LOG = LoggerFactory
			.getLogger(HandshakeOneServlet.class);

	private final AppCredentials m_appCredentials;

	@Inject
	public HandshakeOneServlet(AppCredentials appCredentials) {
		m_appCredentials = appCredentials;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
			UserInfoService prefs) throws ServletException, IOException {
		final OAuthConsumer consumer = new DefaultOAuthConsumer(
				m_appCredentials.getKey(), m_appCredentials.getSecret());
		final OAuthProvider provider = new DefaultOAuthProvider(
				DropboxService.URL_REQUEST_TOKEN,
				DropboxService.URL_ACCESS_TOKEN,
				DropboxService.URL_AUTHORIZATION);

		try {
			String host = "http://" + req.getServerName();
			if (req.getServerPort() != 80) {
				host = host + ':' + req.getLocalPort();
			}
			final String url = provider.retrieveRequestToken(consumer,
					(host + req.getContextPath())
							+ BootstrapListener.HANDSHAKE_TWO_URL + "?path="
							+ req.getParameter("path"));
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