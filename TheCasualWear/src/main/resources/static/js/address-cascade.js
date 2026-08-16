/**
 * address-cascade.js
 * Dùng chung cho dropdown Tỉnh/Thành phố -> Phường/Xã
 *
 * LƯU Ý QUAN TRỌNG:
 * Từ 01/07/2025 Việt Nam đã BỎ cấp Quận/Huyện, chỉ còn 2 cấp hành chính:
 * Tỉnh/Thành phố -> Phường/Xã. Vì vậy API v2 (provinces.open-api.vn) không
 * còn field "districts" nữa, mà trả thẳng "wards" (phường/xã) dưới mỗi tỉnh.
 * Field/DB "district" trong project được GIỮ NGUYÊN TÊN để không phải đổi
 * schema, nhưng thực chất giờ nó lưu tên Phường/Xã.
 *
 * Data: API mở provinces.open-api.vn (không cần key, không cần lưu DB)
 *
 * Cách dùng trong HTML:
 *   <select id="citySelect" th:field="*{city}" class="form-select"
 *           th:attr="data-current=*{city}">
 *       <option value="">-- Chọn Tỉnh/Thành phố --</option>
 *   </select>
 *   <select id="districtSelect" th:field="*{district}" class="form-select"
 *           th:attr="data-current=*{district}" disabled>
 *       <option value="">-- Chọn Phường/Xã --</option>
 *   </select>
 *
 *   <script th:src="@{/js/address-cascade.js}"></script>
 *   <script>
 *       document.addEventListener('DOMContentLoaded', function () {
 *           initAddressCascade('citySelect', 'districtSelect');
 *       });
 *   </script>
 */
(function () {
    const API_BASE = 'https://provinces.open-api.vn/api/v2';

    // Cache danh sách tỉnh trong bộ nhớ để không gọi API lại nếu trang có
    // nhiều cặp select (vd: 1 cho địa chỉ giao hàng, 1 cho địa chỉ xuất hoá đơn)
    let provincesCache = null;

    async function getProvinces() {
        if (provincesCache) return provincesCache;
        const res = await fetch(API_BASE + '/p/');
        if (!res.ok) throw new Error('Không tải được danh sách tỉnh/thành');
        const data = await res.json();
        data.sort((a, b) => a.name.localeCompare(b.name, 'vi'));
        provincesCache = data;
        return data;
    }

    async function getWards(provinceCode) {
        const res = await fetch(API_BASE + '/p/' + provinceCode + '?depth=2');
        if (!res.ok) throw new Error('Không tải được danh sách phường/xã');
        const data = await res.json();
        // API v2 (sau sáp nhập 07/2025): tỉnh -> thẳng phường/xã, field "wards"
        return (data.wards || []).sort((a, b) => a.name.localeCompare(b.name, 'vi'));
    }

    function fillOptions(selectEl, items, placeholder) {
        selectEl.innerHTML = '';
        const optPlaceholder = document.createElement('option');
        optPlaceholder.value = '';
        optPlaceholder.textContent = placeholder;
        selectEl.appendChild(optPlaceholder);

        items.forEach(function (item) {
            const opt = document.createElement('option');
            opt.value = item.name;         // submit đúng tên tỉnh/huyện (giữ tương thích field String cũ)
            opt.textContent = item.name;
            if (item.code !== undefined) opt.dataset.code = item.code;
            selectEl.appendChild(opt);
        });
    }

    async function loadWardsInto(provinceCode, districtSelect, valueToSelect) {
        fillOptions(districtSelect, [], '-- Chọn Phường/Xã --');
        if (!provinceCode) {
            districtSelect.disabled = true;
            return;
        }
        districtSelect.disabled = true;
        try {
            const wards = await getWards(provinceCode);
            fillOptions(districtSelect, wards, '-- Chọn Phường/Xã --');
            if (valueToSelect) districtSelect.value = valueToSelect;
        } finally {
            districtSelect.disabled = false;
        }
    }

    /**
     * @param {string} citySelectId    id của <select> Tỉnh/Thành phố
     * @param {string} districtSelectId id của <select> Quận/Huyện
     */
    async function initAddressCascade(citySelectId, districtSelectId) {
        const citySelect = document.getElementById(citySelectId);
        const districtSelect = document.getElementById(districtSelectId);
        if (!citySelect || !districtSelect) return;

        const currentCity = citySelect.dataset.current || '';
        const currentDistrict = districtSelect.dataset.current || '';

        try {
            const provinces = await getProvinces();
            fillOptions(citySelect, provinces, '-- Chọn Tỉnh/Thành phố --');

            // Prefill khi sửa địa chỉ đã có sẵn (hoặc form bị lỗi validate và render lại)
            if (currentCity) {
                citySelect.value = currentCity;
                const selectedOpt = citySelect.selectedOptions[0];
                const code = selectedOpt ? selectedOpt.dataset.code : null;
                if (code) {
                    await loadWardsInto(code, districtSelect, currentDistrict);
                }
            }
        } catch (e) {
            console.error(e);
            fillOptions(citySelect, [], '-- Không tải được danh sách, thử lại sau --');
        }

        citySelect.addEventListener('change', function () {
            const selectedOpt = citySelect.selectedOptions[0];
            const code = selectedOpt ? selectedOpt.dataset.code : null;
            loadWardsInto(code, districtSelect, null);
        });
    }

    window.initAddressCascade = initAddressCascade;
})();