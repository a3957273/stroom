package stroom.security.server;

import com.google.common.base.Strings;
import org.apache.shiro.web.util.WebUtils;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import stroom.apiclients.AuthenticationServiceClients;
import stroom.auth.service.ApiException;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@Component
public class JWTService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JWTService.class);

    private static final String BEARER = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final String authenticationServiceUrl;
    private final PublicJsonWebKey deserialisedJwk;
    private final String authJwtIssuer;
    private AuthenticationServiceClients authenticationServiceClients;
    private final boolean checkTokenRevocation;

    @Inject
    public JWTService(
            @NotNull @Value("#{propertyConfigurer.getProperty('stroom.auth.service.url')}")
        final String authenticationServiceUrl,
            @NotNull @Value("#{propertyConfigurer.getProperty('stroom.auth.jwt.verificationkey')}")
        final String authJwtVerificationKey,
            @NotNull @Value("#{propertyConfigurer.getProperty('stroom.auth.jwt.issuer')}")
        final String authJwtIssuer,
            @NotNull @Value("#{propertyConfigurer.getProperty('stroom.auth.jwt.enabletokenrevocationcheck')}")
        final boolean enableTokenRevocationCheck,
            final AuthenticationServiceClients authenticationServiceClients) throws JoseException {
        this.authenticationServiceUrl = authenticationServiceUrl;
        this.authJwtIssuer = authJwtIssuer;
        deserialisedJwk = PublicJsonWebKey.Factory.newPublicJwk(authJwtVerificationKey);
        this.authenticationServiceClients = authenticationServiceClients;
        this.checkTokenRevocation = enableTokenRevocationCheck;

        if (authenticationServiceUrl == null) {
            throw new SecurityException("No authentication service URL is defined");
        }
    }

    public boolean containsValidJws(ServletRequest request) {
        Optional<String> authHeader = getAuthHeader(request);
        String jws;
        if (authHeader.isPresent()) {
            String bearerString = authHeader.get();

            if(bearerString.startsWith(BEARER)){
                // This chops out 'Bearer' so we get just the token.
                jws = bearerString.substring(BEARER.length());
            }
            else{
                jws = bearerString;
            }
            LOGGER.debug("Found auth header in request. It looks like this: {}", jws);
        }
        else {
            // If there's no token then we've nothing to do.
            return false;
        }

        try {
            if(checkTokenRevocation) {
                LOGGER.info("Checking token revocation status in remote auth service...");
                JWTAuthenticationToken jwtAuthenticationToken = checkToken(jws);
                return jwtAuthenticationToken.getUserId() != null;
            } else {
                LOGGER.info("Verifying token...");
                Optional<String> usersEmail = verifyToken(jws);
                return usersEmail.isPresent();
            }

        } catch (Exception e){
            LOGGER.error("Unable to verify token:", e.getMessage(), e);
            // If we get an exception verifying the token then we need to log the message
            // and continue as if the token wasn't provided.
            // TODO: decide if this should be handled by an exception and how
            return false;
        }

    }

    public Optional<String> getJws(ServletRequest request){
        Optional<String> authHeader = getAuthHeader(request);
        Optional<String> jws = Optional.empty();
        if (authHeader.isPresent()) {
            String bearerString = authHeader.get();
            if(bearerString.startsWith(BEARER)){
                // This chops out 'Bearer' so we get just the token.
                jws = Optional.of(bearerString.substring(BEARER.length()));
            }
            else{
                jws = Optional.of(bearerString);
            }
            LOGGER.debug("Found auth header in request. It looks like this: {}", jws);
        }
        return jws;
    }

    public JWTAuthenticationToken checkToken(String token) {
        try {
            LOGGER.info("Checking with the Authentication Service that a token is valid.");
            String usersEmail = authenticationServiceClients.newAuthenticationApi().verifyToken(token);
            return new JWTAuthenticationToken(usersEmail, token);
        } catch (ApiException e) {
            throw new RuntimeException("Unable to verify token remotely!", e);
        }
    }

    public Optional<String> verifyToken(String token){
        try {
            JwtConsumer jwtConsumer = newJwsConsumer();
            JwtClaims claims = jwtConsumer.processToClaims(token);
            return Optional.of(claims.getSubject());
        } catch (InvalidJwtException | MalformedClaimException e) {
            LOGGER.warn("Unable to verify token!");
            return Optional.empty();
        }
    }

    public JwtConsumer newJwsConsumer(){
        JwtConsumerBuilder builder = new JwtConsumerBuilder()
                .setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for clock skew
                .setRequireSubject() // the JWT must have a subject claim
                .setVerificationKey(this.deserialisedJwk.getPublicKey()) // verify the signature with the public key
                .setRelaxVerificationKeyValidation() // relaxes key length requirement
                .setJwsAlgorithmConstraints( // only allow the expected signature algorithm(s) in the given context
                        new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.WHITELIST, // which is only RS256 here
                                AlgorithmIdentifiers.RSA_USING_SHA256))
                .setExpectedIssuer(authJwtIssuer);
        return builder.build();
    }

    public static Optional<String> getAuthHeader(ServletRequest request) {
        HttpServletRequest httpServletRequest = WebUtils.toHttp(request);
        return(getAuthHeader(httpServletRequest));
    }

    public static Optional<String> getAuthHeader(HttpServletRequest httpServletRequest){
        String authHeader = httpServletRequest.getHeader(AUTHORIZATION_HEADER);
        return Strings.isNullOrEmpty(authHeader) ? Optional.empty() : Optional.of(authHeader);
    }
}
