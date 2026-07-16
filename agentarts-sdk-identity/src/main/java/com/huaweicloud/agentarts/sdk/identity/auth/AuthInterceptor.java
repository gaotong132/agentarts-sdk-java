package com.huaweicloud.agentarts.sdk.identity.auth;

import com.huaweicloud.agentarts.sdk.core.annotation.RequireAccessToken;
import com.huaweicloud.agentarts.sdk.core.annotation.RequireApiKey;
import com.huaweicloud.agentarts.sdk.core.annotation.RequireStsToken;
import com.huaweicloud.agentarts.sdk.runtime.context.AgentArtsRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.huaweicloud.sdk.agentidentity.v1.model.GetResourceOauth2TokenRequestBody;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
 *     String handle(Map<String, Object> payload, String apiKey);
 * }
 * MyService impl = (payload, apiKey) -> { ... };
 * MyService proxy = AuthInterceptor.wrap(impl, MyService.class, identityClient);
 * // pass null for the credential slot; the interceptor fetches and replaces it
 * proxy.handle(payload, null);
 * }</pre>
 *
 * <p>The injection parameter is selected by the annotation's {@code into}
 * value when parameter metadata is available. For consumer code compiled
 * without {@code -parameters}, a unique compatible type is selected, with the
 * last parameter retained as a backwards-compatible fallback.</p>
 */
public class AuthInterceptor implements InvocationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AuthInterceptor.class);

    private final Object target;
    private final AuthCredentialResolver credentialResolver;

    private AuthInterceptor(Object target, AuthCredentialResolver credentialResolver) {
        this.target = target;
        this.credentialResolver = credentialResolver;
    }

    /**
     * Create a dynamic proxy that intercepts auth-annotated methods.
     *
     * @param target         the object to wrap
     * @param interfaceType  the interface to proxy
     * @param credentialResolver credential value resolver
     * @return proxied instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrap(T target, Class<T> interfaceType, AuthCredentialResolver credentialResolver) {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(interfaceType, "interfaceType must not be null");
        Objects.requireNonNull(credentialResolver, "credentialResolver must not be null");
        if (!interfaceType.isInterface()) {
            throw new IllegalArgumentException("interfaceType must be an interface");
        }
        if (!interfaceType.isInstance(target)) {
            throw new IllegalArgumentException("target must implement " + interfaceType.getName());
        }
        return (T) Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[]{interfaceType},
                new AuthInterceptor(target, credentialResolver));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Check for auth annotations
        RequireAccessToken accessTokenAnn = method.getAnnotation(RequireAccessToken.class);
        RequireApiKey apiKeyAnn = method.getAnnotation(RequireApiKey.class);
        RequireStsToken stsTokenAnn = method.getAnnotation(RequireStsToken.class);

        int annotationCount = (accessTokenAnn != null ? 1 : 0)
                + (apiKeyAnn != null ? 1 : 0)
                + (stsTokenAnn != null ? 1 : 0);
        if (annotationCount > 1) {
            throw new IllegalStateException("Only one Require* annotation is allowed on " + method);
        }

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
        return invokeTarget(method, args);
    }

    private Object handleAccessToken(Method method, Object[] args,
                                      RequireAccessToken ann) throws Throwable {
        String workloadAccessToken = getWorkloadAccessToken();

        GetResourceOauth2TokenRequestBody.Oauth2FlowEnum authFlow =
                ann.authFlow() == RequireAccessToken.AuthFlow.M2M
                        ? GetResourceOauth2TokenRequestBody.Oauth2FlowEnum.M2M
                        : GetResourceOauth2TokenRequestBody.Oauth2FlowEnum.USER_FEDERATION;
        String accessToken = credentialResolver.getResourceOauth2AccessToken(
                ann.providerName(), workloadAccessToken, authFlow,
                ann.scopes().length == 0 ? null : List.of(ann.scopes()),
                blankToNull(ann.callbackUrl()), ann.forceAuthentication());

        LOG.debug("Fetched OAuth2 access token for provider: {}", ann.providerName());
        return invokeWithCredential(method, args, ann.into(), accessToken);
    }

    private Object handleApiKey(Method method, Object[] args,
                                 RequireApiKey ann) throws Throwable {
        String workloadAccessToken = getWorkloadAccessToken();

        String apiKey = credentialResolver.getResourceApiKeyValue(
                ann.providerName(), workloadAccessToken);

        LOG.debug("Fetched API key for provider: {}", ann.providerName());
        return invokeWithCredential(method, args, ann.into(), apiKey);
    }

    private Object handleStsToken(Method method, Object[] args,
                                   RequireStsToken ann) throws Throwable {
        String workloadAccessToken = getWorkloadAccessToken();

        var credentials = credentialResolver.getResourceStsCredentials(
                ann.providerName(), workloadAccessToken, ann.agencySessionName(),
                ann.durationSeconds() > 0 ? ann.durationSeconds() : null,
                blankToNull(ann.policy()), blankToNull(ann.sourceIdentity()), null,
                ann.transitiveTagKeys().length == 0 ? null : List.of(ann.transitiveTagKeys()));

        LOG.debug("Fetched STS token for provider: {}", ann.providerName());
        return invokeWithCredential(method, args, ann.into(), credentials);
    }

    private String getWorkloadAccessToken() {
        // Try runtime context first
        String token = AgentArtsRuntimeContext.getWorkloadAccessToken();
        if (com.huaweicloud.agentarts.sdk.core.util.JsonUtils.isNotBlank(token)) {
            return token;
        }

        // Fall back to local identity bootstrap
        LOG.debug("No workload access token in context, using local identity bootstrap");
        if (credentialResolver instanceof com.huaweicloud.agentarts.sdk.identity.IdentityClient identityClient) {
            return identityClient.ensureLocalAuthToken("default");
        }
        throw new IllegalStateException(
                "No workload access token is present and the configured resolver cannot bootstrap local identity");
    }

    private Object invokeWithCredential(
            Method method, Object[] args, String parameterName, Object credential) throws Throwable {
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            throw new IllegalStateException(
                    "Auth-annotated method must declare an injection parameter: " + method);
        }

        int index = findInjectionIndex(parameters, parameterName, credential);
        Object[] invocationArgs = args != null ? args.clone() : new Object[parameters.length];
        if (invocationArgs.length != parameters.length) {
            throw new IllegalArgumentException("Argument count does not match " + method);
        }
        invocationArgs[index] = credential;
        return invokeTarget(method, invocationArgs);
    }

    private static int findInjectionIndex(
            Parameter[] parameters, String parameterName, Object credential) {
        boolean namesPresent = false;
        for (int i = 0; i < parameters.length; i++) {
            namesPresent |= parameters[i].isNamePresent();
            if (parameters[i].isNamePresent() && parameters[i].getName().equals(parameterName)) {
                requireCompatible(parameters[i], credential);
                return i;
            }
        }
        if (namesPresent) {
            throw new IllegalStateException("Auth injection parameter '"
                    + parameterName + "' does not exist in " + Arrays.toString(parameters));
        }

        List<Integer> compatible = new java.util.ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {
            if (credential == null || parameters[i].getType().isInstance(credential)) {
                compatible.add(i);
            }
        }
        if (compatible.size() == 1) {
            return compatible.get(0);
        }

        int last = parameters.length - 1;
        if (credential == null || parameters[last].getType().isInstance(credential)) {
            return last;
        }
        throw new IllegalStateException("Cannot resolve auth injection parameter '"
                + parameterName + "' from " + Arrays.toString(parameters));
    }

    private static void requireCompatible(Parameter parameter, Object credential) {
        if (credential != null && !parameter.getType().isInstance(credential)) {
            throw new IllegalStateException("Auth credential type " + credential.getClass().getName()
                    + " is not assignable to parameter " + parameter);
        }
    }

    private Object invokeTarget(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
