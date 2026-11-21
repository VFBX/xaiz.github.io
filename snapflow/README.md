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

## App Android — SnapFlow AI v1.3 (bản chính)

- APK: `SnapFlow-AI-v1.3.apk` — 37 KB, đã ký, package `com.xaiz.aidownload`, versionCode 4 (file APK đính kèm trong chat với Muse; repo này chỉ chứa mã nguồn, chưa chứa file APK vì GitHub không nhận file nhị phân qua công cụ đẩy hiện tại)
- Cài đè lên AI Download v1.1 / SnapFlow v1.2 không mất dữ liệu
- Mở web app trong WebView, cầu nối `SnapFlow` / `SnapFlowStudio` / `AIDownload` tải file thật về `Downloads / SnapFlow AI`
- Tải hàng loạt tối đa 20 link, xem trước, lịch sử + yêu thích
- Source Android trong thư mục `android/` (Java gốc, build bằng `build.sh` với aapt2 + javac + d8, không cần Gradle; icon launcher PNG không kèm trong repo này)

## Lưu ý

- Chỉ lấy được bài đăng công khai. Facebook / Instagram / YouTube đôi khi bị chặn, cần đăng nhập.
- Link tải trực tiếp hết hạn nhanh — lỗi thì lấy lại link mới.
- Chỉ tải nội dung bạn có quyền sử dụng, tôn trọng bản quyền của người sáng tạo.

© SnapFlow Studio — SnapFlow AI
