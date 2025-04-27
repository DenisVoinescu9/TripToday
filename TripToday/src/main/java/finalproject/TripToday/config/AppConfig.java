package finalproject.TripToday.config; // Sau pachetul tău de configurare

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {

        // Create Apache HttpClient

        CloseableHttpClient httpClient = HttpClients.createDefault();

        // Create Request Factory based on Apache HTTP Client

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        // Create the bean using the Request Factory

        return new RestTemplate(requestFactory);
    }
}