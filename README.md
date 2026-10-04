# زين أونلاين — مشروع Android قابل للتطوير

## المحتويات
- `project/`: مشروع Android Studio كامل.
- `project/branding/`: الصورة الأصلية وصورة معاينة الأيقونة.
- `project/app/src/main/res/mipmap-*`: أيقونة التطبيق الجديدة.
- `apk/`: آخر APK قابل للتثبيت.

## التشغيل والتطوير
1. افتح مجلد `project/` في Android Studio.
2. استبدل `project/app/google-services.json` بملف Firebase الحقيقي عند استخدام Google/Firebase.
3. عدّل الواجهة في `project/app/src/main/assets/` أو كود Kotlin في `project/app/src/main/java/`.
4. ابنِ التطبيق عبر `./gradlew assembleDebug`.

## ملاحظات
- الأيقونة مبنية من الصورة المرفقة.
- نسخة APK الحالية Debug وموقعة بمفتاح debug للتجربة.
- مشاركة WhatsApp قد تفتح WhatsApp دون إنترنت، لكن الإرسال الفعلي يحتاج اتصال WhatsApp.
