# Authentication Examples - Developer Guide

## Quick Start

### 1. Clone and Setup

```bash
cd authentication-examples
mvn clean install
```

### 2. Run Examples

#### JWT Authentication

```bash
# Start the application
mvn spring-boot:run

# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"pass123"}'

# Access protected resource
curl -X GET http://localhost:8080/api/user-info \
  -H "Authorization: Bearer <token-from-login>"
```

#### OAuth2 with Google

```bash
# Configure in application.yml
spring.security.oauth2.client.registration.google.client-id=YOUR_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_SECRET

# Visit browser
http://localhost:8080/oauth2/authorization/google
```

## File Organization

### SSL/TLS (`ssl-tls/`)

**KeyCertificateLoader.java**
- Load certificates from keystore
- Extract certificate information
- Create SSLContext for HTTPS

**Usage:**
```java
SSLContext ssl = KeyCertificateLoader.createSSLContext(
    "keystore.p12",
    "password"
);
```

### OAuth2 (`oauth2/`)

**OAuth2Config.java**
- Spring Security OAuth2 configuration
- Enables OAuth2 login filter
- Configures redirect URLs

**OAuth2UserController.java**
- Access authorized client
- Extract access token
- Call OAuth2 resource server (Google, GitHub API)

**Configuration:**
```yaml
spring.security.oauth2.client.registration.google:
  client-id: xxx
  client-secret: xxx
  scope: openid,email,profile
```

### JWT (`jwt/`)

**JwtTokenProvider.java**
- Create JWT tokens with claims
- Validate token signature and expiration
- Extract claims (userId, roles, etc.)

**Key Methods:**
- `createToken(userId, username, roles)` - Generate JWT
- `validateToken(token)` - Check validity
- `getUserIdFromToken(token)` - Extract user ID
- `getRolesFromToken(token)` - Extract roles

**JwtAuthenticationFilter.java**
- Extract token from Authorization header
- Validate token
- Set security context

**JwtSecurityConfig.java**
- Configure stateless session management
- Add JWT filter to filter chain
- Define public/protected endpoints

**AuthController.java**
- `/auth/login` - Login endpoint
- `/auth/register` - Register new user
- `/auth/refresh` - Refresh access token

### Kerberos (`kerberos/`)

**KerberosSecurityConfig.java**
- Enable SPNEGO authentication
- Configure Kerberos authentication provider
- Set up SPNEGO entry point

**Requirements:**
- krb5.conf configuration file
- Active Directory or Kerberos KDC
- Valid user credentials

## Configuration Examples

### application.yml

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  application:
    name: authentication-examples
  
  # JWT Configuration
  app:
    jwtSecret: ${JWT_SECRET:your-secret-key-min-32-chars-long-very-important}
    jwtExpiration: 86400  # 24 hours
  
  # Security Configuration
  security:
    user:
      name: admin
      password: password
    
    # OAuth2 Configuration
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid,email,profile
            redirect-uri: "http://localhost:8080/login/oauth2/code/google"
          
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: user:email,read:user
            redirect-uri: "http://localhost:8080/login/oauth2/code/github"
        
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://www.googleapis.com/oauth2/v4/token
            user-info-uri: https://www.googleapis.com/oauth2/v1/userinfo
            user-name-attribute: email
  
  # Kerberos Configuration
  security:
    kerberos:
      krb5-config: /etc/krb5.conf

logging:
  level:
    root: INFO
    com.authentication: DEBUG
    org.springframework.security: DEBUG
```

### krb5.conf (For Kerberos)

```ini
[libdefaults]
    default_realm = EXAMPLE.COM
    kdc_timesync = 1
    ccache_type = 4
    forwardable = true
    proxiable = true

[realms]
    EXAMPLE.COM = {
        kdc = kdc.example.com:88
        admin_server = kdc.example.com:749
        master_kdc = kdc.example.com:88
    }

[domain_realm]
    .example.com = EXAMPLE.COM
    example.com = EXAMPLE.COM

[logging]
    kdc = FILE:/var/log/krb5/kdc.log
    admin_server = FILE:/var/log/krb5/admin_server.log
```

## API Endpoints

### Authentication Endpoints

#### Login
```
POST /auth/login
Content-Type: application/json

{
  "username": "john",
  "password": "password123"
}

Response:
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "userId": "123",
  "username": "john",
  "roles": ["ROLE_USER"]
}
```

#### Register
```
POST /auth/register
Content-Type: application/json

{
  "username": "jane",
  "email": "jane@example.com",
  "password": "password123"
}

Response:
{
  "message": "User registered successfully",
  "timestamp": 1234567890
}
```

#### Refresh Token
```
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}

Response:
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

### Protected Endpoints

#### Get User Info
```
GET /api/user-info
Authorization: Bearer <accessToken>

Response:
{
  "provider": "google",
  "userInfo": {...}
}
```

#### OAuth2 Google Login
```
GET /oauth2/authorization/google
(Redirects to Google login)
```

## Security Best Practices

### JWT

✅ **DO:**
- Use HTTPS only
- Set expiration times (short-lived tokens)
- Use refresh tokens for extended sessions
- Store secret key in environment variables
- Rotate keys periodically
- Include user roles/permissions in claims

❌ **DON'T:**
- Log JWT tokens
- Store sensitive data in JWT (not encrypted, only Base64 encoded)
- Use weak secrets (< 32 characters)
- Hardcode secrets in code
- Use symmetric encryption for critical systems
- Ignore token expiration

### OAuth2

✅ **DO:**
- Use authorization code flow
- Store refresh tokens securely
- Validate state parameter
- Use HTTPS for all OAuth2 communication
- Implement PKCE for mobile apps
- Check token expiration

❌ **DON'T:**
- Expose client secret in frontend code
- Use implicit flow in production
- Store client secret in browser
- Trust unverified tokens
- Mix HTTP and HTTPS

### Kerberos

✅ **DO:**
- Synchronize time across all machines (NTP)
- Use strong encryption (AES-256)
- Protect keytab files (chmod 600)
- Rotate keys regularly
- Monitor KDC logs
- Use Kerberos in corporate networks

❌ **DON'T:**
- Share keytabs over unsecured channels
- Run KDC on untrusted systems
- Disable encryption
- Use weak passwords
- Ignore time synchronization

### SSL/TLS

✅ **DO:**
- Use TLSv1.2 or TLSv1.3
- Use certificates from trusted CAs
- Rotate certificates before expiration
- Store private keys securely
- Use strong cipher suites
- Enable HSTS header

❌ **DON'T:**
- Use SSLv3, TLSv1.0, TLSv1.1
- Use self-signed certs in production
- Expose private keys
- Disable certificate validation
- Use weak cipher suites
- Ignore certificate warnings

## Common Issues and Solutions

### JWT Token Expired
```
Error: Expired JWT token

Solution:
1. Use /auth/refresh endpoint with refresh token
2. Increase token expiration time in application.yml
3. Implement refresh token rotation
```

### OAuth2 Redirect URI Mismatch
```
Error: The redirect URI (http://localhost:8080/...) does not match registered URI

Solution:
1. Configure redirect-uri in application.yml
2. Whitelist URI in OAuth2 provider settings
3. Ensure protocol (http/https) matches
```

### Kerberos Time Skew
```
Error: Clock skew too great

Solution:
1. Sync system time with NTP: ntpdate -s time.nist.gov
2. Check KDC time offset
3. Increase allowed skew in krb5.conf: clockskew = 300
```

### SSL Certificate Errors
```
Error: PKIX path building failed: unable to find valid certification path

Solution:
1. Import certificate into Java keystore
2. Add certificate to truststore
3. Disable certificate validation only for testing (NOT PRODUCTION)
```

## Performance Optimization

### JWT Caching
```java
@Cacheable(value = "tokenCache")
public boolean validateToken(String token) {
    // Validation logic
}
```

### Token Refresh Strategy
```
- Short-lived access token: 15-30 minutes
- Long-lived refresh token: 7-30 days
- Automatic refresh when access token within 1 min of expiration
```

### Connection Pooling
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn integration-test
```

### JWT Token Testing
```java
@Test
public void testCreateToken() {
    String token = tokenProvider.createToken("123", "john", List.of("ROLE_USER"));
    assertTrue(tokenProvider.validateToken(token));
    assertEquals("123", tokenProvider.getUserIdFromToken(token));
}
```

## Deployment

### Docker
```dockerfile
FROM openjdk:11-jre-slim
COPY target/authentication-examples-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: auth
        image: auth-service:1.0.0
        env:
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: auth-secrets
              key: jwt-secret
```

## References

- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT (RFC 7519)](https://tools.ietf.org/html/rfc7519)
- [OAuth 2.0 (RFC 6749)](https://tools.ietf.org/html/rfc6749)
- [Kerberos (RFC 4120)](https://tools.ietf.org/html/rfc4120)
- [TLS 1.3 (RFC 8446)](https://tools.ietf.org/html/rfc8446)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)

## Support

For questions or issues:
1. Check the [Authentication Tutorial](../authentication-tutorial.html)
2. Review Spring Security documentation
3. Check logs with `DEBUG` logging level

---

Last Updated: 2026-04-26
