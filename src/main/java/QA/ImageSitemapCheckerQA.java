package QA;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageSitemapCheckerQA {

    public static void main(String[] args) {
        String sitemapUrl = "https://www.jswonemsme.com/image-sitemap.xml";
        List<PageWithImages> pages = extractUrlsAndImagesFromSitemap(sitemapUrl);

        List<UrlStatus> allUrlStatuses = new ArrayList<>();
        Set<String> uniqueUrls = new HashSet<>();

        for (PageWithImages page : pages) {
            // Only checking image URLs
            for (String imageUrl : page.imageUrls) {
                if (uniqueUrls.add(imageUrl)) {
                    allUrlStatuses.add(checkUrl(imageUrl));
                }
            }
        }

        writeExcelReport(pages, allUrlStatuses);
    }

    public static List<PageWithImages> extractUrlsAndImagesFromSitemap(String sitemapUrl) {
        List<PageWithImages> results = new ArrayList<>();
        try {
            URL url = new URL(sitemapUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder xml = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                xml.append(line);
            }
            reader.close();

            Pattern urlPattern = Pattern.compile("<url>(.*?)</url>");
            Matcher urlMatcher = urlPattern.matcher(xml.toString());

            while (urlMatcher.find()) {
                String urlBlock = urlMatcher.group(1);

                Matcher locMatcher = Pattern.compile("<loc>(.*?)</loc>").matcher(urlBlock);
                String pageUrl = locMatcher.find() ? locMatcher.group(1) : "";

                Matcher imageMatcher = Pattern.compile("<image:loc>(.*?)</image:loc>").matcher(urlBlock);
                List<String> imageUrls = new ArrayList<>();
                while (imageMatcher.find()) {
                    imageUrls.add(imageMatcher.group(1));
                }

                results.add(new PageWithImages(pageUrl, imageUrls));
            }

        } catch (IOException e) {
            System.out.println("❌ Error reading sitemap: " + e.getMessage());
        }

        return results;
    }

    public static UrlStatus checkUrl(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            return new UrlStatus(url, code == 200 ? "VALID" : "❌ BROKEN (" + code + ")");

        } catch (IOException e) {
            return new UrlStatus(url, "❌ ERROR: " + e.getMessage());
        }
    }

    public static void writeExcelReport(List<PageWithImages> pages, List<UrlStatus> statuses) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Image URL Validation");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            int rowNum = 0;
            Row titleRow = sheet.createRow(rowNum++);
            titleRow.createCell(0).setCellValue("Image URL Validation Report");

            // Count only image URLs
            long totalImageUrls = statuses.size();
            long valid = statuses.stream().filter(s -> s.status.equals("VALID")).count();
            long broken = totalImageUrls - valid;

            sheet.createRow(rowNum++).createCell(0).setCellValue("Total Image URLs: " + totalImageUrls);
            sheet.createRow(rowNum++).createCell(0).setCellValue("Valid Image URLs: " + valid);
            sheet.createRow(rowNum++).createCell(0).setCellValue("Broken Image URLs: " + broken);
            rowNum++;

            Row header = sheet.createRow(rowNum++);
            header.createCell(0).setCellValue("Page URL");
            header.createCell(1).setCellValue("Image URL");
            header.createCell(2).setCellValue("Status");
            for (int i = 0; i < 3; i++) header.getCell(i).setCellStyle(headerStyle);

            Map<String, String> urlStatusMap = new HashMap<>();
            for (UrlStatus status : statuses) {
                urlStatusMap.put(status.url, status.status);
            }

            for (PageWithImages page : pages) {
                for (String imageUrl : page.imageUrls) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(page.pageUrl);
                    row.createCell(1).setCellValue(imageUrl);
                    row.createCell(2).setCellValue(urlStatusMap.getOrDefault(imageUrl, "UNKNOWN"));
                }
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);

            File folder = new File("target");
            if (!folder.exists()) folder.mkdirs();

            try (FileOutputStream out = new FileOutputStream("target/full_sitemap_image_only_report.xlsx")) {
                workbook.write(out);
                System.out.println("✅ Excel report saved to: target/full_sitemap_image_only_report.xlsx");
            }

        } catch (IOException e) {
            System.err.println("❌ Failed to write Excel report: " + e.getMessage());
        }
    }

    static class PageWithImages {
        String pageUrl;
        List<String> imageUrls;

        PageWithImages(String pageUrl, List<String> imageUrls) {
            this.pageUrl = pageUrl;
            this.imageUrls = imageUrls;
        }
    }

    static class UrlStatus {
        String url;
        String status;

        UrlStatus(String url, String status) {
            this.url = url;
            this.status = status;
        }
    }
}
