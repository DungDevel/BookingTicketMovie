package com.example.movieapp.Utils

/**
 * Cấu hình cho tính năng Đăng nhập bằng Google.
 *
 * QUAN TRỌNG: Bạn PHẢI thay giá trị WEB_CLIENT_ID bên dưới bằng "Web Client ID"
 * (OAuth 2.0 Client ID loại "Web application") lấy từ Google Cloud Console:
 *   1. Vào https://console.cloud.google.com/apis/credentials (đúng project đang dùng cho app)
 *   2. Tạo OAuth Client ID loại "Android" — điền package name (com.example.movieapp) và
 *      SHA-1 fingerprint (lấy bằng lệnh `./gradlew signingReport`). Bước này KHÔNG cho ra
 *      chuỗi ID cần dán ở đây, nhưng BẮT BUỘC phải tạo để Google nhận diện app.
 *   3. Tạo thêm 1 OAuth Client ID loại "Web application" (không cần điền gì đặc biệt).
 *      Copy Client ID của mục này (dạng "xxxx-xxxx.apps.googleusercontent.com") và dán vào
 *      WEB_CLIENT_ID bên dưới.
 *   4. Dán ĐÚNG chuỗi Web Client ID này vào biến môi trường GOOGLE_CLIENT_ID của backend (.env)
 *      — hai bên phải khớp nhau thì xác thực idToken mới thành công.
 */
object GoogleAuthConfig {
    const val WEB_CLIENT_ID = "535877575057-orhq7uvv6kajbihesqiu4jalpj53gj45.apps.googleusercontent.com"
}