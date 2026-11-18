package com.example.battle_assistant.preparation;

import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/preparation")
public class PreparationController {
    private final AvatarRepository repository;
    private final AvatarCache cache;
    private final String adminToken;
    private final double threshold, margin;
    private final Semaphore capacity = new Semaphore(2);
    public PreparationController(AvatarRepository repository, AvatarCache cache,
            @Value("${preparation.admin-token:}") String adminToken,
            @Value("${preparation.match-threshold:0.90}") double threshold,
            @Value("${preparation.match-margin:0.025}") double margin) {
        this.repository=repository; this.cache=cache; this.adminToken=adminToken;
        if (!Double.isFinite(threshold) || threshold<0 || threshold>1 || !Double.isFinite(margin) || margin<0 || margin>1)
            throw new IllegalArgumentException("匹配阈值必须在 0 到 1 之间");
        this.threshold=threshold; this.margin=margin;
    }
    @GetMapping("/status") public Map<String,Object> status() { return cache.status(); }
    @GetMapping("/avatars") public List<AvatarEntry> list(@RequestHeader(value="X-Admin-Token", required=false) String token) {
        authorize(token); return repository.list();
    }
    @PutMapping("/avatars") public AvatarEntry save(@RequestHeader(value="X-Admin-Token", required=false) String token,
            @RequestBody AvatarEntry entry) {
        authorize(token); repository.save(entry); return entry;
    }
    @DeleteMapping("/avatars") public void delete(@RequestHeader(value="X-Admin-Token", required=false) String token,
            @RequestParam String avatarKey) { authorize(token); repository.delete(avatarKey); }
    private void authorize(String token) {
        if (adminToken.isBlank() || token==null || !MessageDigest.isEqual(adminToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "需要有效的资料管理令牌");
    }
    public record Slot(String side, int position, boolean lead, AvatarMatcher.Match recognition) {}
    public record Result(long processingMs, java.time.Instant catalogLoadedAt, boolean complete, List<Slot> slots) {}
    @PostMapping(value="/recognize", consumes="multipart/form-data")
    public Result recognize(@RequestParam MultipartFile image,
            @RequestParam(defaultValue="0") double contentX, @RequestParam(defaultValue="0") double contentY,
            @RequestParam(defaultValue="1") double contentWidth, @RequestParam(defaultValue="1") double contentHeight)
            throws java.io.IOException {
        long started = System.nanoTime();
        var snapshot = cache.snapshot();
        if (snapshot.templates().isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "头像资料尚未就绪，请先录入并等待预加载");
        if (!capacity.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "识别繁忙，请稍后重试");
        try {
            BufferedImage source = ImageInput.decode(image.getBytes());
            var content = crop(source, contentX, contentY, contentWidth, contentHeight);
            if (content.getWidth()<800 || content.getHeight()<400)
                throw new IllegalArgumentException("游戏画面至少需要 800×400 像素");
            var slots = new ArrayList<Slot>();
            // Normalized coordinates calibrated on user's initial 2316x1080 screenshot,
            // relative to the game rectangle (excluding left/right outer bars).
            double[][] panels = {{.030,.430,.074,.117,.061,.112}, {.757,.557,.075,.120,.061,.112}};
            for (int side=0; side<2; side++) {
                double[] p=panels[side];
                for (int slot=0; slot<6; slot++) {
                    BufferedImage avatar=crop(content,p[0]+slot%3*p[2],p[1]+slot/3*p[3],p[4],p[5]);
                    slots.add(new Slot(side==0 ? "self" : "opponent", slot+1, slot==0,
                        AvatarMatcher.match(AvatarMatcher.feature(avatar),snapshot.templates(),threshold,margin)));
                }
            }
            return new Result((System.nanoTime()-started)/1_000_000, snapshot.loadedAt(),
                slots.stream().allMatch(s->s.recognition().status().equals("matched")), List.copyOf(slots));
        } finally { capacity.release(); }
    }
    static BufferedImage crop(BufferedImage image, double x, double y, double w, double h) {
        if (!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(w)||!Double.isFinite(h)
                ||x<0||y<0||w<=0||h<=0||x+w>1.000001||y+h>1.000001)
            throw new IllegalArgumentException("裁剪区域须位于图片内，坐标使用 0 到 1 的比例");
        int left=(int)Math.round(x*image.getWidth()),top=(int)Math.round(y*image.getHeight());
        int right=Math.min(image.getWidth(),(int)Math.round((x+w)*image.getWidth()));
        int bottom=Math.min(image.getHeight(),(int)Math.round((y+h)*image.getHeight()));
        if (right<=left||bottom<=top) throw new IllegalArgumentException("裁剪区域过小");
        return image.getSubimage(left,top,right-left,bottom-top);
    }
}
