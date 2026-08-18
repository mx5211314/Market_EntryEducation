package com.investedu.smartassistant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.investedu.smartassistant.entity.Diary;
import com.investedu.smartassistant.service.DiaryService;
import com.investedu.smartassistant.util.AuthContext;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/diary")
public class DiaryController {

    private final DiaryService diaryService;
    private final AuthContext authContext;

    public DiaryController(DiaryService diaryService, AuthContext authContext) {
        this.diaryService = diaryService;
        this.authContext = authContext;
    }

    @PostMapping
    public Diary create(@RequestBody Map<String, Object> body) {
        return diaryService.create(authContext.requireUserId(), body);
    }

    @PutMapping("/{id}")
    public Diary update(@PathVariable Long id,
                        @RequestBody Map<String, Object> body) {
        return diaryService.update(id, authContext.requireUserId(), body);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        diaryService.delete(id, authContext.requireUserId());
        return Map.of("message", "删除成功");
    }

    @GetMapping("/list")
    public IPage<Diary> list(@RequestParam(defaultValue = "1") int pageNum,
                             @RequestParam(defaultValue = "5") int pageSize) {
        return diaryService.pageByUser(authContext.requireUserId(), pageNum, Math.min(pageSize, 50));
    }

    /** 到期未回顾的记录 */
    @GetMapping("/pending")
    public List<Diary> pending() {
        return diaryService.pendingReviews(authContext.requireUserId());
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return diaryService.getStats(authContext.requireUserId());
    }

    /** 详情连同纪律分构成一起给，页面才能说清这个分是怎么算出来的 */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Diary diary = diaryService.require(id, authContext.requireUserId());
        Map<String, Object> res = new HashMap<>();
        res.put("diary", diary);
        res.put("disciplineItems", diaryService.disciplineItems(diary));
        return res;
    }

    /** 到期对账：条件触发了吗、你照做了吗 */
    @PostMapping("/{id}/review")
    public Diary review(@PathVariable Long id,
                        @RequestBody Map<String, Object> body) {
        Boolean triggered = asBoolean(body.get("triggered"));
        Boolean executed = asBoolean(body.get("executed"));
        String resultTag = body.get("resultTag") == null ? null : String.valueOf(body.get("resultTag"));
        String note = body.get("note") == null ? null : String.valueOf(body.get("note"));
        return diaryService.review(id, authContext.requireUserId(), triggered, executed, resultTag, note);
    }

    /** AI 复盘教练 */
    @PostMapping("/{id}/coach")
    public Map<String, String> coach(@PathVariable Long id) {
        return Map.of("review", diaryService.coach(id, authContext.requireUserId()));
    }

    /** 表单可选的理由标签由后端给，避免前后端两份清单对不上 */
    @GetMapping("/options")
    public Map<String, Object> options() {
        return Map.of("reasonTags", DiaryService.REASON_TAGS);
    }

    private Boolean asBoolean(Object raw) {
        if (raw instanceof Boolean b) return b;
        return raw != null && Boolean.parseBoolean(String.valueOf(raw));
    }
}
