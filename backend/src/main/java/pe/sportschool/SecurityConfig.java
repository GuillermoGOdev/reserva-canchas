package pe.sportschool;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService users(JdbcTemplate db) {
        return username -> {
            var rows = db.queryForList("SELECT * FROM app_users WHERE username=? AND active=true", username);
            if (rows.isEmpty()) throw new UsernameNotFoundException("Usuario no encontrado");
            var row = rows.getFirst();
            return User.withUsername(username).password((String) row.get("password_hash")).roles((String) row.get("role")).build();
        };
    }
    @Bean AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(List.of(provider));
    }
    @Bean HttpSessionSecurityContextRepository contextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean SecurityFilterChain security(HttpSecurity http, HttpSessionSecurityContextRepository repository) throws Exception {
        return http
            .csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .securityContext(context -> context.securityContextRepository(repository))
            .authorizeHttpRequests(auth -> auth.requestMatchers("/api/csrf", "/api/login", "/api/public/**", "/error").permitAll().anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req,res,ex) -> { res.setStatus(401); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"message\":\"Inicia sesión para continuar\"}"); })
                .accessDeniedHandler((req,res,ex) -> { res.setStatus(403); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"message\":\"No tienes permiso o la sesión ha vencido. Recarga la página.\"}"); }))
            .logout(logout -> logout.logoutUrl("/api/logout").logoutSuccessHandler((req,res,auth) -> res.setStatus(204)))
            .build();
    }
    @Bean CommandLineRunner seedUsers(JdbcTemplate db, PasswordEncoder encoder) {
        return args -> {
            db.update("INSERT INTO app_users(username,name,password_hash,role) VALUES (?,?,?,'ADMIN') ON CONFLICT(username) DO NOTHING", "admin", "Administración", encoder.encode(System.getenv().getOrDefault("ADMIN_PASSWORD", "SportSchool2026!")));
            db.update("INSERT INTO app_users(username,name,password_hash,role) VALUES (?,?,?,'OPERATOR') ON CONFLICT(username) DO NOTHING", "operador", "Atención al cliente", encoder.encode(System.getenv().getOrDefault("OPERATOR_PASSWORD", "Canchas2026!")));
            db.update("INSERT INTO user_sites(user_id,site_id) SELECT u.id,s.id FROM app_users u CROSS JOIN sites s WHERE u.username IN ('admin','operador') ON CONFLICT DO NOTHING");
        };
    }
}
