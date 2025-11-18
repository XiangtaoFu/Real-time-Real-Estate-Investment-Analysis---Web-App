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
    
    /**
     * Get property detail.
     * Preferred: GET /properties/v3/detail?property_id=...
     * Fallback: POST /properties/v3/list with { query: { property_id }, limit: 1 }
     * Note: v2/detail uses different ID format (e.g., O3599084026) and is incompatible with v3 IDs (e.g., 9884614204)
     */
    public JsonNode getPropertyDetail(String propertyId) throws Exception {
        // 1) Try the verified v3/detail endpoint first (GET)
        try {
            String url = "https://" + config.getHost() + "/properties/v3/detail?property_id=" + propertyId;
            return makeGetRequest(url);
        } catch (Exception ex) {
            // Fall through to v3/list fallback if v3/detail isn't available for this ID/account
        }

        // 2) Fallback to v3/list with property_id filter in query object
        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode query = requestBody.putObject("query");
        query.put("property_id", propertyId);
        requestBody.put("limit", 1);
        return makeRequest("/properties/v3/list", requestBody.toString());
    }
    
    public JsonNode getSimilarHomes(String propertyId) throws Exception {
        // Try v3 list-similar-homes first (works with v3 IDs), then fallback to v2
        try {
            String v3 = "https://" + config.getHost() + 
                    "/properties/v3/list-similar-homes?property_id=" + propertyId + "&status=for_sale&limit=10";
            return makeGetRequest(v3);
        } catch (Exception ex) {
            // fallback to v2 (may require v2-style IDs in some cases)
            String v2 = "https://" + config.getHost() + 
                    "/properties/v2/list-similar-homes?property_id=" + propertyId;
            return makeGetRequest(v2);
        }
    }
    
    /**
     * Get sold properties
     * GET /properties/v2/list-sold
     */
    public JsonNode getSoldProperties(String postalCode, Integer limit) throws Exception {
        StringBuilder url = new StringBuilder("https://" + config.getHost() + "/properties/v2/list-sold?");
        
        if (postalCode != null && !postalCode.isBlank()) {
            url.append("postal_code=").append(postalCode).append("&");
        }
        if (limit != null) {
            url.append("limit=").append(limit).append("&");
        }
        url.append("sort=sold_date&offset=0");
        
        return makeGetRequest(url.toString());
    }
    
    /**
     * Get rental properties
     * GET /properties/v2/list-for-rent
     */
    public JsonNode getForRentProperties(String postalCode, Integer limit) throws Exception {
        StringBuilder url = new StringBuilder("https://" + config.getHost() + "/properties/v2/list-for-rent?");
        
        if (postalCode != null && !postalCode.isBlank()) {
            url.append("postal_code=").append(postalCode).append("&");
        }
        if (limit != null) {
            url.append("limit=").append(limit).append("&");
        }
        url.append("sort=relevance&offset=0");
        
        return makeGetRequest(url.toString());
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
     * Convenience: Check current mortgage rates by postal code only (v2)
     */
    public JsonNode checkMortgageRatesByPostal(String postalCode) throws Exception {
        String url = "https://" + config.getHost() + "/mortgage/v2/check-rates?postal_code=" + postalCode;
        return makeGetRequest(url);
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
     * Extended v2 mortgage calculation with optional fields used by examples
     * GET /mortgage/v2/calculate?home_insurance=..&property_tax_rate=..&down_payment=..&price=..&term=..&rate=..&hoa_fees=..&apply_veterans_benefits=false
     */
    public JsonNode calculateMortgageV2Extended(Double price, Double downPayment, Double rate, Integer term,
                                                Double homeInsurance, Double propertyTaxRate,
                                                Double hoaFees, Boolean applyVeteransBenefits) throws Exception {
        StringBuilder url = new StringBuilder("https://" + config.getHost() + "/mortgage/v2/calculate?");
        if (price != null) url.append("price=").append(price).append("&");
        if (downPayment != null) url.append("down_payment=").append(downPayment).append("&");
        if (rate != null) url.append("rate=").append(rate).append("&");
        if (term != null) url.append("term=").append(term).append("&");
        if (homeInsurance != null) url.append("home_insurance=").append(homeInsurance).append("&");
        if (propertyTaxRate != null) url.append("property_tax_rate=").append(propertyTaxRate).append("&");
        if (hoaFees != null) url.append("hoa_fees=").append(hoaFees).append("&");
        if (applyVeteransBenefits != null) url.append("apply_veterans_benefits=").append(applyVeteransBenefits);
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
     * Get finance rates by location (zip)
     * GET /finance/rates?loc=93505
     */
    public JsonNode getFinanceRatesByLoc(String loc) throws Exception {
        String url = "https://" + config.getHost() + "/finance/rates?loc=" + loc;
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
     * Detailed equity rates query matching example params
     * GET /mortgage/check-equity-rates?creditScore=...&loanProduct=...&loanAmount=...&propertyValue=...&mortgageBalance=...&zip=...&state=...
     */
    public JsonNode checkEquityRatesDetailed(String creditScore, String loanProduct,
                                             Double loanAmount, Double propertyValue,
                                             Double mortgageBalance, String zip, String state) throws Exception {
        StringBuilder url = new StringBuilder("https://" + config.getHost() + "/mortgage/check-equity-rates?");
        if (creditScore != null && !creditScore.isBlank()) url.append("creditScore=").append(creditScore).append("&");
        if (loanProduct != null && !loanProduct.isBlank()) url.append("loanProduct=").append(loanProduct).append("&");
        if (loanAmount != null) url.append("loanAmount=").append(loanAmount).append("&");
        if (propertyValue != null) url.append("propertyValue=").append(propertyValue).append("&");
        if (mortgageBalance != null) url.append("mortgageBalance=").append(mortgageBalance).append("&");
        if (zip != null && !zip.isBlank()) url.append("zip=").append(zip).append("&");
        if (state != null && !state.isBlank()) url.append("state=").append(state);
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
     * Detailed affordability calculation matching example params
     * GET /mortgage/calculate-affordability?annual_income=...&debt_to_income_ratio=...&down_payment=...&hoa_fees=...&homeowner_insurance_rate=...&interest_rate=...&is_pmi_included=...&loan_term=...&monthly_debt=...&tax_rate=...
     */
    public JsonNode calculateAffordabilityDetailed(Double annualIncome, Double debtToIncomeRatio,
                                                   Double downPayment, Double hoaFees,
                                                   Double homeownerInsuranceRate, Double interestRate,
                                                   Boolean isPmiIncluded, Integer loanTerm,
                                                   Double monthlyDebt, Double taxRate) throws Exception {
        StringBuilder url = new StringBuilder("https://" + config.getHost() + "/mortgage/calculate-affordability?");
        if (annualIncome != null) url.append("annual_income=").append(annualIncome).append("&");
        if (debtToIncomeRatio != null) url.append("debt_to_income_ratio=").append(debtToIncomeRatio).append("&");
        if (downPayment != null) url.append("down_payment=").append(downPayment).append("&");
        if (hoaFees != null) url.append("hoa_fees=").append(hoaFees).append("&");
        if (homeownerInsuranceRate != null) url.append("homeowner_insurance_rate=").append(homeownerInsuranceRate).append("&");
        if (interestRate != null) url.append("interest_rate=").append(interestRate).append("&");
        if (isPmiIncluded != null) url.append("is_pmi_included=").append(isPmiIncluded).append("&");
        if (loanTerm != null) url.append("loan_term=").append(loanTerm).append("&");
        if (monthlyDebt != null) url.append("monthly_debt=").append(monthlyDebt).append("&");
        if (taxRate != null) url.append("tax_rate=").append(taxRate);
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
     * Detailed (legacy) check-rates endpoint matching example params
     * GET /mortgage/check-rates?creditScore=...&points=...&loanPurpose=...&loanTypes=...&loanPercent=...&propertyPrice=...&zip=...
     */
    public JsonNode checkRatesDetailed(String creditScore, String points, String loanPurpose,
                                       String loanTypes, Double loanPercent, Double propertyPrice,
                                       String zip) throws Exception {
        StringBuilder url = new StringBuilder("https://" + config.getHost() + "/mortgage/check-rates?");
        if (creditScore != null && !creditScore.isBlank()) url.append("creditScore=").append(creditScore).append("&");
        if (points != null && !points.isBlank()) url.append("points=").append(points).append("&");
        if (loanPurpose != null && !loanPurpose.isBlank()) url.append("loanPurpose=").append(loanPurpose).append("&");
        if (loanTypes != null && !loanTypes.isBlank()) url.append("loanTypes=").append(loanTypes).append("&");
        if (loanPercent != null) url.append("loanPercent=").append(loanPercent).append("&");
        if (propertyPrice != null) url.append("propertyPrice=").append(propertyPrice).append("&");
        if (zip != null && !zip.isBlank()) url.append("zip=").append(zip);
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

    /**
     * Alternate legacy mortgage calculation using different param names (hoi, tax_rate, downpayment)
     * GET /mortgage/calculate?hoi=..&tax_rate=..&downpayment=..&price=..&term=..&rate=..
     */
    public JsonNode calculateMortgageAlt(Double price, Double downpayment, Double rate, Integer term,
                                         Double hoi, Double taxRate) throws Exception {
        StringBuilder url = new StringBuilder("https://" + config.getHost() + "/mortgage/calculate?");
        if (hoi != null) url.append("hoi=").append(hoi).append("&");
        if (taxRate != null) url.append("tax_rate=").append(taxRate).append("&");
        if (downpayment != null) url.append("downpayment=").append(downpayment).append("&");
        if (price != null) url.append("price=").append(price).append("&");
        if (term != null) url.append("term=").append(term).append("&");
        if (rate != null) url.append("rate=").append(rate);
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
