# RealtyInUS API Service

Spring Boot service that integrates with Realty In US RapidAPI to provide property search and investment analysis data.

## Features

- Property search by location (postal code, city, state)
- Detailed property information retrieval
- Investment analysis data preparation
- Minimal default values (avoids hardcoded assumptions)
- Data completeness assessment

## Architecture

```
Client → PropertyController → RealtyInUSApiService → RapidAPI
                            ↓
                      PropertyMappingService → CashflowRequest
```

## API Endpoints

### 1. Search Properties

**Endpoint**: `GET /api/properties/search`

**Parameters**:
- `location` (optional): City, state or postal code (e.g., "Boston, MA" or "02215")
- `postalCode` (optional): Specific postal code
- `city` (optional): City name
- `stateCode` (optional): State code (e.g., "MA")
- `status` (default: "for_sale"): Property status
- `page` (default: 1): Page number
- `limit` (default: 20): Results per page

**Example**:
```bash
curl "http://localhost:8080/api/properties/search?postalCode=02215&limit=10"
```

**Response**:
```json
{
  "total": 35,
  "count": 10,
  "properties": [
    {
      "propertyId": "3325825129",
      "address": "30 Fenway Unit 5",
      "city": "Boston",
      "state": "Massachusetts",
      "postalCode": "02215",
      "listPrice": 849000,
      "beds": 0,
      "baths": 1,
      "sqft": 1476,
      "propertyType": "condos",
      "status": "for_sale",
      "photoUrl": "https://...",
      "estimate": 1191700,
      "listDate": "2025-11-06T19:44:21.000000Z"
    }
  ]
}
```

### 2. Get Property Details

**Endpoint**: `GET /api/properties/{propertyId}`

**Example**:
```bash
curl "http://localhost:8080/api/properties/3325825129"
```

**Response**: Returns `EnrichedPropertyDTO` with three sections:
- `propertyInfo`: Complete property details
- `cashflowDefaults`: Pre-filled CashflowRequest with minimal defaults
- `dataCompleteness`: Assessment of available data

### 3. Get Investment Data

**Endpoint**: `GET /api/properties/{propertyId}/investment-data`

Same as endpoint #2, provided for semantic clarity.

## Key Design Principles

### Minimal Default Values

The service follows a "no assumptions" approach:

**✅ Set from API**:
- Property address, city, state, zip
- List price → `offerPrice`
- Estimate → `fmv` (Fair Market Value)
- Number of units (inferred from property type)

**✅ Conservative Standards**:
- `vacancyRate`: 0.05 (5%)
- `annualAppreciation`: 0.04 (4%)
- `rentGrowth`: 0.03 (3%)
- `expenseGrowth`: 0.025 (2.5%)

**❌ Set to NULL** (user must provide):
- `grossRentsAnnual` (monthly rent)
- `propertyTaxes`
- `insurance`
- All utility expenses
- Financing details (`firstPrincipal`, `firstRateAnnual`)
- Closing costs

### Data Completeness Assessment

Each response includes a completeness assessment:

```json
{
  "dataCompleteness": {
    "hasEstimate": true,
    "hasRentEstimate": false,
    "hasTaxData": false,
    "hasHOAData": false,
    "completenessScore": 0.2,
    "missingFields": [
      "grossRentsAnnual (monthly rent)",
      "propertyTaxes",
      "insurance",
      "firstPrincipal (loan amount)",
      "firstRateAnnual (interest rate)"
    ],
    "recommendation": "API provides basic property info. User input required for financial calculations."
  }
}
```

## Setup

### Prerequisites

- Java 17 or higher
- Maven 3.6+ (or use included Maven wrapper)
- RapidAPI key for Realty In US

### Configuration

Edit `src/main/resources/application.yml`:

```yaml
realty:
  rapidapi:
    host: realty-in-us.p.rapidapi.com
    key: ${RAPIDAPI_KEY:your-key-here}
```

Or set environment variable:
```bash
set RAPIDAPI_KEY=your-key-here
```

### Build and Run

**Option 1: Using startup script**
```bash
start-realtyinus.bat
```

**Option 2: Maven wrapper**
```bash
cd realtyinus
mvnw.cmd spring-boot:run
```

**Option 3: System Maven**
```bash
cd realtyinus
mvn spring-boot:run
```

### Test

Run test script:
```bash
test-realtyinus-endpoints.bat
```

Or test manually:
```bash
# Search properties
curl "http://localhost:8080/api/properties/search?city=Boston&stateCode=MA"

# Get property details
curl "http://localhost:8080/api/properties/3325825129"
```

## Integration with Frontend

### React/TypeScript Example

```typescript
// Search properties
const searchProperties = async (location: string) => {
  const response = await fetch(
    `http://localhost:8080/api/properties/search?location=${encodeURIComponent(location)}`
  );
  return await response.json();
};

// Get investment data
const getInvestmentData = async (propertyId: string) => {
  const response = await fetch(
    `http://localhost:8080/api/properties/${propertyId}/investment-data`
  );
  const data = await response.json();
  
  // Use cashflowDefaults as initial form values
  const formData = {
    ...data.cashflowDefaults,
    // User can override any field
  };
  
  // Show data completeness warnings
  if (data.dataCompleteness.completenessScore < 0.5) {
    console.warn('Low data completeness:', data.dataCompleteness.missingFields);
  }
  
  return data;
};
```

## Project Structure

```
realtyinus/
├── src/main/java/com/ireia/realty/
│   ├── RealtyApplication.java           # Spring Boot entry point
│   ├── config/
│   │   └── RealtyApiConfig.java         # Configuration properties
│   ├── controller/
│   │   └── PropertyController.java      # REST endpoints
│   ├── service/
│   │   ├── RealtyInUSApiService.java    # RapidAPI client
│   │   └── PropertyMappingService.java  # Data mapping logic
│   └── dto/
│       ├── PropertySearchRequest.java
│       ├── PropertySearchResponse.java
│       ├── PropertySummaryDTO.java
│       ├── PropertyDetailDTO.java
│       ├── EnrichedPropertyDTO.java
│       ├── CashflowRequest.java
│       └── DataCompletenessDTO.java
└── src/main/resources/
    └── application.yml                  # Configuration
```

## Troubleshooting

### Port Already in Use

If port 8080 is already in use, edit `application.yml`:
```yaml
server:
  port: 8081
```

### API Rate Limits

RapidAPI has rate limits. If you exceed them:
- Check your subscription plan
- Implement caching
- Reduce concurrent requests

### Missing Data

Most properties from Realty In US API do not include:
- Rent estimates
- HOA fees
- Property tax amounts

This is expected. Users must provide these values.

## Next Steps

1. **Start the service**: Run `start-realtyinus.bat`
2. **Test endpoints**: Run `test-realtyinus-endpoints.bat`
3. **Integrate with frontend**: Update React components to call these APIs
4. **Add caching**: Implement Redis/in-memory cache for frequently accessed properties
5. **Error handling**: Add retry logic and better error messages

## License

Part of Real-Time Real Estate Investment Analysis Web App
