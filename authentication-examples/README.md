# Authentication Examples

**Author:** Kanwardeep Singh

Standalone Java implementation files for SSL/TLS, OAuth2, Kerberos, and JWT authentication mechanisms.

## Directory Structure

```
authentication-examples/
├── ssl-tls/
│   └── KeyCertificateLoader.java          # SSL/TLS certificate and key management
├── oauth2/
│   ├── OAuth2Config.java                  # OAuth2 Spring Security configuration
│   └── OAuth2UserController.java           # OAuth2 user information endpoints
├── kerberos/
│   └── KerberosSecurityConfig.java        # Kerberos/SPNEGO configuration
├── jwt/
│   ├── JwtTokenProvider.java              # JWT token creation and validation
│   ├── JwtAuthenticationFilter.java       # JWT request filter
│   ├── JwtSecurityConfig.java             # JWT Spring Security configuration
│   └── AuthController.java                # Login, register, refresh endpoints
└── README.md                              # This file
```

## Dependencies

Add these to your `pom.xml`:

### SSL/TLS
```xml
<dependency>
    <groupId>javax.net.ssl</groupId>
    <artifactId>ssl</artifactId>
</dependency>
```

### OAuth2
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-client</artifactId>
</dependency>
```

### JWT
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Kerberos
```xml
<dependency>
    <groupId>org.springframework.security.kerberos</groupId>
    <artifactId>spring-security-kerberos-core</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security.kerberos</groupId>
    <artifactId>spring-security-kerberos-web</artifactId>
</dependency>
```

## Configuration Files

### application.yml for JWT

```yaml
app:
  jwtSecret: your-secret-key-min-32-chars-long-very-important
  jwtExpiration: 86400  # 24 hours in seconds

spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-google-client-id
            client-secret: your-google-client-secret
            scope: openid,email,profile
          github:
            client-id: your-github-client-id
            client-secret: your-github-client-secret
            scope: user:email,read:user
```

### krb5.conf for Kerberos

```
[libdefaults]
    default_realm = EXAMPLE.COM
    kdc_timesync = 1
    ccache_type = 4

[realms]
    EXAMPLE.COM = {
        kdc = kdc.example.com:88
        admin_server = kdc.example.com:749
    }

[domain_realm]
    .example.com = EXAMPLE.COM
    example.com = EXAMPLE.COM
```

## Usage Examples

### 1. SSL/TLS - KeyCertificateLoader

```java
// Load certificate
KeyStore keyStore = KeyCertificateLoader.loadKeyStore(
    "path/to/keystore.p12",
    "password"
);

// Get certificate info
X509Certificate cert = KeyCertificateLoader.getCertificate(keyStore, "tomcat");
KeyCertificateLoader.displayCertificateInfo(cert);

// Create SSLContext for HTTPS
SSLContext sslContext = KeyCertificateLoader.createSSLContext(
    "path/to/keystore.p12",
    "password"
);
```

### 2. OAuth2 - Login with Google

Add `@Configuration` class and configure in `application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_CLIENT_ID
            client-secret: YOUR_CLIENT_SECRET
```

Then visit `/oauth2/authorization/google`

### 3. JWT - Login and Get Token

```bash
# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"password123"}'

# Response
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "userId": "123",
  "username": "john",
  "roles": ["ROLE_USER"]
}

# Use token in requests
curl -X GET http://localhost:8080/api/protected \
  -H "Authorization: Bearer <accessToken>"
```

### 4. Kerberos - Enterprise Authentication

Set environment variables:

```bash
export KRB5CCNAME=/tmp/krb5cc_1000
export KRB5_CONFIG=/etc/krb5.conf

# Get Kerberos ticket
kinit user@EXAMPLE.COM

# Then access protected endpoint
curl --negotiate -u : http://localhost:8080/secure
```

## Compilation

Compile all classes:

```bash
# Compile SSL/TLS
javac -d bin ssl-tls/KeyCertificateLoader.java

# Compile OAuth2 (requires Spring dependencies)
javac -cp ".:libs/*" -d bin oauth2/*.java

# Compile JWT
javac -cp ".:libs/*" -d bin jwt/*.java

# Compile Kerberos
javac -cp ".:libs/*" -d bin kerberos/*.java
```

## Security Best Practices

### JWT
- ✅ Use HTTPS only
- ✅ Store secret key securely (environment variables, vaults)
- ✅ Include expiration times
- ✅ Use refresh tokens for long-lived sessions
- ❌ Never log JWT tokens
- ❌ Don't store sensitive data in JWT (it's Base64 encoded, not encrypted)

### OAuth2
- ✅ Use authorization code flow (not implicit)
- ✅ Store refresh tokens securely
- ✅ Validate state parameter
- ❌ Never expose client secret in frontend
- ❌ Always use HTTPS for OAuth2

### Kerberos
- ✅ Synchronize time across machines
- ✅ Use strong encryption
- ✅ Protect keytab files (chmod 600)
- ✅ Regular key rotation
- ❌ Never share keytabs

### SSL/TLS
- ✅ Use TLSv1.2 or higher
- ✅ Use certificates from trusted CAs
- ✅ Rotate certificates before expiration
- ✅ Store private keys securely
- ❌ Never expose private key
- ❌ Never use self-signed certs in production

## Testing

Run test suite:

```bash
# Test JWT token creation
mvn test -Dtest=JwtTokenProviderTest

# Test all authentication
mvn test -Dtest=Authentication*
```

## References

- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT.io](https://jwt.io/) - JWT token decoder
- [OAuth 2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)
- [Kerberos Protocol](https://web.mit.edu/kerberos/)
- [SSL/TLS Best Practices](https://wiki.mozilla.org/Security/Server_Side_TLS)

## License

Educational examples - use as reference for your implementations.
