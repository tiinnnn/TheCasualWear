/**
 * notifications.js
 * Đặt file này tại: src/main/resources/static/js/notifications.js
 *
 * Poll định kỳ tới GET /api/notifications/summary để cập nhật badge + danh sách
 * thông báo trong dropdown mà KHÔNG cần reload trang. Hoạt động cho cả
 * admin-layout.html và shop-layout.html vì cả 2 dùng chung các id:
 *   #notifBellBtn, #notifBadge, #notifDropdownMenu, #notifListBody, #notifMarkAllRead
 *
 * Nếu trang không có khu vực thông báo (ví dụ trang login) thì script tự bỏ qua.
 */
(function () {
    var POLL_INTERVAL_MS = 20000; // 20 giây, chỉnh lại tuỳ nhu cầu

    var badge = document.getElementById('notifBadge');
    var listBody = document.getElementById('notifListBody');
    var markAllBtn = document.getElementById('notifMarkAllRead');

    // Không có khu vực thông báo trên trang này (vd: trang login) -> bỏ qua
    if (!listBody) {
        return;
    }

    function escapeHtml(str) {
        var div = document.createElement('div');
        div.textContent = str == null ? '' : String(str);
        return div.innerHTML;
    }

    function renderNotifications(data) {
        var unreadCount = data.unreadCount || 0;
        var notifications = data.notifications || [];

        // Cập nhật badge số lượng chưa đọc
        if (badge) {
            if (unreadCount > 0) {
                badge.textContent = unreadCount > 99 ? '99+' : unreadCount;
                badge.style.display = '';
            } else {
                badge.style.display = 'none';
            }
        }

        // Ẩn/hiện nút "Đánh dấu đã đọc"
        if (markAllBtn) {
            markAllBtn.style.display = unreadCount > 0 ? '' : 'none';
        }

        // Render danh sách thông báo
        if (notifications.length === 0) {
            listBody.innerHTML =
                '<div class="text-center text-muted py-3 small">Không có thông báo nào</div>';
            return;
        }

        var html = notifications.map(function (n) {
            var isRead = !!n.isRead;
            return (
                '<div class="' + (isRead ? '' : 'bg-light') + '">' +
                    '<a href="/notifications/read/' + encodeURIComponent(n.id) + '" ' +
                       'data-id="' + n.id + '" ' +
                       'class="dropdown-item py-2 text-wrap notif-item" style="white-space:normal">' +
                        '<div class="d-flex gap-2 align-items-start">' +
                            '<i class="bi bi-bell mt-1 flex-shrink-0 ' + (isRead ? 'text-muted' : 'text-danger') + '"></i>' +
                            '<div>' +
                                '<p class="mb-0 small">' + escapeHtml(n.message) + '</p>' +
                                '<small class="text-muted">' + escapeHtml(n.createdAtDisplay) + '</small>' +
                            '</div>' +
                        '</div>' +
                    '</a>' +
                '</div>'
            );
        }).join('');

        listBody.innerHTML = html;
    }

    function fetchNotifications() {
        fetch('/api/notifications/summary', {
            headers: { 'X-Requested-With': 'XMLHttpRequest' },
            credentials: 'same-origin'
        })
            .then(function (res) {
                if (!res.ok) {
                    // Chưa đăng nhập hoặc lỗi -> im lặng bỏ qua, không phá layout
                    return Promise.reject(res.status);
                }
                return res.json();
            })
            .then(renderNotifications)
            .catch(function () {
                /* bỏ qua lỗi mạng/401, giữ nguyên nội dung server render lần đầu */
            });
    }

    // Click vào 1 thông báo: KHÔNG chặn hành vi mặc định của thẻ <a>.
    // Endpoint /notifications/read/{id} ở server đã tự đánh dấu đã đọc
    // rồi redirect sang link đích (n.link) — nếu preventDefault() + fetch()
    // ở đây thì fetch sẽ tự âm thầm theo redirect và vứt kết quả đi,
    // trình duyệt không được điều hướng => bấm vào không chuyển trang.
    // Nên cứ để trình duyệt tự nhiên đi theo href là đủ, không cần xử lý gì thêm.

    if (markAllBtn) {
        markAllBtn.addEventListener('click', function (e) {
            e.preventDefault();
            fetch('/notifications/read-all', {
                method: 'GET', // đổi thành POST nếu bạn chuyển endpoint sang POST (khuyến nghị)
                headers: { 'X-Requested-With': 'XMLHttpRequest' },
                credentials: 'same-origin'
            })
                .then(fetchNotifications)
                .catch(function () {});
        });
    }

    // Gọi ngay khi trang load để đồng bộ (dữ liệu server-render ban đầu vẫn
    // hiển thị trong lúc chờ request đầu tiên hoàn tất) + polling định kỳ.
    fetchNotifications();
    setInterval(fetchNotifications, POLL_INTERVAL_MS);
})();