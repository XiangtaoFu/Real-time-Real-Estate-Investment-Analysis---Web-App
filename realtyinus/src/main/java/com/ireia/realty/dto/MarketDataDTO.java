package com.ireia.realty.dto;

import java.util.List;

public class MarketDataDTO {
    private Double averageSoldPrice;
    private Double medianSoldPrice;
    private Integer soldPropertiesCount;
    private Double averageRentPrice;
    private Double medianRentPrice;
    private Integer rentPropertiesCount;
    private List<ComparablePropertyDTO> similarHomes;
    private Double estimatedMonthlyRent;
    private String rentDataSource;
    
    // New fields for real-time market data
    private Double currentMortgageRate;
    private Integer rentalCompsCount;
    private Double averageHoa;
    private Double propertyTaxRate;

    public Double getAverageSoldPrice() { return averageSoldPrice; }
    public void setAverageSoldPrice(Double averageSoldPrice) { this.averageSoldPrice = averageSoldPrice; }
    
    public Double getMedianSoldPrice() { return medianSoldPrice; }
    public void setMedianSoldPrice(Double medianSoldPrice) { this.medianSoldPrice = medianSoldPrice; }
    
    public Integer getSoldPropertiesCount() { return soldPropertiesCount; }
    public void setSoldPropertiesCount(Integer soldPropertiesCount) { this.soldPropertiesCount = soldPropertiesCount; }
    
    public Double getAverageRentPrice() { return averageRentPrice; }
    public void setAverageRentPrice(Double averageRentPrice) { this.averageRentPrice = averageRentPrice; }
    
    public Double getMedianRentPrice() { return medianRentPrice; }
    public void setMedianRentPrice(Double medianRentPrice) { this.medianRentPrice = medianRentPrice; }
    
    public Integer getRentPropertiesCount() { return rentPropertiesCount; }
    public void setRentPropertiesCount(Integer rentPropertiesCount) { this.rentPropertiesCount = rentPropertiesCount; }
    
    public List<ComparablePropertyDTO> getSimilarHomes() { return similarHomes; }
    public void setSimilarHomes(List<ComparablePropertyDTO> similarHomes) { this.similarHomes = similarHomes; }
    
    public Double getEstimatedMonthlyRent() { return estimatedMonthlyRent; }
    public void setEstimatedMonthlyRent(Double estimatedMonthlyRent) { this.estimatedMonthlyRent = estimatedMonthlyRent; }
    
    public String getRentDataSource() { return rentDataSource; }
    public void setRentDataSource(String rentDataSource) { this.rentDataSource = rentDataSource; }
    
    public Double getCurrentMortgageRate() { return currentMortgageRate; }
    public void setCurrentMortgageRate(Double currentMortgageRate) { this.currentMortgageRate = currentMortgageRate; }
    
    public Integer getRentalCompsCount() { return rentalCompsCount; }
    public void setRentalCompsCount(Integer rentalCompsCount) { this.rentalCompsCount = rentalCompsCount; }
    
    public Double getAverageHoa() { return averageHoa; }
    public void setAverageHoa(Double averageHoa) { this.averageHoa = averageHoa; }
    
    public Double getPropertyTaxRate() { return propertyTaxRate; }
    public void setPropertyTaxRate(Double propertyTaxRate) { this.propertyTaxRate = propertyTaxRate; }
}
