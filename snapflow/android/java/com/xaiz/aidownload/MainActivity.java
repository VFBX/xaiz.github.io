package com.xaiz.aidownload;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.util.Base64;
import java.io.File;
import java.io.FileOutputStream;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    public static final String APP_URL = "https://muse.ai/library/artifacts/app-tai-video-qua-link";
    private WebView webView;
    private ProgressBar progress;
    private View errorView;
    private String pendingLink;
    private ValueCallback<Uri[]> fileChooserCallback;
    private FileOutputStream blobOut;
    private File blobFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);
        progress = findViewById(R.id.progress);
        errorView = findViewById(R.id.error_view);
        View headerBar = findViewById(R.id.header_bar);
        headerBar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { Toast.makeText(MainActivity.this, getString(R.string.about), Toast.LENGTH_LONG).show(); }
        });
        Button retry = findViewById(R.id.retry_btn);
        retry.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { loadApp(); }
        });
        Button openBrowser = findViewById(R.id.browser_btn);
        openBrowser.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(APP_URL))); } catch (Exception ignored) {}
            }
        });
        pendingLink = extractLink(getIntent());

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }


        Object downloadBridge = new Object() {
            @JavascriptInterface
            public void download(String url, String filename) {
                try {
                    DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                    req.addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36");
                    req.setDescription("SnapFlow AI");
                    if (filename == null || filename.isEmpty()) filename = URLUtil.guessFileName(url, null, null);
                    req.setTitle(filename);
                    req.allowScanningByMediaScanner();
                    req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SnapFlow AI/" + filename);
                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    dm.enqueue(req);
                    runOnUiThread(new Runnable() { @Override public void run() { Toast.makeText(MainActivity.this, getString(R.string.download_started), Toast.LENGTH_LONG).show(); } });
                } catch (Exception e) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
                }
            }
            @JavascriptInterface
            public void startBlobDownload(String filename, long size) {
                try {
                    if (blobOut != null) { try { blobOut.close(); } catch (Exception ignored) {} }
                    File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SnapFlow AI");
                    if (!dir.exists()) dir.mkdirs();
                    if (filename == null || filename.isEmpty()) filename = "download.mp4";
                    blobFile = new File(dir, filename);
                    blobOut = new FileOutputStream(blobFile, false);
                } catch (Exception ignored) {}
            }
            @JavascriptInterface
            public void writeBlobChunk(String base64) {
                try {
                    if (blobOut != null && base64 != null) blobOut.write(Base64.decode(base64, Base64.DEFAULT));
                } catch (Exception ignored) {}
            }
            @JavascriptInterface
            public void finishBlobDownload() {
                try {
                    if (blobOut != null) { blobOut.flush(); blobOut.close(); blobOut = null; }
                    if (blobFile != null) {
                        Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        scan.setData(Uri.fromFile(blobFile));
                        sendBroadcast(scan);
                    }
                    runOnUiThread(new Runnable() { @Override public void run() { Toast.makeText(MainActivity.this, "Da luu: " + (blobFile != null ? blobFile.getName() : ""), Toast.LENGTH_LONG).show(); } });
                } catch (Exception ignored) {}
            }
        };
        webView.addJavascriptInterface(downloadBridge, "AIDownload");
        webView.addJavascriptInterface(downloadBridge, "SnapFlow");
        webView.addJavascriptInterface(downloadBridge, "SnapFlowStudio");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Keep Muse + download hosts inside the app; open everything else in browser.
                if (url.startsWith("https://muse.ai/") || url.contains("app-tai-video")) {
                    return false;
                }
                if (url.startsWith("https://") || url.startsWith("http://")) {
                    // Let file hosts (CDN video/mp3/image) load inside WebView so the
                    // DownloadListener can catch them; external social links open outside.
                    if (url.contains("tiktok") || url.contains("tiktokcdn") || url.contains("fbcdn") || url.contains("cdninstagram")
                            || url.contains("instagram") || url.contains("twimg") || url.contains("googlevideo") || url.contains("youtube")
                            || url.contains("youtu.be") || url.contains("x.com") || url.contains("twitter")
                            || url.contains("tiktokwm") || url.contains("tikwm") || url.contains("snapcdn")) {
                        return false;
                    }
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
                    return true;
                }
                return false;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                errorView.setVisibility(View.GONE);
                view.evaluateJavascript("(function(){if(window.__aiDlInstalled)return;window.__aiDlInstalled=true;var blobs={};var origCreate=URL.createObjectURL.bind(URL);URL.createObjectURL=function(b){var u=origCreate(b);blobs[u]=b;return u;};var origClick=HTMLAnchorElement.prototype.click;HTMLAnchorElement.prototype.click=function(){var href=this.href||'';if(href.indexOf('blob:')===0&&blobs[href]){var blob=blobs[href];var name=this.download||'download.mp4';var reader=new FileReader();reader.onload=function(){var b64=reader.result.split(',')[1];try{window.AIDownload.startBlobDownload(name,blob.size);}catch(e){}var CH=262144;for(var i=0;i<b64.length;i+=CH){try{window.AIDownload.writeBlobChunk(b64.substring(i,i+CH));}catch(e){}}try{window.AIDownload.finishBlobDownload();}catch(e){}};reader.readAsDataURL(blob);return;}origClick.call(this);};})();", null);
                if (pendingLink != null) {
                    String link = pendingLink.replace("'", "\\'");
                    String js = "(function(){var t=document.querySelector('textarea');"
                            + "if(t){t.value='" + link + "';"
                            + "t.dispatchEvent(new Event('input',{bubbles:true}));"
                            + "t.focus();}})();";
                    view.evaluateJavascript(js, null);
                    pendingLink = null;
                }
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    errorView.setVisibility(View.VISIBLE);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int p) {
                if (p < 100) {
                    progress.setVisibility(View.VISIBLE);
                    progress.setProgress(p);
                } else {
                    progress.setVisibility(View.GONE);
                }
            }
            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(Intent.createChooser(i, "Chọn file"), 1001);
                return true;
            }
        });

        webView.setDownloadListener(new android.webkit.DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                try {
                    DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                    req.setMimeType(mimetype);
                    String cookies = CookieManager.getInstance().getCookie(url);
                    if (cookies != null) req.addRequestHeader("cookie", cookies);
                    req.addRequestHeader("User-Agent", userAgent);
                    req.setDescription("SnapFlow AI");
                    String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                    req.setTitle(fileName);
                    req.allowScanningByMediaScanner();
                    req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SnapFlow AI/" + fileName);
                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    dm.enqueue(req);
                    Toast.makeText(MainActivity.this, getString(R.string.download_started), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
                }
            }
        });

        if (savedInstanceState == null) loadApp();
    }

    private void loadApp() {
        errorView.setVisibility(View.GONE);
        webView.loadUrl(APP_URL);
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
            pendingLink = link;
            loadApp();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && fileChooserCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                result = new Uri[]{data.getData()};
            }
            fileChooserCallback.onReceiveValue(result);
            fileChooserCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
