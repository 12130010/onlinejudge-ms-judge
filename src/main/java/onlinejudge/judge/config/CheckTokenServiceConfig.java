package onlinejudge.judge.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CheckTokenServiceConfig {
	@Autowired
	private Environment env;
	
	@Autowired
	ClientCredentialsResourceDetails clientCredentialsResourceDetails;
	/**
	 * Config for check token oauth
	 * @return
	 */
	@Primary
	@Bean
	public RemoteTokenServices tokenService() {
	    RemoteTokenServices tokenService = new RemoteTokenServices();
	    tokenService.setCheckTokenEndpointUrl(
	    		env.getProperty("oauth2.client.check-token-uri"));
	    tokenService.setClientId(clientCredentialsResourceDetails.getClientId());
	    tokenService.setClientSecret(clientCredentialsResourceDetails.getClientSecret());
	    tokenService.setRestTemplate(restTemplate());
	    return tokenService;
	}
	
	@LoadBalanced
    @Bean
    public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			// Ignore 400
			public void handleError(ClientHttpResponse response) throws IOException {
				if (response.getRawStatusCode() != 400) {
					super.handleError(response);
				}
			}
		});
        return restTemplate;
    }
}
