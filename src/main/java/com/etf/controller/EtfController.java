package com.etf.controller;

import com.etf.domain.EtfHolding;
import com.etf.dto.ScrapeResult;
import com.etf.repository.EtfHoldingRepository;
import com.etf.service.ScrapingService;
import org.jsoup.Jsoup;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/etf")
public class EtfController {

    private final EtfHoldingRepository repository;
    private final ScrapingService scrapingService;

    public EtfController(EtfHoldingRepository repository, ScrapingService scrapingService) {
        this.repository = repository;
        this.scrapingService = scrapingService;
    }

    @GetMapping("/dates")
    public List<LocalDate> getDates() {
        return repository.findDistinctScrapeDates();
    }

    @GetMapping("/dates/{date}/etfs")
    public List<String> getEtfNames(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return repository.findDistinctEtfNamesByDate(date);
    }

    @GetMapping("/dates/{date}/holdings")
    public List<EtfHolding> getHoldings(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String etfName) {
        return repository.findByScrapeDateAndEtfNameOrderByOrderNo(date, etfName);
    }

    @PostMapping("/scrape")
    public ResponseEntity<Map<String, Object>> manualScrape() {
        ScrapeResult result = scrapingService.scrapeAll();
        return ResponseEntity.ok(Map.of(
                "message", "스크래핑 완료",
                "totalNewHoldings", result.getTotalNewHoldings(),
                "date", LocalDate.now().toString(),
                "details", result.getDetails()
        ));
    }

    @GetMapping("/proxy/page")
    public ResponseEntity<String> proxyPage(
            @RequestParam int idx,
            @RequestParam String cate,
            @RequestParam(required = false) String pdfDate) {
        try {
            String url = "https://www.timeetf.co.kr/m11_view.php?idx=" + idx + "&cate=" + cate;
            if (pdfDate != null && !pdfDate.isEmpty()) {
                url += "&pdfDate=" + pdfDate;
            }
            String html = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .referrer("https://www.timeetf.co.kr/")
                    .timeout(15000)
                    .get()
                    .html();
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        } catch (Exception e) {
            return ResponseEntity.status(502)
                    .body("프록시 요청 실패: " + e.getMessage());
        }
    }
}
