package com.example.battle_assistant.preparation;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class AvatarCache {
    public record Snapshot(List<AvatarMatcher.Template> templates, Instant loadedAt) {}
    private final AvatarRepository repository;
    private final URI base;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
        .followRedirects(HttpClient.Redirect.NEVER).build();
    private volatile Snapshot snapshot = new Snapshot(List.of(), null);
    private volatile String lastError;
    public AvatarCache(AvatarRepository repository, @Value("${preparation.avatar-base-url:}") String url) {
        this.repository = repository;
        if (url.isBlank()) base = null;
        else {
            base = URI.create(url.endsWith("/") ? url : url+"/");
            if (!"https".equals(base.getScheme()) || base.getHost()==null || base.getUserInfo()!=null
                    || base.getQuery()!=null || base.getFragment()!=null)
                throw new IllegalArgumentException("头像地址需为无查询参数的 HTTPS 基础地址");
        }
    }
    public Snapshot snapshot() { return snapshot; }
    public Map<String,Object> status() {
        Snapshot current = snapshot;
        var result = new LinkedHashMap<String,Object>();
        result.put("ready", !current.templates().isEmpty());
        result.put("templates", current.templates().size()); result.put("loadedAt", current.loadedAt());
        result.put("lastError", lastError); return result;
    }
    @Scheduled(initialDelay = 1000, fixedDelayString = "${preparation.refresh-ms:60000}")
    public void scheduledRefresh() {
        try { refresh(); }
        catch (Exception e) { lastError = "资料预加载失败；检查数据库、OSS 地址、读取权限及图片格式"; }
    }

    private static HttpResponse.BodyHandler<byte[]> limitedBody() {
        return info -> new HttpResponse.BodySubscriber<byte[]>() {
            private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
            private java.util.concurrent.Flow.Subscription subscription;
            private long bytes;
            public java.util.concurrent.CompletionStage<byte[]> getBody() { return delegate.getBody(); }
            public void onSubscribe(java.util.concurrent.Flow.Subscription s) { subscription=s; delegate.onSubscribe(s); }
            public void onNext(List<java.nio.ByteBuffer> buffers) {
                for (var buffer : buffers) bytes += buffer.remaining();
                if (bytes > 8_000_000) {
                    subscription.cancel(); delegate.onError(new java.io.IOException("头像超过 8MB"));
                } else delegate.onNext(buffers);
            }
            public void onError(Throwable error) { delegate.onError(error); }
            public void onComplete() { delegate.onComplete(); }
        };
    }
    public synchronized void refresh() throws Exception {
        if (base == null) { lastError = "尚未配置 BATTLE_AVATAR_BASE_URL"; return; }
        var entries = repository.list();
        if (entries.size()>5000) throw new IllegalArgumentException("头像数量超过当前版本上限 5000");
        var next = new ArrayList<AvatarMatcher.Template>();
        Map<String,String> traits = new HashMap<>();
        Map<AvatarEntry,AvatarMatcher.Template> existing = new HashMap<>();
        for (var t : snapshot.templates()) existing.put(t.entry(), t);
        for (var entry : entries) {
            String identity = entry.speciesKey()+"\n"+entry.form();
            String previous = traits.putIfAbsent(identity, entry.popularTrait());
            if (previous != null && !previous.equals(entry.popularTrait()))
                throw new IllegalArgumentException("相同精灵形态的热门特性必须一致");
            if (existing.containsKey(entry)) { next.add(existing.get(entry)); continue; }
            var request = HttpRequest.newBuilder(base.resolve(entry.avatarKey())).timeout(Duration.ofSeconds(5)).GET().build();
            // Full bounded body read under HttpRequest timeout (no unbounded streaming read).
            var response = client.send(request, limitedBody());
            if (response.statusCode()!=200 || response.body().length>8_000_000)
                throw new IllegalStateException("头像下载失败");
            next.add(new AvatarMatcher.Template(entry, AvatarMatcher.feature(ImageInput.decode(response.body()))));
        }
        snapshot = new Snapshot(List.copyOf(next), Instant.now());
        lastError = null;
    }
}
