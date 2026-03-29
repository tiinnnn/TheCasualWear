package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.service.AppUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AppUserService appUserService;

    public SecurityConfig(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    // Load user từ DB cho Spring Security
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            var user = appUserService.getUserByUsername(username);

            if (!user.getEnabled()) {
                throw new UsernameNotFoundException("Tài khoản đã bị khóa!");
            }

            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    user.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority(role.getName()))
                            .toList()
            );
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/shop/**",
                                "/product/**",
                                "/auth/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**"
                        ).permitAll()

                        // Chỉ ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Chỉ DELIVERY
                        .requestMatchers("/delivery/**").hasRole("DELIVERY")

                        // CUSTOMER + ADMIN (đã đăng nhập)
                        .requestMatchers(
                                "/cart/**",
                                "/order/**",
                                "/account/**"
                        ).hasAnyRole("CUSTOMER", "ADMIN")

                        .anyRequest().authenticated()
                )

                .csrf(csrf -> csrf.disable())

                .formLogin(form -> form
                        .loginPage("/auth/login")           // trang login tự tạo
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/auth/login?error=true")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // Xử lý khi không có quyền truy cập
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/error/403")
                );

        return http.build();
    }
}