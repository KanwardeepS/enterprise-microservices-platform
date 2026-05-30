package com.authentication.ssl;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

/**
 * SSL/TLS Certificate and Key Management
 * 
 * This class demonstrates how to load SSL certificates and private keys
 * from a keystore, extract certificate information, and create an SSLContext
 * for secure HTTPS communication.
 * 
 * Usage: Provide paths to keystore files and passwords to load certificates.
 */
public class KeyCertificateLoader {
    
    /**
     * Load keystore with private key and certificate
     * 
     * @param keystorePath Path to PKCS12 keystore file (e.g., keystore.p12)
     * @param password     Keystore password
     * @return             KeyStore object containing private key and certificate
     * @throws Exception   If keystore cannot be loaded
     */
    public static KeyStore loadKeyStore(String keystorePath, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, password.toCharArray());
        }
        return keyStore;
    }
    
    /**
     * Extract private key from keystore
     * 
     * Security Note: Private key never leaves the keystore during SSL operations
     * 
     * @param keyStore KeyStore instance (from loadKeyStore)
     * @param alias    Alias of the key entry (typically "tomcat")
     * @param password Password for the key entry
     * @return         PrivateKey object
     * @throws Exception If key cannot be extracted
     */
    public static PrivateKey getPrivateKey(KeyStore keyStore, String alias, String password) throws Exception {
        return (PrivateKey) keyStore.getKey(alias, password.toCharArray());
    }
    
    /**
     * Extract certificate from keystore
     * 
     * @param keyStore KeyStore instance (from loadKeyStore)
     * @param alias    Alias of the certificate entry
     * @return         X509Certificate object
     * @throws Exception If certificate cannot be extracted
     */
    public static X509Certificate getCertificate(KeyStore keyStore, String alias) throws Exception {
        return (X509Certificate) keyStore.getCertificate(alias);
    }
    
    /**
     * Create SSLContext from keystore
     * 
     * This is the main method to use for creating HTTPS connections
     * 
     * @param keystorePath Path to PKCS12 keystore file
     * @param password     Keystore password
     * @return             SSLContext configured for TLSv1.2
     * @throws Exception   If SSL context cannot be created
     */
    public static SSLContext createSSLContext(String keystorePath, String password) throws Exception {
        // Load keystore
        KeyStore keyStore = loadKeyStore(keystorePath, password);
        
        // Initialize KeyManagerFactory
        // KeyManagerFactory manages the server's private key and certificate
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, password.toCharArray());
        
        // Create SSLContext with TLSv1.2 (minimum secure version)
        // Note: Avoid SSLv3, TLSv1.0, TLSv1.1 - they are deprecated and insecure
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), null, null);
        
        return sslContext;
    }
    
    /**
     * Display certificate details for verification
     * 
     * This helps verify that the correct certificate is being used
     * 
     * @param cert X509Certificate to display
     */
    public static void displayCertificateInfo(X509Certificate cert) {
        System.out.println("===== SSL Certificate Information =====");
        System.out.println("Subject: " + cert.getSubjectDN());
        System.out.println("Issuer: " + cert.getIssuerDN());
        System.out.println("Valid From: " + cert.getNotBefore());
        System.out.println("Valid Until: " + cert.getNotAfter());
        System.out.println("Serial: " + cert.getSerialNumber());
        System.out.println("Signature Algorithm: " + cert.getSigAlgName());
        System.out.println("Public Key Algorithm: " + cert.getPublicKey().getAlgorithm());
        System.out.println("========================================");
    }
    
    /**
     * Example usage
     */
    public static void main(String[] args) throws Exception {
        // Example: Load certificate and display information
        String keystorePath = "path/to/keystore.p12";
        String password = "keystore-password";
        
        try {
            // Load keystore
            KeyStore keyStore = loadKeyStore(keystorePath, password);
            
            // Get certificate
            X509Certificate cert = getCertificate(keyStore, "tomcat");
            
            // Display certificate info
            displayCertificateInfo(cert);
            
            // Create SSL context (for use in your application)
            SSLContext sslContext = createSSLContext(keystorePath, password);
            System.out.println("\n✅ SSLContext created successfully with TLSv1.2");
            
        } catch (Exception e) {
            System.err.println("❌ Error loading certificate: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
