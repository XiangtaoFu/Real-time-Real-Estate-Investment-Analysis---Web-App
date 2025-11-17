package com.ireia.realty.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ireia.realty.dto.*;
import com.ireia.realty.service.MarketDataEnrichmentService;
import com.ireia.realty.service.PropertyMappingService;
import com.ireia.realty.service.RealtyInUSApiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/properties")
@CrossOrigin(origins = "*")
public class PropertyController {
    
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
            JsonNode detailResponse = apiService.getPropertyDetail(propertyId);
            PropertyDetailDTO detail = mappingService.mapDetailResponse(detailResponse);
            
            if (!includeMarketData) {
                return mappingService.enrichPropertyData(detail);
            }
            
            // Use the new MarketDataEnrichmentService to fetch real-time market data
            MarketDataDTO marketData = marketDataService.enrichWithMarketData(detail);
            
            // Build enriched property with real market data
            EnrichedPropertyDTO enriched = mappingService.enrichPropertyDataWithMarket(detail, marketData);
            
            return enriched;
        } catch (Exception e) {
            // Log the detailed error
            System.err.println("ERROR in getInvestmentData: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get investment data: " + e.getMessage(), e);
        }
    }
}
