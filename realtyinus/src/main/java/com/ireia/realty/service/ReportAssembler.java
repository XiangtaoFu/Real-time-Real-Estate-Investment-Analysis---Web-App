package com.ireia.realty.service;

import com.ireia.realty.dto.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportAssembler {

    private final CashflowService cashflowService;
    private final DataCompletenessAnalyzer completenessAnalyzer;

    public ReportAssembler(CashflowService cashflowService, DataCompletenessAnalyzer completenessAnalyzer) {
        this.cashflowService = cashflowService;
        this.completenessAnalyzer = completenessAnalyzer;
    }

    public InvestmentReportResponseDTO generateReport(InvestmentReportRequestDTO req) {
        // 1) Build CashflowRequest from inputs
        CashflowRequest cf = toCashflowRequest(req);

        // 2) Run calculator
        CashflowResponse calc = cashflowService.analyze(cf);

        // 3) Assemble response JSON matching the sample structure (propertyInfo + analysis)
        InvestmentReportResponseDTO out = new InvestmentReportResponseDTO();
        out.generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Property info with sources
        out.propertyInfo = new InvestmentReportResponseDTO.PropertyInfoWithSources();
        if (req.apiData != null) {
            out.propertyInfo.propertyId = req.apiData.propertyId;
            out.propertyInfo.address = req.apiData.address;
            out.propertyInfo.fullAddress = String.format("%s, %s, %s %s",
                    nzs(req.apiData.address), nzs(req.apiData.city), nzs(req.apiData.state), nzs(req.apiData.zip)).trim();
            out.propertyInfo.zip = req.apiData.zip;
            out.propertyInfo.listPrice = req.apiData.listPrice;
            out.propertyInfo.fmv = req.apiData.estimate;
            out.propertyInfo.propertyType = req.apiData.propertyType;
            out.propertyInfo.beds = req.apiData.beds;
            out.propertyInfo.baths = req.apiData.baths;
            out.propertyInfo.sqft = req.apiData.sqft;
            out.propertyInfo.yearBuilt = req.apiData.yearBuilt;
            out.propertyInfo.status = req.apiData.status;
            out.propertyInfo.daysOnMarket = req.apiData.daysOnMarket;
            out.propertyInfo.listingDate = req.apiData.listingDate;
        }

        // Offer price from user input or fallback to listPrice
        Double offer = req.userInput != null && req.userInput.offerPrice != null
                ? req.userInput.offerPrice : (req.apiData != null ? req.apiData.listPrice : null);
        out.propertyInfo.offerPrice = offer;

        // Field sources map
        Map<String, InvestmentReportResponseDTO.DataSource> sources = new HashMap<>();
        putSource(sources, "listPrice", "api", "high", "Realty In US detail");
        putSource(sources, "fmv", req.apiData != null && req.apiData.estimate != null ? "api" : "market_estimate",
                req.apiData != null && req.apiData.estimate != null ? "medium" : "low", "AVM or market computation");
        putSource(sources, "offerPrice", req.userInput != null && req.userInput.offerPrice != null ? "user_input" : "api",
                req.userInput != null && req.userInput.offerPrice != null ? "high" : "medium", "User-confirmed or list price");
        out.propertyInfo.fieldSources = sources;

        // Choose monthly rent used (market vs user) for echo on propertyInfo
        Double chosenMonthlyRent = null;
        if (req.useMarketRent != null && req.useMarketRent && req.marketData != null) {
            chosenMonthlyRent = req.marketData.estimatedMonthlyRent;
        } else if (req.userInput != null) {
            chosenMonthlyRent = req.userInput.actualMonthlyRent;
        }
        out.propertyInfo.monthlyRent = chosenMonthlyRent;

        // Analysis mapping from calculator result
        out.analysis = new InvestmentReportResponseDTO.InvestmentAnalysis();
        out.analysis.yearOne = toYearOne(calc);
        out.analysis.projection = toProjection(calc);
        out.analysis.exit = toExit(calc, offer);
        out.analysis.irr = calc != null ? calc.irr : null;
        out.analysis.warnings = new ArrayList<>();
        out.analysis.assumptions = new ArrayList<>();

        // Echo the exact request used by the calculator
        out.cashflowRequest = cf;

        // Data quality
        out.dataQuality = completenessAnalyzer.analyzeCompleteness(req, calc);

        // Data sources breakdown (simple counts)
        out.dataSources = new InvestmentReportResponseDTO.DataSourceBreakdown();
        out.dataSources.apiProvidedCount = countApiProvided(req);
        out.dataSources.marketEstimatedCount = countMarketEstimated(req);
        out.dataSources.userInputCount = countUserProvided(req);
        out.dataSources.industryStandardCount = 3; // vacancy/management/repairs

        return out;
    }

    private CashflowRequest toCashflowRequest(InvestmentReportRequestDTO req) {
        CashflowRequest r = new CashflowRequest();
        if (req.apiData != null) {
            r.address = req.apiData.address; r.city = req.apiData.city;
            r.state = req.apiData.state; r.zip = req.apiData.zip;
            r.fmv = req.apiData.estimate;
        }
        // Offer price: user override -> list price
        Double offer = req.userInput != null && req.userInput.offerPrice != null
                ? req.userInput.offerPrice : (req.apiData != null ? req.apiData.listPrice : null);
        r.offerPrice = offer;

        // Income (annual)
        Double monthlyRent = (req.useMarketRent != null && req.useMarketRent && req.marketData != null)
                ? req.marketData.estimatedMonthlyRent : (req.userInput != null ? req.userInput.actualMonthlyRent : null);
        r.grossRentsAnnual = monthlyRent != null ? monthlyRent * 12 : null;
        r.numberOfUnits = 1; // simple inference; can be enhanced later
        r.parkingAnnual = nz(req.userInput != null ? req.userInput.parkingIncome : null);
        r.storageAnnual = nz(req.userInput != null ? req.userInput.storageIncome : null);
        r.otherIncomeAnnual = nz(req.userInput != null ? req.userInput.otherIncome : null);

        // Rates
        r.vacancyRate = nz(req.marketData != null ? req.marketData.recommendedVacancyRate : 0.05);
        r.managementRate = req.userInput != null && req.userInput.maintenance != null ? null : 0.08; // mgmt default 8%
        r.repairsRate = 0.05;

        // Expenses (annual)
        r.propertyTaxes = req.userInput != null && req.userInput.actualPropertyTax != null
                ? req.userInput.actualPropertyTax : (req.marketData != null ? req.marketData.estimatedPropertyTax : null);
        r.insurance = req.userInput != null && req.userInput.actualInsurance != null
                ? req.userInput.actualInsurance : (req.marketData != null ? req.marketData.estimatedInsurance : null);
        r.associationFees = req.userInput != null && req.userInput.actualHOA != null ? req.userInput.actualHOA : null;
        r.electricity = req.userInput != null ? req.userInput.utilities : null;
        r.gas = null; r.waterSewer = null; // user can split later

        // Financing
        Double rate = (req.useMarketRate != null && req.useMarketRate && req.marketData != null)
                ? req.marketData.currentMortgageRate : (req.userInput != null ? req.userInput.mortgageRate : null);
        r.firstRateAnnual = rate;
        Integer term = req.userInput != null ? req.userInput.loanTermYears : 30;
        r.firstAmortYears = term != null ? term : 30;
        r.firstInterestOnlyYears = req.userInput != null ? req.userInput.interestOnlyYears : null;

        Double dpPct = req.userInput != null ? req.userInput.downPaymentPercent : 0.2; // default 20%
        if (offer != null && dpPct != null) {
            r.firstPrincipal = offer * (1 - dpPct);
        }

        // Closing costs (optional)
        // Left null for now; could be populated from userInput includeClosingCosts flag

        // Growth and hold
        r.annualAppreciation = req.userInput != null && req.userInput.expectedAppreciation != null
                ? req.userInput.expectedAppreciation : 0.04;
        r.holdYears = req.userInput != null && req.userInput.holdYears != null ? req.userInput.holdYears : 10;
        r.rentGrowth = 0.03; r.expenseGrowth = 0.025; r.exitCostRate = 0.06;
        r.managementBase = "EGI";
        return r;
    }

    private InvestmentReportResponseDTO.YearOneMetrics toYearOne(CashflowResponse calc) {
        if (calc == null || calc.summary == null) return null;
        CashflowResponse.Summary s = calc.summary;
        InvestmentReportResponseDTO.YearOneMetrics y = new InvestmentReportResponseDTO.YearOneMetrics();
        y.purchasePrice = null; // could be set from request if needed in the response
        y.cashToClose = s.cashToClose; y.loanAmount = null; y.downPayment = null;
        y.grossIncome = s.totalIncomeY1; y.vacancyLoss = s.vacancyLossY1; y.effectiveGrossIncome = s.egiY1;
        y.operatingExpenses = s.totalExpensesY1; y.netOperatingIncome = s.noiY1;
        y.annualDebtService = s.annualDebtServiceY1; y.monthlyPayment = s.monthlyProfitY1 != null ? s.monthlyProfitY1 : null;
        y.dscr = s.dscrY1; y.monthlyCashflow = s.monthlyProfitY1; y.annualCashflow = s.monthlyProfitY1 != null ? s.monthlyProfitY1 * 12 : null;
        y.capRate = s.capRatePPY1; y.cashOnCashReturn = s.cashOnCashY1; y.grm = s.grmY1;
        // Expense to income ratio
        if (s.totalIncomeY1 != null && s.totalIncomeY1 > 0) {
            double ratio = s.totalExpensesY1 != null ? s.totalExpensesY1 / s.totalIncomeY1 : 0.0;
            y.expenseRatio = ratio; y.expenseToIncomeRatio = ratio;
        }
        y.loanToValue = s.ltvFMV; y.equityROI = s.equityROIY1; y.appreciationROI = s.appreciationROIY1; y.totalROI = s.totalROIY1;

        // Equity multiple = (Σ yearly cashflows + net sale) / cash to close
        if (calc.projection != null && !calc.projection.isEmpty() && s.cashToClose != null && s.cashToClose > 0) {
            double sumCF = 0.0; for (CashflowResponse.YearRow r : calc.projection) sumCF += (r.cashflow != null ? r.cashflow : 0.0);
            double netSale = calc.exitNetProceeds != null ? calc.exitNetProceeds : 0.0;
            y.equityMultiple = (sumCF + netSale) / s.cashToClose;
        }
        return y;
    }

    private List<InvestmentReportResponseDTO.YearlyProjection> toProjection(CashflowResponse calc) {
        List<InvestmentReportResponseDTO.YearlyProjection> res = new ArrayList<>();
        if (calc == null || calc.projection == null) return res;
        for (CashflowResponse.YearRow r : calc.projection) {
            InvestmentReportResponseDTO.YearlyProjection y = new InvestmentReportResponseDTO.YearlyProjection();
            y.year = r.year; y.totalIncome = r.totalIncome; y.vacancyLoss = r.vacancyLoss;
            y.management = r.management; y.repairsRateBased = r.repairsRateBased;
            y.operatingExpenses = r.totalExpenses; y.noi = r.noi; y.debtService = r.annualDebtService; y.cashflow = r.cashflow;
            y.principalPaydown = r.principalPaiddown; y.loanBalance = r.loanBalance; y.propertyValue = r.propertyValue;
            y.endingBalanceFirst = r.endingBalanceFirst; y.endingBalanceSecond = r.endingBalanceSecond;
            y.equity = (r.propertyValue != null && r.loanBalance != null) ? (r.propertyValue - r.loanBalance) : null;
            res.add(y);
        }
        double cumulative = 0;
        for (InvestmentReportResponseDTO.YearlyProjection y : res) {
            cumulative += nz(y.cashflow);
            y.cumulativeCashflow = cumulative;
        }
        return res;
    }

    private InvestmentReportResponseDTO.ExitAnalysis toExit(CashflowResponse calc, Double purchasePrice) {
        if (calc == null) return null;
        InvestmentReportResponseDTO.ExitAnalysis e = new InvestmentReportResponseDTO.ExitAnalysis();
        e.exitYear = calc.projection != null ? calc.projection.size() : null;
        if (calc.projection != null && !calc.projection.isEmpty()) {
            CashflowResponse.YearRow last = calc.projection.get(calc.projection.size() - 1);
            e.propertyValue = last.propertyValue; e.loanBalance = last.loanBalance;
        }
        e.netProceeds = calc.exitNetProceeds; e.sellingCosts = null; e.totalReturn = null; e.totalCashInvested = null; e.totalProfit = null;
        e.totalROI = null;
        return e;
    }

    private void putSource(Map<String, InvestmentReportResponseDTO.DataSource> map,
                           String field, String src, String conf, String details) {
        InvestmentReportResponseDTO.DataSource ds = new InvestmentReportResponseDTO.DataSource();
        ds.source = src; ds.confidence = conf; ds.details = details;
        map.put(field, ds);
    }

    private int countApiProvided(InvestmentReportRequestDTO req) {
        int c = 0; if (req.apiData == null) return 0;
        if (req.apiData.propertyId != null) c++; if (req.apiData.address != null) c++;
        if (req.apiData.city != null) c++; if (req.apiData.state != null) c++; if (req.apiData.zip != null) c++;
        if (req.apiData.listPrice != null) c++; if (req.apiData.estimate != null) c++;
        if (req.apiData.beds != null) c++; if (req.apiData.baths != null) c++; if (req.apiData.sqft != null) c++;
        if (req.apiData.yearBuilt != null) c++; if (req.apiData.propertyType != null) c++;
        if (req.apiData.lastSoldPrice != null) c++; if (req.apiData.lastSoldDate != null) c++;
        return c;
    }

    private int countMarketEstimated(InvestmentReportRequestDTO req) {
        int c = 0; if (req.marketData == null) return 0;
        if (req.marketData.estimatedMonthlyRent != null) c++; if (req.marketData.medianRentPrice != null) c++;
        if (req.marketData.avgSoldPrice != null) c++; if (req.marketData.currentMortgageRate != null) c++;
        if (req.marketData.estimatedPropertyTax != null) c++; if (req.marketData.estimatedInsurance != null) c++;
        if (req.marketData.estimatedHOA != null) c++;
        return c;
    }

    private int countUserProvided(InvestmentReportRequestDTO req) {
        int c = 0; if (req.userInput == null) return 0;
        if (req.userInput.offerPrice != null) c++; if (req.userInput.downPaymentPercent != null) c++;
        if (req.userInput.actualMonthlyRent != null) c++; if (req.userInput.parkingIncome != null) c++;
        if (req.userInput.storageIncome != null) c++; if (req.userInput.otherIncome != null) c++;
        if (req.userInput.actualPropertyTax != null) c++; if (req.userInput.actualInsurance != null) c++;
        if (req.userInput.actualHOA != null) c++; if (req.userInput.utilities != null) c++;
        if (req.userInput.maintenance != null) c++; if (req.userInput.mortgageRate != null) c++;
        if (req.userInput.loanTermYears != null) c++; if (req.userInput.interestOnlyYears != null) c++;
        if (req.userInput.holdYears != null) c++; if (req.userInput.expectedAppreciation != null) c++;
        return c;
    }

    private Double nz(Double v) { return v == null ? 0.0 : v; }
    private String nzs(String s) { return s == null ? "" : s; }
}
