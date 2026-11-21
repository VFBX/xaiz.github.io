package com.xaiz.aidownload;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SnapFlow AI v2.0 — SnapFlow Studio. NATIVE, khong dung trinh duyet nhung,
 * khong mo trang web ben ngoai. Moi chuc nang chay trong app:
 * dan link -> goi truc tiep API giai ma (TikWM cho TikTok, FxTwitter cho X,
 * OpenGraph cho Facebook / Instagram / YouTube, link file truc tiep) ->
 * hien ket qua + anh bia -> tai bang DownloadManager ve Downloads / SnapFlow AI.
 * Lich su luu trong SharedPreferences tren may.
 */
public class MainActivity extends Activity {
    private static final String UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Mobile Safari/537.36";

    private EditText linkInput;
    private Button fetchBtn;
    private ProgressBar progress;
    private TextView statusText;
    private ImageView coverImage;
    private TextView resultTitle;
    private TextView resultAuthor;
    private TextView resultStats;
    private LinearLayout optionsBox;
    private LinearLayout historyBox;

    static class Opt {
        String id; String kind; String label; String detail; String url; boolean noWatermark;
        Opt(String id, String kind, String label, String detail, String url, boolean noWatermark) {
            this.id = id; this.kind = kind; this.label = label; this.detail = detail;
            this.url = url; this.noWatermark = noWatermark;
        }
    }

    static class Resolved {
        String platform; String sourceUrl; String title = ""; String authorName = "";
        String authorHandle = ""; String kind = "video"; String coverUrl = "";
        Long durationSec; Long playCount; Long likeCount; Long commentCount; Long shareCount;
        List<Opt> options = new ArrayList<Opt>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        linkInput = findViewById(R.id.link_input);
        fetchBtn = findViewById(R.id.fetch_btn);
        progress = findViewById(R.id.progress);
        statusText = findViewById(R.id.status_text);
        coverImage = findViewById(R.id.cover_image);
        resultTitle = findViewById(R.id.result_title);
        resultAuthor = findViewById(R.id.result_author);
        resultStats = findViewById(R.id.result_stats);
        optionsBox = findViewById(R.id.options_box);
        historyBox = findViewById(R.id.history_box);

        findViewById(R.id.header_bar).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Toast.makeText(MainActivity.this, getString(R.string.about), Toast.LENGTH_LONG).show();
            }
        });
        findViewById(R.id.paste_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pasteLink(); }
        });
        findViewById(R.id.clear_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                linkInput.setText("");
                clearResults();
                statusText.setText("");
            }
        });
        fetchBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { fetchLinks(); }
        });

        renderHistory();
        String shared = extractLink(getIntent());
        if (shared != null) {
            linkInput.setText(shared);
            fetchLinks();
        }
    }

    private void pasteLink() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) {
                Toast.makeText(this, R.string.no_clipboard, Toast.LENGTH_SHORT).show();
                return;
            }
            CharSequence t = clip.getItemAt(0).getText();
            if (t == null || t.toString().trim().isEmpty()) {
                Toast.makeText(this, R.string.no_clipboard, Toast.LENGTH_SHORT).show();
                return;
            }
            String cur = linkInput.getText().toString();
            String pasted = t.toString().trim();
            linkInput.setText(cur.trim().isEmpty() ? pasted : cur.trim() + "\n" + pasted);
            Toast.makeText(this, R.string.pasted, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.no_clipboard, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearResults() {
        optionsBox.removeAllViews();
        coverImage.setVisibility(View.GONE);
        resultTitle.setVisibility(View.GONE);
        resultAuthor.setVisibility(View.GONE);
        resultStats.setVisibility(View.GONE);
    }

    private void fetchLinks() {
        final List<String> urls = extractUrls(linkInput.getText().toString());
        if (urls.isEmpty()) {
            statusText.setText(getString(R.string.no_link));
            return;
        }
        clearResults();
        fetchBtn.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(30);
        statusText.setText(getString(R.string.fetching));
        new Thread(new Runnable() {
            @Override public void run() {
                final List<String> errors = new ArrayList<String>();
                for (final String raw : urls) {
                    try {
                        final Resolved r = resolveOne(raw);
                        runOnUiThread(new Runnable() {
                            @Override public void run() { showResult(r); }
                        });
                        saveHistory(r);
                        if (r.coverUrl != null && !r.coverUrl.isEmpty()) {
                            final Bitmap bmp = loadBitmap(r.coverUrl);
                            if (bmp != null) {
                                runOnUiThread(new Runnable() {
                                    @Override public void run() {
                                        coverImage.setImageBitmap(bmp);
                                        coverImage.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }
                    } catch (final Exception e) {
                        errors.add(e.getMessage() != null ? e.getMessage() : "Loi khong xac dinh.");
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        progress.setVisibility(View.GONE);
                        fetchBtn.setEnabled(true);
                        renderHistory();
                        if (!errors.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            for (String e : errors) sb.append(e).append("\n");
                            statusText.setText(sb.toString().trim());
                        } else {
                            statusText.setText("Da lay xong " + urls.size() + " link — bam nut ben duoi moi muc de tai ve may.");
                        }
                    }
                });
            }
        }).start();
    }

    private void showResult(final Resolved r) {
        resultTitle.setText(r.title);
        resultTitle.setVisibility(View.VISIBLE);
        String author = r.authorName;
        if (r.authorHandle != null && !r.authorHandle.isEmpty()) {
            author = author.isEmpty() ? r.authorHandle : author + " " + r.authorHandle;
        }
        if (!author.isEmpty()) {
            resultAuthor.setText(author + " • " + platformName(r.platform));
            resultAuthor.setVisibility(View.VISIBLE);
        }
        StringBuilder stats = new StringBuilder();
        if (r.playCount != null) stats.append("👁 ").append(formatCount(r.playCount)).append("  ");
        if (r.likeCount != null) stats.append("♥ ").append(formatCount(r.likeCount)).append("  ");
        if (r.commentCount != null) stats.append("💬 ").append(formatCount(r.commentCount)).append("  ");
        if (r.durationSec != null) stats.append("⏱ ").append(r.durationSec).append("s");
        if (stats.length() > 0) {
            resultStats.setText(stats.toString().trim());
            resultStats.setVisibility(View.VISIBLE);
        }
        for (final Opt o : r.options) {
            Button b = new Button(this);
            b.setText(o.label + "\n" + o.detail);
            b.setTextSize(14);
            b.setAllCaps(false);
            b.setBackgroundColor(0xFFE3F2FD);
            b.setTextColor(0xFF0D47A1);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 8, 0, 0);
            b.setLayoutParams(lp);
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { downloadOption(o, r.title); }
            });
            optionsBox.addView(b);
        }
    }

    private String platformName(String p) {
        if ("tiktok".equals(p)) return "TikTok";
        if ("facebook".equals(p)) return "Facebook";
        if ("instagram".equals(p)) return "Instagram";
        if ("x".equals(p)) return "X";
        if ("youtube".equals(p)) return "YouTube";
        if ("direct".equals(p)) return "Link truc tiep";
        return p;
    }

    private String formatCount(long n) {
        if (n >= 1000000) return String.format("%.1fM", n / 1000000.0);
        if (n >= 1000) return String.format("%.1fK", n / 1000.0);
        return String.valueOf(n);
    }

    private void downloadOption(Opt o, String title) {
        try {
            String ext = ".mp4";
            String low = o.url.toLowerCase();
            if ("audio".equals(o.kind)) ext = ".mp3";
            else if ("image".equals(o.kind)) ext = low.contains(".png") ? ".png" : low.contains(".webp") ? ".webp" : ".jpg";
            else if (low.contains(".webm")) ext = ".webm";
            else if (low.contains(".mov")) ext = ".mov";
            String base = title == null ? "video" : title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
            if (base.isEmpty()) base = "video";
            if (base.length() > 80) base = base.substring(0, 80);
            String filename = base + "_" + o.id + ext;
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(o.url));
            req.addRequestHeader("User-Agent", UA);
            req.setTitle(filename);
            req.setDescription("SnapFlow AI");
            req.allowScanningByMediaScanner();
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SnapFlow AI/" + filename);
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            dm.enqueue(req);
            Toast.makeText(this, getString(R.string.download_started), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(o.url))); } catch (Exception ignored) {}
        }
    }

    // ---------- Lich su (luu tren may) ----------
    private void saveHistory(Resolved r) {
        try {
            SharedPreferences sp = getSharedPreferences("snapflow_history", MODE_PRIVATE);
            String old = sp.getString("items", "");
            String line = r.platform + "|||" + r.title.replaceAll("[\\r\\n|]", " ") + "|||" + r.sourceUrl;
            String updated = line + "\n" + old;
            String[] parts = updated.split("\n");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length && i < 10; i++) {
                if (parts[i].trim().isEmpty()) continue;
                if (sb.length() > 0) sb.append("\n");
                sb.append(parts[i]);
            }
            sp.edit().putString("items", sb.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void renderHistory() {
        historyBox.removeAllViews();
        SharedPreferences sp = getSharedPreferences("snapflow_history", MODE_PRIVATE);
        String items = sp.getString("items", "");
        if (items.trim().isEmpty()) {
            TextView t = new TextView(this);
            t.setText(getString(R.string.history_empty));
            t.setTextColor(0xFF64748B);
            t.setTextSize(13);
            historyBox.addView(t);
            return;
        }
        for (String line : items.split("\n")) {
            final String[] p = line.split("\\|\\|\\|", -1);
            if (p.length < 3) continue;
            TextView t = new TextView(this);
            t.setText("• [" + platformName(p[0]) + "] " + p[1]);
            t.setTextColor(0xFF0F172A);
            t.setTextSize(13);
            t.setPadding(0, 8, 0, 8);
            final String url = p[2];
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    linkInput.setText(url);
                    fetchLinks();
                }
            });
            historyBox.addView(t);
        }
    }

    // ---------- Mang: goi truc tiep API giai ma ----------
    private String fetchText(String urlStr, int timeoutMs) throws Exception {
        HttpURLConnection c = null;
        try {
            URL url = new URL(urlStr);
            c = (HttpURLConnection) url.openConnection();
            c.setInstanceFollowRedirects(true);
            c.setConnectTimeout(timeoutMs);
            c.setReadTimeout(timeoutMs);
            c.setRequestMethod("GET");
            c.setRequestProperty("User-Agent", UA);
            c.setRequestProperty("Accept", "text/html,application/json,*/*");
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) throw new Exception("May chu tra loi HTTP " + code + ".");
            InputStream in = c.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private Bitmap loadBitmap(String urlStr) {
        HttpURLConnection c = null;
        try {
            URL url = new URL(urlStr);
            c = (HttpURLConnection) url.openConnection();
            c.setInstanceFollowRedirects(true);
            c.setConnectTimeout(12000);
            c.setReadTimeout(12000);
            c.setRequestProperty("User-Agent", UA);
            InputStream in = c.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            byte[] bytes = out.toByteArray();
            if (bytes.length == 0 || bytes.length > 5000000) return null;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private String expandShortLink(String url) {
        try {
            URL u = new URL(url);
            String host = u.getHost().toLowerCase();
            if (!host.equals("vt.tiktok.com") && !host.equals("vm.tiktok.com") && !host.equals("t.tiktok.com")) return url;
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setInstanceFollowRedirects(true);
            c.setConnectTimeout(12000);
            c.setReadTimeout(12000);
            c.setRequestMethod("GET");
            c.setRequestProperty("User-Agent", UA);
            c.getResponseCode();
            String fin = c.getURL().toString();
            c.disconnect();
            return fin != null && !fin.equals(url) ? fin : url;
        } catch (Exception e) {
            return url;
        }
    }

    private List<String> extractUrls(String text) {
        List<String> out = new ArrayList<String>();
        if (text == null) return out;
        Matcher m = Pattern.compile("https?://\\S+").matcher(text);
        while (m.find()) {
            String u = m.group().replaceAll("[).,;!?\\u2026]+$", "");
            if (!out.contains(u)) out.add(u);
        }
        return out;
    }

    private String detectPlatform(String raw) {
        String u = raw.toLowerCase();
        if (u.contains("tiktok.com") || u.contains("douyin.com")) return "tiktok";
        if (u.contains("instagram.com")) return "instagram";
        if (u.contains("facebook.com") || u.contains("fb.watch") || u.contains("fb.com")) return "facebook";
        if (u.contains("twitter.com") || Pattern.compile("(^|\\.)x\\.com").matcher(u).find()) return "x";
        if (u.contains("youtube.com") || u.contains("youtu.be")) return "youtube";
        if (Pattern.compile("\\.(mp4|mov|webm|jpg|jpeg|png|webp)(\\?|#|$)").matcher(u).find()) return "direct";
        return "unknown";
    }

    private Resolved resolveOne(String raw) throws Exception {
        String url = expandShortLink(raw);
        String platform = detectPlatform(url);
        if ("unknown".equals(platform)) {
            throw new Exception("Chua nhan dien duoc link nay. App ho tro link TikTok, Facebook, Instagram, X, YouTube va link file .mp4/.jpg truc tiep.");
        }
        Resolved r;
        if ("tiktok".equals(platform)) r = resolveTikTok(url);
        else if ("x".equals(platform)) r = resolveX(url);
        else if ("direct".equals(platform)) r = resolveDirect(url);
        else r = resolveOpenGraph(url, platform);
        r.platform = platform;
        r.sourceUrl = url;
        return r;
    }

    private static String js(JSONObject o, String key) {
        if (o == null) return "";
        String v = o.optString(key, "");
        return v == null ? "" : v;
    }

    private static Long jn(JSONObject o, String key) {
        if (o == null || o.isNull(key)) return null;
        try { return o.getLong(key); } catch (Exception e) { return null; }
    }

    private Resolved resolveTikTok(String url) throws Exception {
        String api = "https://www.tikwm.com/api/?url=" + Uri.encode(url);
        JSONObject json = new JSONObject(fetchText(api, 20000));
        if (json.optInt("code", -1) != 0) {
            throw new Exception("TikTok khong tra ve video cho link nay. Link co the da bi xoa, de o che do rieng tu, hoac link rut gon da het han — hay mo video trong TikTok, bam Chia se → Sao chep lien ket roi thu lai.");
        }
        JSONObject d = json.optJSONObject("data");
        if (d == null) throw new Exception("Khong doc duoc du lieu video TikTok.");
        JSONObject author = d.optJSONObject("author");
        Resolved r = new Resolved();
        r.title = js(d, "title").isEmpty() ? "Video TikTok" : js(d, "title");
        r.authorName = author != null ? js(author, "nickname") : "";
        String uid = author != null ? js(author, "unique_id") : "";
        r.authorHandle = uid.isEmpty() ? "" : "@" + uid;
        String hd = js(d, "hdplay");
        String play = js(d, "play");
        String wm = js(d, "wmplay");
        if (!hd.isEmpty()) {
            r.options.add(new Opt("hd", "video", "Video HD — Khong logo", "Chat luong cao nhat TikTok cung cap, sac net, khong dinh watermark", hd, true));
        }
        if (!play.isEmpty() && !play.equals(hd)) {
            r.options.add(new Opt("sd", "video", hd.isEmpty() ? "Video — Khong logo" : "Video thuong — Khong logo", "Ban khong watermark, tai nhanh hon ban HD", play, true));
        }
        JSONArray images = d.optJSONArray("images");
        if (images != null) {
            for (int i = 0; i < images.length(); i++) {
                String img = images.optString(i, "");
                if (!img.isEmpty()) {
                    r.options.add(new Opt("img-" + i, "image", "Anh " + (i + 1) + " — Anh goc", "Anh trong bai dang dang truot (photo post)", img, true));
                }
            }
        }
        String music = js(d, "music");
        if (!music.isEmpty()) {
            JSONObject mi = d.optJSONObject("music_info");
            String mt = mi != null ? js(mi, "title") : "";
            r.options.add(new Opt("mp3", "audio", "Nhac nen — MP3", mt.isEmpty() ? "Tai rieng phan am thanh cua video" : mt, music, true));
        }
        if (!wm.isEmpty() && !wm.equals(play) && !wm.equals(hd)) {
            r.options.add(new Opt("wm", "video", "Video — Co logo (du phong)", "Chi dung khi cac ban khong logo o tren bi loi", wm, false));
        }
        if (r.options.isEmpty()) throw new Exception("Video nay khong co file tai xuong cong khai.");
        r.kind = (images != null && images.length() > 0) ? "photo" : "video";
        String cover = js(d, "origin_cover");
        r.coverUrl = cover.isEmpty() ? js(d, "cover") : cover;
        r.durationSec = jn(d, "duration");
        r.playCount = jn(d, "play_count");
        r.likeCount = jn(d, "digg_count");
        r.commentCount = jn(d, "comment_count");
        r.shareCount = jn(d, "share_count");
        return r;
    }

    private Resolved resolveX(String url) throws Exception {
        Matcher m = Pattern.compile("(?:twitter\\.com|x\\.com)/([^/]+)/status/(\\d+)", Pattern.CASE_INSENSITIVE).matcher(url);
        if (!m.find()) {
            throw new Exception("Link X chua dung dang bai dang. Hay sao chep link dang x.com/<ten>/status/<so>.");
        }
        String user = m.group(1);
        String id = m.group(2);
        JSONObject json = new JSONObject(fetchText("https://api.fxtwitter.com/" + user + "/status/" + id, 20000));
        JSONObject tweet = json.optJSONObject("tweet");
        if (tweet == null) {
            throw new Exception("Khong lay duoc bai dang X nay. Bai co the da bi xoa, tai khoan de rieng tu, hoac X dang gioi han truy cap — thu lai sau it phut.");
        }
        JSONObject author = tweet.optJSONObject("author");
        JSONObject media = tweet.optJSONObject("media");
        Resolved r = new Resolved();
        r.title = js(tweet, "text").isEmpty() ? "Bai dang X" : js(tweet, "text");
        r.authorName = author != null ? js(author, "name") : "";
        String sn = author != null ? js(author, "screen_name") : "";
        r.authorHandle = sn.isEmpty() ? "" : "@" + sn;
        r.playCount = jn(tweet, "views");
        r.likeCount = jn(tweet, "likes");
        r.commentCount = jn(tweet, "replies");
        r.shareCount = jn(tweet, "retweets");
        JSONArray videos = media != null ? media.optJSONArray("videos") : null;
        String thumb = "";
        if (videos != null) {
            for (int vi = 0; vi < videos.length(); vi++) {
                JSONObject vr = videos.optJSONObject(vi);
                if (vr == null) continue;
                if (vi == 0) thumb = js(vr, "thumbnail_url");
                String bestUrl = js(vr, "url");
                long bestBitrate = -1;
                JSONArray variants = vr.optJSONArray("variants");
                if (variants != null) {
                    for (int i = 0; i < variants.length(); i++) {
                        JSONObject c = variants.optJSONObject(i);
                        if (c == null) continue;
                        long br = jn(c, "bitrate") == null ? 0 : jn(c, "bitrate");
                        String cu = js(c, "url");
                        if (!cu.isEmpty() && br > bestBitrate) { bestBitrate = br; bestUrl = cu; }
                    }
                }
                if (!bestUrl.isEmpty()) {
                    r.options.add(new Opt("vid-" + vi, "video", videos.length() > 1 ? "Video " + (vi + 1) + " — Chat luong cao nhat" : "Video — Chat luong cao nhat", "File goc tu X, khong qua nen lai", bestUrl, true));
                }
            }
        }
        JSONArray photos = media != null ? media.optJSONArray("photos") : null;
        if (photos != null) {
            for (int pi = 0; pi < photos.length(); pi++) {
                JSONObject pr = photos.optJSONObject(pi);
                String pu = pr != null ? js(pr, "url") : "";
                if (!pu.isEmpty()) {
                    r.options.add(new Opt("img-" + pi, "image", "Anh " + (pi + 1) + " — Anh goc", "Anh dinh kem trong bai dang", pu + "?name=orig", true));
                    if (thumb.isEmpty()) thumb = pu;
                }
            }
        }
        if (r.options.isEmpty()) throw new Exception("Bai dang X nay khong co video hoac anh dinh kem de tai.");
        r.kind = (videos != null && videos.length() > 0) ? "video" : "photo";
        r.coverUrl = thumb;
        return r;
    }

    private String metaContent(String html, String prop) {
        Pattern p1 = Pattern.compile("<meta[^>]+(?:property|name)=[\"']" + Pattern.quote(prop) + "[\"'][^>]+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(html);
        if (m1.find()) return m1.group(1).replace("&amp;", "&");
        Pattern p2 = Pattern.compile("<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+(?:property|name)=[\"']" + Pattern.quote(prop) + "[\"']", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(html);
        if (m2.find()) return m2.group(1).replace("&amp;", "&");
        return "";
    }

    private boolean looksLikeMediaFile(String u) {
        if (u == null || u.isEmpty()) return false;
        String l = u.toLowerCase();
        if (Pattern.compile("\\.(mp4|mov|webm|m3u8)(\\?|#|$)").matcher(l).find()) return true;
        return l.contains("fbcdn") || l.contains("cdninstagram") || l.contains("scontent")
                || l.contains("googlevideo") || l.contains("videoplayback") || l.contains("video.twimg");
    }

    private Resolved resolveOpenGraph(String url, String platform) throws Exception {
        String name = "facebook".equals(platform) ? "Facebook" : "instagram".equals(platform) ? "Instagram" : "YouTube";
        String html;
        try {
            html = fetchText(url, 15000);
        } catch (Exception e) {
            throw new Exception(name + " dang chan lay du lieu truc tiep. Hay chac chan bai dang o che do cong khai, hoac thu lai sau it phut.");
        }
        String ogVideo = metaContent(html, "og:video:secure_url");
        if (ogVideo.isEmpty()) ogVideo = metaContent(html, "og:video");
        String imageUrl = metaContent(html, "og:image");
        String title = metaContent(html, "og:title");
        if (title.isEmpty()) title = "Bai dang " + name;
        String videoUrl = "";
        if (!ogVideo.isEmpty() && looksLikeMediaFile(ogVideo)
                && !Pattern.compile("/(embed|watch)(/|\\?|#|$)").matcher(ogVideo).find()) {
            videoUrl = ogVideo;
        }
        if ("youtube".equals(platform) && videoUrl.isEmpty()) {
            throw new Exception("YouTube khong cong khai file video tai truc tiep cho link nay (chi co trang xem nhung, tai ve se la file hong mo khong len). Hay thu link TikTok / X, hoac mo video tren YouTube de xem truc tiep.");
        }
        Resolved r = new Resolved();
        r.title = title;
        if (!videoUrl.isEmpty()) {
            r.options.add(new Opt("og-video", "video", "Video — File goc", "File video cong khai ma " + name + " cung cap cho bai dang nay", videoUrl, true));
        }
        if (!imageUrl.isEmpty()) {
            r.options.add(new Opt("og-image", "image", videoUrl.isEmpty() ? "Anh — Anh goc" : "Anh bia cua video", "Anh cong khai cua bai dang", imageUrl, true));
        }
        if (r.options.isEmpty()) {
            throw new Exception(name + " khong cong khai file tai cho link nay (thuong gap voi Reel/video yeu cau dang nhap hoac bai rieng tu). Meo: mo bai dang, kiem tra dang o che do Cong khai roi sao chep lai link va thu lai.");
        }
        r.kind = videoUrl.isEmpty() ? "photo" : "video";
        r.coverUrl = imageUrl;
        return r;
    }

    private Resolved resolveDirect(String url) {
        boolean isImg = Pattern.compile("\\.(jpg|jpeg|png|webp)(\\?|#|$)", Pattern.CASE_INSENSITIVE).matcher(url).find();
        Resolved r = new Resolved();
        String name = url;
        try {
            String path = new URL(url).getPath();
            String[] seg = path.split("/");
            String last = seg.length > 0 ? seg[seg.length - 1] : "";
            if (!last.isEmpty()) name = Uri.decode(last);
        } catch (Exception ignored) {}
        r.title = name;
        r.kind = isImg ? "photo" : "video";
        r.coverUrl = isImg ? url : "";
        r.options.add(new Opt("file", isImg ? "image" : "video", isImg ? "Anh — File goc" : "Video — File goc", "Link file truc tiep ban da dan", url, true));
        return r;
    }

    private String extractLink(Intent intent) {
        if (intent == null) return null;
        String text = null;
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            text = intent.getStringExtra(Intent.EXTRA_TEXT);
        } else if (intent.getData() != null) {
            text = intent.getData().toString();
        }
        if (text == null) return null;
        Matcher m = Pattern.compile("https?://\\S+").matcher(text);
        return m.find() ? m.group() : null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String link = extractLink(intent);
        if (link != null) {
            linkInput.setText(link);
            fetchLinks();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
