package com.dali.ecommerce.config;

import com.dali.ecommerce.service.AdminUserDetailsService;
import com.dali.ecommerce.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Your constructor and other beans...
    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final AdminUserDetailsService adminUserDetailsService;
    private final AuthenticationSuccessHandler adminAuthenticationSuccessHandler;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          @Qualifier("customAuthenticationSuccessHandler") AuthenticationSuccessHandler customAuthenticationSuccessHandler,
                          AdminUserDetailsService adminUserDetailsService,
                          @Qualifier("adminAuthenticationSuccessHandler") AuthenticationSuccessHandler adminAuthenticationSuccessHandler) {
        this.customUserDetailsService = customUserDetailsService;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.adminUserDetailsService = adminUserDetailsService;
        this.adminAuthenticationSuccessHandler = adminAuthenticationSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider customerAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public DaoAuthenticationProvider adminAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(adminUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }


    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**")
                .authorizeHttpRequests(auth -> auth
                        // *** THE FIX IS HERE ***
                        // We tell the admin chain that navigating to /forgot-password is also permitted.
                        .requestMatchers("/admin/login", "/forgot-password").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .successHandler(this.adminAuthenticationSuccessHandler)
                        .failureUrl("/admin/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login")
                        .permitAll()
                )
                .authenticationProvider(adminAuthenticationProvider());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain customerFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // This chain remains the same as our last correct version.
                        .requestMatchers("/login", "/register").anonymous()
                        .requestMatchers("/", "/shop/**", "/stores/**", "/product/**", "/css/**", "/images/**", "/cart/**", "/forgot-password").permitAll()
                        .requestMatchers("/profile", "/checkout/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(this.customAuthenticationSuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                .authenticationProvider(customerAuthenticationProvider());

        return http.build();
    }
}