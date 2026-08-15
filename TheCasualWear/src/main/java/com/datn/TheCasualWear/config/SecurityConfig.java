package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.util.HomeRedirectResolver;
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
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AppUserRepository appUserRepository;

    public SecurityConfig(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return value -> {
            var user = appUserRepository.findByUsernameOrEmailOrPhone(value)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Không tìm thấy tài khoản!"));
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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // MỚI: thay cho defaultSuccessUrl("/", true) — trước đây mọi role đăng
    // nhập xong đều bị ép về "/" bất kể quyền hạn. Handler này gọi
    // HomeRedirectResolver để bắn ADMIN/OWNER -> /admin, CASHIER -> /cashier,
    // CUSTOMER (và mọi role khác) -> "/". Đây là nơi DUY NHẤT xử lý redirect
    // sau login thực sự (không phải AuthController, vì POST /auth/login được
    // Spring Security filter xử lý trực tiếp, không đi qua controller).
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String redirect = HomeRedirectResolver.resolveHomeRedirect(authentication);
            // resolveHomeRedirect trả về dạng "redirect:/xxx", bỏ prefix để dùng với sendRedirect
            String targetUrl = redirect.startsWith("redirect:")
                    ? redirect.substring("redirect:".length())
                    : redirect;
            response.sendRedirect(request.getContextPath() + targetUrl);
        };
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
                                "/lien-he",
                                "/chinh-sach-doi-tra",
                                "/forgot-password",
                                "/forgot-password/reset",
                                "/css/**", "/js/**", "/images/**",
                                "/webjars/**", "/collections/**"
                        ).permitAll()

                        // Chỉ ADMIN / OWNER
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OWNER")
                        .requestMatchers("/admin/collections/**").hasAnyRole("ADMIN", "OWNER")

                        // Chỉ OWNER
                        .requestMatchers("/admin/employees/*/role/**").hasRole("OWNER")

                        // Cashier bán hàng tại quầy (admin/owner cũng được phép vào để hỗ trợ)
                        .requestMatchers("/cashier/**").hasAnyRole("CASHIER", "OWNER", "ADMIN")

                        // Khách đăng nhập
                        .requestMatchers(
                                "/cart/**",
                                "/order/**",
                                "/account/**",
                                "/wishlist/**"
                        ).hasAnyRole("CUSTOMER", "ADMIN", "OWNER")

                        .anyRequest().authenticated()
                )

                .rememberMe(remember -> remember
                        .key("casualwear-secret-key")
                        .tokenValiditySeconds(3 * 24 * 60 * 60)
                )

                .csrf(csrf -> csrf.disable())

                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(authenticationSuccessHandler())
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

                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/error/403")
                );

        return http.build();
    }
}