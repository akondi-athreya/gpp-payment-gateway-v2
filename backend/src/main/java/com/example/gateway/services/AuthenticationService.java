package com.example.gateway.services;

import com.example.gateway.models.Merchant;
import com.example.gateway.repositories.MerchantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthenticationService {
    
    @Autowired
    private MerchantRepository merchantRepository;
    
    /**
     * Authenticate merchant using API key and secret
     * @param apiKey The API key from request header
     * @param apiSecret The API secret from request header
     * @return Merchant object if authenticated, null otherwise
     * @throws IllegalArgumentException if credentials are invalid
     */
    public Merchant authenticateMerchant(String apiKey, String apiSecret) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("AUTHENTICATION_ERROR|Missing X-Api-Key header");
        }
        
        if (apiSecret == null || apiSecret.isEmpty()) {
            throw new IllegalArgumentException("AUTHENTICATION_ERROR|Missing X-Api-Secret header");
        }
        
        // Find merchant by API key
        Optional<Merchant> merchantOpt = merchantRepository.findByApiKey(apiKey);
        
        if (!merchantOpt.isPresent()) {
            throw new IllegalArgumentException("AUTHENTICATION_ERROR|Invalid API key");
        }
        
        Merchant merchant = merchantOpt.get();
        
        // Verify API secret
        if (!merchant.getApiSecret().equals(apiSecret)) {
            throw new IllegalArgumentException("AUTHENTICATION_ERROR|Invalid API secret");
        }
        
        // Check if merchant is active
        if (!merchant.getIsActive()) {
            throw new IllegalArgumentException("AUTHENTICATION_ERROR|Merchant account is disabled");
        }
        
        return merchant;
    }
}
