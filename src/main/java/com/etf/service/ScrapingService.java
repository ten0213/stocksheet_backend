package com.etf.service;

import com.etf.domain.EtfHolding;
import com.etf.domain.EtfInfo;
import com.etf.dto.ScrapeResult;
import com.etf.dto.ScrapeResult.EtfScrapeDetail;
import com.etf.dto.ScrapeResult.ScrapeStatus;
import com.etf.repository.EtfHoldingRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScrapingService {

    private static final Logger log = LoggerFactory.getLogger(ScrapingService.class);
    private final EtfHoldingRepository repository;

    public ScrapingService(EtfHoldingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ScrapeResult scrapeAll() {
        LocalDate today = LocalDate.now();
        ScrapeResult result = new ScrapeResult();

        for (EtfInfo etf : EtfInfo.values()) {
            String name = etf.getDisplayName();

            if (repository.existsByScrapeDateAndEtfName(today, name)) {
                log.info("Data already exists for {} on {}", name, today);
                long existingCount = repository.countByScrapeDateAndEtfName(today, name);
                result.addDetail(new EtfScrapeDetail(
                        name, ScrapeStatus.ALREADY_EXISTS, (int) existingCount,
                        "오늘(" + today + ") 데이터가 이미 존재합니다 (" + existingCount + "건)"
                ));
                continue;
            }

            try {
                List<EtfHolding> holdings = scrapeEtf(etf, today);

                if (holdings.isEmpty()) {
                    log.warn("No data found for {} on {}", name, today);
                    result.addDetail(new EtfScrapeDetail(
                            name, ScrapeStatus.EMPTY, 0,
                            "외부 사이트에서 가져올 데이터가 없습니다"
                    ));
                } else {
                    log.info("Scraped {} holdings for {}", holdings.size(), name);
                    result.addDetail(new EtfScrapeDetail(
                            name, ScrapeStatus.SCRAPED, holdings.size(),
                            holdings.size() + "건 스크래핑 완료"
                    ));
                }
            } catch (Exception e) {
                log.error("Failed to scrape {}: {}", name, e.getMessage());
                result.addDetail(new EtfScrapeDetail(
                        name, ScrapeStatus.FAILED, 0,
                        "스크래핑 실패: " + e.getMessage()
                ));
            }
        }

        return result;
    }

    @Transactional
    public List<EtfHolding> scrapeEtf(EtfInfo etfInfo, LocalDate date) throws IOException {
        Document doc = Jsoup.connect(etfInfo.getUrl())
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .referrer("https://www.timeetf.co.kr/")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .timeout(15000)
                .get();

        Elements rows = doc.select("table.moreList1 tbody tr");
        List<EtfHolding> holdings = new ArrayList<>();

        int orderNo = 1;
        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() < 5) continue;

            String stockCode = cells.get(0).text().trim();
            String stockName = cells.get(1).text().trim();
            String quantityStr = removeCommas(cells.get(2).text().trim());
            String valuationStr = removeCommas(cells.get(3).text().trim());
            String weightStr = cells.get(4).text().trim();

            if (stockName.isEmpty()) continue;

            int currentOrder;
            if (stockCode.isEmpty() || stockName.contains("현금") || stockName.contains("원화")) {
                currentOrder = -1;
            } else {
                currentOrder = orderNo++;
            }

            EtfHolding holding = new EtfHolding(
                    etfInfo.getDisplayName(),
                    currentOrder,
                    stockCode,
                    stockName,
                    parseLongSafe(quantityStr),
                    parseLongSafe(valuationStr),
                    parseDoubleSafe(weightStr),
                    date
            );

            holdings.add(holding);
        }

        if (holdings.isEmpty()) {
            return holdings;
        }

        return repository.saveAll(holdings);
    }

    private String removeCommas(String str) {
        return str.replace(",", "");
    }

    private Long parseLongSafe(String str) {
        try { return Long.parseLong(str); }
        catch (NumberFormatException e) { return null; }
    }

    private Double parseDoubleSafe(String str) {
        try { return Double.parseDouble(str); }
        catch (NumberFormatException e) { return null; }
    }
}
