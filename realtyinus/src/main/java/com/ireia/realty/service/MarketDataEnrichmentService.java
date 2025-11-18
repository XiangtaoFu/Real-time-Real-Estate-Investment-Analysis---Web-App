package com.ireia.realty.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ireia.realty.dto.ComparablePropertyDTO;
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
            marketData.setEstimatedMonthlyRent(0.0);
            marketData.setRentalCompsCount(0);
        }
        
        try {
            // 2b. Fetch similar homes by property_id for comps reference
            List<ComparablePropertyDTO> comps = fetchSimilarHomesByPropertyId(property.getPropertyId());
            // Fallback: approximate comps from zip if API returns empty
            if (comps == null || comps.isEmpty()) {
                comps = approximateCompsByZip(property);
                logger.info("Using fallback comps from zip: {} items", comps != null ? comps.size() : 0);
            }
            marketData.setSimilarHomes(comps);
            computeComparableStats(marketData, comps);
            logger.info("Fetched {} similar homes for comps", comps != null ? comps.size() : 0);
        } catch (Exception e) {
            logger.warn("Failed to fetch similar homes: {}", e.getMessage());
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
            marketData.setAverageHoa(0.0);
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
            marketData.setPropertyTaxRate(0.0);
        }
        
        return marketData;
    }

    private void computeComparableStats(MarketDataDTO marketData, List<ComparablePropertyDTO> comps) {
        if (comps == null) return;
        List<Double> prices = new ArrayList<>();
        for (ComparablePropertyDTO c : comps) {
            if (c.getPrice() != null && c.getPrice() > 0) prices.add(c.getPrice());
        }
        if (prices.isEmpty()) return;
        marketData.setCompsCount(prices.size());
        double avg = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        marketData.setCompsAveragePrice(avg);
        prices.sort(Double::compareTo);
        double median = prices.get(prices.size() / 2);
        marketData.setCompsMedianPrice(median);
        if (prices.size() >= 4) {
            int n = prices.size();
            int q1Idx = (int)Math.floor(0.25 * (n - 1));
            int q3Idx = (int)Math.ceil(0.75 * (n - 1));
            double q1 = prices.get(q1Idx);
            double q3 = prices.get(q3Idx);
            marketData.setSuggestedOfferLow(q1);
            marketData.setSuggestedOfferHigh(q3);
        } else {
            marketData.setSuggestedOfferLow(median * 0.98);
            marketData.setSuggestedOfferHigh(median * 1.02);
        }
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
     * Fetch similar homes using the property's ID via v2 list-similar-homes.
     */
    private List<ComparablePropertyDTO> fetchSimilarHomesByPropertyId(String propertyId) throws Exception {
        if (propertyId == null || propertyId.isBlank()) return new ArrayList<>();
        JsonNode resp = apiService.getSimilarHomes(propertyId);
        JsonNode results = resp.path("data").path("home_search").path("results");
        List<ComparablePropertyDTO> out = new ArrayList<>();
        if (results.isArray()) {
            for (JsonNode r : results) {
                ComparablePropertyDTO comp = new ComparablePropertyDTO();
                comp.setPropertyId(r.path("property_id").asText(null));
                JsonNode addr = r.path("location").path("address");
                comp.setAddress(addr.path("line").asText(null));
                if (r.path("list_price").isNumber()) comp.setPrice(r.path("list_price").asDouble());
                JsonNode desc = r.path("description");
                if (desc.path("beds").isNumber()) comp.setBeds(desc.path("beds").asInt());
                if (desc.path("baths").isNumber()) comp.setBaths(desc.path("baths").asInt());
                if (desc.path("sqft").isNumber()) comp.setSqft(desc.path("sqft").asInt());
                comp.setStatus(r.path("status").asText(null));
                comp.setSoldDate(r.path("last_sold_date").asText(null));
                out.add(comp);
            }
        }
        return out;
    }

    /**
     * Fallback comps: query for-sale in the same zip and pick properties with similar type and size.
     */
    private List<ComparablePropertyDTO> approximateCompsByZip(PropertyDetailDTO subject) throws Exception {
        if (subject.getPostalCode() == null || subject.getPostalCode().isBlank()) return new ArrayList<>();
        JsonNode resp = apiService.searchProperties(subject.getPostalCode(), null, null, "for_sale", 1, 30);
        JsonNode results = resp.path("data").path("home_search").path("results");
        List<ComparablePropertyDTO> comps = new ArrayList<>();
        if (results.isArray()) {
            for (JsonNode r : results) {
                JsonNode desc = r.path("description");
                String type = desc.path("type").asText(null);
                if (subject.getPropertyType() != null && type != null &&
                        !type.toLowerCase().contains(subject.getPropertyType().toLowerCase())) {
                    continue; // skip very different types
                }
                Integer beds = desc.path("beds").isNumber() ? desc.path("beds").asInt() : null;
                Integer baths = desc.path("baths").isNumber() ? desc.path("baths").asInt() : null;
                Integer sqft = desc.path("sqft").isNumber() ? desc.path("sqft").asInt() : null;
                if (subject.getBeds() != null && beds != null && Math.abs(beds - subject.getBeds()) > 1) continue;
                if (subject.getBaths() != null && baths != null && Math.abs(baths - subject.getBaths()) > 1) continue;
                if (subject.getSqft() != null && sqft != null) {
                    double ratio = sqft / (double) Math.max(subject.getSqft(), 1);
                    if (ratio < 0.7 || ratio > 1.3) continue; // within ~30%
                }
                ComparablePropertyDTO comp = new ComparablePropertyDTO();
                comp.setPropertyId(r.path("property_id").asText(null));
                JsonNode addr = r.path("location").path("address");
                comp.setAddress(addr.path("line").asText(null));
                if (r.path("list_price").isNumber()) comp.setPrice(r.path("list_price").asDouble());
                comp.setBeds(beds); comp.setBaths(baths); comp.setSqft(sqft);
                comp.setStatus(r.path("status").asText(null));
                comps.add(comp);
            }
        }
        // Sort by price proximity to subject list price
        if (subject.getListPrice() != null) {
            comps.sort((a,b) -> {
                double ap = a.getPrice() != null ? a.getPrice() : Double.MAX_VALUE;
                double bp = b.getPrice() != null ? b.getPrice() : Double.MAX_VALUE;
                return Double.compare(Math.abs(ap - subject.getListPrice()), Math.abs(bp - subject.getListPrice()));
            });
        }
        // Return top 8 as comps
        return comps.size() > 8 ? comps.subList(0, 8) : comps;
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
