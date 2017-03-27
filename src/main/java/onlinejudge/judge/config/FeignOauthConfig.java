package onlinejudge.judge.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.web.client.RestTemplate;

import com.netflix.governator.annotations.binding.Primary;

import feign.RequestInterceptor;
import onlinejudge.judge.config.feign.MyClientCredentialsAccessTokenProvider;
import onlinejudge.judge.config.feign.MyOAuth2FeignRequestInterceptor;

@Configuration
public class FeignOauthConfig {
	@Autowired
	RestTemplate restTemplate;
	
	@Bean
	@ConfigurationProperties(prefix = "security.oauth2.client")
	public ClientCredentialsResourceDetails clientCredentialsResourceDetails() {
		return new ClientCredentialsResourceDetails();
	}

//	@Bean
//	public RequestInterceptor oauth2FeignRequestInterceptor(){
//		return new OAuth2FeignRequestInterceptor(new DefaultOAuth2ClientContext(), clientCredentialsResourceDetails());
//	}

	@Primary
	@Bean
	public RequestInterceptor oauth2FeignRequestInterceptor(){
		MyClientCredentialsAccessTokenProvider clientAccessTokenProvider = new MyClientCredentialsAccessTokenProvider();
		clientAccessTokenProvider.setRestTemplate(restTemplate);
		
		AccessTokenProvider accessTokenProvider = new AccessTokenProviderChain(Arrays
				.<AccessTokenProvider> asList(clientAccessTokenProvider));
		
		return new MyOAuth2FeignRequestInterceptor(new DefaultOAuth2ClientContext(), clientCredentialsResourceDetails(),accessTokenProvider);
	}

	@Bean
	public OAuth2RestTemplate clientCredentialsRestTemplate() {
		return new OAuth2RestTemplate(clientCredentialsResourceDetails());
	}
}
