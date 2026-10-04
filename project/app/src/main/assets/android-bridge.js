(function () {
  'use strict';
  if (!window.Android || !Android.isNativeApp()) return;
  function snapshot() {
    var keys = ['products','sales','returns','customerDebts','customerPhones','expenses','receipts','auditLogs','users','appSettings'];
    var out = { settings: {} };
    keys.forEach(function (key) {
      var raw = localStorage.getItem('zo_' + key);
      if (raw !== null) { try { out[key] = JSON.parse(raw); } catch (_) { out[key] = raw; } }
    });
    ['storeName','discountLimit','alerts','currency'].forEach(function (key) { out.settings[key] = localStorage.getItem('zo_' + key) || ''; });
    return out;
  }
  function restore() {
    try {
      var data = JSON.parse(Android.loadSnapshot() || '{}');
      Object.keys(data).forEach(function (key) {
        if (key === 'settings') return;
        localStorage.setItem('zo_' + key, JSON.stringify(data[key]));
      });
      Object.keys(data.settings || {}).forEach(function (key) { localStorage.setItem('zo_' + key, data.settings[key] || ''); });
    } catch (_) {}
  }
  restore();
  window.addEventListener('load', function () {
    var original = window.saveData;
    if (typeof original === 'function') window.saveData = function () { original.apply(this, arguments); Android.saveSnapshot(JSON.stringify(snapshot())); };
    window.nativeSaveSnapshot = function () { Android.saveSnapshot(JSON.stringify(snapshot())); };
    window.nativeSyncNow = function () { Android.syncNow(); };
    window.onNativeAppLockFailed = function () {
      alert('يجب التحقق بالبصمة أو إدخال رمز قفل الهاتف لفتح التطبيق.');
      setTimeout(function () { Android.authenticateAppLock(); }, 250);
    };
    Android.appReady(localStorage.getItem('zo_isLoggedIn') === 'true');
  });
}());
