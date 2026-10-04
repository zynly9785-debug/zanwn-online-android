(function () {
    'use strict';

    var endpoint = 'api.php';
    var saveTimer = null;
    var retryTimer = null;
    var syncTimer = null;
    var requestInFlight = false;
    var loadCompleted = false;
    var retryDelay = 1000;
    var lastSnapshotSignature = '';
    var pendingStoragePrefix = 'zo_sync_pending_';
    var syncInterval = 15000;

    function accountKey() {
        return (localStorage.getItem('zo_storeKey') || localStorage.getItem('zo_currentUser') || '').trim();
    }

    function pendingKey(key) {
        return pendingStoragePrefix + key;
    }

    function snapshot() {
        return {
            products: window.products || [],
            sales: window.sales || [],
            returnsList: window.returnsList || [],
            customerDebts: window.customerDebts || {},
            customerPhones: window.customerPhones || {},
            expensesList: window.expensesList || [],
            receiptsList: window.receiptsList || [],
            auditLogs: window.auditLogs || [],
            users: window.users || [],
            settings: {
                storeName: localStorage.getItem('zo_storeName') || '',
                discountLimit: localStorage.getItem('zo_discountLimit') || '10',
                alerts: localStorage.getItem('zo_alerts') || 'yes',
                appSettings: window.appSettings || null
            }
        };
    }

    function signature(data) {
        return JSON.stringify(data);
    }

    function savePending(key, data) {
        if (!key) return;
        localStorage.setItem(pendingKey(key), JSON.stringify(data));
    }

    function readPending(key) {
        if (!key) return null;
        var value = localStorage.getItem(pendingKey(key));
        if (!value) return null;
        try {
            var data = JSON.parse(value);
            return data && typeof data === 'object' ? data : null;
        } catch (error) {
            console.error('[Zanwn SQL sync] Invalid pending snapshot:', error);
            localStorage.removeItem(pendingKey(key));
            return null;
        }
    }

    function clearPending(key, sentSignature) {
        var current = readPending(key);
        if (current && signature(current) === sentSignature) {
            localStorage.removeItem(pendingKey(key));
        }
    }

    function showSyncError(message) {
        console.error('[Zanwn SQL sync]', message);
        var banner = document.getElementById('smartAlertBanner');
        if (banner) {
            banner.textContent = 'تنبيه: تعذر مزامنة البيانات مع قاعدة البيانات. ستتم إعادة المحاولة تلقائياً.';
            banner.style.display = 'block';
        }
    }

    function hideSyncError() {
        var banner = document.getElementById('smartAlertBanner');
        if (banner && banner.textContent.indexOf('تعذر مزامنة') !== -1) {
            banner.style.display = 'none';
        }
    }

    function scheduleRetry() {
        window.clearTimeout(retryTimer);
        retryTimer = window.setTimeout(function () {
            retryTimer = null;
            pushSnapshot();
        }, retryDelay);
        retryDelay = Math.min(retryDelay * 2, 60000);
    }

    function pushSnapshot() {
        var key = accountKey();
        if (!key || requestInFlight) return Promise.resolve();

        var current = snapshot();
        var queued = readPending(key);
        var data = queued || current;
        var dataSignature = signature(data);
        savePending(key, data);
        requestInFlight = true;

        return fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ key: key, snapshot: data })
        }).then(function (response) {
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return response.json();
        }).then(function () {
            clearPending(key, dataSignature);
            retryDelay = 1000;
            hideSyncError();
            if (signature(snapshot()) !== dataSignature) {
                schedulePush();
            }
        }).catch(function (error) {
            showSyncError(error.message);
            scheduleRetry();
        }).then(function () {
            requestInFlight = false;
        });
    }

    function schedulePush() {
        var key = accountKey();
        if (!key || (window.appSettings && window.appSettings.sync && window.appSettings.sync.enabled === false)) return;
        savePending(key, snapshot());
        window.clearTimeout(saveTimer);
        saveTimer = window.setTimeout(function () {
            saveTimer = null;
            pushSnapshot();
        }, 250);
    }

    function applySnapshot(data) {
        if (!data || typeof data !== 'object') return;
        window.products = Array.isArray(data.products) ? data.products : [];
        window.sales = Array.isArray(data.sales) ? data.sales : [];
        window.returnsList = Array.isArray(data.returnsList) ? data.returnsList : [];
        window.customerDebts = data.customerDebts && typeof data.customerDebts === 'object' ? data.customerDebts : {};
        window.customerPhones = data.customerPhones && typeof data.customerPhones === 'object' ? data.customerPhones : {};
        window.expensesList = Array.isArray(data.expensesList) ? data.expensesList : [];
        window.receiptsList = Array.isArray(data.receiptsList) ? data.receiptsList : [];
        window.auditLogs = Array.isArray(data.auditLogs) ? data.auditLogs : [];
        window.users = Array.isArray(data.users) ? data.users : [];
        localStorage.setItem('zo_products', JSON.stringify(window.products));
        localStorage.setItem('zo_sales', JSON.stringify(window.sales));
        localStorage.setItem('zo_returns', JSON.stringify(window.returnsList));
        localStorage.setItem('zo_customerDebts', JSON.stringify(window.customerDebts));
        localStorage.setItem('zo_customerPhones', JSON.stringify(window.customerPhones));
        localStorage.setItem('zo_expenses', JSON.stringify(window.expensesList));
        localStorage.setItem('zo_receipts', JSON.stringify(window.receiptsList));
        localStorage.setItem('zo_auditLogs', JSON.stringify(window.auditLogs));
        localStorage.setItem('zo_users', JSON.stringify(window.users));
        var settings = data.settings || {};
        if (Object.prototype.hasOwnProperty.call(settings, 'storeName')) localStorage.setItem('zo_storeName', settings.storeName || '');
        if (Object.prototype.hasOwnProperty.call(settings, 'discountLimit')) localStorage.setItem('zo_discountLimit', settings.discountLimit || '10');
        if (Object.prototype.hasOwnProperty.call(settings, 'alerts')) localStorage.setItem('zo_alerts', settings.alerts || 'yes');
        if (settings.appSettings && typeof settings.appSettings === 'object') {
            if (typeof window.applySyncedAppSettings === 'function') {
                window.applySyncedAppSettings(settings.appSettings);
            } else {
                localStorage.setItem('zo_appSettings', JSON.stringify(settings.appSettings));
            }
        }
    }

    function loadSnapshot() {
        var key = accountKey();
        if (!key || requestInFlight) return Promise.resolve();
        if (readPending(key)) {
            return pushSnapshot();
        }
        requestInFlight = true;
        return fetch(endpoint + '?key=' + encodeURIComponent(key), { cache: 'no-store' })
            .then(function (response) {
                if (!response.ok) throw new Error('HTTP ' + response.status);
                return response.json();
            })
            .then(function (result) {
                if (result.found) {
                    applySnapshot(result.snapshot);
                    if (typeof window.updateUI === 'function') window.updateUI();
                } else {
                    savePending(key, snapshot());
                    requestInFlight = false;
                    return pushSnapshot();
                }
                hideSyncError();
            })
            .catch(function (error) {
                showSyncError(error.message);
                scheduleRetry();
            })
            .then(function () {
                requestInFlight = false;
                loadCompleted = true;
            });
    }

    function periodicSync() {
        var key = accountKey();
        if (!key || !loadCompleted) return;
        var currentSignature = signature(snapshot());
        if (currentSignature !== lastSnapshotSignature) {
            lastSnapshotSignature = currentSignature;
            schedulePush();
        } else if (readPending(key)) {
            pushSnapshot();
        }
    }

    function configureSync() {
        if (syncTimer) window.clearInterval(syncTimer);
        var seconds = window.appSettings && window.appSettings.sync ? Number(window.appSettings.sync.intervalSeconds) : 15;
        syncInterval = Math.max(5, Math.min(seconds || 15, 3600)) * 1000;
        syncTimer = window.setInterval(periodicSync, syncInterval);
    }

    window.addEventListener('load', function () {
        window.serverSyncPush = schedulePush;
        window.serverSyncConfigure = configureSync;
        var originalSaveData = window.saveData;
        if (typeof originalSaveData === 'function') {
            window.saveData = function () {
                originalSaveData();
                lastSnapshotSignature = signature(snapshot());
                schedulePush();
            };
        }
        window.addEventListener('online', function () {
            retryDelay = 1000;
            loadSnapshot();
        });
        window.addEventListener('visibilitychange', function () {
            if (document.visibilityState === 'visible') loadSnapshot();
        });
        window.addEventListener('storage', function (event) {
            if (event.key && event.key.indexOf(pendingStoragePrefix) === 0) schedulePush();
        });
        configureSync();
        window.setTimeout(function () {
            lastSnapshotSignature = signature(snapshot());
            loadSnapshot();
        }, 0);
    });
}());
