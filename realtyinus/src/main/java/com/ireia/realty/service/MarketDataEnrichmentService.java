package com.ireia.realty.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ireia.realty.dto.MarketDataDTO;
import com.ireia.realty.dto.PropertyDetailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to enrich property data with real-time market data from various APIs
 */
@Service
public class MarketDataEnrichmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataEnrichmentService.class);
    
    @Autowired
    private RealtyInUSApiService apiService;
    
    /**
     * Fetch and calculate all market data for a property
     */
    public MarketDataDTO enrichWithMarketData(PropertyDetailDTO property) {
        MarketDataDTO marketData = new MarketDataDTO();
        
        try {
            // 1. Get current mortgage rates
            Double mortgageRate = fetchCurrentMortgageRate(
                property.getState(), 
                property.getPostalCode()
            );
            marketData.setCurrentMortgageRate(mortgageRate);
            logger.info("Fetched mortgage rate: {}%", mortgageRate);
            
        } catch (Exception e) {
            logger.warn("Failed to fetch mortgage rate, using default: {}", e.getMessage());
            marketData.setCurrentMortgageRate(6.5); // Fallback
        }
        
        try {
            // 2. Calculate estimated rent from rental comparables
            Double estimatedRent = calculateEstimatedRent(
                property.getCity(), 
                property.getState(),
                property.getPostalCode(),
                property.getBeds(),
                property.getBaths()
            );
            marketData.setEstimatedMonthlyRent(estimatedRent);
            marketData.setRentalCompsCount(8); // Update based on actual API response
            logger.info("Calculated estimated rent: ${}", estimatedRent);
            
        } catch (Exception e) {
            logger.warn("Failed to calculate rent estimate: {}", e.getMessage());
            marketData.setEstimatedMonthlyRent(null);
        }
        
        try {
            // 3. Get average HOA from similar homes
            Double avgHoa = calculateAverageHoa(
                property.getCity(),
                property.getState(),
                property.getPropertyType()
            );
            marketData.setAverageHoa(avgHoa);
            logger.info("Calculated average HOA: ${}", avgHoa);
            
        } catch (Exception e) {
            logger.warn("Failed to calculate average HOA: {}", e.getMessage());
            marketData.setAverageHoa(null);
        }
        
        try {
            // 4. Calculate property tax rate from sold properties
            Double taxRate = calculatePropertyTaxRate(
                property.getCity(),
                property.getState(),
                property.getPostalCode()
            );
            marketData.setPropertyTaxRate(taxRate);
            logger.info("Calculated property tax rate: {}%", taxRate);
            
        } catch (Exception e) {
            logger.warn("Failed to calculate tax rate: {}", e.getMessage());
            marketData.setPropertyTaxRate(null);
        }
        
        return marketData;
    }
    
    /**
     * Fetch current mortgage rate for the property location
     */
    private Double fetchCurrentMortgageRate(String state, String zipCode) throws Exception {
        JsonNode response = apiService.checkMortgageRates(state, zipCode);
        
        // Parse the mortgage rate from API response
        // API structure: {"data": {"loan_analysis": {"market": {"mortgage_data": {"average_rates": [...]}}}}}
        JsonNode averageRates = response.path("data")
                                        .path("loan_analysis")
                                        .path("market")
                                        .path("mortgage_data")
                                        .path("average_rates");
        
        if (averageRates.isArray() && averageRates.size() > 0) {
            // Get the 30-year fixed rate
            for (JsonNode rateNode : averageRates) {
                JsonNode loanType = rateNode.path("loan_type");
                String loanId = loanType.path("loan_id").asText("");
                
                if ("thirty_year_fix".equals(loanId)) {
                    double rate = rateNode.path("rate").asDouble();
                    return Math.round(rate * 10000.0) / 100.0; // Convert 0.06362 to 6.36%
                }
            }
            // If no 30-year fixed found, return the first rate
            double rate = averageRates.get(0).path("rate").asDouble();
            return Math.round(rate * 10000.0) / 100.0;
        }
        
        throw new Exception("No mortgage rates found in API response");
    }
    
    /**
     * Calculate estimated monthly rent based on rental comparables
     */
    private Double calculateEstimatedRent(String city, String state, String zipCode, 
                                         Integer beds, Integer baths) throws Exception {
        // Use postal code to get rental comparables
        JsonNode response = apiService.getForRentProperties(zipCode, 10);
        
        // Parse rental listings and calculate average
        JsonNode results = response.path("data").path("home_search").path("results");
        
        List<Double> rentals = new ArrayList<>();
        
        if (results.isArray()) {
            for (JsonNode rental : results) {
                // Filter by similar bed/bath count (within 1)
                JsonNode description = rental.path("description");
                int rentalBeds = description.path("beds").asInt(0);
                int rentalBaths = description.path("baths").asInt(0);
                
                if (Math.abs(rentalBeds - (beds != null ? beds : 0)) <= 1 &&
                    Math.abs(rentalBaths - (baths != null ? baths : 0)) <= 1) {
                    
                    double rent = rental.path("list_price").asDouble(0);
                    if (rent > 0) {
                        rentals.add(rent);
                    }
                }
            }
        }
        
        if (rentals.isEmpty()) {
            throw new Exception("No comparable rentals found");
        }
        
        // Calculate average rent
        double sum = rentals.stream().mapToDouble(Double::doubleValue).sum();
        return (double) Math.round(sum / rentals.size());
    }
    
    /**
     * Calculate average HOA fee from similar homes
     */
    private Double calculateAverageHoa(String city, String state, String propertyType) throws Exception {
        // Note: getSimilarHomes requires propertyId, so we'll search by city/state instead
        // This is a workaround - ideally we'd call with propertyId if we had it
        logger.warn("HOA calculation needs propertyId - returning default 0 for now");
        return 0.0; // Will need to enhance this with propertyId-based search
    }
    
    /**
     * Calculate property tax rate from recently sold properties
     */
    private Double calculatePropertyTaxRate(String city, String state, String zipCode) throws Exception {
        JsonNode response = apiService.getSoldProperties(zipCode, 10);
        
        JsonNode results = response.path("data").path("home_search").path("results");
        
        List<Double> taxRates = new ArrayList<>();
        
        if (results.isArray()) {
            for (JsonNode sold : results) {
                // Calculate tax rate: (annual_tax / sale_price) * 100
                double annualTax = sold.path("tax_history").path("tax").asDouble(0);
                double salePrice = sold.path("sold_price").asDouble(0);
                
                if (annualTax > 0 && salePrice > 0) {
                    double rate = (annualTax / salePrice) * 100;
                    taxRates.add(rate);
                }
            }
        }
        
        if (taxRates.isEmpty()) {
            throw new Exception("No tax rate data found in sold comparables");
        }
        
        // Calculate average tax rate
        double sum = taxRates.stream().mapToDouble(Double::doubleValue).sum();
        return Math.round((sum / taxRates.size()) * 100.0) / 100.0; // Round to 2 decimals
    }
}
