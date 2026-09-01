package net.filemaid.server;

import java.io.IOException;
import net.filemaid.application.port.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean UserDetailsService userDetailsService(UserAccountRepository users){return username->users.findByUsername(username).map(account->User.withUsername(account.username()).password(account.passwordHash()).roles("ADMIN").build()).orElseThrow(()->new org.springframework.security.core.userdetails.UsernameNotFoundException("账号不存在"));}
    @Bean SecurityFilterChain security(HttpSecurity http,FileMaidProperties properties)throws Exception{
        if(!properties.auth().enabled()){http.csrf(csrf->csrf.disable()).authorizeHttpRequests(auth->auth.anyRequest().permitAll());return http.build();}
        var csrf=CookieCsrfTokenRepository.withHttpOnlyFalse();csrf.setCookiePath("/");
        http.csrf(config->config.csrfTokenRepository(csrf).csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .authorizeHttpRequests(auth->auth.requestMatchers("/","/index.html","/assets/**","/favicon.ico","/api/v1/auth/status","/api/v1/auth/setup","/api/v1/auth/login","/api/v1/system/health","/actuator/health").permitAll().anyRequest().authenticated())
                .formLogin(form->form.loginProcessingUrl("/api/v1/auth/login").successHandler((request,response,authentication)->json(response,200,"{\"success\":true}")).failureHandler((request,response,failure)->json(response,401,"{\"error\":\"用户名或密码错误\"}")))
                .logout(logout->logout.logoutUrl("/api/v1/auth/logout").logoutSuccessHandler((request,response,authentication)->json(response,200,"{\"success\":true}")))
                .exceptionHandling(errors->errors.authenticationEntryPoint((request,response,failure)->json(response,401,"{\"error\":\"需要登录\"}")));
        return http.build();
    }
    private static void json(jakarta.servlet.http.HttpServletResponse response,int status,String body)throws IOException{response.setStatus(status);response.setContentType(MediaType.APPLICATION_JSON_VALUE);response.setCharacterEncoding("UTF-8");response.getWriter().write(body);}
}
