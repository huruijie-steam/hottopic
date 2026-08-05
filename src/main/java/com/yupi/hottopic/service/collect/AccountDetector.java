package com.yupi.hottopic.service.collect;

import com.yupi.hottopic.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 账号检测(需求 HC-4):当监控关键词恰好是 B 站 UP 主时,直接拉取该账号最新视频。
 * 与原项目一致,当前仅支持 B 站;检测失败返回空,不影响主流程。
 */
@Service
public class AccountDetector {

    private static final Logger log = LoggerFactory.getLogger(AccountDetector.class);

    private final BilibiliFetcher bilibiliFetcher;

    public AccountDetector(BilibiliFetcher bilibiliFetcher) {
        this.bilibiliFetcher = bilibiliFetcher;
    }

    /** 检测到的账号信息 */
    public record AccountInfo(String platform, String name, String id, long followers,
                              boolean verified, String sign, String avatar) {
    }

    /** 检测结果:账号列表 + 该账号最新内容 */
    public record DetectResult(List<AccountInfo> accounts, List<SearchResult> results) {
        public static DetectResult empty() {
            return new DetectResult(List.of(), List.of());
        }
    }

    /** 检测关键词是否为平台账号,并拉取最新内容 */
    public DetectResult detectAndFetch(String keyword) {
        try {
            BilibiliFetcher.BilibiliUser user = bilibiliFetcher.searchUser(keyword);
            if (user == null) {
                return DetectResult.empty();
            }
            log.info("🎯 检测到 B 站账号: {} ({} 粉丝)", user.name(), user.fans());
            AccountInfo account = new AccountInfo(
                    "bilibili", user.name(), user.mid(), user.fans(),
                    user.verifiedType() >= 0, user.sign(), user.avatar());
            List<SearchResult> videos = bilibiliFetcher.getUserVideos(user.mid());
            log.info("  账号最新视频: {} 条", videos.size());
            return new DetectResult(List.of(account), videos);
        } catch (Exception e) {
            log.warn("账号检测失败 \"{}\": {}", keyword, e.getMessage());
            return DetectResult.empty();
        }
    }
}
