package com.horsefire.tiddly.appengine;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import com.google.appengine.tools.development.testing.BaseDevAppServerTestConfig;

public class TestEnvironment extends BaseDevAppServerTestConfig {

	@Override
	public File getAppDir() {
		return new File(".");
	}

	@Override
	public List<URL> getClasspath() {
		return Collections.emptyList();
	}

	@Override
	public File getSdkRoot() {
		return new File(".");
	}
}
