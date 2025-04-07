package org.example;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sitemapvalidation {

    public static void main(String[] args) {
        String sitemapUrl = "https://qa-ssr.msme.jswone.in/sitemap.xml"; // Replace with your sitemap URL
        List<String> urls = extractUrlsFromSitemap(sitemapUrl);

        System.out.println("🔍 Found " + urls.size() + " URLs. Checking...");

        List<UrlStatus> urlStatusList = checkUrls(urls);

        // Generate clean PDF report
        generatePDFReport(urlStatusList, urls.size());
    }

    // Extracts URLs from the sitemap
    public static List<String> extractUrlsFromSitemap(String sitemapUrl) {
        List<String> urls = new ArrayList<>();
        try {
            URL url = new URL(sitemapUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder xmlContent = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                xmlContent.append(line);
            }
            reader.close();

            Pattern pattern = Pattern.compile("<loc>(.*?)</loc>");
            Matcher matcher = pattern.matcher(xmlContent.toString());

            while (matcher.find()) {
                urls.add(matcher.group(1)); // Add extracted URL to the list
            }

        } catch (IOException e) {
            System.out.println("Failed to fetch sitemap: " + e.getMessage());
        }

        return urls;
    }

    // Checks the status of each URL in the list
    public static List<UrlStatus> checkUrls(List<String> urls) {
        List<UrlStatus> results = new ArrayList<>();

        for (String url : urls) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);  // Connection timeout
                conn.setReadTimeout(5000);     // Read timeout

                int responseCode = conn.getResponseCode();
                String status = (responseCode == 200) ? "VALID" : "BROKEN (" + responseCode + ")";
                results.add(new UrlStatus(url, status));

            } catch (IOException e) {
                results.add(new UrlStatus(url, "ERROR: " + e.getMessage()));
            }
        }

        return results;
    }

    // Generate a clean PDF report
    private static void generatePDFReport(List<UrlStatus> urlStatusList, int totalUrls) {
        try {
            // Prepare the PDF writer and document
            PdfWriter writer = new PdfWriter("sitemap_validation_report.pdf");
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Title
            document.add(new Paragraph("Sitemap URL Validation Report")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            // Summary Section
            int validUrls = (int) urlStatusList.stream().filter(status -> status.status.equals("VALID")).count();
            int brokenUrls = (int) urlStatusList.stream().filter(status -> status.status.startsWith("BROKEN")).count();

            document.add(new Paragraph("Summary:")
                    .setFontSize(14)
                    .setBold());
            document.add(new Paragraph("Total URLs: " + totalUrls));
            document.add(new Paragraph("Valid URLs: " + validUrls));
            document.add(new Paragraph("Broken URLs: " + brokenUrls));
            document.add(new Paragraph("\n"));

            // Table Section
            float[] columnWidths = {200F, 100F}; // Define column widths
            Table table = new Table(columnWidths);
            table.addHeaderCell(new Cell().add(new Paragraph("URL")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Status")).setBold());

            // Add rows to the table
            for (UrlStatus status : urlStatusList) {
                table.addCell(new Cell().add(new Paragraph(status.url)));
                table.addCell(new Cell().add(new Paragraph(status.status)).setTextAlignment(TextAlignment.CENTER));
            }

            document.add(table);

            // Close the document
            document.close();
            System.out.println("✅ PDF report generated: sitemap_validation_report.pdf");
        } catch (Exception e) {
            System.err.println("Error generating PDF report: " + e.getMessage());
        }
    }

    // Helper class to store URL and status
    static class UrlStatus {
        String url;
        String status;

        UrlStatus(String url, String status) {
            this.url = url;
            this.status = status;
        }
    }
}
