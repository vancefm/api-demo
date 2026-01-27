package com.demo.application.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
// note: BouncyCastle ASN.1 classes referenced via fully-qualified names to avoid import name collisions

/**
 * Service for issuing and validating JWTs and exposing the public JWKS.
 *
 * <p>Key material is loaded once on startup from one of the supported sources
 * (keystore, PEM env var, or PEM file). The public key is exposed via JWKS so
 * gateways and clients can verify JWT signatures without needing the private key.</p>
 */
@Service
public class JwtService {
    private final JwtProperties props;
    private final Environment env;
    private RSAKey rsaJwk;

    public JwtService(JwtProperties props, Environment env) {
        this.props = props;
        this.env = env;
        try {
            loadKeyMaterial();
        } catch (Exception e) {
            // No key configured in environment — keep rsaJwk null (dev fallback)
        }
    }

    /**
     * Loads RSA signing keys from one of the supported sources in priority order:
     * <ol>
     *   <li>PKCS#12 keystore (env: SECURITY_JWT_KEYSTORE_PATH/PASSWORD/KEY_ALIAS)</li>
     *   <li>PKCS#8 PEM value (env: SECURITY_JWT_PRIVATE_KEY)</li>
     *   <li>PEM file path (env: JWT_PRIVATE_KEY_PATH or app property)</li>
     * </ol>
     *
     * <p>Enforces RSA-4096 minimum and builds an RSA JWK for signing/verification.</p>
     */
    private void loadKeyMaterial() throws Exception {
        String keySource = props.getKeySource();

        if ("keystore".equalsIgnoreCase(keySource)) {
            String ksPath = env.getProperty("SECURITY_JWT_KEYSTORE_PATH");
            String ksPassword = env.getProperty("SECURITY_JWT_KEYSTORE_PASSWORD");
            String keyAlias = env.getProperty("SECURITY_JWT_KEY_ALIAS", props.getKid());
            if (ksPath != null && ksPassword != null && keyAlias != null) {
                try (InputStream is = java.nio.file.Files.newInputStream(java.nio.file.Path.of(ksPath))) {
                    KeyStore ks = KeyStore.getInstance("PKCS12");
                    ks.load(is, ksPassword.toCharArray());
                    KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) ks.getEntry(keyAlias, new KeyStore.PasswordProtection(ksPassword.toCharArray()));
                    PrivateKey priv = entry.getPrivateKey();
                    RSAPrivateKey rsaPriv = (RSAPrivateKey) priv;
                    RSAPublicKey rsaPub = (RSAPublicKey) entry.getCertificate().getPublicKey();
                    validateKeySize(rsaPub);
                    rsaJwk = new RSAKey.Builder(rsaPub).privateKey(rsaPriv).keyID(props.getKid()).build();
                    return;
                }
            }
        }

        // Try environment PEM variable (PKCS#8 PEM)
        String pem = env.getProperty("SECURITY_JWT_PRIVATE_KEY");
        if (pem != null && !pem.isBlank()) {
            RSAPrivateKey priv = parsePkcs8Pem(pem);
            RSAPublicKey pub = derivePublicKey(priv);
            validateKeySize(pub);
            rsaJwk = new RSAKey.Builder(pub).privateKey(priv).keyID(props.getKid()).build();
            return;
        }
        // Try filesystem path (property or env override)
        String pemPath = env.getProperty("JWT_PRIVATE_KEY_PATH");
        if ((pemPath == null || pemPath.isBlank()) && props.getPrivateKeyPath() != null) {
            pemPath = props.getPrivateKeyPath();
        }
        if (pemPath != null && !pemPath.isBlank()) {
            java.nio.file.Path path = java.nio.file.Path.of(pemPath);
            if (java.nio.file.Files.exists(path)) {
                String filePem = java.nio.file.Files.readString(path, StandardCharsets.UTF_8);
                RSAPrivateKey priv = parsePkcs8Pem(filePem);
                RSAPublicKey pub = derivePublicKey(priv);
                validateKeySize(pub);
                rsaJwk = new RSAKey.Builder(pub).privateKey(priv).keyID(props.getKid()).build();
                return;
            } else {
                throw new IllegalStateException("Configured JWT private key path does not exist: " + pemPath);
            }
        }
    }

    private void validateKeySize(RSAPublicKey pub) {
        int bits = pub.getModulus().bitLength();
        if (bits < 4096) {
            throw new IllegalStateException("Configured RSA key is too small: " + bits + " bits. Require RSA-4096 or larger.");
        }
    }

    private RSAPrivateKey parsePkcs8Pem(String pem) throws Exception {
        String normalized = pem.replaceAll("-----(BEGIN|END) [A-Z ]+-----", "").replaceAll("\r?\n", "");
        try {
            byte[] der = Base64.getDecoder().decode(normalized);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey pk = kf.generatePrivate(spec);
            return (RSAPrivateKey) pk;
        } catch (Exception pkcs8Ex) {
            try (java.io.StringReader sr = new java.io.StringReader(pem);
                 PEMParser parser = new PEMParser(sr)) {
                Object obj = parser.readObject();
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                if (obj instanceof PEMKeyPair) {
                    PrivateKey pk = converter.getPrivateKey(((PEMKeyPair) obj).getPrivateKeyInfo());
                    return (RSAPrivateKey) pk;
                }
                if (obj != null) {
                    try {
                        PrivateKey pk = converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) obj);
                        return (RSAPrivateKey) pk;
                    } catch (ClassCastException cce) {
                        if (obj instanceof org.bouncycastle.asn1.pkcs.RSAPrivateKey) {
                            org.bouncycastle.asn1.pkcs.RSAPrivateKey rsa = (org.bouncycastle.asn1.pkcs.RSAPrivateKey) obj;
                            java.math.BigInteger modulus = rsa.getModulus();
                            java.math.BigInteger publicExp = rsa.getPublicExponent();
                            java.math.BigInteger privateExp = rsa.getPrivateExponent();
                            java.math.BigInteger p = rsa.getPrime1();
                            java.math.BigInteger q = rsa.getPrime2();
                            java.math.BigInteger dp = rsa.getExponent1();
                            java.math.BigInteger dq = rsa.getExponent2();
                            java.math.BigInteger qi = rsa.getCoefficient();
                            java.security.spec.RSAPrivateCrtKeySpec spec = new java.security.spec.RSAPrivateCrtKeySpec(modulus, publicExp, privateExp, p, q, dp, dq, qi);
                            KeyFactory kf = KeyFactory.getInstance("RSA");
                            return (RSAPrivateKey) kf.generatePrivate(spec);
                        }
                    }
                }
            }
            throw pkcs8Ex;
        }
    }

    private RSAPublicKey derivePublicKey(RSAPrivateKey priv) throws Exception {
        if (priv instanceof RSAPrivateCrtKey crt) {
            RSAPublicKeySpec pubSpec = new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(pubSpec);
        }
        throw new IllegalStateException("Cannot derive public key; provide keystore or a certificate alongside the private key.");
    }

    /**
     * Creates a signed JWT for the given subject using the configured RSA key.
     *
     * <p>Includes standard claims (sub, iat, exp, jti) and merges in configured
     * static claims plus any extra claims provided at call time.</p>
     *
     * <p>If no signing key is configured (dev fallback), returns a random UUID instead.</p>
     */
    public String createToken(String subject, Map<String, Object> extraClaims) {
        if (rsaJwk == null) {
            // Development fallback: return UUID token when no key configured (insecure)
            return UUID.randomUUID().toString();
        }

        try {
            RSAPrivateKey privateKey = rsaJwk.toRSAPrivateKey();

            Instant now = Instant.now();
            Instant exp = now.plus(props.getTtl());

            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(exp))
                    .jwtID(UUID.randomUUID().toString());

            if (props.getClaims() != null) {
                props.getClaims().forEach(claims::claim);
            }

            if (extraClaims != null) {
                extraClaims.forEach(claims::claim);
            }

            SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.parse(props.getAlg())).keyID(props.getKid()).build(), claims.build());
            JWSSigner signer = new RSASSASigner(privateKey);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validates a JWT by verifying the RSA signature and expiration timestamp.
     * Returns false on any parse or verification error.
     */
    public boolean validateToken(String token) {
        if (rsaJwk == null) {
            return token != null && !token.isBlank();
        }
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            RSAPublicKey pub = rsaJwk.toRSAPublicKey();
            RSASSAVerifier verifier = new RSASSAVerifier(pub);
            if (!signedJWT.verify(verifier)) return false;
            Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
            return exp != null && exp.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the public JWK set used by clients to verify JWT signatures.
     * If no key is configured, the set is empty.
     */
    public JWKSet getPublicJwkSet() {
        if (rsaJwk != null) {
            return new JWKSet(rsaJwk.toPublicJWK());
        }
        return new JWKSet();
    }
}
