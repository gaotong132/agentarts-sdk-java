package com.huaweicloud.agentarts.sdk.service.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.agentarts.sdk.core.APIException;
import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.SignMode;
import com.huaweicloud.agentarts.sdk.core.signer.V11Signer;
import com.huaweicloud.sdk.core.auth.AKSKSigner;
import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.http.HttpMethod;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Base HTTP client for AgentArts services with signing support.
 * Supports two signing modes:
 * <ul>
 *   <li>{@link SignMode#V11_HMAC_SHA256} — AgentArts V11 signer with HKDF key derivation</li>
 *   <li>{@link SignMode#SDK_HMAC_SHA256} — Standard Huawei Cloud SDK-HMAC-SHA256</li>
 * </ul>
 *
 * <p>All responses are opened in streaming mode. If the Content-Type indicates
 * a streaming response ({@code text/event-stream} or {@code application/x-ndjson}),
 * the body is left unconsumed and available via {@link RequestResult#iterLines()}.</p>
 */
public class BaseHttpClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BaseHttpClient.class);
    private static final ObjectMapper OBJECT_MAPPER = com.huaweicloud.agentarts.sdk.core.util.JsonUtils.MAPPER;
    private static final String STREAM_SSE = "text/event-stream";
    private static final String STREAM_NDJSON = "application/x-ndjson";

    private final RequestConfig config;
    private final boolean openAkSk;
    private final SignMode signMode;
    private final String regionId;
    private final WebClient webClient;
    private final Vertx vertx;
    private final boolean ownVertx;

    // Mutable state (thread-safe)
    private final Map<String, String> defaultHeaders = new ConcurrentHashMap<>();
    private volatile String authHeader;

    /**
     * Create a BaseHttpClient with a shared Vert.x instance.
     */
    public BaseHttpClient(RequestConfig config, boolean openAkSk, SignMode signMode,
                          String regionId, Vertx vertx) {
        this.config = config != null ? config : new RequestConfig();
        this.openAkSk = openAkSk;
        this.signMode = signMode != null ? signMode : SignMode.SDK_HMAC_SHA256;
        this.regionId = regionId != null ? regionId : Constants.getRegion();
        this.vertx = vertx;
        this.ownVertx = false;
        this.webClient = createWebClient(vertx);
    }

    /**
     * Create a BaseHttpClient with its own Vert.x instance.
     */
    public BaseHttpClient(RequestConfig config, boolean openAkSk, SignMode signMode, String regionId) {
        this.config = config != null ? config : new RequestConfig();
        this.openAkSk = openAkSk;
        this.signMode = signMode != null ? signMode : SignMode.SDK_HMAC_SHA256;
        this.regionId = regionId != null ? regionId : Constants.getRegion();
        this.vertx = Vertx.vertx();
        this.ownVertx = true;
        this.webClient = createWebClient(vertx);
    }

    /**
     * Convenience constructor with default SignMode.SDK_HMAC_SHA256.
     */
    public BaseHttpClient(RequestConfig config) {
        this(config, false, SignMode.SDK_HMAC_SHA256, null);
    }

    private WebClient createWebClient(Vertx vertx) {
        WebClientOptions options = new WebClientOptions()
                .setConnectTimeout((int) (config.getTimeoutSeconds() * 1000))
                .setIdleTimeout((int) config.getTimeoutSeconds())
                .setTrustAll(!config.isVerifySsl());
        return WebClient.create(vertx, options);
    }

    // ========================
    // Public HTTP methods
    // ========================

    public Mono<RequestResult> get(String url, Map<String, String> headers) {
        return request("GET", url, headers, null, null);
    }

    public Mono<RequestResult> get(String url) {
        return get(url, null);
    }

    public Mono<RequestResult> post(String url, Map<String, String> headers, Object body) {
        return request("POST", url, headers, body, null);
    }

    public Mono<RequestResult> post(String url, Object body) {
        return post(url, null, body);
    }

    public Mono<RequestResult> put(String url, Map<String, String> headers, Object body) {
        return request("PUT", url, headers, body, null);
    }

    public Mono<RequestResult> put(String url, Object body) {
        return put(url, null, body);
    }

    public Mono<RequestResult> patch(String url, Map<String, String> headers, Object body) {
        return request("PATCH", url, headers, body, null);
    }

    public Mono<RequestResult> delete(String url, Map<String, String> headers) {
        return request("DELETE", url, headers, null, null);
    }

    public Mono<RequestResult> delete(String url) {
        return delete(url, null);
    }

    // ========================
    // Auth management
    // ========================

    /**
     * Set the Authorization header.
     *
     * @param scheme auth scheme (e.g., "Bearer")
     * @param token  the token value
     */
    public void setAuthToken(String scheme, String token) {
        this.authHeader = scheme + " " + token;
    }

    public void setAuthToken(String token) {
        setAuthToken("Bearer", token);
    }

    /** Clear the Authorization header. */
    public void clearAuth() {
        this.authHeader = null;
    }

    /** Set a default header that will be sent with every request. */
    public void setHeader(String name, String value) {
        defaultHeaders.put(name, value);
    }

    public RequestConfig getConfig() {
        return config;
    }

    public SignMode getSignMode() {
        return signMode;
    }

    // ========================
    // Core request method
    // ========================

    /**
     * Execute an HTTP request with optional signing.
     *
     * @param method      HTTP method (GET, POST, PUT, PATCH, DELETE)
     * @param url         relative URL (will be prepended with base URL)
     * @param headers     additional request headers (nullable)
     * @param body        request body (will be serialized to JSON if not null)
     * @param queryParams query parameters (nullable)
     * @return Mono emitting the RequestResult
     */
    public Mono<RequestResult> request(String method, String url,
                                        Map<String, String> headers,
                                        Object body,
                                        Map<String, List<String>> queryParams) {
        return Mono.fromFuture(() -> executeRequest(method, url, headers, body, queryParams))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private CompletableFuture<RequestResult> executeRequest(String method, String url,
                                                             Map<String, String> headers,
                                                             Object body,
                                                             Map<String, List<String>> queryParams) {
        try {
            // Build full URL
            String fullUrl = config.getBaseUrl() + url;
            if (queryParams != null && !queryParams.isEmpty()) {
                fullUrl = appendQueryParams(fullUrl, queryParams);
            }

            // Merge headers
            Map<String, String> mergedHeaders = new HashMap<>(defaultHeaders);
            if (headers != null) {
                mergedHeaders.putAll(headers);
            }
            if (authHeader != null) {
                mergedHeaders.put("Authorization", authHeader);
            }

            // Set Content-Type for body requests
            if (body != null && !mergedHeaders.containsKey("Content-Type")) {
                mergedHeaders.put("Content-Type", "application/json");
            }

            // Sign the request if AK/SK signing is enabled
            if (openAkSk) {
                signRequest(method, fullUrl, mergedHeaders, body, queryParams);
            }

            // Serialize body
            String bodyStr = null;
            if (body != null) {
                if (body instanceof String) {
                    bodyStr = (String) body;
                } else {
                    bodyStr = OBJECT_MAPPER.writeValueAsString(body);
                }
            }

            // Build Vert.x request
            URI uri = URI.create(fullUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            if (port == -1) {
                port = "https".equals(uri.getScheme()) ? 443 : 80;
            }
            String path = uri.getRawPath();
            if (uri.getRawQuery() != null) {
                path = path + "?" + uri.getRawQuery();
            }

            HttpRequest<Buffer> request = webClient
                    .request(io.vertx.core.http.HttpMethod.valueOf(method), port, host, path)
                    .ssl("https".equals(uri.getScheme()))
                    .timeout((long) (config.getTimeoutSeconds() * 1000));

            // Set headers
            for (Map.Entry<String, String> entry : mergedHeaders.entrySet()) {
                request.putHeader(entry.getKey(), entry.getValue());
            }

            // Send request
            CompletableFuture<RequestResult> future = new CompletableFuture<>();

            if (bodyStr != null) {
                request.sendBuffer(Buffer.buffer(bodyStr), ar -> handleResponse(ar, future));
            } else {
                request.send(ar -> handleResponse(ar, future));
            }

            return future;
        } catch (Exception e) {
            LOG.error("Request error: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(
                    RequestResult.builder()
                            .success(false)
                            .statusCode(0)
                            .error("Request error: " + e.getMessage())
                            .build()
            );
        }
    }

    private void handleResponse(io.vertx.core.AsyncResult<HttpResponse<Buffer>> ar,
                                 CompletableFuture<RequestResult> future) {
        if (ar.failed()) {
            LOG.error("Request failed: {}", ar.cause().getMessage(), ar.cause());
            future.complete(RequestResult.builder()
                    .success(false)
                    .statusCode(0)
                    .error("Request error: " + ar.cause().getMessage())
                    .build());
            return;
        }

        try {
            HttpResponse<Buffer> response = ar.result();
            int statusCode = response.statusCode();
            boolean success = statusCode >= 200 && statusCode < 300;

            // Extract response headers
            Map<String, String> responseHeaders = new HashMap<>();
            response.headers().forEach(entry ->
                    responseHeaders.put(entry.getKey(), entry.getValue()));

            // Check for streaming content type
            String contentType = responseHeaders.getOrDefault("Content-Type", "").toLowerCase();
            boolean isStreaming = contentType.contains(STREAM_SSE) || contentType.contains(STREAM_NDJSON);

            if (isStreaming) {
                // For streaming responses, provide the body as a Flux
                Buffer body = response.body();
                String bodyStr = body != null ? body.toString(StandardCharsets.UTF_8) : "";

                Flux<String> lineStream = Flux.fromArray(bodyStr.split("\n"))
                        .filter(line -> !line.isEmpty());

                Flux<byte[]> byteStream = Flux.just(bodyStr.getBytes(StandardCharsets.UTF_8));

                future.complete(RequestResult.builder()
                        .success(success)
                        .statusCode(statusCode)
                        .headers(responseHeaders)
                        .streaming(true)
                        .lineStream(lineStream)
                        .byteStream(byteStream)
                        .build());
            } else {
                // For non-streaming, parse the body
                Buffer body = response.body();
                String bodyStr = body != null ? body.toString(StandardCharsets.UTF_8) : "";
                Object data = null;
                String error = null;

                if (!bodyStr.isEmpty()) {
                    try {
                        data = OBJECT_MAPPER.readTree(bodyStr);
                    } catch (Exception e) {
                        data = bodyStr;
                    }

                    // Extract error message for failed responses
                    if (!success) {
                        error = extractErrorFromBody(data);
                    }
                }

                future.complete(RequestResult.builder()
                        .success(success)
                        .statusCode(statusCode)
                        .data(data)
                        .error(error)
                        .headers(responseHeaders)
                        .streaming(false)
                        .build());
            }
        } catch (Exception e) {
            LOG.error("Response handling error: {}", e.getMessage(), e);
            future.complete(RequestResult.builder()
                    .success(false)
                    .statusCode(0)
                    .error("Unexpected error: " + e.getMessage())
                    .build());
        }
    }

    // ========================
    // Signing
    // ========================

    private void signRequest(String method, String fullUrl, Map<String, String> headers,
                              Object body, Map<String, List<String>> queryParams) {
        try {
            String ak = Constants.getAk();
            String sk = Constants.getSk();

            if (ak.isEmpty() || sk.isEmpty()) {
                LOG.error("AK/SK not configured — skipping {} signing; request will be rejected by server", signMode);
                return;
            }

            if (signMode == SignMode.V11_HMAC_SHA256) {
                signRequestV11(method, fullUrl, headers, body, queryParams, ak, sk);
            } else {
                signRequestSdk(method, fullUrl, headers, body, queryParams, ak, sk);
            }
        } catch (Exception e) {
            LOG.error("Failed to sign request — request will be sent unsigned: {}", e.getMessage(), e);
        }
    }

    /**
     * Sign using Huawei Cloud SDK standard SDK-HMAC-SHA256 (via built-in AKSKSigner).
     */
    private void signRequestSdk(String method, String fullUrl, Map<String, String> headers,
                                 Object body, Map<String, List<String>> queryParams,
                                 String ak, String sk) throws Exception {
        URI uri = URI.create(fullUrl);
        String endpoint = uri.getScheme() + "://" + uri.getHost();
        if (uri.getPort() > 0 && uri.getPort() != 443 && uri.getPort() != 80) {
            endpoint += ":" + uri.getPort();
        }
        String path = uri.getRawPath();

        BasicCredentials credentials = new BasicCredentials().withAk(ak).withSk(sk);
        String securityToken = Constants.getSecurityToken();
        if (!securityToken.isEmpty()) {
            credentials.withSecurityToken(securityToken);
        }

        com.huaweicloud.sdk.core.http.HttpRequest.HttpRequestBuilder builder = com.huaweicloud.sdk.core.http.HttpRequest
                .newBuilder()
                .withEndpoint(endpoint)
                .withPath(path)
                .withMethod(HttpMethod.valueOf(method));

        // Parse query params from the URL (they're embedded in fullUrl, not in the queryParams arg)
        String rawQuery = uri.getRawQuery();
        if (rawQuery != null && !rawQuery.isEmpty()) {
            for (String pair : rawQuery.split("&")) {
                String[] kv = pair.split("=", 2);
                String key = java.net.URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = kv.length > 1 ? java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                builder.addQueryParam(key, List.of(value));
            }
        }
        // Also add explicit queryParams if provided
        if (queryParams != null) {
            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                builder.addQueryParam(entry.getKey(), entry.getValue());
            }
        }

        String bodyStr = "";
        if (body != null) {
            if (body instanceof String) {
                bodyStr = (String) body;
            } else {
                bodyStr = OBJECT_MAPPER.writeValueAsString(body);
            }
        }
        if (!"GET".equals(method) && !"HEAD".equals(method) && !bodyStr.isEmpty()) {
            builder.withBodyAsString(bodyStr);
            builder.withContentType("application/json");
        }

        com.huaweicloud.sdk.core.http.HttpRequest sdkRequest = builder.build();
        Map<String, String> signedHeaders = AKSKSigner.getInstance().sign(sdkRequest, credentials);
        headers.putAll(signedHeaders);
    }

    /**
     * Sign using V11-HMAC-SHA256 with HKDF key derivation (AgentArts custom signer).
     */
    private void signRequestV11(String method, String fullUrl, Map<String, String> headers,
                                 Object body, Map<String, List<String>> queryParams,
                                 String ak, String sk) throws Exception {
        URI uri = URI.create(fullUrl);
        String host = uri.getHost();
        String path = uri.getRawPath();

        headers.put("Host", host);
        headers.put("x-sdk-content-sha256", "UNSIGNED-PAYLOAD");

        String securityToken = Constants.getSecurityToken();
        if (!securityToken.isEmpty()) {
            headers.put("X-Security-Token", securityToken);
        }

        V11Signer signer = new V11Signer(ak, sk, regionId);
        signer.sign(method, path, queryParams, headers);
    }

    // ========================
    // Helpers
    // ========================

    private String appendQueryParams(String url, Map<String, List<String>> queryParams) {
        StringBuilder sb = new StringBuilder(url);
        sb.append(url.contains("?") ? "&" : "?");
        boolean first = true;
        TreeMap<String, List<String>> sorted = new TreeMap<>(queryParams);
        for (Map.Entry<String, List<String>> entry : sorted.entrySet()) {
            String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            for (String value : entry.getValue()) {
                if (!first) {
                    sb.append('&');
                }
                sb.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                first = false;
            }
        }
        return sb.toString();
    }

    private String extractErrorFromBody(Object data) {
        if (data instanceof JsonNode) {
            JsonNode json = (JsonNode) data;
            if (json.has("error")) {
                JsonNode errNode = json.get("error");
                if (errNode.isTextual()) return errNode.asText();
                if (errNode.isObject() && errNode.has("message")) return errNode.get("message").asText();
            }
            if (json.has("message")) return json.get("message").asText();
            if (json.has("error_msg")) return json.get("error_msg").asText();
        }
        if (data instanceof String) return (String) data;
        return null;
    }

    @Override
    public void close() {
        webClient.close();
        if (ownVertx) {
            vertx.close();
        }
    }
}
