package com.horsefire.tiddly.appengine;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.IAnswer;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.horsefire.tiddly.appengine.dropbox.AppCredentials;
import com.horsefire.tiddly.appengine.dropbox.DropboxService;
import com.horsefire.tiddly.appengine.dropbox.DropboxService.UnauthorizedException;

public class WikiServletTest {

	private final LocalServiceTestHelper m_helper = new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig());
	private WikiServlet m_servlet;
	private final AtomicReference<byte[]> m_bytes = new AtomicReference<byte[]>();

	@Before
	public void setUp() throws IOException, UnauthorizedException {
		m_helper.setUp();

		AppCredentials appCredentials = createMock(AppCredentials.class);
		expect(appCredentials.getKey()).andStubReturn("key");
		expect(appCredentials.getSecret()).andStubReturn("secret");

		DropboxService service = createMock(DropboxService.class);
		expect(
				service.getBytes((String) anyObject(), (String) anyObject(),
						(String) anyObject())).andStubAnswer(
				new IAnswer<byte[]>() {
					@Override
					public byte[] answer() throws Throwable {
						return m_bytes.get();
					}
				});

		JSONObject meta = createMock(JSONObject.class);
		expect(
				service.getMetadata((String) anyObject(), (String) anyObject(),
						(String) anyObject())).andStubReturn(meta);
		expect(meta.get(DropboxService.META_KEY_BYTES)).andAnswer(
				new IAnswer<Object>() {
					@Override
					public Object answer() throws Throwable {
						return new Long(m_bytes.get().length);
					}
				});
		expect(meta.get(DropboxService.META_KEY_REV))
				.andStubReturn("revnumber");

		replay(appCredentials, service, meta);
		m_servlet = new WikiServlet(service);
	}

	@After
	public void tearDown() {
		m_helper.tearDown();
	}

	@Test
	public void simpleGet() throws ServletException, IOException {
		HttpServletRequest req = createMock(HttpServletRequest.class);
		String cookieValue = "someValue";
		expect(req.getCookies()).andReturn(
				new Cookie[] { new Cookie(UserInfoService.COOKIE_KEY,
						cookieValue) });
		expect(req.getPathInfo()).andReturn("/Wiki/wiki.html");
		expect(req.getQueryString()).andStubReturn("tiddler=Home");

		HttpServletResponse resp = createMock(HttpServletResponse.class);
		StringWriter output = new StringWriter();
		PrintWriter writer = new PrintWriter(output);
		expect(resp.getWriter()).andStubReturn(writer);

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Entity entity = new Entity("Prefs", cookieValue);
		entity.setProperty(UserInfoService.DB_KEY_OAUTH_TOKEN_KEY, "oauthKey");
		entity.setProperty(UserInfoService.DB_KEY_OAUTH_TOKEN_SECRET,
				"oauthSecret");
		ds.put(entity);

		m_bytes.set(getFile("base.html").getBytes("UTF-8"));

		replay(req, resp);
		m_servlet.doGet(req, resp);
		writer.close();
		assertEquals(
				"<html><body><pre>!Header One\nText\n!!Header two\n* List item one\n* List item two\n\n"
						+ "Click <a href=\"/wiki/Wiki/wiki.html?tiddler=Blank\">here</a> to go to "
						+ "<a href=\"/wiki/Wiki/wiki.html?tiddler=Blank\">Blank</a></pre></body></html>",
				output.toString());
		verify(req, resp);
	}

	public static String getFile(String filename) throws IOException {
		InputStream in = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(filename);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder result = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			result.append(line).append('\n');
		}
		in.close();
		return result.toString();
	}
}
