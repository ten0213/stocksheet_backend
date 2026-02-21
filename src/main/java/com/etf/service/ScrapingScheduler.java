package com.etf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScrapingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScrapingScheduler.class);
    private final ScrapingService scrapingService;

    public ScrapingScheduler(ScrapingService scrapingService) {
        this.scrapingService = scrapingService;
    }

    @Scheduled(cron = "${etf.scrape.cron}")
    public void scheduledScrape() {
        log.info("Scheduled ETF scraping started");
        var result = scrapingService.scrapeAll();
        log.info("Scheduled ETF scraping completed: {} new holdings saved", result.getTotalNewHoldings());
    }
}
