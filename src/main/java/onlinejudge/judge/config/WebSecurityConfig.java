package onlinejudge.judge.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends ResourceServerConfigurerAdapter{
	@Autowired
	private Environment env;
	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
//				.antMatchers("/" , "/about","/upfile").permitAll()
				.antMatchers("/" , "/about").permitAll()
				.anyRequest().authenticated().and().httpBasic();
	}
//	@Override
//	public void configure(HttpSecurity http) throws Exception {
//	    http.requestMatcher(new OAuthRequestedMatcher())
//	    .authorizeRequests()
//	        .anyRequest().authenticated();
//	}
//
//	private static class OAuthRequestedMatcher implements RequestMatcher {
//	    @Override
//	    public boolean matches(HttpServletRequest request) {
//	        String auth = request.getHeader("Authorization");
//	        // Determine if the client request contained an OAuth Authorization
//	        return (auth != null) && (auth.startsWith("Bearer") || auth.startsWith("bearer"));
//	    }
//	}
}
