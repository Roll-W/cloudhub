package org.huel.cloudhub.client.configuration;

import org.huel.cloudhub.client.configuration.filter.SessionRequestFilter;
import org.huel.cloudhub.client.configuration.properties.WebUrlsProperties;
import org.huel.cloudhub.client.service.user.UserDetailsServiceImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.UrlAuthorizationConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;

/**
 * Spring Security配置
 *
 * @author RollW
 */
@Configuration
@EnableWebSecurity
@EnableGlobalAuthentication
 @EnableGlobalMethodSecurity(prePostEnabled=true)
public class WebSecurityConfiguration {
    private final UserDetailsServiceImpl userDetailsService;

    private final WebUrlsProperties urlsProperties;

    private final CustomSecurityMetaSource customSecurityMetaSource;


    public WebSecurityConfiguration(UserDetailsServiceImpl userDetailsService,
                                    WebUrlsProperties urlsProperties,
                                    CustomSecurityMetaSource customSecurityMetaSource) {
        this.userDetailsService = userDetailsService;
        this.urlsProperties = urlsProperties;
        this.customSecurityMetaSource = customSecurityMetaSource;
    }

    protected void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception{
            authenticationManagerBuilder.userDetailsService(userDetailsService);
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity security) throws Exception {
        security.csrf().disable()
                .authorizeRequests().antMatchers("/").permitAll();
        // 暂时禁用Spring Security
        ApplicationContext applicationContext = security.getSharedObject(ApplicationContext.class);

        security.apply(new UrlAuthorizationConfigurer<>(applicationContext))
                .withObjectPostProcessor(new ObjectPostProcessor<FilterSecurityInterceptor>() {

                    @Override
                    public <O extends FilterSecurityInterceptor> O postProcess(O object) {
                        object.setSecurityMetadataSource(customSecurityMetaSource);
                        object.setRejectPublicInvocations(true);
                        return object;
                    }
                });


        return security.build();

//        security.addFilterBefore(sessionRequestFilter(),
//                UsernamePasswordAuthenticationFilter.class);
//
//        security.formLogin()
//                .permitAll()
//                .loginPage(urlsProperties.getFrontendUrl() + "/login")
//                .loginProcessingUrl("/api/user/login");
//        security.logout()
//                .permitAll()
//                .logoutUrl("/api/user/logout")
//                .deleteCookies("cloudhub_cookie");
//        security.authorizeRequests()
//                .antMatchers("/api/user/login/**").permitAll()
//                .antMatchers("/api/user/login").permitAll()
//                .antMatchers("/api/user/test").permitAll()
//                .antMatchers("/anonymous*").anonymous()
//                .antMatchers("/api/user/register/**").permitAll()
//                .antMatchers("/api/common/**").permitAll()
//                .antMatchers("/api/user/message/**").permitAll()
//                .antMatchers("/api/user/logout/").permitAll()
//                .antMatchers("/api/user/admin/**").hasRole("ADMIN");
//        security
//                .userDetailsService(userDetailsService);
//
////                .and()
////                    .rememberMe()
////                    .key("RememberMeKey...547DSdplowFrHtqs4eTGsr1aSQh13F4Kl3ZIQBTzeIViG7Qbvl9tNG5wE67F")
//        security
//                .csrf().disable();
//        security.sessionManagement()
//                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
//
//        security
//                .authorizeRequests()
//                .anyRequest().authenticated();
//        return security.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().antMatchers("/css/**")
                .antMatchers("/404.html")
                .antMatchers("/500.html")
                .antMatchers("/error.html")
                .antMatchers("/html/**")
                .antMatchers("/js/**")
                .antMatchers("/api/user/login");
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRequestFilter sessionRequestFilter() {
        return new SessionRequestFilter();
    }
}
