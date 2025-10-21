package com.ncinga.runner.libs.httpClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class HTTPClient {
    private final String url;
    private final Method method;
    private final Map<String, Object> headers;
    private final Object payload;
    private final String mediaType;
    private HTTPClient(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers;
        this.payload = builder.payload;
        this.mediaType = builder.mediaType;
    }

    public Object exchange() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url));
        if (headers != null) {
            headers.forEach((k, v) -> requestBuilder.header(k, String.valueOf(v)));
        }
        String contentType = (mediaType != null ? mediaType : "application/json");
        requestBuilder.header("Content-Type", contentType);
        String body = (payload != null) ? JsonUtil.toJson(payload) : "";

        switch (method) {
            case GET -> requestBuilder.GET();
            case POST -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
            case PUT -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
            case DELETE -> requestBuilder.DELETE();
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static class Builder {
        private String url;
        private Method method;
        private Map<String, Object> headers;
        private Object payload;
        private String mediaType = "application/json";

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder method(Method method) {
            this.method = method;
            return this;
        }

        public Builder headers(Map<String, Object> headers) {
            this.headers = headers;
            return this;
        }

        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        public Builder mediaType(String mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public HTTPClient build() {
            return new HTTPClient(this);
        }
    }
}
