# Tài liệu API Dịch vụ Chương trình (Program Service)

**Phiên bản:** 2.1.0
**Cập nhật lần cuối:** 28-11-2025
**Bảng mã:** UTF-8

---

## 1. Tổng quan

Tài liệu này cung cấp một tham chiếu đầy đủ cho API của Dịch vụ Chương trình. Dịch vụ này chịu trách nhiệm quản lý các chương trình của người dùng, nội dung, theo dõi tiến độ và các chức năng liên quan.

### 1.1. URL Gốc (Base URL)

Tất cả các điểm cuối (endpoint) của API đều tương đối so với URL gốc của dịch vụ.

-   **Production:** `https://api.smokefree.app/program`
-   **Development:** `http://localhost:8080`

### 1.2. Xác thực (Authentication)

Tất cả các yêu cầu (request) phải được xác thực thông qua API Gateway. Các header sau đây là bắt buộc trên tất cả các yêu cầu gửi đến:

| Header          | Mô tả                                     | Ví dụ                                  |
| --------------- | ----------------------------------------- | -------------------------------------- |
| `Authorization` | Bearer token để xác thực.                  | `Bearer <JWT_TOKEN>`                   |
| `X-User-Id`     | UUID của người dùng đã được xác thực.      | `a1b2c3d4-e5f6-7890-1234-567890abcdef` |
| `X-User-Role`   | Vai trò của người dùng đã được xác thực.   | `USER`, `COACH`, hoặc `ADMIN`          |

### 1.3. Vai trò Người dùng (User Roles)

| Vai trò  | Quyền hạn                                                              |
| --------- | ----------------------------------------------------------------------- |
| `ADMIN`   | Toàn quyền truy cập vào tất cả tài nguyên để quản lý và quản trị.       |
| `COACH`   | Quyền chỉ đọc (read-only) đối với dữ liệu của người dùng/chương trình được chỉ định. |
| `USER`    | Chỉ truy cập vào dữ liệu của chính mình (chương trình, bài học, quiz, v.v.). |

### 1.4. Các Mã Trạng thái HTTP Phổ biến (Common HTTP Status Codes)

| Mã    | Trạng thái             | Mô tả                                                                 |
| ----- | ---------------------- | --------------------------------------------------------------------- |
| `200` | OK                     | Yêu cầu đã thành công.                                                |
| `201` | Created                | Tài nguyên đã được tạo thành công.                                    |
| `400` | Bad Request            | Yêu cầu không hợp lệ (ví dụ: JSON sai định dạng, thiếu tham số).       |
| `401` | Unauthorized           | Xác thực thất bại hoặc không được cung cấp.                           |
| `402` | Payment Required       | Hành động yêu cầu cần có gói thuê bao (ví dụ: bản dùng thử đã hết hạn). |
| `403` | Forbidden              | Người dùng đã xác thực không có quyền truy cập vào tài nguyên này.     |
| `404` | Not Found              | Không tìm thấy tài nguyên được yêu cầu.                               |
| `409` | Conflict               | Yêu cầu không thể hoàn thành do xung đột với trạng thái hiện tại của tài nguyên (ví dụ: tạo một mục bị trùng lặp). |
| `501` | Not Implemented        | Máy chủ không hỗ trợ chức năng cần thiết để thực hiện yêu cầu.         |

---

## 2. Giới thiệu & Ghi danh (Onboarding & Enrollment)

Xử lý các luồng giới thiệu ban đầu cho người dùng và ghi danh vào chương trình.

### `POST /api/onboarding/baseline`

Gửi câu trả lời đánh giá ban đầu của người dùng và nhận đề xuất về chương trình phù hợp.

-   **Quyền hạn:** `USER`
-   **Nội dung yêu cầu (Request Body):** `QuizAnswerReq`
    ```json
    {
      "answers": [
        { "q": 1, "score": 4 }, // Số thứ tự câu hỏi và điểm (1-5)
        { "q": 2, "score": 3 }
        // ... tối đa 10 câu trả lời
      ]
    }
    ```
-   **Phản hồi thành công (200 OK):** `BaselineResultRes`
    ```json
    {
      "totalScore": 35,
      "severity": "HIGH", // Mức độ nghiêm trọng được tính toán (LOW, MODERATE, HIGH)
      "recommendedTemplateId": "uuid-cua-goi-y-muc-do-cao",
      "recommendedTemplateCode": "PLAN_HIGH_30D",
      "options": [ // Danh sách các gói có sẵn, với một gói được đánh dấu là đề xuất
        {
          "id": "uuid-cua-goi-y-muc-do-thap",
          "code": "PLAN_LOW_30D",
          "name": "Chương trình Nhẹ 30 ngày",
          "totalDays": 30,
          "recommended": false
        },
        {
          "id": "uuid-cua-goi-y-muc-do-cao",
          "code": "PLAN_HIGH_30D",
          "name": "Chương trình Chuyên sâu 30 ngày",
          "totalDays": 30,
          "recommended": true
        }
      ]
    }
    ```

### `POST /v1/programs/{planTemplateId}/join`

Ghi danh người dùng vào một chương trình mới dựa trên một mẫu kế hoạch đã chọn.

-   **Quyền hạn:** `USER`
-   **Tham số đường dẫn (Path Parameters):**
    | Tham số        | Kiểu   | Mô tả                               |
    | ---------------- | ------ | ----------------------------------- |
    | `planTemplateId` | `UUID` | ID của `PlanTemplate` để tham gia. |
-   **Nội dung yêu cầu (Request Body - Tùy chọn):**
    ```json
    {
      "trial": true // Mặc định là true nếu bỏ qua. Đặt là false để tham gia chương trình trả phí ngay lập tức.
    }
    ```
-   **Phản hồi thành công (200 OK):** `EnrollmentRes`
    ```json
    {
      "id": "uuid-cua-chuong-trinh-moi",
      "userId": "uuid-cua-nguoi-dung",
      "planTemplateId": "uuid-cua-mau-ke-hoach",
      "planCode": "PLAN_HIGH_30D",
      "status": "ACTIVE",
      "startAt": "2025-11-28T10:00:00Z",
      "endAt": null, // Hiện chưa được triển khai
      "trialUntil": "2025-12-05T10:00:00Z" // Là null nếu không phải chương trình dùng thử
    }
    ```

---

## 3. Chương trình (Programs)

Các endpoint để quản lý chương trình của người dùng.

### 3.1. Endpoint cho Người dùng (`/v1/programs`)

### `POST /v1/programs`

Tạo một chương trình mới cho người dùng. Sao chép các bài học từ một mẫu kế hoạch và tự động gán các bài quiz hệ thống.

-   **Quyền hạn:** `USER`
-   **Nội dung yêu cầu (Request Body):** `CreateProgramReq`
    ```json
    {
      "planDays": 30 // Chỉ định thời lượng mong muốn của chương trình
    }
    ```
-   **Phản hồi thành công (200 OK):** `ProgramRes`
    ```json
    {
      "id": "uuid-cua-chuong-trinh-moi",
      "status": "ACTIVE",
      "planDays": 30,
      "startDate": "2025-11-28",
      "currentDay": 1,
      "severity": "HIGH",
      "totalScore": 0,
      "entitlements": {
        "tier": "BASIC",
        "features": []
      },
      "access": {
        "entState": "ACTIVE",
        "entExp": "2026-11-28T10:00:00Z",
        "tier": "BASIC"
      }
    }
    ```

### `GET /v1/programs/active`

Lấy thông tin chương trình đang hoạt động của người dùng.

-   **Quyền hạn:** `USER`
-   **Phản hồi thành công (200 OK):** `ProgramRes` (cấu trúc tương tự như trên)
-   **Phản hồi lỗi:** `402 Payment Required` nếu bản dùng thử của người dùng đã hết hạn.

### `GET /v1/programs`

Liệt kê tất cả các chương trình thuộc về người dùng.

-   **Quyền hạn:** `USER`
-   **Phản hồi thành công (200 OK):** `List<ProgramRes>`

### 3.2. Endpoint Quản lý (`/api/programs`)

### `GET /api/programs/{id}/progress`

Lấy chi tiết tiến độ cho một chương trình cụ thể.

-   **Quyền hạn:** `USER` (chủ sở hữu), `COACH`, `ADMIN`
-   **Tham số đường dẫn (Path Parameters):**
    | Tham số | Kiểu   | Mô tả                  |
    | --------- | ------ | ---------------------- |
    | `id`      | `UUID` | ID của chương trình.   |
-   **Phản hồi thành công (200 OK):** `ProgramProgressRes`
    ```json
    {
      "id": "uuid-cua-chuong-trinh",
      "status": "ACTIVE",
      "currentDay": 15,
      "planDays": 30,
      "percentComplete": 50.0,
      "daysRemaining": 15,
      "stepsCompleted": 0, // Ghi chú: Dữ liệu tạm, chưa được triển khai
      "stepsTotal": 0,     // Ghi chú: Dữ liệu tạm, chưa được triển khai
      "streakCurrent": 10,
      "trialRemainingDays": 5 // Là null nếu không phải bản dùng thử
    }
    ```

### `GET /api/programs/{id}/trial-status`

Kiểm tra trạng thái dùng thử của một chương trình.

-   **Quyền hạn:** `USER` (chủ sở hữu), `COACH`, `ADMIN`
-   **Tham số đường dẫn (Path Parameters):**
    | Tham số | Kiểu   | Mô tả                  |
    | --------- | ------ | ---------------------- |
    | `id`      | `UUID` | ID của chương trình.   |
-   **Phản hồi thành công (200 OK):** `TrialStatusRes`
    ```json
    {
      "isTrial": true,
      "trialStartedAt": "2025-11-20T10:00:00Z",
      "trialEndExpected": "2025-11-27T10:00:00Z",
      "remainingDays": 7,
      "canUpgradeNow": true
    }
    ```

### `POST /api/programs/{id}/end`
### `POST /api/programs/{id}/pause`
### `POST /api/programs/{id}/resume`

Thay đổi trạng thái của một chương trình.

-   **Quyền hạn:** `USER` (chủ sở hữu), `ADMIN`
-   **Tham số đường dẫn (Path Parameters):**
    | Tham số | Kiểu   | Mô tả                  |
    | --------- | ------ | ---------------------- |
    | `id`      | `UUID` | ID của chương trình.   |
-   **Phản hồi thành công (200 OK):** `ProgramRes`

### `POST /api/programs/{id}/upgrade-from-trial`

Nâng cấp một chương trình từ dùng thử lên trả phí. Thường được gọi bởi Dịch vụ Thanh toán (Payment Service).

-   **Quyền hạn:** `ADMIN`, `PAYMENT_SERVICE`
-   **Tham số đường dẫn (Path Parameters):**
    | Tham số | Kiểu   | Mô tả                  |
    | --------- | ------ | ---------------------- |
    | `id`      | `UUID` | ID của chương trình.   |
-   **Phản hồi thành công (200 OK):** `ProgramRes`

### `POST /api/programs/{id}/extend-trial`

Gia hạn thời gian dùng thử cho một chương trình.

-   **Quyền hạn:** `ADMIN`
-   **Tham số đường dẫn (Path Parameters):**
    | Tham số | Kiểu   | Mô tả                  |
    | --------- | ------ | ---------------------- |
    | `id`      | `UUID` | ID của chương trình.   |
-   **Nội dung yêu cầu (Request Body):** `ExtendTrialReq`
    ```json
    {
      "additionalDays": 7
    }
    ```
-   **Phản hồi thành công (200 OK):** `TrialStatusRes`

### `PATCH /api/programs/{id}/current-day`

Cập nhật thủ công ngày hiện tại của một chương trình.

-   **Quyền hạn:** `ADMIN`
-   **Tham số đường dẫn (Path Parameters):**
    | Tham số | Kiểu   | Mô tả                  |
    | --------- | ------ | ---------------------- |
    | `id`      | `UUID` | ID của chương trình.   |
-   **Nội dung yêu cầu (Request Body):** `UpdateCurrentDayReq`
    ```json
    {
      "currentDay": 10
    }
    ```
-   **Phản hồi thành công (200 OK):** `ProgramRes`

---

## 4. Nội dung & Mẫu kế hoạch (Content & Plan Templates)

Các endpoint để quản lý các module nội dung và mẫu kế hoạch.

### 4.1. Mẫu kế hoạch (`/api/plan-templates`)

### `GET /api/plan-templates`

Liệt kê tất cả các mẫu kế hoạch có sẵn.

-   **Quyền hạn:** `ADMIN`, `COACH`, `USER`
-   **Phản hồi thành công (200 OK):** `List<PlanTemplateSummaryRes>`
    ```json
    [
      {
        "id": "uuid-cua-ke-hoach",
        "code": "PLAN_LOW_30D",
        "name": "Chương trình Nhẹ 30 ngày",
        "description": "Một khởi đầu nhẹ nhàng cho hành trình không khói thuốc của bạn.",
        "totalDays": 30
      }
    ]
    ```

### `GET /api/plan-templates/{id}`

Lấy chi tiết đầy đủ của một mẫu kế hoạch, bao gồm các bài học (steps).

-   **Quyền hạn:** `ADMIN`, `COACH`, `USER`
-   **Tham số đường dẫn (Path Parameters):**
    | Tham số | Kiểu   | Mô tả                     |
    | --------- | ------ | ------------------------- |
    | `id`      | `UUID` | ID của mẫu kế hoạch.     |
-   **Phản hồi thành công (200 OK):** `PlanTemplateDetailRes`
    ```json
    {
      "id": "uuid-cua-ke-hoach",
      "code": "PLAN_LOW_30D",
      // ...các trường khác
      "steps": [
        {
          "stepNo": 1,
          "title": "Chào mừng!",
          "details": "Giới thiệu về chương trình.",
          "dayOffset": 1,
          "type": "ARTICLE"
        }
      ]
    }
    ```

### `GET /api/plan-templates/{id}/days`
### `GET /api/plan-templates/by-code/{code}/days`

Lấy lịch trình hàng ngày cho một mẫu kế hoạch.

-   **Quyền hạn:** `ADMIN`, `COACH`, `USER`
-   **Tham số truy vấn (Query Parameters):**
    | Tham số | Kiểu      | Mặc định | Mô tả                                                 |
    | --------- | --------- | -------- | ----------------------------------------------------- |
    | `expand`  | `boolean` | `false`  | Nếu `true`, bao gồm toàn bộ nội dung của các module. |
    | `lang`    | `string`  | `vi`     | Ngôn ngữ mong muốn cho nội dung.                      |
-   **Phản hồi thành công (200 OK):** `PlanDaysRes`

### `GET /api/plan-templates/recommendation`

Đề xuất một mẫu kế hoạch dựa trên điểm số mức độ nghiêm trọng.

-   **Quyền hạn:** `ADMIN`, `COACH`, `USER`
-   **Tham số truy vấn (Query Parameters):**
    | Tham số   | Kiểu     | Mô tả                                       |
    | ---------- | -------- | ------------------------------------------- |
    | `severity` | `string` | `LOW`, `MODERATE`, `HIGH`, hoặc một điểm số. |
-   **Phản hồi thành công (200 OK):** `PlanRecommendationRes`

### 4.2. Module Nội dung (`/api/modules`)

### `POST /api/modules`

Tạo một module nội dung mới.

-   **Quyền hạn:** `ADMIN`
-   **Nội dung yêu cầu (Request Body):** `ContentModuleCreateReq`
-   **Phản hồi thành công (201 Created):** `ContentModuleRes`

### `PUT /api/modules/{id}`

Cập nhật một module nội dung đã có.

-   **Quyền hạn:** `ADMIN`
-   **Tham số đường dẫn (Path Parameters):**
    | Tham số | Kiểu   | Mô tả                       |
    | --------- | ------ | --------------------------- |
    | `id`      | `UUID` | ID của module nội dung.     |
-   **Nội dung yêu cầu (Request Body):** `ContentModuleUpdateReq`
-   **Phản hồi thành công (200 OK):** `ContentModuleRes`

### `DELETE /api/modules/{id}`

Xóa một module nội dung.

-   **Quyền hạn:** `ADMIN`
-   **Tham số đường dẫn (Path Parameters):**
    | Tham số | Kiểu   | Mô tả                       |
    | --------- | ------ | --------------------------- |
    | `id`      | `UUID` | ID của module nội dung.     |
-   **Phản hồi thành công (204 No Content):**

### `GET /api/modules`

Tìm kiếm các module nội dung.

-   **Quyền hạn:** `ADMIN`
-   **Tham số truy vấn (Query Parameters):**
    | Tham số | Kiểu      | Mặc định | Mô tả                               |
    | --------- | --------- | -------- | --------------------------------- |
    | `q`       | `string`  | `null`   | Chuỗi truy vấn tìm kiếm.          |
    | `lang`    | `string`  | `null`   | Lọc theo ngôn ngữ.               |
    | `page`    | `integer` | `0`      | Số trang cần lấy.                 |
    | `size`    | `integer` | `20`     | Số lượng mục trên mỗi trang.      |
-   **Phản hồi thành công (200 OK):** `Page<ContentModuleRes>`

### `GET /api/modules/{id}`
### `GET /api/modules/by-code/{code}`

Lấy phiên bản mới nhất của một module nội dung theo ID hoặc mã.

-   **Quyền hạn:** `ADMIN`, `COACH` (chỉ đọc), Công khai tra cứu theo mã.
-   **Tham số truy vấn (Query Parameters):**
    | Tham số | Kiểu     | Mặc định | Mô tả               |
    | --------- | -------- | -------- | ------------------- |
    | `lang`    | `string` | `null`   | Lọc theo ngôn ngữ. |
-   **Phản hồi thành công (200 OK):** `ContentModuleRes`

### `GET /api/modules/by-code/{code}/versions`

Liệt kê tất cả các phiên bản của một module nội dung.

-   **Quyền hạn:** `ADMIN`
-   **Phản hồi thành công (200 OK):** `List<ContentModuleRes>`

---

## 5. Thực thi Chương trình (Program Execution)

Các endpoint để tương tác với một chương trình đang chạy.

### 5.1. Bài học (`/api/programs/{programId}/steps`)

Quản lý các nhiệm vụ hàng ngày hoặc "bài học" trong một chương trình.

-   **Quyền hạn:** `USER`/`ADMIN` (ghi), `COACH` (chỉ đọc)
-   **Tham số đường dẫn (Path Parameters):**
    | Tham số    | Kiểu   | Mô tả                  |
    | ----------- | ------ | ---------------------- |
    | `programId` | `UUID` | ID của chương trình.   |

-   **`GET /`**: Liệt kê tất cả các bài học được gán cho chương trình.
-   **`GET /{id}`**: Lấy một bài học cụ thể.
-   **`GET /today`**: Lấy các bài học được lên lịch cho ngày hôm nay.
-   **`POST /`**: Tạo một bài học mới. (Body: `CreateStepAssignmentReq`)
-   **`DELETE /{id}`**: Xóa một bài học.
-   **`PATCH /{id}/status`**: Cập nhật trạng thái của một bài học. (Body: `UpdateStepStatusReq {status, note}`)
-   **`POST /{id}/skip`**: Bỏ qua một bài học (đặt trạng thái thành `SKIPPED`).
-   **`PATCH /{id}/reschedule`**: Đặt lại lịch cho một bài học. (Body: `RescheduleStepReq {newScheduledAt}`)

### 5.2. Sự kiện Hút thuốc (`/api/programs/{programId}/smoke-events`)

Ghi lại các sự cố hút thuốc.

-   **Quyền hạn:** `USER`/`ADMIN` (ghi), `COACH` (chỉ đọc)
-   **Tham số đường dẫn (Path Parameters):**
    | Tham số    | Kiểu   | Mô tả                  |
    | ----------- | ------ | ---------------------- |
    | `programId` | `UUID` | ID của chương trình.   |

-   **`POST /`**: Tạo một sự kiện hút thuốc mới. (Body: `CreateSmokeEventReq`)
-   **`GET /history`**: Lấy danh sách các sự kiện hút thuốc gần đây. (Query: `?size=`)
-   **`GET /stats`**: Lấy thống kê về các sự kiện hút thuốc. (Query: `?period=DAY|WEEK|MONTH`)
    -   **Ghi chú:** Trường `trend[]` trong phản hồi hiện là dữ liệu tạm và sẽ trống.

### 5.3. Chuỗi ngày (`/api/programs/{programId}/streak`)

Quản lý chuỗi ngày không hút thuốc.

-   **Quyền hạn:** `USER`/`ADMIN` (ghi), `COACH` (chỉ đọc)
-   **Tham số đường dẫn (Path Parameters):**
    | Tham số    | Kiểu   | Mô tả                  |
    | ----------- | ------ | ---------------------- |
    | `programId` | `UUID` | ID của chương trình.   |

-   **`GET /`**: Lấy chuỗi ngày hiện tại.
-   **`POST /start`**: Bắt đầu một chuỗi ngày mới (hoặc trả về chuỗi đang mở nếu có).
-   **`POST /break`**: Phá vỡ chuỗi ngày hiện tại. (Body: `BreakStreakReq`)
-   **`GET /history`**: Lấy danh sách các chuỗi ngày trong quá khứ. (Query: `?size=`)
-   **`GET /breaks`**: Lấy danh sách tất cả các lần phá vỡ chuỗi. (Query: `?size=`)

---

## 6. Trắc nghiệm (Quizzes)

Các endpoint để tạo, quản lý và làm bài trắc nghiệm.

### 6.1. Quản trị Quiz (`/v1/admin/quizzes`)

-   **Quyền hạn:** `ADMIN`

-   **`POST /`**: Tạo một mẫu quiz mới. (Body: `CreateTemplateReq`)
-   **`PUT /{id}`**: Cập nhật siêu dữ liệu của một mẫu quiz. (Body: `UpdateTemplateReq`)
-   **`PUT /{id}/archive`**: Lưu trữ một mẫu quiz.
-   **`POST /{id}/publish`**: Xuất bản một mẫu quiz để sử dụng.
-   **`POST /{id}/questions`**: Thêm một câu hỏi vào mẫu quiz. (Body: `AddQuestionReq`)
-   **`POST /{id}/questions/{qNo}/choices`**: Thêm một lựa chọn vào câu hỏi. (Body: `AddChoiceReq`)

### 6.2. Luồng làm Quiz của Người dùng (`/v1/me/quizzes`)

-   **Quyền hạn:** `USER` (chủ sở hữu)

-   **`GET /`**: Liệt kê tất cả các bài quiz đến hạn của người dùng.
-   **`POST /{templateId}/open`**: Bắt đầu một lần làm bài quiz mới. Trả về các câu hỏi và lựa chọn.
-   **`PUT /{attemptId}/answer`**: Lưu câu trả lời của người dùng cho một câu hỏi cụ thể. (Body: `AnswerReq`)
-   **`POST /{attemptId}/submit`**: Nộp bài quiz để chấm điểm. `severity` trong phản hồi được tính toán thông qua `SeverityRuleService`.

### 6.3. Endpoint Tạm thời (Placeholder)

Các endpoint sau đây đã được lên kế hoạch nhưng chưa được triển khai. Chúng sẽ trả về `501 Not Implemented`.

-   `/me/quiz/{templateId}/attempts`
-   `/attempts/{attemptId}`
-   `/retry`

---

## 7. Bảng điều khiển & Gói thuê bao (Dashboard & Subscription)

Các endpoint cho tổng quan dành riêng cho người dùng và quản lý gói thuê bao.

### `GET /api/me`

Lấy một chế độ xem bảng điều khiển tổng hợp cho người dùng hiện tại.

-   **Quyền hạn:** `USER`
-   **Phản hồi thành công (200 OK):** `DashboardRes`
    -   **Ghi chú:** Đối tượng `subscription` hiện được giả lập (mock) để trả về gói `BASIC`.

### `GET /api/subscriptions/me`

Lấy trạng thái gói thuê bao của người dùng hiện tại.

-   **Quyền hạn:** `USER`
-   **Phản hồi thành công (200 OK):** `SubscriptionStatusRes`
    -   **Ghi chú:** Hiện đang được giả lập.

### `POST /api/subscriptions/upgrade`

Nâng cấp gói thuê bao của người dùng.

-   **Quyền hạn:** `USER`
-   **Nội dung yêu cầu (Request Body):** `UpgradeReq {targetTier}`
-   **Phản hồi thành công (200 OK):** `SubscriptionStatusRes`
    -   **Ghi chú:** Hiện đang được giả lập để cấp một gói thuê bao 30 ngày cho cấp độ mục tiêu.
