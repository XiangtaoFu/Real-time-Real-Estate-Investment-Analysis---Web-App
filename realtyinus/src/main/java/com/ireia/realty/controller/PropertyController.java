package com.ireia.realty.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ireia.realty.dto.*;
import com.ireia.realty.service.MarketDataEnrichmentService;
import com.ireia.realty.service.PropertyMappingService;
import com.ireia.realty.service.RealtyInUSApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/properties")
@CrossOrigin(origins = "*")
public class PropertyController {
    
    private static final Logger logger = LoggerFactory.getLogger(PropertyController.class);
    
    private final RealtyInUSApiService apiService;
    private final PropertyMappingService mappingService;
    private final MarketDataEnrichmentService marketDataService;
    
    public PropertyController(RealtyInUSApiService apiService, 
                             PropertyMappingService mappingService,
                             MarketDataEnrichmentService marketDataService) {
        this.apiService = apiService;
        this.mappingService = mappingService;
        this.marketDataService = marketDataService;
    }
    
    @GetMapping("/search")
    public PropertySearchResponse searchProperties(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String stateCode,
            @RequestParam(defaultValue = "for_sale") String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) throws Exception {
        
        String searchPostalCode = postalCode;
        String searchCity = city;
        String searchStateCode = stateCode;
        
        if (location != null && !location.isBlank()) {
            String[] parts = location.split(",");
            if (parts.length >= 2) {
                searchCity = parts[0].trim();
                searchStateCode = parts[1].trim();
            } else if (parts[0].trim().matches("\\d{5}")) {
                searchPostalCode = parts[0].trim();
            } else {
                searchCity = parts[0].trim();
            }
        }
        
        JsonNode apiResponse = apiService.searchProperties(
            searchPostalCode, searchCity, searchStateCode, status, page, limit
        );
        
        return mappingService.mapSearchResponse(apiResponse);
    }
    
    @GetMapping("/{propertyId}")
    public EnrichedPropertyDTO getPropertyDetail(@PathVariable String propertyId) throws Exception {
        JsonNode apiResponse = apiService.getPropertyDetail(propertyId);
        PropertyDetailDTO detail = mappingService.mapDetailResponse(apiResponse);
        return mappingService.enrichPropertyData(detail);
    }
    
    @GetMapping("/{propertyId}/investment-data")
    public EnrichedPropertyDTO getInvestmentData(
            @PathVariable String propertyId,
            @RequestParam(defaultValue = "true") Boolean includeMarketData,
            @RequestParam(defaultValue = "false") Boolean includeMortgageRates) throws Exception {
        
        try {
            logger.info("=== Investment Data Request Started ===");
            logger.info("Property ID: " + propertyId);
            logger.info("Include Market Data: " + includeMarketData);
            
            logger.info("Fetching property detail from API...");
            JsonNode detailResponse = apiService.getPropertyDetail(propertyId);
            logger.info("Property detail fetched successfully");
            
            logger.info("Mapping property detail...");
            PropertyDetailDTO detail = mappingService.mapDetailResponse(detailResponse);
            logger.info("Property detail mapped: " + detail.getAddress());
            
            if (!includeMarketData) {
                logger.info("Skipping market data enrichment");
                return mappingService.enrichPropertyData(detail);
            }
            
            // Use the new MarketDataEnrichmentService to fetch real-time market data
            logger.info("Enriching with market data...");
            MarketDataDTO marketData = marketDataService.enrichWithMarketData(detail);
            logger.info("Market data enriched successfully");
            
            // Build enriched property with real market data
            logger.info("Building enriched response...");
            EnrichedPropertyDTO enriched = mappingService.enrichPropertyDataWithMarket(detail, marketData);
            logger.info("=== Investment Data Request Completed ===");
            
            return enriched;
        } catch (Exception e) {
            // Log the detailed error
            logger.error("=== ERROR in getInvestmentData ===");
            logger.error("Exception class: " + e.getClass().getName());
            logger.error("Message: " + e.getMessage());
            logger.error("Stack trace:", e);
            throw new RuntimeException("Failed to get investment data: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{propertyId}/similar")
    public java.util.List<ComparablePropertyDTO> getSimilarHomes(
            @PathVariable String propertyId,
            @RequestParam(defaultValue = "5") Integer limit) throws Exception {
        try {
            logger.info("Fetching similar homes for property {} with limit {}", propertyId, limit);
            // Use enrichment service (includes propertyId-based comps + zip fallback)
            JsonNode detailResponse = apiService.getPropertyDetail(propertyId);
            PropertyDetailDTO detail = mappingService.mapDetailResponse(detailResponse);
            MarketDataDTO market = marketDataService.enrichWithMarketData(detail);
            java.util.List<ComparablePropertyDTO> comps = market != null ? market.getSimilarHomes() : java.util.Collections.emptyList();
            if (comps == null) return java.util.Collections.emptyList();
            if (limit != null && limit > 0 && comps.size() > limit) {
                return comps.subList(0, limit);
            }
            return comps;
        } catch (Exception e) {
            logger.error("Failed to fetch similar homes: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch similar homes: " + e.getMessage(), e);
        }
    }
}
