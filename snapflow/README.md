# SnapFlow AI — by SnapFlow Studio

Tải video qua link: TikTok, Facebook, Instagram, X, YouTube Shorts.
Video không logo, HD sắc nét.

## Web — chạy ngay trên trình duyệt

Mở: **https://vfbx.github.io/xaiz.github.io/snapflow/**

Bản web tĩnh (1 file `index.html`, trắng-xanh, gọn gàng):
- **TikTok:** lấy trực tiếp qua API tikwm — video không logo, bản có logo, nhạc nền MP3, bài ảnh từng ảnh gốc, xem trước trong trang.
- **X (Twitter):** lấy qua API fxtwitter — video chất lượng cao nhất + ảnh gốc.
- **Link file trực tiếp** `.mp4` / `.jpg` / `.png` / `.mp3`: tải thẳng.
- **Facebook / Instagram / YouTube:** bản web tĩnh không trích xuất trực tiếp được (cần đăng nhập hoặc máy chủ) — dùng app Android bên dưới.

## App Android — SnapFlow AI v2.0 NATIVE (bản chính, thay v1.3)

- APK: `SnapFlow-AI-v2.0.apk` — 45 KB, đã ký, package `com.xaiz.aidownload`, versionCode 5 (file APK đính kèm trong chat với Muse; repo này chỉ chứa mã nguồn, chưa chứa file APK vì GitHub không nhận file nhị phân qua công cụ đẩy hiện tại)
- Cài đè lên v1.3 / v1.2 / AI Download v1.1 không mất dữ liệu
- **NATIVE hoàn toàn bằng Java thuần, KHÔNG WebView, KHÔNG mở Muse/Chrome:** gọi thẳng TikWM (TikTok) + FxTwitter (X) + OpenGraph (Facebook/Instagram/YouTube) + link file trực tiếp, tải bằng DownloadManager về `Downloads / SnapFlow AI`
- Tải hàng loạt (dán nhiều link, lấy lần lượt), xem trước, lịch sử lưu trên máy, Chia sẻ link từ TikTok/Facebook/Instagram/X sang app là tự điền + tự lấy
- Source v2.0 trong thư mục `android-native/`; source v1.3 (bản WebView cũ) còn trong `android/` (Java gốc, build bằng `build.sh` với aapt2 + javac + d8, không cần Gradle; icon launcher PNG không kèm trong repo này)

## Lưu ý

- Chỉ lấy được bài đăng công khai. Facebook / Instagram / YouTube đôi khi bị chặn, cần đăng nhập.
- Link tải trực tiếp hết hạn nhanh — lỗi thì lấy lại link mới.
- Chỉ tải nội dung bạn có quyền sử dụng, tôn trọng bản quyền của người sáng tạo.

© SnapFlow Studio — SnapFlow AI
