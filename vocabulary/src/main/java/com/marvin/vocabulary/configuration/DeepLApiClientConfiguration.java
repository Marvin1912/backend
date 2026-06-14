package com.marvin.vocabulary.configuration;

import com.generated.deepl.ApiClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Configures the shared DeepL {@link ApiClient} bean once at startup with the base path and authentication
 * header, so that request-handling code does not need to mutate the shared singleton on every call.
 */
@Component
public class DeepLApiClientConfiguration {

    private static final String DEFAULT_DEEPL_URL = "https://api-free.deepl.com";
    private static final String DEEPL_AUTH_HEADER_FORMAT = "DeepL-Auth-Key %s";

    private final String deepLApiUrl;
    private final String deepLApiKey;
    private final ApiClient apiClient;

    /**
     * Constructs a new DeepLApiClientConfiguration with required dependencies.
     *
     * @param deepLApiUrl the DeepL API URL
     * @param deepLApiKey the DeepL API key
     * @param apiClient   the shared DeepL API client to configure
     */
    public DeepLApiClientConfiguration(
            @Value("${vocabulary.deepl.url:" + DEFAULT_DEEPL_URL + "}") String deepLApiUrl,
            @Value("${vocabulary.deepl.api-key:}") String deepLApiKey,
            ApiClient apiClient
    ) {
        this.deepLApiUrl = deepLApiUrl;
        this.deepLApiKey = deepLApiKey;
        this.apiClient = apiClient;
    }

    /**
     * Configures the shared DeepL API client with the base path and authentication header once at startup.
     */
    @PostConstruct
    public void configureApiClient() {
        apiClient.setBasePath(deepLApiUrl);
        apiClient.addDefaultHeader(HttpHeaders.AUTHORIZATION, String.format(DEEPL_AUTH_HEADER_FORMAT, deepLApiKey));
    }

}
