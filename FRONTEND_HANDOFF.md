# Frontend Integration Guide 

Base URL: http://localhost:8081

CORS: Enabled

Last updated: 2025-11-18

## 1. Workflow Overview

- Search: User searches by ZIP/city/state/status; backend uses Realty In US v3/list.
- Detail: User selects a listing by property_id; backend uses v3/detail (fallback v3/list).
- Similar homes (comps): Retrieved by property_id; backend uses v3/list-similar-homes (fallback v2; ZIP-based approximation if empty).
- Compute + recommendations:
  - Backend aggregates detail + market estimates (mortgage rate, rent, HOA/tax fallbacks) into investment-data.
  - Recommendations from comps: average/median + suggested offer range (IQR).
  - User can override/complete missing fields in UI, then submit to report API (JSON/PDF).
- Output: JSON report for UI + basic PDF (iText layout planned).

## 2. Endpoints

### 2.1 Search properties
- GET `/api/properties/search`
- Query params:
  - `postalCode?=02171`
  - `city?=Boston`
  - `stateCode?=MA`
  - `status?=for_sale|for_rent|sold` (default: `for_sale`)
  - `page?=1` (default 1) `limit?=20` (default 20)
- Use: list view & pagination.

### 2.2 Property detail
- GET `/api/properties/{propertyId}`
- Behavior: v3/detail primary; v3/list fallback.

### 2.3 Similar homes (comps)
- GET `/api/properties/{propertyId}/similar?limit=10`
- Behavior: v3 list-similar-homes (status=for_sale), fallback v2; if empty → ZIP approximation.
- Returns: ComparableProperty[] { propertyId, address, price, beds, baths, sqft, status, soldDate }.

### 2.4 Investment data (detail + market + recommendations)
- GET `/api/properties/{propertyId}/investment-data`
- Returns EnrichedPropertyDTO with:
  - propertyInfo: id, address, city, state, postalCode, listPrice, beds/baths/sqft, yearBuilt, status, last sold, coordinates, etc.
  - marketData:
    - estimatedMonthlyRent, currentMortgageRate, propertyTaxRate, averageHoa, rentalCompsCount
    - similarHomes[]
    - Recommendations: compsAveragePrice, compsMedianPrice, compsCount, suggestedOfferLow, suggestedOfferHigh
  - cashflow defaults + metadata; dataCompleteness for UI prompts.

### 2.5 Generate report (JSON)
- POST `/api/report/generate`
- Body: InvestmentReportRequestDTO (see section 3)
- Returns InvestmentReportResponseDTO (see section 4)

### 2.6 Generate report (PDF)
- POST `/api/report/generate/pdf`
- Current PDF is minimal; iText 8 layout planned (tables/format; filename: `detailed_property_{id}_{yyyyMMdd_HHmmss}.pdf`).

## 3. UI Fields to Collect/Toggle (Report Request)

Organize a Report Wizard with: System (read-only), Market (optional), User (editable).

### 3.1 System (read-only, from detail/investment-data)
- propertyInfo:
  - propertyId, address, city, state, postalCode(zip)
  - status, listPrice, estimatedValue(FMV), beds, baths, sqft, yearBuilt
  - daysOnMarket, listingDate (if available)
- Market highlights (read-only, with "Use" toggle):
  - estimatedMonthlyRent, currentMortgageRate
  - averageHoa, propertyTaxRate (may be 0 fallback)
  - Recommendations: compsAveragePrice, compsMedianPrice, suggestedOfferLow/High
  - similarHomes[] table for review

### 3.2 Market estimated (optional inputs in request)
- estimatedMonthlyRent
- currentMortgageRate
- estimatedPropertyTax, estimatedInsurance, estimatedHOA
- recommendedVacancyRate (0.05), recommendedManagementRate (0.08), recommendedRepairsRate (0.05)

### 3.3 User input (editable, highest priority)
- Purchase: offerPrice (default listPrice), downPaymentPercent (default 0.20)
- Income: actualMonthlyRent (required if not using market rent), parkingIncome, storageIncome, otherIncome
- Expenses(annual): actualPropertyTax, actualInsurance, actualHOA, utilities, maintenance
- Financing: mortgageRate (required if not using market rate), loanTermYears (default 30), interestOnlyYears (default 0)
- Strategy: holdYears (default 10), expectedAppreciation (default 0.04)
- Flags: useMarketRent, useMarketRate, includeClosingCosts

Validation:
- Money: >= 0; Percent: 0–1 (or show 0–100% and convert).
- useMarketRent=false → require actualMonthlyRent; useMarketRate=false → require mortgageRate.
- Localize currency/percent; ISO dates.

## 4. Report JSON Schema (Key Fields)

Top-level
- `generatedAt: string`
- `propertyInfo: object`
- `analysis: object`
- `cashflowRequest: object` (echo of calculator inputs)
- `dataQuality: object`
- `dataSources: object`

propertyInfo
- propertyId, address, fullAddress, zip, listPrice, offerPrice, fmv, propertyType,
- beds, baths, sqft, yearBuilt, photoUrl,
- status, monthlyRent, daysOnMarket, listingDate,
- fieldSources: map<string, { source, confidence, details }>

analysis.yearOne
- purchasePrice, cashToClose, loanAmount, downPayment,
- grossIncome, vacancyLoss, effectiveGrossIncome,
- operatingExpenses, netOperatingIncome,
- annualDebtService, monthlyPayment, dscr,
- monthlyCashflow, annualCashflow,
- capRate, cashOnCashReturn, grm,
- expenseRatio, expenseToIncomeRatio (new),
- loanToValue, equityROI, appreciationROI, totalROI,
- equityMultiple (new)

analysis.projection[]
- year, totalIncome, vacancyLoss (new),
- management (new), repairsRateBased (new), operatingExpenses, noi,
- debtService, cashflow, principalPaydown, loanBalance,
- endingBalanceFirst (new), endingBalanceSecond (new),
- propertyValue, equity, cumulativeCashflow

analysis.exit
- exitYear, propertyValue, loanBalance, equity, sellingCosts, netProceeds,
- totalReturn, totalCashInvested, totalProfit, totalROI

analysis.irr, warnings[], assumptions[]

cashflowRequest (echo)
- address, city, state, zip, fmv, offerPrice, annualAppreciation,
- grossRentsAnnual, numberOfUnits, parkingAnnual, storageAnnual, laundryVendingAnnual, otherIncomeAnnual,
- vacancyRate, managementRate, repairsRate,
- propertyTaxes, insurance, electricity, gas, waterSewer, cable, caretaking, advertising,
- associationFees, pest, security, trash, misc, commonAreaMaintenance, capitalImprovements, accounting, legal,
- badDebts, evictions, otherExpenses,
- firstPrincipal, firstRateAnnual, firstAmortYears, firstInterestOnlyYears,
- secondPrincipal, secondRateAnnual, secondAmortYears,
- otherMonthlyFinancingCosts,
- repairs, repairsContingency, lenderFee, brokerFee, environmentals, inspections, appraisals, transferTax, legalClose, otherClosingCosts,
- holdYears, rentGrowth, expenseGrowth, exitCostRate, managementBase

## 5. Sample Payloads

Generate report (use market rent/rate)
```json
{
  "apiData": {
    "propertyId": "3247828734",
    "address": "10 Seaport Dr Apt 2311",
    "city": "Quincy",
    "state": "MA",
    "zip": "02171",
    "listPrice": 639900,
    "estimate": 640000,
    "beds": 2,
    "baths": 2,
    "sqft": 1159,
    "yearBuilt": 2003,
    "propertyType": "condos",
    "status": "for_sale",
    "daysOnMarket": 12,
    "listingDate": "2025-10-28"
  },
  "marketData": {
    "estimatedMonthlyRent": 2800,
    "currentMortgageRate": 0.0637,
    "estimatedPropertyTax": 0,
    "estimatedInsurance": 0,
    "estimatedHOA": 0,
    "recommendedVacancyRate": 0.05,
    "recommendedManagementRate": 0.08,
    "recommendedRepairsRate": 0.05
  },
  "userInput": {
    "offerPrice": 639900,
    "downPaymentPercent": 0.20,
    "loanTermYears": 30,
    "interestOnlyYears": 0,
    "holdYears": 10,
    "expectedAppreciation": 0.04
  },
  "useMarketRent": true,
  "useMarketRate": true,
  "includeClosingCosts": false
}
```

Generate report (user overrides rent/rate)
```json
{
  "apiData": {
    "propertyId": "3247828734",
    "address": "10 Seaport Dr Apt 2311",
    "city": "Quincy",
    "state": "MA",
    "zip": "02171",
    "listPrice": 639900
  },
  "userInput": {
    "offerPrice": 615000,
    "downPaymentPercent": 0.25,
    "actualMonthlyRent": 3000,
    "mortgageRate": 0.062,
    "loanTermYears": 30,
    "holdYears": 10
  },
  "useMarketRent": false,
  "useMarketRate": false
}
```

## 6. PowerShell Test Commands (Windows)

Run these in Windows PowerShell; they pretty-print JSON.

Search (ZIP 02171)
```powershell
(Invoke-RestMethod -TimeoutSec 30 -Uri 'http://localhost:8081/api/properties/search?postalCode=02171&status=for_sale&limit=5' -Method GET) | ConvertTo-Json -Depth 6
```

Detail (by propertyId)
```powershell
(Invoke-RestMethod -TimeoutSec 30 -Uri 'http://localhost:8081/api/properties/3247828734' -Method GET) | ConvertTo-Json -Depth 6
```

Similar homes (top 5)
```powershell
(Invoke-RestMethod -TimeoutSec 30 -Uri 'http://localhost:8081/api/properties/3247828734/similar?limit=5' -Method GET) | ConvertTo-Json -Depth 6
```

Investment data (comps + recommendations)
```powershell
(Invoke-RestMethod -TimeoutSec 60 -Uri 'http://localhost:8081/api/properties/3247828734/investment-data' -Method GET) | ConvertTo-Json -Depth 7
```

Generate report (JSON)
```powershell
$body = @'
{
  "apiData": {
    "propertyId": "3247828734",
    "address": "10 Seaport Dr Apt 2311",
    "city": "Quincy",
    "state": "MA",
    "zip": "02171",
    "listPrice": 639900
  },
  "userInput": {
    "offerPrice": 639900,
    "downPaymentPercent": 0.20,
    "loanTermYears": 30,
    "interestOnlyYears": 0,
    "holdYears": 10
  },
  "useMarketRent": false,
  "useMarketRate": false
}
'@
(Invoke-RestMethod -TimeoutSec 60 -Uri 'http://localhost:8081/api/report/generate' -Method POST -ContentType 'application/json' -Body $body) | ConvertTo-Json -Depth 8
```

Generate report (PDF → file)
```powershell
$body = @'
{ "apiData": { "propertyId": "3247828734", "address": "10 Seaport Dr Apt 2311", "city": "Quincy", "state": "MA", "zip": "02171", "listPrice": 639900 }, "userInput": { "offerPrice": 639900, "downPaymentPercent": 0.20, "loanTermYears": 30, "holdYears": 10 }, "useMarketRent": false, "useMarketRate": false }
'@
Invoke-WebRequest -TimeoutSec 60 -Uri 'http://localhost:8081/api/report/generate/pdf' -Method POST -ContentType 'application/json' -Body $body -OutFile 'report.pdf'
```

## 7. Next steps (optional)

- PDF: iText 8 layout (tables/formatting; unified currency/%; filename pattern).
- UI: “Use market” toggles, recommendation band & comps table; display data completeness prompts.
- Tests: JSON snapshot for `/api/report/generate`; e2e: search → detail → investment‑data → report.
