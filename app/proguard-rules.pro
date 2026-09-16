# Giu ten cac service duoc khai bao trong manifest, vi he thong goi qua ten lop.
-keep class vn.huytl.homeworkgate.guard.GuardAccessibilityService { *; }
-keep class vn.huytl.homeworkgate.telegram.ApprovalService { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
