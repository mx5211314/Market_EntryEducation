package com.investedu.smartassistant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.investedu.smartassistant.entity.Diary;
import com.investedu.smartassistant.entity.User;
import com.investedu.smartassistant.service.DiaryService;
import com.investedu.smartassistant.service.UserService;
import com.investedu.smartassistant.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/diary")
public class DiaryController {

    private final DiaryService diaryService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public DiaryController(DiaryService diaryService, UserService userService, JwtUtil jwtUtil) {
        this.diaryService = diaryService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public Diary create(@RequestHeader("Authorization") String authHeader,
                        @RequestBody Map<String, Object> body) {
        return diaryService.create(getUserId(authHeader), body);
    }

    @PutMapping("/{id}")
    public Diary update(@RequestHeader("Authorization") String authHeader,
                        @PathVariable Long id,
                        @RequestBody Map<String, Object> body) {
        return diaryService.update(id, getUserId(authHeader), body);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@RequestHeader("Authorization") String authHeader,
                                      @PathVariable Long id) {
        diaryService.delete(id, getUserId(authHeader));
        return Map.of("message", "删除成功");
    }

    @GetMapping("/list")
    public IPage<Diary> list(@RequestHeader("Authorization") String authHeader,
                             @RequestParam(defaultValue = "1") int pageNum,
                             @RequestParam(defaultValue = "5") int pageSize) {
        return diaryService.pageByUser(getUserId(authHeader), pageNum, Math.min(pageSize, 50));
    }

    /** 到期未回顾的记录 */
    @GetMapping("/pending")
    public List<Diary> pending(@RequestHeader("Authorization") String authHeader) {
        return diaryService.pendingReviews(getUserId(authHeader));
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestHeader("Authorization") String authHeader) {
        return diaryService.getStats(getUserId(authHeader));
    }

    /** 详情连同纪律分构成一起给，页面才能说清这个分是怎么算出来的 */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@RequestHeader("Authorization") String authHeader,
                                      @PathVariable Long id) {
        Diary diary = diaryService.require(id, getUserId(authHeader));
        Map<String, Object> res = new HashMap<>();
        res.put("diary", diary);
        res.put("disciplineItems", diaryService.disciplineItems(diary));
        return res;
    }

    /** 到期对账：条件触发了吗、你照做了吗 */
    @PostMapping("/{id}/review")
    public Diary review(@RequestHeader("Authorization") String authHeader,
                        @PathVariable Long id,
                        @RequestBody Map<String, Object> body) {
        Boolean triggered = asBoolean(body.get("triggered"));
        Boolean executed = asBoolean(body.get("executed"));
        String resultTag = body.get("resultTag") == null ? null : String.valueOf(body.get("resultTag"));
        String note = body.get("note") == null ? null : String.valueOf(body.get("note"));
        return diaryService.review(id, getUserId(authHeader), triggered, executed, resultTag, note);
    }

    /** AI 复盘教练 */
    @PostMapping("/{id}/coach")
    public Map<String, String> coach(@RequestHeader("Authorization") String authHeader,
                                     @PathVariable Long id) {
        return Map.of("review", diaryService.coach(id, getUserId(authHeader)));
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

    private Long getUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.findByUsername(username);
        return user.getId();
    }
}
