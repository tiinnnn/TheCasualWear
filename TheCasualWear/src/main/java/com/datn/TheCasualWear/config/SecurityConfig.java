package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.util.HomeRedirectResolver;
import jakarta.servlet.http.Cookie;
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
import org.springframework.security.web.session.HttpSessionEventPublisher;

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

    // BẮT BUỘC khi dùng maximumSessions(): giúp Spring Security nhận biết
    // khi nào 1 HttpSession thực sự bị hủy (logout, timeout, invalidate)
    // để cập nhật đúng SessionRegistry. Thiếu bean này, registry sẽ bị "rác"
    // và có thể chặn nhầm login hợp lệ sau một thời gian sử dụng.
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
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
            // Tài khoản quyền cao (ADMIN/OWNER/CASHIER) không được phép dùng
            // remember-me: nếu lỡ tick chọn, cookie sẽ bị xóa ngay sau khi
            // xác thực thành công, buộc phải đăng nhập lại bằng mật khẩu
            // mỗi khi session hết hạn. Bảo mật ưu tiên hơn tiện lợi với các
            // role này; CUSTOMER vẫn được giữ remember-me bình thường.
            boolean isPrivileged = authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_OWNER") || r.equals("ROLE_CASHIER"));

            if (isPrivileged) {
                Cookie cookie = new Cookie("remember-me", null);
                cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
                cookie.setHttpOnly(true);
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            }

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
                                "/webjars/**", "/collections/**",

                                // Trang xem clearance sale — khách vãng lai (chưa đăng nhập)
                                // cũng cần xem được, chỉ hành động mua/checkout mới cần qua
                                // luồng guest riêng (/cart-guest, /order/checkout-guest...).
                                "/clearance",

                                // MỚI (4.1): checkout khách vãng lai — không yêu cầu đăng nhập.
                                // Đặt TRƯỚC .requestMatchers("/order/**").hasAnyRole(...) và
                                // "/cart/**".hasAnyRole(...) bên dưới; Spring Security xét theo
                                // thứ tự khai báo nên rule permitAll cụ thể hơn này phải đứng
                                // trước rule chung mới có hiệu lực.
                                "/cart-guest/**",
                                "/order/checkout-guest",
                                "/order/success-guest/**",
                                "/order/apply-voucher-guest",
                                "/order/lookup-guest",

                                // MỚI: callback VNPay cho khách vãng lai — VNPay redirect thẳng
                                // về đây sau khi thanh toán, không mang theo Authentication nào
                                // cả (guest vốn chưa đăng nhập), nên bắt buộc phải permitAll
                                // giống các route guest khác ở trên. Thiếu dòng này, request bị
                                // rơi vào rule "/order/**".hasAnyRole(...) bên dưới -> bị chặn
                                // redirect về /auth/login, và đơn hàng sẽ KHÔNG được tạo dù
                                // khách đã thanh toán thành công.
                                "/order/vnpay-return-guest",

                                // MỚI: AJAX tính lại phí ship khi khách (kể cả guest) chọn
                                // xong Tỉnh/Quận-Huyện/Phường-Xã ở checkout-guest.html. Thiếu
                                // dòng này, request rơi vào rule "/order/**".hasAnyRole(...)
                                // bên dưới -> bị chặn -> fetchShippingFee() ở checkout-guest.html
                                // âm thầm catch lỗi (chỉ console.error), khiến phí ship không
                                // bao giờ được cập nhật lại sau khi guest chọn địa chỉ.
                                "/order/shipping-fee-preview",

                                // MỚI: proxy GHN (province/district/ward) dùng chung cho cả
                                // checkout user lẫn guest (xem ghi chú trong GhnController) —
                                // guest chưa đăng nhập nên bắt buộc phải permitAll, nếu không
                                // sẽ rơi vào .anyRequest().authenticated() và bị redirect sang
                                // /auth/login, khiến dropdown tỉnh/thành ở checkout-guest.html
                                // không load được (fetch nhận về HTML login page thay vì JSON).
                                "/api/ghn/**"
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

                // Giới hạn 1 session/tài khoản tại 1 thời điểm. Khi đăng nhập
                // ở nơi mới, session cũ (nếu còn) sẽ bị đá ra thay vì chặn
                // login mới (maxSessionsPreventsLogin mặc định là false) —
                // phù hợp với cashier/admin đổi máy mà quên đăng xuất.
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/auth/login?expired=true")
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