# زين أونلاين — Android Native

هذا المشروع يحوّل واجهة `zanwn-online` الأصلية إلى تطبيق Android يعمل بنمط **Offline First** مع الحفاظ على `index.html` والواجهة الحالية.

## ما تم تضمينه

- WebView محلي يحمّل الواجهة من `app/src/main/assets/`.
- `AndroidBridge` باسم `Android` لربط JavaScript بـ Kotlin.
- Room Database بجداول `products`, `sales`, `customers`, `expenses`, `sync_queue`.
- طابور مزامنة محلي لا يفقد التعديلات عند انقطاع الإنترنت.
- Firebase Authentication عبر Google ID token وFirestore.
- مزامنة دورية كل 60 ثانية، مع تحديث `updatedAt`.
- دعم مشاركة النص عبر WhatsApp وSMS والمشاركة العامة.
- حماية PIN باستخدام `EncryptedSharedPreferences` وكشف توفر البصمة عبر `SecurityManager`.
- ملف `android-bridge.js` يزامن حفظ الواجهة الأصلية مع التخزين الأصلي، دون إعادة تصميم الشاشات.

## فتح وبناء المشروع

1. افتح مجلد `zanwn-android` في Android Studio Hedgehog أو أحدث.
2. انتظر Gradle Sync.
3. استبدل `app/google-services.json` بملف Firebase الحقيقي من مشروعك.
4. في Firebase فعّل Google Sign-In وCloud Firestore، ثم أضف قواعد Firestore المناسبة.
5. من شاشة النسخ السحابي اضغط **ربط حساب Google**، اختر الحساب، ثم اضغط **مزامنة الآن**.
5. شغّل التطبيق على جهاز Android API 26 أو أحدث.

ملف `app/google-services.json` المرفق **تجريبي فقط** ولا يحتوي مفاتيح صالحة. لن تعمل مصادقة Google أو Firestore فعليًا حتى يتم استبداله. قبل الربط لا يرفع التطبيق أي بيانات، وبعد الفصل تبقى البيانات المحلية محفوظة.

## ربط JavaScript بالجسر

```javascript
if (window.Android && Android.isNativeApp()) {
  Android.saveSnapshot(JSON.stringify(snapshot));
  Android.shareWhatsApp(text);
  Android.shareSms(text);
  Android.syncNow();
}
```

## ملاحظات مهمة

- لم يتم حذف أو إعادة تصميم عناصر `index.html`؛ أضيف مرجع واحد فقط إلى `android-bridge.js`.
- PHP وSQL Server بقيا في النسخة الأصلية للويب، لكن التطبيق الأصلي يعتمد على Room محليًا وFirestore للمزامنة.
- إخراج APK يتطلب Android SDK وGradle/Android Studio، وهي غير مثبتة في بيئة إنشاء الملفات الحالية.
