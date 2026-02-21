package com.etf.domain;

public enum EtfInfo {
    K_RENEWABLE("TIME K신재생에너지액티브", "https://www.timeetf.co.kr/m11_view.php?idx=16&cate=002"),
    KOREA_DIVIDEND("TIME Korea플러스배당액티브", "https://www.timeetf.co.kr/m11_view.php?idx=12&cate=002"),
    KOSPI("TIME 코스피액티브", "https://www.timeetf.co.kr/m11_view.php?idx=11&cate=002"),
    KOREA_VALUEUP("TIME 코리아밸류업액티브", "https://www.timeetf.co.kr/m11_view.php?idx=15&cate=002"),
    K_INNOVATION("TIME K이노베이션액티브", "https://www.timeetf.co.kr/m11_view.php?idx=17&cate=002");

    private final String displayName;
    private final String url;

    EtfInfo(String displayName, String url) {
        this.displayName = displayName;
        this.url = url;
    }

    public String getDisplayName() { return displayName; }
    public String getUrl() { return url; }
}
