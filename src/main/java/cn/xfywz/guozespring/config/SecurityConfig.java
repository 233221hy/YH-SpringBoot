package cn.xfywz.guozespring.config;
import cn.xfywz.guozespring.filter.JwtTokenAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtTokenAuthFilter jwtTokenAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // 开启跨域（修复）
                .csrf(AbstractHttpConfigurer::disable)

                // 核心修复：无状态JWT，禁用Session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/manage/login",
                                "/yeemanage/login",
                                "/course/**",
                                "/test/**",
                                "/captcha",
                                "/verify",
                                "/user/**",
                                "/manage/school_list_noLogin",
                                "/teacher/course/**",
                                "/school/async_export_result",
                                "/school/async_export_download"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
            .securityContext(securityContext -> securityContext.requireExplicitSave(false))
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
        ;

        // JWT过滤器
        http.addFilterBefore(jwtTokenAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
