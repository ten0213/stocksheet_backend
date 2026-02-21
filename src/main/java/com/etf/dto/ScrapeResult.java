package com.etf.dto;

import java.util.ArrayList;
import java.util.List;

public class ScrapeResult {

    private int totalNewHoldings;
    private final List<EtfScrapeDetail> details = new ArrayList<>();

    public void addDetail(EtfScrapeDetail detail) {
        if (detail.getStatus() == ScrapeStatus.SCRAPED) {
            totalNewHoldings += detail.getHoldingsCount();
        }
        details.add(detail);
    }

    public int getTotalNewHoldings() { return totalNewHoldings; }
    public List<EtfScrapeDetail> getDetails() { return details; }

    public static class EtfScrapeDetail {
        private final String etfName;
        private final ScrapeStatus status;
        private final int holdingsCount;
        private final String message;

        public EtfScrapeDetail(String etfName, ScrapeStatus status, int holdingsCount, String message) {
            this.etfName = etfName;
            this.status = status;
            this.holdingsCount = holdingsCount;
            this.message = message;
        }

        public String getEtfName() { return etfName; }
        public ScrapeStatus getStatus() { return status; }
        public int getHoldingsCount() { return holdingsCount; }
        public String getMessage() { return message; }
    }

    public enum ScrapeStatus {
        SCRAPED,
        ALREADY_EXISTS,
        EMPTY,
        FAILED
    }
}
