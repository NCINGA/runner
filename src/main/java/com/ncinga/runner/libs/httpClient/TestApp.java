package com.ncinga.runner.libs.httpClient;

import java.util.HashMap;
import java.util.Map;

public class TestApp {
    public static void main(String[] args) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "foo");
        payload.put("body", "bar");
        payload.put("userId", 1);

        HTTPClient client = new HTTPClient.Builder()
                .url("https://jsonplaceholder.typicode.com/posts")
                .method(Method.POST)
                .payload(payload)
                .mediaType("application/json")
                .build();

        Object response = client.exchange();
        System.out.println("Response: " + response);

    }
}
