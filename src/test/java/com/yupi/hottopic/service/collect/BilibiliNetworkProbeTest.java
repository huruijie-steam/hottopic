package com.yupi.hottopic.service.collect;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 临时网络验证:B站用户搜索接口(需真实网络,默认禁用)
 */
@Disabled("需要真实网络,手动启用验证")
class BilibiliNetworkProbeTest {

    @Test
    void searchUser_老番茄() {
        BilibiliFetcher fetcher = new BilibiliFetcher();
        BilibiliFetcher.BilibiliUser user = fetcher.searchUser("老番茄");
        System.out.println("=== searchUser result: " + user);
        assertNotNull(user, "老番茄应能搜索到");
    }
}
