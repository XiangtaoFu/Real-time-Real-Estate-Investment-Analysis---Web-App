package com.ireia.realty.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ireia.realty.config.RealtyApiConfig;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class RealtyInUSApiService {
    
    private final RealtyApiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public RealtyInUSApiService(RealtyApiConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public JsonNode searchProperties(String postalCode, String city, String stateCode, 
                                     String status, Integer page, Integer limit) throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        
        if (limit != null) requestBody.put("limit", limit);
        if (page != null) requestBody.put("offset", (page - 1) * (limit != null ? limit : 20));
        if (postalCode != null && !postalCode.isBlank()) {
            requestBody.put("postal_code", postalCode);
        }
        if (city != null && !city.isBlank()) {
            requestBody.put("city", city);
        }
        if (stateCode != null && !stateCode.isBlank()) {
            requestBody.put("state_code", stateCode);
        }
        
        if (status != null && !status.isBlank()) {
            requestBody.putArray("status").add(status);
        } else {
            requestBody.putArray("status").add("for_sale");
        }
        
        ObjectNode sort = requestBody.putObject("sort");
        sort.put("direction", "desc");
        sort.put("field", "list_date");
        
        return makeRequest("/properties/v3/list", requestBody.toString());
    }
    
    public JsonNode getPropertyDetail(String propertyId) throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("property_id", propertyId);
        
        return makeRequest("/properties/v3/detail", requestBody.toString());
    }
    
    public JsonNode getSimilarHomes(String propertyId) throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("property_id", propertyId);
        
        return makeRequest("/properties/v3/list-similar-homes", requestBody.toString());
    }
    
    public JsonNode getSoldProperties(String postalCode, Integer limit) throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        if (postalCode != null) requestBody.put("postal_code", postalCode);
        if (limit != null) requestBody.put("limit", limit);
        requestBody.putArray("status").add("sold");
        
        ObjectNode sort = requestBody.putObject("sort");
        sort.put("direction", "desc");
        sort.put("field", "sold_date");
        
        return makeRequest("/properties/v2/list-sold", requestBody.toString());
    }
    
    public JsonNode getForRentProperties(String postalCode, Integer limit) throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        if (postalCode != null) requestBody.put("postal_code", postalCode);
        if (limit != null) requestBody.put("limit", limit);
        requestBody.putArray("status").add("for_rent");
        
        ObjectNode sort = requestBody.putObject("sort");
        sort.put("direction", "desc");
        sort.put("field", "list_date");
        
        return makeRequest("/properties/v2/list-for-rent", requestBody.toString());
    }
    
    /**
     * Check current mortgage rates
     * GET /mortgage/v2/check-rates
     */
    public JsonNode checkMortgageRates(String stateCode, String zipCode) throws Exception {
        StringBuilder url = new StringBuilder("https://" + config.getHost() + "/mortgage/v2/check-rates?");
        
        if (stateCode != null && !stateCode.isBlank()) {
            url.append("state_code=").append(stateCode).append("&");
        }
        if (zipCode != null && !zipCode.isBlank()) {
            url.append("postal_code=").append(zipCode);
        }
        
        return makeGetRequest(url.toString());
    }
    
    /**
     * Calculate mortgage payment
     * GET /mortgage/v2/calculate
     */
    public JsonNode calculateMortgage(Double price, Double downPayment, Double rate, Integer term) throws Exception {
        StringBuilder url = new StringBuilder("https://" + config.getHost() + "/mortgage/v2/calculate?");
        
        if (price != null) url.append("price=").append(price).append("&");
        if (downPayment != null) url.append("down_payment=").append(downPayment).append("&");
        if (rate != null) url.append("rate=").append(rate).append("&");
        if (term != null) url.append("term=").append(term);
        
        return makeGetRequest(url.toString());
    }
    
    /**
     * Get finance rates
     * GET /finance/rates
     */
    public JsonNode getFinanceRates() throws Exception {
        String url = "https://" + config.getHost() + "/finance/rates";
        return makeGetRequest(url);
    }
    
    /**
     * Check equity rates for refinancing
     * GET /mortgage/check-equity-rates
     */
    public JsonNode checkEquityRates(String stateCode, String zipCode) throws Exception {
        StringBuilder url = new StringBuilder("https://" + config.getHost() + "/mortgage/check-equity-rates?");
        
        if (stateCode != null && !stateCode.isBlank()) {
            url.append("state_code=").append(stateCode).append("&");
        }
        if (zipCode != null && !zipCode.isBlank()) {
            url.append("zip=").append(zipCode);
        }
        
        return makeGetRequest(url.toString());
    }
    
    /**
     * Calculate affordability
     * GET /mortgage/calculate-affordability
     */
    public JsonNode calculateAffordability(Double income, Double monthlyDebts, Double downPayment, 
                                          Double rate, Integer term) throws Exception {
        StringBuilder url = new StringBuilder("https://" + config.getHost() + "/mortgage/calculate-affordability?");
        
        if (income != null) url.append("income=").append(income).append("&");
        if (monthlyDebts != null) url.append("monthly_debts=").append(monthlyDebts).append("&");
        if (downPayment != null) url.append("down_payment=").append(downPayment).append("&");
        if (rate != null) url.append("rate=").append(rate).append("&");
        if (term != null) url.append("term=").append(term);
        
        return makeGetRequest(url.toString());
    }
    
    /**
     * Check rates (deprecated endpoint)
     * GET /mortgage/check-rates
     */
    public JsonNode checkRatesDeprecated(String stateCode, String zipCode) throws Exception {
        StringBuilder url = new StringBuilder("https://" + config.getHost() + "/mortgage/check-rates?");
        
        if (stateCode != null && !stateCode.isBlank()) {
            url.append("state_code=").append(stateCode).append("&");
        }
        if (zipCode != null && !zipCode.isBlank()) {
            url.append("zip=").append(zipCode);
        }
        
        return makeGetRequest(url.toString());
    }
    
    /**
     * Calculate mortgage (deprecated endpoint)
     * GET /mortgage/calculate
     */
    public JsonNode calculateMortgageDeprecated(Double price, Double downPayment, 
                                               Double rate, Integer term) throws Exception {
        StringBuilder url = new StringBuilder("https://" + config.getHost() + "/mortgage/calculate?");
        
        if (price != null) url.append("price=").append(price).append("&");
        if (downPayment != null) url.append("down_payment=").append(downPayment).append("&");
        if (rate != null) url.append("rate=").append(rate).append("&");
        if (term != null) url.append("term=").append(term);
        
        return makeGetRequest(url.toString());
    }
    
    private JsonNode makeRequest(String endpoint, String requestBody) throws Exception {
        String url = "https://" + config.getHost() + endpoint;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("x-rapidapi-host", config.getHost())
                .header("x-rapidapi-key", config.getKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("API request failed with status " + response.statusCode() 
                    + ": " + response.body());
        }
        
        return objectMapper.readTree(response.body());
    }
    
    private JsonNode makeGetRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("x-rapidapi-host", config.getHost())
                .header("x-rapidapi-key", config.getKey())
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("API request failed with status " + response.statusCode() 
                    + ": " + response.body());
        }
        
        return objectMapper.readTree(response.body());
    }
}
