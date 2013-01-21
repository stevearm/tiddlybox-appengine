package com.horsefire.tiddly.appengine;

import java.io.IOException;
import java.io.PrintWriter;

import com.horsefire.tiddly.Wiki;
import com.horsefire.tiddly.WikiService;

public class WikiServletNode {

	private final PrintWriter m_out;
	private final WikiService m_service;

	public WikiServletNode(PrintWriter writer, WikiService service) {
		m_out = writer;
		m_service = service;
	}

	public void render() throws IOException {
		Wiki wiki = m_service.get();

		m_out.print(wiki.getHeader());
		m_out.print(wiki.getStore());
		m_out.print(wiki.getPostStore());
		m_out.print("<script type=\"text/javascript\" src=\"/tiddlybox.js\"></script>");
		m_out.print(wiki.getPostScript());
	}
}
