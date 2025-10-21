package com.ncinga.runner.libs.httpClient

class Test {
    String testMethod() {
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
        print("Response: " + response);
        return response;
    }

}
