package com.mynt.mynt.service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.JWTClaimsSet;

public class JWT {

    private byte[] secretKey;

    // Generate a secret key for HMAC-SHA256 (HS256)
    public void generateSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            keyGenerator.init(256); // Specify the key size
            this.secretKey = keyGenerator.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public String createJWT(String clientName, long expirationTimeMillis) {
        try {

            // 1.create header that has algo and type
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

            // 2.add infomation(claims) into data
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(clientName)
                    .expirationTime(new Date(new Date().getTime() + expirationTimeMillis))
                    .build();

            SignedJWT jwt = new SignedJWT(header, claimsSet);

            // Create HMAC signer
            // 3. create signature
            JWSSigner signer = new MACSigner(this.secretKey);

            // Apply the HMAC protection
            jwt.sign(signer);

            // Serialize to compact form
            return jwt.serialize();

        } catch (JOSEException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean authenticateJWT(String token) {
        try {

            // Parse the token
            SignedJWT jwt = SignedJWT.parse(token);

            // Create HMAC verifier
            JWSVerifier verifier =  new MACVerifier(this.secretKey);
            System.out.println(new MACVerifier(this.secretKey).toString());

            // Verify the token's HMAC
            if (jwt.verify(verifier)) {

                // Check expiration time
                Date expirationTime = jwt.getJWTClaimsSet().getExpirationTime();
                if (!new Date().before(expirationTime)) { return false; }

                // add rules here to check the payload/data of the JWT

                return true;
            } else {
                return false;
            }
        } catch (ParseException | JOSEException e) {
            e.printStackTrace();
        }
        return false;
    }

}
