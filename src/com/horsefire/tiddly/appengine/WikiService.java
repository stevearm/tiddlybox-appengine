package com.horsefire.tiddly.appengine;

import java.io.BufferedReader;
import java.io.IOException;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikiService {

	private static final Logger LOG = LoggerFactory
			.getLogger(WikiService.class);

	private static String getToEndOf(String base, String searchFor) {
		int index = base.indexOf(searchFor);
		if (index == -1) {
			LOG.error("Could not find '{}' in base string", searchFor);
			throw new IndexOutOfBoundsException();
		}
		return base.substring(0, index + searchFor.length());
	}

	private static String getFromStartOf(String base, String searchFor) {
		int index = base.indexOf(searchFor);
		if (index == -1) {
			LOG.error("Could not find '{}' in base string", searchFor);
			throw new IndexOutOfBoundsException();
		}
		return base.substring(index);
	}

	private final FileService m_fileService;

	public WikiService(FileService fileService) {
		m_fileService = fileService;
	}

	public String prepareToServe(String path)
			throws OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException,
			IOException, ParseException {
		String original = new String(m_fileService.getFile(path));
		StringBuilder result = new StringBuilder();
		result.append(getToEndOf(original, "<!--POST-SCRIPT-START-->"));
		result.append("<script type=\"text/javascript\" src=\"/tiddlybox.js\"></script>");
		result.append(getFromStartOf(original, "<!--POST-SCRIPT-END-->"));
		return result.toString();
	}

	public void saveNewStore(BufferedReader newStore, String wikiPath)
			throws IOException, OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException,
			ParseException {
		StringBuilder result = new StringBuilder();
		byte[] originalFile = m_fileService.getFile(wikiPath);
		result.append(
				getToEndOf(new String(originalFile), "<!--POST-SHADOWAREA-->"))
				.append('\n');
		result.append("<div id=\"storeArea\">").append('\n');
		String line = null;
		while ((line = newStore.readLine()) != null) {
			result.append(line).append('\n');
		}
		result.append("</div>").append('\n');
		result.append(getFromStartOf(new String(originalFile),
				"<!--POST-STOREAREA-->"));

		m_fileService.putFile(wikiPath, result.toString().getBytes());
	}
}
