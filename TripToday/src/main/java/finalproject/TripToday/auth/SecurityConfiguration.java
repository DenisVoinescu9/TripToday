package finalproject.TripToday.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Value("${AUTH0_ISSUER}")
    private String AUTH0_ISSUER;
    @Value("${AUTH0_CLIENT_ID}")
    private String AUTH0_CLIENT_ID;



    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {

        // Configure paths based on authentication status

        http    .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/css/**", "/js/**","/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/","/home","/contact", "/guides", "/api/v2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(login -> login
                 .defaultSuccessUrl("/", true)
        )


                .logout(logout -> logout
                        .addLogoutHandler(logoutHandler()));
        return http.build();
    }


    private LogoutHandler logoutHandler() {
        return (request, response, authentication) -> {
            try {
                String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
                response.sendRedirect(AUTH0_ISSUER + "v2/logout?client_id=" + AUTH0_CLIENT_ID);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
