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
            // 方案一:视频搜索接口(稳定)前 3 页中作者名精确匹配关键词。
            // bili_user 用户搜索接口对非浏览器指纹风控严格(概率 403/412),
            // 而视频搜索接口稳定;视频搜索按 pubdate 排序,翻页扩大低频 UP 主的匹配窗口。
            for (int page = 1; page <= 3; page++) {
                List<SearchResult> pageResults = bilibiliFetcher.fetchPage(keyword, page);
                List<SearchResult> authorVideos = pageResults.stream()
                        .filter(v -> keyword.equalsIgnoreCase(v.getAuthorName()))
                        .toList();
                if (!authorVideos.isEmpty()) {
                    log.info("🎯 视频搜索命中账号: {} (第 {} 页,{} 条最新内容)", keyword, page, authorVideos.size());
                    authorVideos.forEach(v -> v.setAccountContent(true));
                    return new DetectResult(List.of(), authorVideos);
                }
            }

            // 方案二:用户搜索接口精确匹配 + UP 主空间最新视频(接口不稳定,作为补充)
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
            videos.forEach(v -> v.setAccountContent(true));
            return new DetectResult(List.of(account), videos);
        } catch (Exception e) {
            log.warn("账号检测失败 \"{}\": {}", keyword, e.getMessage());
            return DetectResult.empty();
        }
    }
}
