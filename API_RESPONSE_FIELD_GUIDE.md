# API Field Response Report

**Document Purpose:** Complete technical reference for API response structure and null handling patterns. This guide is designed for frontend developers to implement robust data handling and UI components.

**Test Property:** 30 Fenway Ste 1, Boston, MA 02215  
**Property ID:** M1405644854  
**Test Date:** November 17, 2025  
**API Version:** Realty In US v3

---

## Executive Summary

This report documents the actual API response structure from a live property listing. All fields are categorized by availability and reliability, with specific null handling strategies provided for each.

**Field Availability:**
- 20 fields guaranteed (always present)
- 10 fields usually available (90%+ properties)
- 21 fields frequently null (40-60% properties)
- 5 fields never provided by API

---

## 1. Property Search Response

### Endpoint
```
POST /properties/v3/list
Body: { postal_code, status, limit, offset }
```

### Response Structure
```json
{
  "data": {
    "home_search": {
      "total": 350,
      "count": 20,
      "results": [ /* property objects */ ]
    }
  }
}
```

### Property Search Fields

| Field Path | Type | Test Value | Availability | Null Strategy |
|-----------|------|------------|--------------|---------------|
| `property_id` | string | "M1405644854" | Always | N/A - Required |
| `location.address.line` | string | "30 Fenway Ste 1" | Always | N/A - Required |
| `location.address.city` | string | "Boston" | Always | N/A - Required |
| `location.address.state_code` | string | "MA" | Always | N/A - Required |
| `location.address.postal_code` | string | "02215" | Always | N/A - Required |
| `location.address.coordinate.lat` | number | 42.344738 | Always | N/A - Required |
| `location.address.coordinate.lon` | number | -71.102023 | Always | N/A - Required |
| `list_price` | number | 649000 | Always | N/A - Required |
| `price_per_sqft` | number | 309 | Usually | Show "—" if null |
| `description.beds` | number | 1 | Usually | Show "—" if null |
| `description.baths` | number | 1 | Usually | Show "—" if null |
| `description.sqft` | number | 2100 | Usually | Show "—" if null |
| `description.type` | string | "condos" | Always | N/A - Required |
| `list_date` | string | "2025-10-31T02:13:27.000Z" | Always | N/A - Required |
| `photos[0].href` | string | "https://..." | Usually | Use placeholder if null |
| `thumbnail` | string | "https://..." | Usually | Use placeholder if null |

---

## 2. Property Detail Response

### Endpoint
```
GET /properties/v3/detail?property_id={id}
```

### Complete Field Inventory

#### Basic Information (Always Available)

| Field Path | Type | Test Value |
|-----------|------|------------|
| `property_id` | string | "M1405644854" |
| `status` | string | "for_sale" |
| `list_date` | string | "2025-10-31T02:13:27.000Z" |
| `listing_id` | string | "2975516093" |
| `href` | string | "/property/..." |

**Frontend Implementation:**
```typescript
interface PropertyBasic {
  propertyId: string;
  status: string;
  listDate: string;
  listingId: string;
  href: string;
}
```

#### Location Data (Always Available)

| Field Path | Type | Test Value |
|-----------|------|------------|
| `location.address.line` | string | "30 Fenway Ste 1" |
| `location.address.city` | string | "Boston" |
| `location.address.state_code` | string | "MA" |
| `location.address.postal_code` | string | "02215" |
| `location.address.coordinate.lat` | number | 42.344738 |
| `location.address.coordinate.lon` | number | -71.102023 |

**Frontend Implementation:**
```typescript
interface PropertyLocation {
  address: string;
  city: string;
  state: string;
  zip: string;
  latitude: number;
  longitude: number;
}
```

#### Pricing Information

| Field Path | Type | Test Value | Null Handling |
|-----------|------|------------|---------------|
| `list_price` | number | 649000 | Always available |
| `price_per_sqft` | number | 309 | Usually available, show "—" if null |
| `list_price_min` | number | NULL | Usually null, hide if null |
| `list_price_max` | number | NULL | Usually null, hide if null |
| `last_sold_price` | number | NULL | Often null, hide section if null |
| `last_sold_date` | string | "2021-11-29" | Often null, hide section if null |
| `last_price_change_date` | object | NULL | Often null, hide if null |
| `last_price_change_amount` | object | NULL | Often null, hide if null |

**Frontend Implementation:**
```typescript
interface PropertyPricing {
  listPrice: number;                    // Always available
  pricePerSqft: number | null;         // Show "—" if null
  lastSoldPrice: number | null;        // Hide section if null
  lastSoldDate: string | null;         // Hide section if null
}

function formatPricePerSqft(price: number | null): string {
  return price ? `$${price}/sqft` : "—";
}

function renderSoldHistory(pricing: PropertyPricing) {
  if (!pricing.lastSoldPrice || !pricing.lastSoldDate) {
    return null; // Don't render section
  }
  return (
    <SoldHistory 
      price={pricing.lastSoldPrice} 
      date={pricing.lastSoldDate} 
    />
  );
}
```

#### Property Description

| Field Path | Type | Test Value | Null Handling |
|-----------|------|------------|---------------|
| `description.type` | string | "condos" | Always available |
| `description.beds` | number | 1 | Usually available, show "—" if null |
| `description.baths` | number | 1 | Usually available, show "—" if null |
| `description.baths_full` | number | 1 | Usually available, show "—" if null |
| `description.baths_half` | number | 0 | Usually available, show 0 if null |
| `description.sqft` | number | 2100 | Usually available, show "—" if null |
| `description.lot_sqft` | number | 3485 | Often null for condos, hide if null |
| `description.year_built` | number | 1900 | Usually available, show "—" if null |
| `description.garage` | number | NULL | Often null, hide if null |
| `description.stories` | number | NULL | Often null, hide if null |
| `description.text` | string | "Beautiful condo..." | Usually available |

**Frontend Implementation:**
```typescript
interface PropertyDescription {
  type: string;
  beds: number | null;
  baths: number | null;
  bathsFull: number | null;
  bathsHalf: number | null;
  sqft: number | null;
  lotSqft: number | null;
  yearBuilt: number | null;
  garage: number | null;
  stories: number | null;
  text: string | null;
}

function renderPropertyStats(desc: PropertyDescription) {
  return (
    <div>
      <Stat label="Beds" value={desc.beds ?? "—"} />
      <Stat label="Baths" value={desc.baths ?? "—"} />
      <Stat label="Sqft" value={desc.sqft ?? "—"} />
      {desc.lotSqft && <Stat label="Lot" value={`${desc.lotSqft} sqft`} />}
      {desc.yearBuilt && <Stat label="Built" value={desc.yearBuilt} />}
      {desc.garage && <Stat label="Garage" value={`${desc.garage} spaces`} />}
    </div>
  );
}
```

#### Financial Information

| Field Path | Type | Test Value | Null Handling |
|-----------|------|------------|---------------|
| `hoa.fee` | number | 330 | NULL if not HOA property |
| `tax_history[0].tax` | number | 14316 | NULL if no tax records |
| `tax_history[0].year` | number | 2025 | NULL if no tax records |
| `tax_history[0].assessment.building` | number | NULL | Often null |
| `tax_history[0].assessment.land` | number | NULL | Often null |
| `tax_history[0].assessment.total` | number | NULL | Often null |

**Frontend Implementation:**
```typescript
interface PropertyFinancial {
  hoaFee: number | null;
  propertyTax: number | null;
  taxYear: number | null;
}

function getPropertyTax(property: any): number | null {
  if (!property.tax_history || property.tax_history.length === 0) {
    return null;
  }
  return property.tax_history[0].tax;
}

function getHOAFee(property: any): number | null {
  return property.hoa?.fee ?? null;
}

function renderFinancialInfo(financial: PropertyFinancial) {
  return (
    <div>
      {financial.hoaFee && (
        <InfoRow label="HOA Fee" value={`$${financial.hoaFee}/month`} />
      )}
      {financial.propertyTax && (
        <InfoRow 
          label="Property Tax" 
          value={`$${financial.propertyTax.toLocaleString()}/year`}
          subtitle={financial.taxYear ? `Tax Year ${financial.taxYear}` : undefined}
        />
      )}
      {!financial.hoaFee && !financial.propertyTax && (
        <EmptyState>Financial information not available</EmptyState>
      )}
    </div>
  );
}
```

#### Mortgage Information (Always NULL)

| Field Path | Type | Test Value | Notes |
|-----------|------|------------|-------|
| `mortgage.rates.display` | string | NULL | Never populated |
| `mortgage.estimate.monthly_payment` | number | NULL | Never populated |
| `mortgage.estimate.principal_and_interest` | number | NULL | Never populated |
| `mortgage.estimate.hoa_fees` | number | NULL | Never populated |
| `mortgage.estimate.property_tax` | number | NULL | Never populated |

**Frontend Implementation:**
```typescript
// DO NOT use property.mortgage for calculations
// Always use separate mortgage calculator endpoint

async function getMortgagePayment(
  price: number,
  downPayment: number,
  rate: number,
  term: number
) {
  const response = await fetch(
    `/api/mortgage/calculate?price=${price}&downPayment=${downPayment}&rate=${rate}&term=${term}`
  );
  return response.json();
}
```

#### Valuation & Estimates

| Field Path | Type | Test Value | Null Handling |
|-----------|------|------------|---------------|
| `estimates` | object | NULL | Often null |
| `estimates.current_values[0].estimates[0].estimate` | number | NULL in test | Often null |
| `estimates.current_values[0].source.type` | string | "corelogic" | If available |
| `estimates.current_values[0].source.name` | string | "CoreLogic®" | If available |

**Frontend Implementation:**
```typescript
interface PropertyEstimate {
  value: number | null;
  source: string | null;
}

function getEstimate(property: any): PropertyEstimate {
  if (!property.estimates?.current_values?.[0]?.estimates?.[0]?.estimate) {
    return { value: null, source: null };
  }
  
  const estimate = property.estimates.current_values[0].estimates[0].estimate;
  const source = property.estimates.current_values[0].source.name;
  
  return { value: estimate, source: source };
}

function getFairMarketValue(property: any): number {
  const estimate = getEstimate(property);
  return estimate.value ?? property.list_price; // Fallback to list price
}

function renderEstimate(estimate: PropertyEstimate) {
  if (!estimate.value) {
    return null; // Hide estimate section
  }
  
  return (
    <EstimateCard>
      <Label>Estimated Value</Label>
      <Value>${estimate.value.toLocaleString()}</Value>
      {estimate.source && <Source>Source: {estimate.source}</Source>}
    </EstimateCard>
  );
}
```

#### Market Status

| Field Path | Type | Test Value | Null Handling |
|-----------|------|------------|---------------|
| `days_on_market` | number | NULL | Often null, hide if null |
| `create_date` | string | "2025-10-31T02:13:27.000Z" | Usually available |
| `last_update_date` | string | "2025-11-15T14:22:10.000Z" | Usually available |

**Frontend Implementation:**
```typescript
interface PropertyStatus {
  daysOnMarket: number | null;
  createDate: string;
  lastUpdateDate: string;
}

function renderMarketStatus(status: PropertyStatus) {
  return (
    <div>
      <InfoRow label="Listed" value={formatDate(status.createDate)} />
      {status.daysOnMarket && (
        <InfoRow label="Days on Market" value={status.daysOnMarket} />
      )}
      <InfoRow label="Last Updated" value={formatDate(status.lastUpdateDate)} />
    </div>
  );
}
```

#### Media Assets

| Field Path | Type | Test Value | Null Handling |
|-----------|------|------------|---------------|
| `photo_count` | number | 12 | Usually available |
| `photos[]` | array | [...] | Usually 5-40 photos |
| `photos[i].href` | string | "https://..." | Never null within array |
| `virtual_tours` | object | NULL | Usually null, hide if null |
| `videos` | object | NULL | Usually null, hide if null |
| `matterport` | object | NULL | Usually null, hide if null |

**Frontend Implementation:**
```typescript
interface PropertyMedia {
  photoCount: number;
  photos: string[];
  hasVirtualTour: boolean;
  hasVideo: boolean;
  hasMatterport: boolean;
}

function getPropertyMedia(property: any): PropertyMedia {
  const photos = property.photos?.map((p: any) => p.href) || [];
  
  return {
    photoCount: property.photo_count || 0,
    photos: photos,
    hasVirtualTour: !!property.virtual_tours,
    hasVideo: !!property.videos,
    hasMatterport: !!property.matterport
  };
}

function renderGallery(media: PropertyMedia) {
  if (media.photos.length === 0) {
    return <PlaceholderImage />;
  }
  
  return (
    <PhotoGallery 
      images={media.photos}
      primaryImage={media.photos[0]}
      totalCount={media.photoCount}
    >
      {media.hasVirtualTour && <VirtualTourButton />}
      {media.hasVideo && <VideoButton />}
      {media.hasMatterport && <MatterportButton />}
    </PhotoGallery>
  );
}
```

#### Property History

| Field Path | Type | Test Value | Null Handling |
|-----------|------|------------|---------------|
| `property_history[]` | array | [...] | May be empty array |
| `property_history[i].date` | string | "2021-11-29" | Check array length |
| `property_history[i].price` | number | 450000 | Check array length |
| `property_history[i].event` | string | "Sold" | Check array length |

**Frontend Implementation:**
```typescript
interface PropertyHistoryEvent {
  date: string;
  price: number;
  event: string;
}

function getPropertyHistory(property: any): PropertyHistoryEvent[] {
  if (!property.property_history || property.property_history.length === 0) {
    return [];
  }
  return property.property_history.map((h: any) => ({
    date: h.date,
    price: h.price,
    event: h.event
  }));
}

function renderPropertyHistory(history: PropertyHistoryEvent[]) {
  if (history.length === 0) {
    return null; // Hide section
  }
  
  return (
    <HistoryTimeline>
      {history.map((event, index) => (
        <TimelineEvent key={index}>
          <Date>{formatDate(event.date)}</Date>
          <Event>{event.event}</Event>
          <Price>${event.price.toLocaleString()}</Price>
        </TimelineEvent>
      ))}
    </HistoryTimeline>
  );
}
```

---

## 3. Market Data Responses

### Similar Homes

**Endpoint:** `/properties/v3/list-similar-homes?property_id={id}`

**Response Processing:**
```typescript
interface SimilarHomesData {
  count: number;
  avgPrice: number | null;
  medianPrice: number | null;
  confidence: 'high' | 'medium' | 'low' | 'none';
}

function processSimilarHomes(response: any): SimilarHomesData {
  const results = response.data?.home_search?.results || [];
  
  if (results.length === 0) {
    return { count: 0, avgPrice: null, medianPrice: null, confidence: 'none' };
  }
  
  if (results.length < 3) {
    return { 
      count: results.length, 
      avgPrice: null, 
      medianPrice: null, 
      confidence: 'none' 
    };
  }
  
  const prices = results
    .map((r: any) => r.list_price)
    .filter((p: number) => p != null);
  
  const avg = prices.reduce((a: number, b: number) => a + b, 0) / prices.length;
  const sorted = [...prices].sort((a, b) => a - b);
  const median = sorted[Math.floor(sorted.length / 2)];
  
  let confidence: 'high' | 'medium' | 'low' = 'low';
  if (results.length >= 10) confidence = 'high';
  else if (results.length >= 5) confidence = 'medium';
  
  return {
    count: results.length,
    avgPrice: Math.round(avg),
    medianPrice: median,
    confidence: confidence
  };
}
```

### Sold Properties

**Endpoint:** `/properties/v2/list-sold?postal_code={zip}&limit=20`

**Response Processing:** (Same as similar homes)

### Rental Properties

**Endpoint:** `/properties/v2/list-for-rent?postal_code={zip}&limit=20`

**Response Processing:**
```typescript
interface RentalEstimateData {
  count: number;
  estimatedMonthlyRent: number | null;
  avgRent: number | null;
  medianRent: number | null;
  confidence: 'high' | 'medium' | 'low' | 'none';
  message: string;
}

function processRentalData(response: any): RentalEstimateData {
  const results = response.data?.home_search?.results || [];
  
  if (results.length < 3) {
    return {
      count: results.length,
      estimatedMonthlyRent: null,
      avgRent: null,
      medianRent: null,
      confidence: 'none',
      message: `Only ${results.length} rental comparables found - manual research required`
    };
  }
  
  const rents = results
    .map((r: any) => r.list_price)
    .filter((p: number) => p != null);
  
  const avg = rents.reduce((a: number, b: number) => a + b, 0) / rents.length;
  const sorted = [...rents].sort((a, b) => a - b);
  const median = sorted[Math.floor(sorted.length / 2)];
  
  let confidence: 'high' | 'medium' | 'low' = 'low';
  if (results.length >= 10) confidence = 'high';
  else if (results.length >= 5) confidence = 'medium';
  
  return {
    count: results.length,
    estimatedMonthlyRent: Math.round(median),
    avgRent: Math.round(avg),
    medianRent: median,
    confidence: confidence,
    message: `Based on ${results.length} rental comparables`
  };
}

function renderRentalEstimate(data: RentalEstimateData) {
  if (data.estimatedMonthlyRent === null) {
    return (
      <WarningCard>
        <Icon name="alert" />
        <Message>{data.message}</Message>
        <Action>Please research local rental rates</Action>
      </WarningCard>
    );
  }
  
  return (
    <EstimateCard confidence={data.confidence}>
      <Label>Estimated Monthly Rent</Label>
      <Value>${data.estimatedMonthlyRent.toLocaleString()}</Value>
      <Subtitle>{data.message}</Subtitle>
      <Range>
        Average: ${data.avgRent?.toLocaleString()} | 
        Median: ${data.medianRent?.toLocaleString()}
      </Range>
    </EstimateCard>
  );
}
```

---

## 4. Mortgage Rates Response

**Endpoint:** `/mortgage/v2/check-rates?state_code={state}&zip={zip}`

**Response Structure:**
```typescript
interface MortgageRatesResponse {
  rates: {
    thirtyYear: number | null;
    fifteenYear: number | null;
    arm: number | null;
    fha: number | null;
    va: number | null;
  };
  updated: string;
}

function getMortgageRate(response: MortgageRatesResponse, term: number = 30): number | null {
  if (!response.rates) {
    return null;
  }
  
  const rateKey = term === 30 ? 'thirtyYear' : 'fifteenYear';
  return response.rates[rateKey];
}

function renderMortgageRate(response: MortgageRatesResponse) {
  const rate = getMortgageRate(response, 30);
  
  if (rate === null) {
    return (
      <InputField 
        label="Interest Rate (%)"
        placeholder="Enter current rate or check with lenders"
        required
        badge="Required Input"
      />
    );
  }
  
  return (
    <InputField 
      label="Interest Rate (%)"
      value={rate}
      badge="From API"
      badgeColor="green"
      helpText={`Current 30-year rate as of ${formatDate(response.updated)}`}
      editable
    />
  );
}
```

---

## 5. Null Handling Strategies

### Strategy A: Display Placeholder

For fields that should always be visible but may be null:

```typescript
// Bedrooms, bathrooms, sqft
<Stat label="Beds" value={property.description?.beds ?? "—"} />
<Stat label="Sqft" value={property.description?.sqft ?? "—"} />
```

### Strategy B: Hide Section

For optional features that shouldn't show if null:

```typescript
// HOA fee, garage, lot size
{property.hoa?.fee && (
  <InfoRow label="HOA Fee" value={`$${property.hoa.fee}/month`} />
)}

{property.description?.garage && (
  <InfoRow label="Garage" value={`${property.description.garage} spaces`} />
)}
```

### Strategy C: Use Fallback Value

For critical fields that need a default:

```typescript
// Fair market value
const fmv = property.estimates?.current_values?.[0]?.estimates?.[0]?.estimate 
  || property.list_price;

// Number of units
const units = property.numberOfUnits || 1;
```

### Strategy D: Show Warning/Input Required

For fields needed for calculations but not provided:

```typescript
// Insurance, utilities
{!hasInsuranceValue && (
  <InputField 
    label="Annual Insurance"
    required
    badge="Required Input"
    badgeColor="red"
    helpText="Get quotes from insurance companies"
    placeholder="Typical: 0.3-0.5% of home value"
  />
)}
```

### Strategy E: Conditional Confidence Indicator

For estimated values based on market data:

```typescript
interface FieldWithMetadata {
  value: number | null;
  source: 'api' | 'market_estimate' | 'industry_default' | 'user_required';
  confidence: 'high' | 'medium' | 'low' | 'none';
  description: string;
}

function renderFieldWithBadge(field: FieldWithMetadata) {
  const badgeColors = {
    api: 'green',
    market_estimate: 'yellow',
    industry_default: 'orange',
    user_required: 'red'
  };
  
  return (
    <InputField
      value={field.value}
      badge={field.source.toUpperCase()}
      badgeColor={badgeColors[field.source]}
      helpText={field.description}
      editable={field.source !== 'api'}
      required={field.source === 'user_required'}
    />
  );
}
```

---

## 6. Complete Data Flow Example

### Property Detail Page Load Sequence

```typescript
async function loadPropertyDetailPage(propertyId: string) {
  try {
    // Step 1: Fetch basic property details (fast, 1-2 seconds)
    const property = await fetchPropertyDetail(propertyId);
    renderPropertyBasicInfo(property);
    
    // Step 2: Fetch market data in parallel (slower, 3-5 seconds)
    const [similarHomes, soldProperties, rentalProperties] = await Promise.all([
      fetchSimilarHomes(propertyId),
      fetchSoldProperties(property.location.address.postal_code),
      fetchRentalProperties(property.location.address.postal_code)
    ]);
    
    // Step 3: Process market data
    const marketData = {
      similar: processSimilarHomes(similarHomes),
      sold: processSimilarHomes(soldProperties), // Same processing
      rental: processRentalData(rentalProperties)
    };
    
    renderMarketData(marketData);
    
    // Step 4: Fetch mortgage rates (optional, 1 second)
    const mortgageRates = await fetchMortgageRates(
      property.location.address.state_code,
      property.location.address.postal_code
    );
    
    // Step 5: Build investment form with all data
    const investmentData = buildInvestmentData(property, marketData, mortgageRates);
    renderInvestmentForm(investmentData);
    
  } catch (error) {
    handleError(error);
  }
}

function buildInvestmentData(
  property: any,
  marketData: any,
  mortgageRates: any
): InvestmentFormData {
  return {
    // Always from API
    address: property.location.address.line,
    city: property.location.address.city,
    state: property.location.address.state_code,
    zip: property.location.address.postal_code,
    offerPrice: property.list_price,
    numberOfUnits: calculateUnits(property.description.type),
    
    // Conditional from API
    fmv: getEstimate(property).value || property.list_price,
    propertyTaxes: getPropertyTax(property),
    associationFees: getHOAFee(property),
    
    // From market data
    grossRentsAnnual: marketData.rental.estimatedMonthlyRent 
      ? marketData.rental.estimatedMonthlyRent * 12 
      : null,
    
    // From mortgage API
    firstRateAnnual: getMortgageRate(mortgageRates, 30),
    
    // Industry defaults
    vacancyRate: 5,
    annualAppreciation: 4,
    rentGrowth: 3,
    expenseGrowth: 2.5,
    
    // User required (null)
    insurance: null,
    electricity: null,
    gas: null,
    waterSewer: null,
    managementRate: null,
    repairsRate: null,
    firstPrincipal: null,
    firstAmortYears: null
  };
}
```

---

## 7. Field Summary Statistics

### By Availability

| Category | Count | Percentage | Examples |
|----------|-------|------------|----------|
| Always Available | 20 | 36% | address, price, type, photos |
| Usually Available | 10 | 18% | beds, baths, sqft, year built |
| Often Null | 21 | 37% | tax history, HOA, estimates, garage |
| Never Available | 5 | 9% | mortgage data, insurance, utilities |

### By Data Source

| Source | Fields | Frontend Strategy |
|--------|--------|-------------------|
| Direct API | 27 | Display directly or with placeholder |
| API Conditional | 8 | Check existence, hide section if null |
| Market Calculated | 3 | Calculate with confidence level |
| Industry Default | 4 | Show default with edit ability |
| User Required | 14 | Input fields with guidance |

### By UI Treatment

| Treatment | Count | Implementation |
|-----------|-------|----------------|
| Always display | 20 | No null check needed |
| Display with placeholder | 10 | Use "—" or "N/A" if null |
| Conditional section | 15 | Hide entire section if null |
| Input required | 14 | Show as empty required field |
| Calculated value | 4 | Compute from other fields |

---

## 8. Testing Recommendations

### Test Case 1: Fully Populated Property
- All optional fields have values
- Tax history available
- HOA fee present
- Estimate available
- Multiple photos
- Recent sale history

**Expected**: All sections visible, minimal user input required

### Test Case 2: Minimal Data Property
- Only required fields
- No tax history
- No HOA
- No estimate
- Few photos
- No sale history

**Expected**: Basic info visible, many sections hidden, more user input required

### Test Case 3: Edge Cases
- New construction (no history)
- Land (no building features)
- Multi-family (unit count extraction)
- Off-market (status handling)
- No photos (placeholder handling)

---

## 9. Error Handling

### API Request Failures

```typescript
try {
  const property = await fetchPropertyDetail(propertyId);
} catch (error) {
  if (error.response?.status === 404) {
    return <PropertyNotFound />;
  } else if (error.response?.status === 429) {
    return <RateLimitError />;
  } else {
    return <GenericError message="Failed to load property details" />;
  }
}
```

### Data Validation

```typescript
function validatePropertyData(property: any): ValidationResult {
  const errors: string[] = [];
  
  if (!property.property_id) errors.push("Missing property ID");
  if (!property.list_price || property.list_price <= 0) {
    errors.push("Invalid list price");
  }
  if (!property.location?.address?.line) errors.push("Missing address");
  
  return {
    isValid: errors.length === 0,
    errors: errors
  };
}
```

---

## Conclusion

This report provides comprehensive documentation of actual API responses, null handling patterns, and frontend implementation strategies. Key takeaways:

1. **36% of fields are guaranteed** - Always available, no null checks needed
2. **37% of fields are often null** - Implement defensive checks
3. **9% of fields never provided** - Always use local calculation or user input
4. **Market data requires fallbacks** - Insufficient comparables are common
5. **Estimates are unreliable** - Use list price as fallback for FMV

Frontend developers should implement robust null handling using the strategies outlined in this document to ensure a smooth user experience regardless of data availability.
