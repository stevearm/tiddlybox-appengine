package com.horsefire.tiddly.appengine;

import java.io.IOException;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.horsefire.tiddly.appengine.dropbox.DropboxService;
import com.horsefire.tiddly.appengine.dropbox.DropboxService.UnauthorizedException;

public class FileService {

	private static final Logger LOG = LoggerFactory
			.getLogger(FileService.class);

	private static final String KEY_BYTES = "bytes";
	private static final String KEY_REV = "revision";
	private static final int MAX_BYTES = 1024 * 1024;

	private final DatastoreService m_datastore;
	private final UserInfoService m_userService;
	private final DropboxService m_dropboxService;

	public FileService(DatastoreService datastore, UserInfoService userService,
			DropboxService dropboxService) {
		m_datastore = datastore;
		m_userService = userService;
		m_dropboxService = dropboxService;
	}

	private Key getKey(String path) {
		Key parent = m_userService.getDbKey();
		return KeyFactory.createKey(parent, "File", path);
	}

	private void save(Key key, String rev, byte[] bytes) {
		Entity entity = new Entity(key);
		entity.setProperty(KEY_REV, rev);
		entity.setProperty(KEY_BYTES, new Blob(bytes));
		m_datastore.put(entity);
	}

	public byte[] getFile(String path) throws IOException,
			UnauthorizedException {
		JSONObject metadata = m_dropboxService.getMetadata(
				m_userService.getOauthTokenKey(),
				m_userService.getOauthTokenSecret(), path);
		long size = (Long) metadata.get(DropboxService.META_KEY_BYTES);
		if (size > MAX_BYTES) {
			LOG.error("Skipping {} because {} bytes is larger than 1MB", path,
					size);
			throw new UnsupportedOperationException(
					"Cannot work with files over 1MB");
		}
		String currentRev = (String) metadata.get(DropboxService.META_KEY_REV);

		Key key = getKey(path);
		try {
			Entity entity = m_datastore.get(key);
			String rev = (String) entity.getProperty(KEY_REV);
			if (rev.equals(currentRev)) {
				Blob result = (Blob) entity.getProperty(KEY_BYTES);
				LOG.debug("Cache hit {}", path);
				return result.getBytes();
			}
		} catch (EntityNotFoundException e) {
			LOG.debug("Cache miss {}", path);
		}
		byte[] bytes = m_dropboxService.getBytes(
				m_userService.getOauthTokenKey(),
				m_userService.getOauthTokenSecret(), path);
		save(key, currentRev, bytes);
		return bytes;
	}

	public void putFile(String path, byte[] contents) throws IOException,
			UnauthorizedException {
		LOG.debug("Saving to {}", path);
		JSONObject metadata = m_dropboxService.putBytes(
				m_userService.getOauthTokenKey(),
				m_userService.getOauthTokenSecret(), path, contents);
		String currentRev = (String) metadata.get(DropboxService.META_KEY_REV);
		save(getKey(path), currentRev, contents);
	}
}
