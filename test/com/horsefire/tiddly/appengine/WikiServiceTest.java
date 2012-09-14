package com.horsefire.tiddly.appengine;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

import org.easymock.EasyMock;

public class WikiServiceTest extends TestCase {

	public static String getFile(String filename) throws IOException {
		InputStream in = WikiServiceTest.class.getResourceAsStream(filename);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder result = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			result.append(line).append('\n');
		}
		in.close();
		return result.toString();
	}

	public static void assertByLine(String expected, String actual)
			throws IOException {
		BufferedReader expectedReader = new BufferedReader(new StringReader(
				expected));
		BufferedReader actualReader = new BufferedReader(new StringReader(
				actual));

		String previous = null;
		while (true) {
			String expectedLine = expectedReader.readLine();
			String actualLine = actualReader.readLine();
			previous = actualLine;
			if (expectedLine == null) {
				if (actualLine != null) {
					fail("Expected null, but got '" + actualLine + "'");
				}
				return;
			}
			if (!expectedLine.equals(actualLine)) {
				System.out.println("Expected: " + expectedLine);
				System.out.println("Got:");
				System.out.println(previous);
				System.out.println(actualLine);
				System.out.println(actualReader.readLine());
				fail("Expected '" + expectedLine + "', but got '" + actualLine
						+ "'");
			}
		}
	}

	public void testGet() throws Exception {
		UserInfoService userService = createMock(UserInfoService.class);
		FileService fileService = EasyMock.createMock(FileService.class);

		String path = "/Wiki/wiki.html";
		expect(userService.getWikiPath()).andStubReturn(path);
		expect(fileService.getFile(path)).andReturn(
				getFile("base.html").getBytes());

		replay(userService, fileService);

		WikiService service = new WikiService(userService, fileService);
		String toServe = service.prepareToServe();
		assertByLine(getFile("serve.html"), toServe);

		verify(userService, fileService);
	}

	public void testPut() throws Exception {
		UserInfoService userService = createMock(UserInfoService.class);
		FileService fileService = EasyMock.createMock(FileService.class);

		String path = "/Wiki/wiki.html";
		expect(userService.getWikiPath()).andStubReturn(path);
		expect(fileService.getFile(path)).andReturn(
				getFile("base.html").getBytes());

		final AtomicReference<String> savedFile = new AtomicReference<String>();
		FileService stubFileService = new FileService(null, null, null) {
			public void putFile(String path, byte[] contents) {
				savedFile.set(new String(contents));
			}
		};
		fileService.putFile((String) EasyMock.anyObject(),
				(byte[]) EasyMock.anyObject());
		expectLastCall().andStubDelegateTo(stubFileService);

		replay(userService, fileService);

		WikiService service = new WikiService(userService, fileService);
		BufferedReader reader = new BufferedReader(new StringReader(
				getFile("modStore1.html")));
		service.saveNewStore(reader);
		assertByLine(getFile("mod1.html"), savedFile.get());

		verify(userService, fileService);
	}
}
