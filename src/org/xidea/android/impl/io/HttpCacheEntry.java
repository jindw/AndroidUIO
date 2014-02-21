package org.xidea.android.impl.io;

import java.net.URI;


import org.xidea.android.SQLiteMapper.SQLiteProperty;
import org.xidea.android.SQLiteMapper.SQLiteEntry;

@SQLiteEntry(version=1,name="HTTP_CACHE")
public class HttpCacheEntry {
	@SQLiteProperty("PRIMARY KEY AUTOINCREMENT")
	public long id;
	@SQLiteProperty("UNIQUE NOT NULL")
	public URI uri;
	@SQLiteProperty
	public String requestHeaders;
	@SQLiteProperty
	public String responseHeaders;
	@SQLiteProperty
	public String responseBody;
	@SQLiteProperty
	public String contentType;
	@SQLiteProperty
	public String charset;
	@SQLiteProperty
	public Integer contentLength;
	@SQLiteProperty
	public Long ttl;//time to alive:max-age
	@SQLiteProperty
	public Long lastModified;
	@SQLiteProperty
	public Long lastSaved;
	@SQLiteProperty
	public int hit;
	@SQLiteProperty
	public String etag;
	
}

