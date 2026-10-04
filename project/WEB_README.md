# زين أونلاين على XAMPP وSQL Server 2014

تم الإبقاء على الواجهة والحقول والشاشات الموجودة في `index.html` كما هي، وإضافة طبقة خادم تحفظ بيانات التطبيق في SQL Server بدل الاعتماد على `localStorage` وحده.

## التشغيل المحلي

1. انسخ المجلد إلى `C:\xampp\htdocs\zanwn-online`.
2. شغّل Apache من لوحة XAMPP.
3. ثبّت Microsoft Drivers for PHP for SQL Server المتوافق مع إصدار PHP الموجود في XAMPP، ثم فعّل `php_sqlsrv.dll` و`php_pdo_sqlsrv.dll` من `php.ini`.
4. نفّذ [schema.sql](./schema.sql) على SQL Server 2014.
5. عدّل [config.php](./config.php)، أو عرّف المتغيرات `ZANWN_SQL_SERVER` و`ZANWN_SQL_DATABASE` و`ZANWN_SQL_USER` و`ZANWN_SQL_PASSWORD`.
6. افتح `http://localhost/zanwn-online/index.html`.

عند تعذر الاتصال، تبقى الواجهة قابلة للاستخدام بالبيانات المحلية ويظهر تنبيه واضح، ولا يتم حذف بيانات المتصفح. بعد نجاح الاتصال تُحمّل بيانات الحساب وتُرسل التغييرات تلقائيًا إلى `dbo.AppSnapshots`.

## ملاحظة

تستخدم طبقة التوافق الحالية لقطة JSON حتى لا تتغير بنية الواجهة الحالية أو حساباتها. يمكن لاحقًا ترحيل الجداول إلى جداول علائقية مستقلة مع إبقاء نفس الواجهة وواجهة API.
