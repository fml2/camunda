/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.operate.webapp.security.oauth2;

import io.camunda.identity.sdk.IdentityConfiguration;
import io.camunda.operate.property.OperateProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static io.camunda.operate.OperateProfileService.IDENTITY_AUTH_PROFILE;
import static io.camunda.operate.webapp.security.BaseWebConfigurer.sendJSONErrorMessage;

@Component
@Profile(IDENTITY_AUTH_PROFILE)
public class IdentityOAuth2WebConfigurer {

  public static final String SPRING_SECURITY_OAUTH_2_RESOURCESERVER_JWT_ISSUER_URI =
      "spring.security.oauth2.resourceserver.jwt.issuer-uri";
  // Where to find the public key to validate signature,
  // which was created from authorization server's private key
  public static final String SPRING_SECURITY_OAUTH_2_RESOURCESERVER_JWT_JWK_SET_URI =
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri";

  public static final String JWKS_PATH = "/protocol/openid-connect/certs";

  private static final Logger LOGGER = LoggerFactory.getLogger(IdentityOAuth2WebConfigurer.class);

  @Autowired
  private Environment env;

  @Autowired
  private IdentityConfiguration identityConfiguration;

  @Autowired
  private OperateProperties operateProperties;

  @Autowired
  private IdentityJwt2AuthenticationTokenConverter jwtConverter;

  public void configure(HttpSecurity http) throws Exception {
    if (isJWTEnabled()) {
      http.oauth2ResourceServer(
          serverCustomizer ->
              serverCustomizer.authenticationEntryPoint(this::authenticationFailure)
                  .jwt(jwtCustomizer -> jwtCustomizer.jwtAuthenticationConverter(jwtConverter)
                      .jwkSetUri(getJwkSetUriProperty())));
      LOGGER.info("Enabled OAuth2 JWT access to Operate API");
    }
  }

  private String getJwkSetUriProperty() {
    String backendUri = identityConfiguration.getIssuerBackendUrl() + JWKS_PATH;

    // Certain configurations such as Microsoft Entra use a different URI format and need
    // this value as it comes in from the Spring Security environment variable. To ensure
    // backwards compatibility, only use the different format if a specific property config
    // has been set by the user
    if (operateProperties.isUseSpringSecurityJwkUri()) {
      if (env.containsProperty(SPRING_SECURITY_OAUTH_2_RESOURCESERVER_JWT_JWK_SET_URI)) {
        backendUri = env.getProperty(SPRING_SECURITY_OAUTH_2_RESOURCESERVER_JWT_JWK_SET_URI);
        LOGGER.info("JWK override uri property set, using value in SPRING_SECURITY_OAUTH_2_RESOURCESERVER_JWT_JWK_SET_URI for issuer authentication");
      }
      else {
        LOGGER.warn("JWK override uri property set but SPRING_SECURITY_OAUTH_2_RESOURCESERVER_JWT_JWK_SET_URI is not present");
      }
    }

    LOGGER.info("Using {} for issuer authentication", backendUri);

    return backendUri;
  }

  private void authenticationFailure(final HttpServletRequest request,
      final HttpServletResponse response, final AuthenticationException e) throws IOException {
    sendJSONErrorMessage(response, e.getMessage());
  }

  protected boolean isJWTEnabled() {
    return env.containsProperty(SPRING_SECURITY_OAUTH_2_RESOURCESERVER_JWT_ISSUER_URI)
        || env.containsProperty(SPRING_SECURITY_OAUTH_2_RESOURCESERVER_JWT_JWK_SET_URI);
  }

}
