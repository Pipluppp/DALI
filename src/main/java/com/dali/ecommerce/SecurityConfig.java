package com.dali.ecommerce;

import com.dali.ecommerce.admin.AdminAuthenticationSuccessHandler;
import com.dali.ecommerce.admin.AdminUserDetailsService;
import com.dali.ecommerce.cart.CartMergingHandler;
import com.dali.ecommerce.account.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final CartMergingHandler cartMergingHandler;
    private final AdminUserDetailsService adminUserDetailsService;
    private final AdminAuthenticationSuccessHandler adminAuthenticationSuccessHandler;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          CartMergingHandler cartMergingHandler,
                          AdminUserDetailsService adminUserDetailsService,
                          AdminAuthenticationSuccessHandler adminAuthenticationSuccessHandler) {
        this.customUserDetailsService = customUserDetailsService;
        this.cartMergingHandler = cartMergingHandler;
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
                .csrf(AbstractHttpConfigurer::disable)
                .securityMatcher("/admin/**")
                .authorizeHttpRequests(auth -> auth
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
                // CSRF is enabled by default, no need to ignore non-existent webhook paths.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/checkout/recalculate")) // Keep for HTMX if needed
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login").anonymous()
                        // PERMIT ALL these paths, removing webhooks
                        .requestMatchers("/", "/register", "/shop/**", "/stores", "/stores/search", "/api/stores", "/product/**", "/css/**", "/images/**", "/js/**", "/cart/**", "/forgot-password", "/reset-password", "/api/locations/**", "/payment/callback/**").permitAll()
                        .requestMatchers("/profile", "/checkout/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(this.cartMergingHandler)
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