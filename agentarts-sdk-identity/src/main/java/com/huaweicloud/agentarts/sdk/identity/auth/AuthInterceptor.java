package com.huaweicloud.agentarts.sdk.identity.auth;

import com.huaweicloud.agentarts.sdk.core.annotation.RequireAccessToken;
import com.huaweicloud.agentarts.sdk.core.annotation.RequireApiKey;
import com.huaweicloud.agentarts.sdk.core.annotation.RequireStsToken;
import com.huaweicloud.agentarts.sdk.identity.IdentityClient;
import com.huaweicloud.agentarts.sdk.runtime.context.AgentArtsRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * Dynamic proxy interceptor that automatically fetches credentials
 * before invoking methods annotated with {@link RequireAccessToken},
 * {@link RequireApiKey}, or {@link RequireStsToken}.
 *
 * <p>Dynamic proxy interceptor for @Require* annotation enforcement.
 * Uses {@link Proxy} (not Spring AOP) to keep the SDK lightweight.</p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * interface MyService {
 *     @RequireApiKey(providerName = "myProvider")
 *     String handle(Map<String, Object> payload, GetResourceApiKeyResponse apiKey);
 * }
 * MyService impl = (payload, apiKey) -> { ... };
 * MyService proxy = AuthInterceptor.wrap(impl, MyService.class, identityClient);
 * // pass null for the credential slot; the interceptor fetches and replaces it
 * proxy.handle(payload, null);
 * }</pre>
 *
 * <p><b>Convention:</b> the credential is the <em>last</em> parameter of the
 * annotated method. The caller passes {@code null} (or any placeholder) for
 * that slot; the interceptor fetches the credential and replaces that argument
 * before delegating to the target.</p>
 */
public class AuthInterceptor implements InvocationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AuthInterceptor.class);

    private final Object target;
    private final IdentityClient identityClient;

    private AuthInterceptor(Object target, IdentityClient identityClient) {
        this.target = target;
        this.identityClient = identityClient;
    }

    /**
     * Create a dynamic proxy that intercepts auth-annotated methods.
     *
     * @param target         the object to wrap
     * @param interfaceType  the interface to proxy
     * @param identityClient the IdentityClient for credential fetching
     * @return proxied instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrap(T target, Class<T> interfaceType, IdentityClient identityClient) {
        return (T) Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[]{interfaceType},
                new AuthInterceptor(target, identityClient));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Check for auth annotations
        RequireAccessToken accessTokenAnn = method.getAnnotation(RequireAccessToken.class);
        RequireApiKey apiKeyAnn = method.getAnnotation(RequireApiKey.class);
        RequireStsToken stsTokenAnn = method.getAnnotation(RequireStsToken.class);

        if (accessTokenAnn != null) {
            return handleAccessToken(method, args, accessTokenAnn);
        }
        if (apiKeyAnn != null) {
            return handleApiKey(method, args, apiKeyAnn);
        }
        if (stsTokenAnn != null) {
            return handleStsToken(method, args, stsTokenAnn);
        }

        // No auth annotation — pass through
        return method.invoke(target, args);
    }

    private Object handleAccessToken(Method method, Object[] args,
                                      RequireAccessToken ann) throws Exception {
        String workloadAccessToken = getWorkloadAccessToken();

        var response = identityClient.getResourceOauth2Token(
                ann.providerName(), workloadAccessToken);

        LOG.debug("Fetched OAuth2 access token for provider: {}", ann.providerName());

        Object[] newArgs = replaceLastArg(args, response);
        return method.invoke(target, newArgs);
    }

    private Object handleApiKey(Method method, Object[] args,
                                 RequireApiKey ann) throws Exception {
        String workloadAccessToken = getWorkloadAccessToken();

        var response = identityClient.getResourceApiKey(
                ann.providerName(), workloadAccessToken);

        LOG.debug("Fetched API key for provider: {}", ann.providerName());

        Object[] newArgs = replaceLastArg(args, response);
        return method.invoke(target, newArgs);
    }

    private Object handleStsToken(Method method, Object[] args,
                                   RequireStsToken ann) throws Exception {
        String workloadAccessToken = getWorkloadAccessToken();

        var response = identityClient.getResourceStsToken(
                ann.providerName(), workloadAccessToken, ann.agencySessionName());

        LOG.debug("Fetched STS token for provider: {}", ann.providerName());

        Object[] newArgs = replaceLastArg(args, response);
        return method.invoke(target, newArgs);
    }

    private String getWorkloadAccessToken() {
        // Try runtime context first
        String token = AgentArtsRuntimeContext.getWorkloadAccessToken();
        if (com.huaweicloud.agentarts.sdk.core.util.JsonUtils.isNotBlank(token)) {
            return token;
        }

        // Fall back to local identity bootstrap
        LOG.debug("No workload access token in context, using local identity bootstrap");
        return identityClient.ensureLocalAuthToken("default");
    }

    /**
     * Replace the last argument (the credential slot) with the fetched credential.
     * If there are no arguments, the credential becomes the sole argument.
     */
    private static Object[] replaceLastArg(Object[] args, Object newArg) {
        if (args == null || args.length == 0) {
            return new Object[]{newArg};
        }
        Object[] newArgs = args.clone();
        newArgs[newArgs.length - 1] = newArg;
        return newArgs;
    }
}
