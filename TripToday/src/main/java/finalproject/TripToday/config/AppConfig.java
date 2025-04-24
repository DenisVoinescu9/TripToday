package finalproject.TripToday.config; // Sau pachetul tău de configurare

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig { // Numește clasa cum dorești

    @Bean
    public RestTemplate restTemplate() {
        // 1. Creează un client Apache HttpClient
        //    Poți folosi createDefault() sau îl poți customiza mai mult dacă e nevoie
        //    (ex: setare timeout-uri, pool de conexiuni etc.)
        CloseableHttpClient httpClient = HttpClients.createDefault();

        // 2. Creează un Request Factory bazat pe clientul Apache HttpClient
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        // 3. Creează bean-ul RestTemplate folosind acest request factory
        return new RestTemplate(requestFactory);
    }
}