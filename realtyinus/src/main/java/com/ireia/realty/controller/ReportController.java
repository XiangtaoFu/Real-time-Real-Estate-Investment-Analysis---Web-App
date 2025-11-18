package com.ireia.realty.controller;

import com.ireia.realty.dto.InvestmentReportRequestDTO;
import com.ireia.realty.dto.InvestmentReportResponseDTO;
import com.ireia.realty.service.ReportAssembler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportAssembler reportAssembler;

    public ReportController(ReportAssembler reportAssembler) {
        this.reportAssembler = reportAssembler;
    }

    @PostMapping("/generate")
    public ResponseEntity<InvestmentReportResponseDTO> generateJson(@RequestBody InvestmentReportRequestDTO request) {
        InvestmentReportResponseDTO resp = reportAssembler.generateReport(request);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/generate/pdf")
    public ResponseEntity<byte[]> generatePdf(@RequestBody InvestmentReportRequestDTO request) {
        InvestmentReportResponseDTO resp = reportAssembler.generateReport(request);
        // Lightweight PDF using plain text; can be upgraded to iText if available
        String text = "Investment Report\n\n" +
                "Property: " + (resp.propertyInfo != null ? resp.propertyInfo.fullAddress : "") + "\n" +
                "Offer Price: " + (resp.propertyInfo != null ? resp.propertyInfo.offerPrice : "") + "\n" +
                "Year-1 NOI: " + (resp.analysis != null && resp.analysis.yearOne != null ? resp.analysis.yearOne.netOperatingIncome : "") + "\n" +
                "IRR: " + (resp.analysis != null ? resp.analysis.irr : "") + "\n";
        byte[] pdfBytes = SimplePdf.of(text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=investment-report.pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    // Minimalist PDF generator to avoid external deps; replace with iText for styling
    static class SimplePdf {
        static byte[] of(String text) {
            StringBuilder sb = new StringBuilder();
            sb.append("%PDF-1.4\n");
            String content = text.replace("(", "\\(").replace(")", "\\)");
            String stream = "BT /F1 12 Tf 72 720 Td (" + content + ") Tj ET";
            String xref;
            int pos1, pos2, pos3, pos4;

            sb.append("1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n");
            sb.append("2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n");
            sb.append("3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>endobj\n");
            pos1 = sb.length();
            sb.append("4 0 obj<< /Length ");
            String streamBody = "stream\n" + stream + "\nendstream\nendobj\n";
            pos2 = sb.length();
            sb.append("       ");
            pos3 = sb.length();
            sb.append(streamBody);
            sb.append("5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj\n");
            pos4 = sb.length();

            // Now build xref
            String pdfSoFar = sb.toString();
            int idx4 = pdfSoFar.indexOf("4 0 obj");
            int len = stream.length() + 2; // \n\n around stream
            // Rebuild with correct length
            String beforeLen = pdfSoFar.substring(0, pos2);
            String afterLen = pdfSoFar.substring(pos3);
            String rebuilt = beforeLen + String.valueOf(stream.length()) + afterLen;

            // Recompute xref offsets
            byte[] bytes = rebuilt.getBytes();
            String reb = new String(bytes);
            int off1 = reb.indexOf("1 0 obj");
            int off2 = reb.indexOf("2 0 obj");
            int off3 = reb.indexOf("3 0 obj");
            int off4 = reb.indexOf("4 0 obj");
            int off5 = reb.indexOf("5 0 obj");

            StringBuilder finalPdf = new StringBuilder(reb);
            int xrefPos = finalPdf.length();
            finalPdf.append("xref\n0 6\n");
            finalPdf.append(String.format("%010d %05d f \n", 0, 65535));
            finalPdf.append(String.format("%010d %05d n \n", off1, 0));
            finalPdf.append(String.format("%010d %05d n \n", off2, 0));
            finalPdf.append(String.format("%010d %05d n \n", off3, 0));
            finalPdf.append(String.format("%010d %05d n \n", off4, 0));
            finalPdf.append(String.format("%010d %05d n \n", off5, 0));
            finalPdf.append("trailer<< /Size 6 /Root 1 0 R >>\nstartxref\n");
            finalPdf.append(xrefPos).append("\n%%EOF");
            return finalPdf.toString().getBytes();
        }
    }
}
