package com.etf.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "etf_holding", indexes = {
    @Index(name = "idx_scrape_date", columnList = "scrapeDate"),
    @Index(name = "idx_date_etf", columnList = "scrapeDate, etfName")
})
public class EtfHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String etfName;

    private Integer orderNo;
    private String stockCode;

    @Column(nullable = false)
    private String stockName;

    private Long quantity;
    private Long valuationAmount;
    private Double weight;

    @Column(nullable = false)
    private LocalDate scrapeDate;

    public EtfHolding() {}

    public EtfHolding(String etfName, Integer orderNo, String stockCode, String stockName,
                      Long quantity, Long valuationAmount, Double weight, LocalDate scrapeDate) {
        this.etfName = etfName;
        this.orderNo = orderNo;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.quantity = quantity;
        this.valuationAmount = valuationAmount;
        this.weight = weight;
        this.scrapeDate = scrapeDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEtfName() { return etfName; }
    public void setEtfName(String etfName) { this.etfName = etfName; }
    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }
    public Long getQuantity() { return quantity; }
    public void setQuantity(Long quantity) { this.quantity = quantity; }
    public Long getValuationAmount() { return valuationAmount; }
    public void setValuationAmount(Long valuationAmount) { this.valuationAmount = valuationAmount; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public LocalDate getScrapeDate() { return scrapeDate; }
    public void setScrapeDate(LocalDate scrapeDate) { this.scrapeDate = scrapeDate; }
}
