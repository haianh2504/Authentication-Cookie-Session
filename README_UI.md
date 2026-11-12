# README UI & Workflow

## 1. Tổng quan

Authentication & Cookie Session là dự án học tập xây dựng website thư viện ảnh Ronaldo. Visitor xem thư viện công khai; một admin đăng nhập để tải ảnh lên, xóa ảnh và đăng xuất. Tài liệu mô tả frontend Next.js/TypeScript giao tiếp với backend Spring Boot qua REST API, nhưng README ghi frontend chưa có cấu trúc hoặc lệnh chạy đã xác minh. Tài liệu này tổng hợp `README.md`, `PRD.md`, `api_contract.md` và `use_case.md`; đây là đặc tả từ tài liệu, không xác nhận mọi chức năng đã được triển khai.

## 2. Danh sách pages/routes

Tài liệu nguồn không chốt URL route phía frontend. Các mục dưới đây là màn hình/chức năng suy ra trực tiếp từ use case, không phải route đã xác nhận.

| Page/chức năng | Route frontend | Mục đích | Người dùng/điều kiện |
| --- | --- | --- | --- |
| Thư viện ảnh | Chưa xác định | Hiển thị ảnh công khai và tải thêm khi cuộn | Visitor; không cần đăng nhập |
| Đăng nhập admin | Chưa xác định | Nhập email và mật khẩu để tạo phiên admin | Admin có thông tin hợp lệ |
| Trạng thái phiên/admin | Chưa xác định; có thể là trạng thái trên giao diện hiện có | Xác định phiên hiện tại còn hợp lệ để mở quyền quản trị | Admin có cookie phiên hợp lệ |
| Quản lý ảnh | Chưa xác định; tài liệu không xác nhận đây là page riêng | Chọn, xem trước, tải lên hoặc xóa ảnh | Admin đã xác thực |

API routes được xác nhận: `GET /api/v1/csrf-token`, `GET /api/v1/images`, `POST /api/v1/auth/admin`, `GET /api/v1/auth/admin/sessions`, `POST /api/v1/images`, `DELETE /api/v1/images/{imageId}`, `DELETE /api/v1/auth/admin/sessions`.

## 3. Chi tiết từng page/chức năng

### Thư viện ảnh — route frontend chưa xác định

- **Mục đích:** Cho visitor xem ảnh mới nhất trước và tải lần lượt các trang ảnh khi cuộn.
- **Sections/UI:** Danh sách ảnh; hành vi infinite scroll. Khi không có ảnh, hiển thị trạng thái thư viện trống. Tài liệu không nêu bố cục hoặc component cụ thể.
- **Sự kiện và tương tác:**

| Section/control | Sự kiện | Kết quả/route đích | API | Trạng thái/ghi chú |
| --- | --- | --- | --- | --- |
| Mở thư viện | Tải trang đầu, bỏ qua `nextCursor` | Hiển thị tối đa 10 ảnh | `GET /api/v1/images` | `200 OK`; response có `items` và `pagination` |
| Cuộn tới cuối danh sách | Tải tiếp bằng `nextCursor` từ response trước | Nối trang ảnh tiếp theo; dừng khi `hasNextPage` là `false` | `GET /api/v1/images?nextCursor={opaqueCursor}` | Mỗi request tối đa 10 ảnh; client không giải mã/tạo cursor |
| Danh sách rỗng | Nhận `items: []` | Hiển thị trạng thái thư viện trống | `GET /api/v1/images` | `200 OK`; khi hết dữ liệu `nextCursor: null` |
| Lỗi tải | Nhận lỗi từ backend | Hiển thị lỗi/tải thất bại chung | `GET /api/v1/images` | `500 Internal Server Error`; chi tiết nội bộ chỉ ghi log |

- **Dữ liệu hiển thị:** Mỗi item có `imageId` và `imageURL`. Backend sắp xếp theo `created_at DESC, id DESC`; `created_at` không bắt buộc có trong item công khai.
- **Trạng thái giao diện:** Có trạng thái rỗng và lỗi được mô tả. Loading UI cụ thể chưa xác định.

### Đăng nhập admin — route frontend chưa xác định

- **Mục đích:** Xác thực admin bằng email/mật khẩu và tạo phiên server-side trong Redis.
- **Sections/UI:** Form có trường `email`, `password` và hành động gửi đăng nhập; giao diện chi tiết chưa xác định.
- **Sự kiện và tương tác:**

| Section/control | Sự kiện | Kết quả/route đích | API | Trạng thái/ghi chú |
| --- | --- | --- | --- | --- |
| Form đăng nhập | Gửi `email`, `password` | Thành công: trình duyệt nhận cookie phiên; đích UI sau đăng nhập chưa xác định | `POST /api/v1/auth/admin` | `201 Created`, `data.authenticated: true` |
| Form đăng nhập | Thiếu hoặc sai định dạng trường | Giữ/hiển thị lỗi validation; cách hiển thị chưa xác định | `POST /api/v1/auth/admin` | `400 Bad Request` |
| Form đăng nhập | Thông tin xác thực không hợp lệ | Báo lỗi chung, không tiết lộ sai email hay mật khẩu | `POST /api/v1/auth/admin` | `401 Unauthorized` |
| Form đăng nhập | Dịch vụ xác thực lỗi | Báo lỗi chung | `POST /api/v1/auth/admin` | `500 Internal Server Error` |

- **Dữ liệu hiển thị:** Không có hồ sơ người dùng trong response thành công; chỉ có `authenticated: true`. Session ID không được trả trong body.
- **Trạng thái giao diện:** Validation và lỗi xác thực được quy định về trạng thái HTTP; cách trình bày cụ thể chưa xác định. Giới hạn login: 3 request/giây.

### Trạng thái phiên admin — route frontend chưa xác định

- **Mục đích:** Khôi phục trạng thái đăng nhập hiện tại để frontend quyết định có hiển thị quyền quản trị hay đưa admin về đăng nhập.
- **Sections/UI:** Trạng thái admin đã xác thực/chưa xác thực; không có page/component cụ thể được chốt.
- **Sự kiện và tương tác:**

| Section/control | Sự kiện | Kết quả/route đích | API | Trạng thái/ghi chú |
| --- | --- | --- | --- | --- |
| Khởi tạo/kiểm tra trạng thái admin | Gửi cookie phiên tự động | Phiên hợp lệ: cho phép trạng thái admin | `GET /api/v1/auth/admin/sessions` | `200 OK`, `data.authenticated: true` |
| Kiểm tra trạng thái admin | Cookie thiếu, sai hoặc hết hạn | Trở về trạng thái chưa đăng nhập/đưa về trang đăng nhập; route cụ thể chưa xác định | `GET /api/v1/auth/admin/sessions` | `401 Unauthorized`; không log/trả raw session ID |
| Kiểm tra trạng thái admin | Lỗi kiểm tra phiên | Hiển thị trạng thái lỗi chung | `GET /api/v1/auth/admin/sessions` | `500 Internal Server Error` |

- **Dữ liệu hiển thị:** Response thành công chỉ xác nhận `authenticated: true`.
- **Trạng thái giao diện:** Phiên có thời hạn 10 phút và dùng session rotation theo yêu cầu dự án.

### Quản lý ảnh — route frontend chưa xác định

- **Mục đích:** Cho admin đã xác thực thêm ảnh vào thư viện hoặc xóa ảnh hiện có.
- **Sections/UI:** Chọn file; xem trước tạm thời ảnh cục bộ trước khi gửi; điều khiển tải lên; hành động xác nhận xóa. Tài liệu không xác nhận đây là page riêng hay vị trí các điều khiển.
- **Sự kiện và tương tác:**

| Section/control | Sự kiện | Kết quả/route đích | API | Trạng thái/ghi chú |
| --- | --- | --- | --- | --- |
| Chọn ảnh | Hiển thị preview cục bộ tạm thời | Preview trước khi gửi; không lưu preview ở backend | Không gọi API được xác nhận khi chỉ chọn file | Tệp tối đa 15 MB; phần mở rộng được phép: `.jpg`, `.jpeg`, `.png`, `.HEIC`, `.HEIF`, `.WebP`, `.AVIF` |
| Gửi ảnh | Gửi multipart field `file`, cookie phiên và CSRF header | Thành công: thêm ảnh vào đầu thư viện/refresh danh sách | `POST /api/v1/images` | `201 Created`; response có `imageId`, `imageURL` |
| Gửi ảnh | Tệp thiếu/rỗng, quá lớn, sai định dạng hoặc nội dung không hợp lệ | Hiển thị lỗi phù hợp; chi tiết UI chưa xác định | `POST /api/v1/images` | `400`, `413` hoặc `415`; yêu cầu admin và CSRF |
| Gửi ảnh | Phiên không hợp lệ/không đủ quyền | Không thực hiện upload | `POST /api/v1/images` | `401 Unauthorized` hoặc `403 Forbidden` |
| Gửi ảnh | Storage hoặc database lỗi | Hiển thị lỗi chung | `POST /api/v1/images` | `500`; nếu insert DB lỗi sau upload Storage, backend thử xóa object bù trừ |
| Xác nhận xóa ảnh | Gửi `imageId` của ảnh đã chọn | Thành công: bỏ ảnh khỏi thư viện đang hiển thị | `DELETE /api/v1/images/{imageId}` | `200 OK`, `data.deleted: true`; backend xóa object Storage rồi bản ghi DB |
| Xác nhận xóa ảnh | Ảnh không tồn tại/lỗi xóa | Hiển thị lỗi; chính sách nhất quán cụ thể chưa đầy đủ | `DELETE /api/v1/images/{imageId}` | `404 Not Found` hoặc `500`; yêu cầu admin |

- **Dữ liệu hiển thị:** Ảnh dùng `imageURL`; định danh xóa là UUID `imageId`. Không có title, caption, description hoặc metadata người dùng.
- **Trạng thái giao diện:** Các lỗi HTTP được nêu trên; cách xác nhận thao tác, loading và xử lý retry chưa được mô tả đầy đủ. Không tự retry khi kết quả upload không rõ do timeout.

### Đăng xuất admin — điều khiển giao diện chưa xác định

- **Mục đích:** Hủy phiên admin hiện tại và chuyển giao diện về trạng thái chưa xác thực.
- **Sections/UI:** Hành động đăng xuất; vị trí nút chưa xác định.
- **Sự kiện và tương tác:**

| Section/control | Sự kiện | Kết quả/route đích | API | Trạng thái/ghi chú |
| --- | --- | --- | --- | --- |
| Đăng xuất | Gửi yêu cầu với cookie phiên và CSRF header | Backend hủy phiên, xóa/hết hạn cookie; frontend về trạng thái chưa đăng nhập | `DELETE /api/v1/auth/admin/sessions` | `200 OK`, `data.authenticated: false` |
| Đăng xuất | Phiên không hợp lệ hoặc backend lỗi | Hiển thị lỗi/chuyển trạng thái phù hợp; chi tiết UI chưa xác định | `DELETE /api/v1/auth/admin/sessions` | `401 Unauthorized` hoặc `500 Internal Server Error` |

- **Dữ liệu hiển thị:** Không có dữ liệu người dùng được trả thêm.
- **Trạng thái giao diện:** Sau khi đăng xuất thành công, các thao tác được bảo vệ không còn xác thực.

## 4. Luồng chính giữa các routes

Frontend route đích không được xác định trong tài liệu. Sơ đồ mô tả luồng người dùng và API, không khẳng định cấu trúc trang:

```mermaid
flowchart TD
    A[Visitor mở thư viện] --> B[GET /api/v1/images, không nextCursor]
    B --> C[Hiển thị tối đa 10 ảnh]
    C --> D{Còn trang tiếp?}
    D -- Có --> E[Cuộn: GET /api/v1/images?nextCursor=opaque]
    E --> C
    D -- Không --> F[Dừng tải thêm]
    A --> G[Admin yêu cầu GET /api/v1/csrf-token]
    G --> H[Đọc cookie XSRF-TOKEN]
    H --> I[POST /api/v1/auth/admin]
    I -->|201 + cookie phiên| J[GET /api/v1/auth/admin/sessions]
    J -->|200| K[Chọn/xem trước ảnh]
    K --> L[POST /api/v1/images + X-XSRF-TOKEN]
    L -->|201| C
    J -->|401| M[Trạng thái chưa đăng nhập]
    K --> N[Xóa ảnh]
    N --> O[DELETE /api/v1/images/{imageId} + X-XSRF-TOKEN]
    O -->|200| C
    K --> P[Đăng xuất]
    P --> Q[DELETE /api/v1/auth/admin/sessions + X-XSRF-TOKEN]
    Q -->|200| M
```

CSRF token nằm trong cookie `XSRF-TOKEN`, không nằm trong `data.token`. Với request thay đổi trạng thái dùng cookie phiên, frontend gửi giá trị cookie trong `X-XSRF-TOKEN`. Login thành công tạo cookie phiên; session ID thô không đi vào request/response body.

## 5. Bản đồ UI ↔ API

| Page/section | Hành động | Method + endpoint | Gửi/nhận | Ghi chú |
| --- | --- | --- | --- | --- |
| Khởi tạo bảo vệ CSRF | Lấy token | `GET /api/v1/csrf-token` | Gửi không body; nhận cookie `XSRF-TOKEN`, envelope `data: {}` | Public; `200 OK` |
| Thư viện ảnh | Tải trang đầu | `GET /api/v1/images` | Không body/query; nhận `items[{imageId,imageURL}]`, `pagination{limit,nextCursor,hasNextPage}` | Public; tối đa 10 ảnh |
| Thư viện ảnh | Tải trang kế | `GET /api/v1/images?nextCursor={opaqueCursor}` | Gửi cursor opaque; nhận cùng cấu trúc | Public; bỏ qua cursor khi khởi tạo |
| Đăng nhập | Gửi thông tin admin | `POST /api/v1/auth/admin` | JSON `{email,password}`; nhận `authenticated: true` và cookie phiên | `201 Created`; không cần CSRF theo contract |
| Trạng thái phiên | Kiểm tra phiên | `GET /api/v1/auth/admin/sessions` | Cookie phiên; nhận `authenticated: true` | `200 OK`; `401` nếu thiếu/hết hạn |
| Quản lý ảnh | Tải ảnh lên | `POST /api/v1/images` | multipart `file`, cookie phiên, `X-XSRF-TOKEN`; nhận `imageId`, `imageURL` | `201 Created`; chỉ admin |
| Quản lý ảnh | Xóa ảnh | `DELETE /api/v1/images/{imageId}` | UUID path variable, cookie phiên, `X-XSRF-TOKEN`; nhận `deleted`, `imageId` | `200 OK`; chỉ admin |
| Đăng xuất | Hủy phiên | `DELETE /api/v1/auth/admin/sessions` | Cookie phiên, `X-XSRF-TOKEN`; nhận `authenticated: false` | `200 OK`; chỉ phiên đã xác thực |

Mọi response dùng envelope `status`, `code`, `message`, `timestamp`, `data`. Cookie phát triển local dùng `SameSite=Lax`, host-only; cookie phiên là `HttpOnly`, còn cookie CSRF phải đọc được từ frontend. Bật `Secure` khi triển khai HTTPS.

## 6. Điểm chưa rõ hoặc mâu thuẫn

1. **Route và bố cục frontend:** Tài liệu không cung cấp URL route, wireframe, tên section/control cụ thể, route đích sau login hoặc quy tắc điều hướng sau hết phiên. README nói frontend dự kiến dùng Next.js nhưng chưa có cấu trúc/lệnh chạy đã xác minh. Cần chốt khi triển khai frontend.
2. **Mức độ triển khai:** README nêu requirements không chứng minh tính năng đã triển khai; roadmap còn đánh dấu gallery, auth/session và delete là chưa hoàn tất, trong khi upload UC04 được ghi đã triển khai. Bảng này là workflow theo đặc tả, không phải trạng thái runtime.
3. **Tài liệu Use Case có nội dung cũ:** Phần đầu `use_case.md` nói các endpoint ngoài CSRF và upload còn deferred, và phần open design decisions lặp lại việc endpoint chưa chốt. `api_contract.md` cùng các phần cập nhật khác của README/Use Case đã quy định cụ thể endpoint session, logout và delete; bảng API ở đây theo `api_contract.md` làm nguồn chuẩn cho method/path/payload/response.
4. **Xóa và nhất quán dữ liệu:** API contract quy định xóa Storage rồi xóa DB, nhưng Use Case yêu cầu xác định cách xử lý lỗi từng phần/missing Storage object. Cần chốt bù trừ hoặc reconciliation để tránh lệch dữ liệu.
5. **Session hết hạn:** Tài liệu yêu cầu phiên 10 phút, nhưng cách gia hạn thời gian phiên khi hoạt động và hành vi UI ở thời điểm hết hạn chưa nêu.
6. **Chi tiết lỗi/UX:** Một số use case nói “appropriate/suitable” client error; API contract đưa mã HTTP chính nhưng chưa có danh mục error code đầy đủ. Loading, toast, confirm dialog và retry UI chưa được đặc tả.
7. **Cookie production:** Cấu hình local đã rõ; tên cookie phiên và giá trị production cụ thể (đặc biệt `Domain` khi frontend/backend khác subdomain) còn để mở.
