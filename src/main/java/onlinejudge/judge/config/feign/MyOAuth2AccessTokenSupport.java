package onlinejudge.judge.config.feign;

import org.springframework.security.oauth2.client.token.OAuth2AccessTokenSupport;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

public class MyOAuth2AccessTokenSupport extends OAuth2AccessTokenSupport{
	RestOperations restTemplate;
	
	public MyOAuth2AccessTokenSupport() {
		super();
	}
	
	public void setRestTemplate(RestOperations restTemplate) {
		this.restTemplate = restTemplate;
		setMessageConverters(new RestTemplate().getMessageConverters());
	}

	@Override
	protected RestOperations getRestTemplate() {
		return this.restTemplate;
	}
}
