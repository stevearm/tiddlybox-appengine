package com.horsefire.tiddly.appengine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.horsefire.tiddly.appengine.dropbox.HandshakeOneServlet;
import com.horsefire.tiddly.appengine.dropbox.HandshakeTwoServlet;

public class BootstrapListener extends GuiceServletContextListener {

	public static final String HANDSHAKE_ONE_URL = "/handshake/one";
	public static final String HANDSHAKE_TWO_URL = "/handshake/two";
	public static final String WIKI_URL = "/wiki";

	private static final Logger LOG = LoggerFactory
			.getLogger(BootstrapListener.class);

	@Override
	protected Injector getInjector() {
		LOG.info("Starting tiddlybox bootstrap");
		LOG.debug("Testing debug logging");
		return Guice.createInjector(new MyServletModule());
	}

	private class MyServletModule extends ServletModule {

		@Override
		protected void configureServlets() {
			serve(HANDSHAKE_ONE_URL).with(HandshakeOneServlet.class);
			serve(HANDSHAKE_TWO_URL).with(HandshakeTwoServlet.class);
			serve(WIKI_URL + "/*").with(WikiServlet.class);
		}
	}
}
