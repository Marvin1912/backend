package com.marvin.vocabulary.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.generated.deepl.ApiClient;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class DeepLApiClientConfigurationTest {

    @Test
    void configureShouldSetBasePathAndAuthorizationHeaderOnApiClient() throws ReflectiveOperationException {
        final ApiClient apiClient = new ApiClient();
        final DeepLApiClientConfiguration configuration = new DeepLApiClientConfiguration(
                "https://api-free.deepl.com",
                "test-api-key",
                apiClient
        );

        configuration.configureApiClient();

        assertThat(apiClient.getBasePath()).isEqualTo("https://api-free.deepl.com");
        assertThat(readDefaultHeaders(apiClient).getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("DeepL-Auth-Key test-api-key");
    }

    private HttpHeaders readDefaultHeaders(ApiClient apiClient) throws ReflectiveOperationException {
        final Field field = ApiClient.class.getDeclaredField("defaultHeaders");
        field.setAccessible(true);
        return (HttpHeaders) field.get(apiClient);
    }

}
