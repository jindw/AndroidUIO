package org.xidea.android.impl.http;

import java.net.URI;


import org.xidea.android.SQLiteMapper.SQLiteProperty;
import org.xidea.android.SQLiteMapper.SQLiteEntry;

@SQLiteEntry(version=1,name="UIO_HTTP_CACHE")
public class HttpCacheEntry {
	@SQLiteProperty("PRIMARY KEY NOT NULL")
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
	public Long lastAccess;
	@SQLiteProperty
	public int hit;
	@SQLiteProperty
	public String etag;
	
}

