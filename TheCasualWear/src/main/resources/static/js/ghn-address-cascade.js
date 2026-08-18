/**
 * ghn-address-cascade.js
 * Cascade 3 cấp Tỉnh -> Quận/Huyện -> Phường/Xã THEO MÃ GHN (khác hệ mã
 * provinces.open-api.vn của address-cascade.js cũ — không dùng chung được,
 * xem ghi chú trong GhnService.java).
 *
 * Dùng riêng cho checkout (4.5) để lấy đủ ghnDistrictId/ghnWardCode cho GHN
 * Calculate Fee. KHÔNG thay thế address-cascade.js cũ — các trang khác
 * (sổ địa chỉ...) vẫn dùng cascade 2 cấp cũ như trước, ngoài phạm vi này.
 *
 * Gọi qua backend (/api/ghn/*) chứ KHÔNG gọi thẳng GHN từ trình duyệt, vì
 * master-data/* của GHN cần header Token — lộ token ra JS là rủi ro bảo mật.
 *
 * Cách dùng:
 *   initGhnAddressCascade({
 *       provinceSelectId: 'ghnProvinceSelect',
 *       districtSelectId: 'ghnDistrictSelect',
 *       wardSelectId: 'ghnWardSelect',
 *       // Prefill khi form redisplay sau lỗi validate (đọc từ data-current-* trên provinceSelect)
 *       onWardSelected: function ({ provinceId, provinceName, districtId, districtName, wardCode, wardName }) {
 *           // set hidden fields + gọi AJAX tính phí ở đây
 *       }
 *   });
 */
(function () {
    const API_BASE = '/api/ghn';

    let provincesCache = null;

    async function getProvinces() {
        if (provincesCache) return provincesCache;
        const res = await fetch(API_BASE + '/provinces');
        if (!res.ok) throw new Error('Không tải được danh sách tỉnh/thành (GHN)');
        const body = await res.json();
        if (body.error) throw new Error(body.error);
        provincesCache = body;
        return body;
    }

    async function getDistricts(provinceId) {
        const res = await fetch(API_BASE + '/districts?provinceId=' + encodeURIComponent(provinceId));
        if (!res.ok) throw new Error('Không tải được danh sách quận/huyện (GHN)');
        const body = await res.json();
        if (body.error) throw new Error(body.error);
        return body;
    }

    async function getWards(districtId) {
        const res = await fetch(API_BASE + '/wards?districtId=' + encodeURIComponent(districtId));
        if (!res.ok) throw new Error('Không tải được danh sách phường/xã (GHN)');
        const body = await res.json();
        if (body.error) throw new Error(body.error);
        return body;
    }

    function fillOptions(selectEl, items, placeholder) {
        selectEl.innerHTML = '';
        const optPlaceholder = document.createElement('option');
        optPlaceholder.value = '';
        optPlaceholder.textContent = placeholder;
        selectEl.appendChild(optPlaceholder);

        items.forEach(function (item) {
            const opt = document.createElement('option');
            opt.value = item.id;              // GHN id (province/district) hoặc ward code — KHÁC value=name như cascade cũ
            opt.textContent = item.name;
            opt.dataset.name = item.name;      // cần tên thật để set vào city/district (String) song song với mã
            selectEl.appendChild(opt);
        });
    }

    function currentSelection(select) {
        const opt = select.selectedOptions[0];
        if (!opt || !opt.value) return null;
        return { id: opt.value, name: opt.dataset.name || opt.textContent };
    }

    /**
     * @param {object} config
     * @param {string} config.provinceSelectId
     * @param {string} config.districtSelectId
     * @param {string} config.wardSelectId
     * @param {function} [config.onWardSelected] callback({provinceId, provinceName, districtId, districtName, wardCode, wardName})
     * @param {function} [config.onIncomplete] callback() — gọi khi chưa chọn đủ 3 cấp (để FE reset phí ship về ước tính)
     * @param {function} [config.onError] callback(error) — GHN lỗi, FE tự quyết định hiển thị gì
     * @returns {Promise<{applySelection: function(string,string,string):Promise<void>}>}
     *          applySelection(provinceId, districtId, wardCode) — gọi lại để prefill
     *          động (VD: khách click chọn 1 địa chỉ đã lưu trong sổ), không chỉ chạy
     *          1 lần lúc trang load.
     */
    async function initGhnAddressCascade(config) {
        const provinceSelect = document.getElementById(config.provinceSelectId);
        const districtSelect = document.getElementById(config.districtSelectId);
        const wardSelect = document.getElementById(config.wardSelectId);
        if (!provinceSelect || !districtSelect || !wardSelect) return;

        function notifyWardSelected() {
            const p = currentSelection(provinceSelect);
            const d = currentSelection(districtSelect);
            const w = currentSelection(wardSelect);
            if (p && d && w && typeof config.onWardSelected === 'function') {
                config.onWardSelected({
                    provinceId: p.id, provinceName: p.name,
                    districtId: d.id, districtName: d.name,
                    wardCode: w.id, wardName: w.name
                });
            } else if (typeof config.onIncomplete === 'function') {
                config.onIncomplete();
            }
        }

        async function loadDistrictsInto(provinceId, valueToSelect) {
            fillOptions(districtSelect, [], '-- Chọn Quận/Huyện --');
            fillOptions(wardSelect, [], '-- Chọn Quận/Huyện trước --');
            districtSelect.disabled = true;
            wardSelect.disabled = true;
            if (!provinceId) return;
            districtSelect.disabled = true;
            try {
                const districts = await getDistricts(provinceId);
                fillOptions(districtSelect, districts, '-- Chọn Quận/Huyện --');
                if (valueToSelect) districtSelect.value = valueToSelect;
            } catch (e) {
                console.error(e);
                if (typeof config.onError === 'function') config.onError(e);
            } finally {
                districtSelect.disabled = false;
            }
        }

        async function loadWardsInto(districtId, valueToSelect) {
            fillOptions(wardSelect, [], '-- Chọn Phường/Xã --');
            wardSelect.disabled = true;
            if (!districtId) return;
            try {
                const wards = await getWards(districtId);
                fillOptions(wardSelect, wards, '-- Chọn Phường/Xã --');
                if (valueToSelect) wardSelect.value = valueToSelect;
            } catch (e) {
                console.error(e);
                if (typeof config.onError === 'function') config.onError(e);
            } finally {
                wardSelect.disabled = false;
            }
        }

        async function applySelection(provinceId, districtId, wardCode) {
            if (!provinceId) {
                fillOptions(districtSelect, [], '-- Chọn Quận/Huyện --');
                fillOptions(wardSelect, [], '-- Chọn Quận/Huyện trước --');
                districtSelect.disabled = true;
                wardSelect.disabled = true;
                if (typeof config.onIncomplete === 'function') config.onIncomplete();
                return;
            }
            provinceSelect.value = provinceId;
            await loadDistrictsInto(provinceId, districtId);
            if (districtId) {
                await loadWardsInto(districtId, wardCode);
                notifyWardSelected();
            } else if (typeof config.onIncomplete === 'function') {
                config.onIncomplete();
            }
        }

        // Prefill khi form redisplay sau lỗi validate — đọc mã cũ từ data-current-* trên provinceSelect
        const currentProvinceId = provinceSelect.dataset.currentProvinceId || '';
        const currentDistrictId = provinceSelect.dataset.currentDistrictId || '';
        const currentWardCode = provinceSelect.dataset.currentWardCode || '';

        try {
            const provinces = await getProvinces();
            fillOptions(provinceSelect, provinces, '-- Chọn Tỉnh/Thành phố --');

            if (currentProvinceId) {
                await applySelection(currentProvinceId, currentDistrictId, currentWardCode);
            }
        } catch (e) {
            console.error(e);
            fillOptions(provinceSelect, [], '-- Không tải được danh sách, thử lại sau --');
            if (typeof config.onError === 'function') config.onError(e);
        }

        provinceSelect.addEventListener('change', async function () {
            const p = currentSelection(provinceSelect);
            await loadDistrictsInto(p ? p.id : null, null);
            notifyWardSelected();
        });

        districtSelect.addEventListener('change', async function () {
            const d = currentSelection(districtSelect);
            await loadWardsInto(d ? d.id : null, null);
            notifyWardSelected();
        });

        wardSelect.addEventListener('change', notifyWardSelected);

        return { applySelection: applySelection };
    }

    window.initGhnAddressCascade = initGhnAddressCascade;
})();
