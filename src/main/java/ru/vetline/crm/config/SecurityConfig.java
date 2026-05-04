package ru.vetline.crm.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import ru.vetline.crm.entity.User;
import ru.vetline.crm.repository.UserRepository;
import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            User user = userRepository.findByEmailAndActiveTrue(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + email));
            if (user.isLocked())
                throw new org.springframework.security.authentication
                        .LockedException("Учётная запись заблокирована");
            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPassword())
                    .roles(user.getRole().name())
                    .build();
        };
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        var p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService());
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authProvider())
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/webhook/**").permitAll()
                .requestMatchers("/css/**","/js/**","/favicon.ico","/login","/login/**").permitAll()
                .requestMatchers("/dashboard/**").hasRole("DIRECTOR")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(f -> f
                .loginPage("/login")
                .defaultSuccessUrl("/tickets", true)
                .failureHandler((req, res, ex) -> {
                    String email = req.getParameter("username");
                    userRepository.findByEmail(email).ifPresent(u -> {
                        int att = u.getFailedAttempts() + 1;
                        u.setFailedAttempts(att);
                        if (att >= 5) u.setLockedUntil(LocalDateTime.now().plusMinutes(30));
                        userRepository.save(u);
                    });
                    res.sendRedirect("/login?error");
                })
                .permitAll()
            )
            .logout(l -> l
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .deleteCookies("JSESSIONID")
            )
            .csrf(c -> c.ignoringRequestMatchers("/api/**"));
        return http.build();
    }
}
