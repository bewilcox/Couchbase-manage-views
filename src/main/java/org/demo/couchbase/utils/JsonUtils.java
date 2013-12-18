package org.demo.couchbase.utils;

import org.codehaus.jackson.map.ObjectMapper;

public class JsonUtils {

	private static ObjectMapper mapper;
	
	public static ObjectMapper getMapper(){
		if(mapper == null) {
			mapper = new ObjectMapper();
		}
		return mapper;
	}
}
