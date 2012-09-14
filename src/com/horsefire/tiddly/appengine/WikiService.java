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

	private final UserInfoService m_userService;
	private final FileService m_fileService;

	public WikiService(UserInfoService userService, FileService fileService) {
		m_userService = userService;
		m_fileService = fileService;
	}

	public String prepareToServe() throws OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException,
			IOException, ParseException {
		String original = new String(m_fileService.getFile(m_userService
				.getWikiPath()));
		StringBuilder result = new StringBuilder();
		result.append(getToEndOf(original, "<!--POST-SCRIPT-START-->"));
		result.append("<script type=\"text/javascript\" src=\"/tiddlybox.js\"></script>");
		result.append("<script type=\"text/javascript\">var tiddlybox_post_url = '"
				+ ServletMapper.WIKI
				+ m_userService.getWikiPath()
				+ "'</script>");
		result.append(getFromStartOf(original, "<!--POST-SCRIPT-END-->"));
		return result.toString();
	}

	public void saveNewStore(BufferedReader newStore) throws IOException,
			OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException, ParseException {
		StringBuilder result = new StringBuilder();
		String wikiPath = m_userService.getWikiPath();
		result.append(
				getToEndOf(new String(m_fileService.getFile(wikiPath)),
						"<!--POST-SHADOWAREA-->")).append('\n');
		result.append("<div id=\"storeArea\">").append('\n');
		String line = null;
		while ((line = newStore.readLine()) != null) {
			result.append(line).append('\n');
		}
		result.append("</div>").append('\n');
		result.append(getFromStartOf(
				new String(m_fileService.getFile(wikiPath)),
				"<!--POST-STOREAREA-->"));

		m_fileService.putFile(wikiPath, result.toString().getBytes());
	}
}
