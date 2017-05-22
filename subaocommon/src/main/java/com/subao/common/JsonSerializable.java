package com.subao.common;

import java.io.IOException;

import android.util.JsonWriter;

public interface JsonSerializable {
	
	void serialize(JsonWriter writer) throws IOException;

}
