# Tài Liệu API Program Service (Tiếng Việt)

**Phiên bản:** 2.0 (Hợp nhất Flow Tạo Program)
**Cập nhật lần cuối:** 02/12/2025
**Mã hóa:** UTF-8

---

## 1. Tổng quan

Tài liệu này cung cấp hướng dẫn chi tiết cho Program Service API. Service này chịu trách nhiệm quản lý chương trình của người dùng, nội dung bài học, theo dõi tiến độ và các tính năng liên quan.

### 1.1. Base URL

Tất cả các endpoint API đều tương đối với Base URL của service.

-   **Production:** `https://api.smokefree.app/program`
-   **Development:** `http://localhost:8080`

### 1.2. Xác thực (Authentication)

Tất cả các yêu cầu (request) phải được xác thực thông qua API Gateway. Các header sau là bắt buộc:

| Header          | Mô tả                                           | Ví dụ                                  |
| --------------- | ----------------------------------------------- | -------------------------------------- |
| `Authorization` | Bearer token dùng để xác thực.                  | `Bearer <JWT_TOKEN>`                   |
| `X-User-Id`     | UUID của người dùng đã đăng nhập.               | `a1b2c3d4-e5f6-7890-1234-567890abcdef` |
| `X-User-Role`   | Vai trò (Role) của người dùng.                  | `USER`, `COACH`, hoặc `ADMIN`          |
| `X-User-Tier`   | Hạng thành viên (Tier) của người dùng (Tùy chọn)| `BASIC`, `PREMIUM`, hoặc `VIP`         |

### 1.3. Các mã trạng thái HTTP chung (Common Status Codes)

| Mã    | Trạng thái             | Mô tả                                                                       |
| ----- | ---------------------- | --------------------------------------------------------------------------- |
| `200` | OK                     | Yêu cầu thành công.                                                         |
| `201` | Created                | Tài nguyên đã được tạo thành công.                                          |
| `400` | Bad Request            | Yêu cầu không hợp lệ (sai JSON, thiếu tham số...).                          |
| `401` | Unauthorized           | Xác thực thất bại hoặc chưa đăng nhập.                                      |
| `402` | Payment Required       | Yêu cầu trả phí (ví dụ: hết hạn dùng thử Trial).                            |
| `403` | Forbidden              | Người dùng không có quyền truy cập tài nguyên này.                          |
| `404` | Not Found              | Không tìm thấy tài nguyên yêu cầu.                                          |
| `409` | Conflict               | Xung đột dữ liệu (ví dụ: tạo trùng lặp).                                    |

---

## 2. Quy trình Onboarding & Đăng ký

Xử lý luồng người dùng mới và đăng ký tham gia chương trình.

### `POST /api/onboarding/baseline`

Gửi câu trả lời cho bài đánh giá đầu vào (Baseline Assessment) và nhận gợi ý lộ trình.

-   **Quyền:** `USER`
-   **Lưu ý Frontend:** Gọi API này sau khi user trả lời xong bộ câu hỏi lấy từ API `GET /baseline/quiz`. Response sẽ trả về `recommendedTemplateId`, hãy dùng ID này để gọi API tạo program.
-   **Request Body:** `QuizAnswerReq`
    ```json
    {
      "templateId": "uuid-of-template",
      "answers": [
        { "q": 1, "score": 4 }, // Số thứ tự câu hỏi (q) và điểm số (score)
        { "q": 2, "score": 3 }
      ]
    }
    ```
-   **Success Response (200 OK):** `BaselineResultRes`

### `GET /api/onboarding/baseline/quiz`

Lấy nội dung bộ câu hỏi đánh giá đầu vào.

-   **Quyền:** `USER`
-   **Success Response (200 OK):** `OpenAttemptRes`

---

## 3. Quản lý Chương trình (Programs)

Các endpoint để quản lý chương trình cai thuốc của người dùng.

### 3.1. Dành cho Người dùng (`/v1/programs`)

### `POST /v1/programs`

Tạo một chương trình mới (Bắt đầu lộ trình). Hỗ trợ cả chế độ Dùng thử (Trial) và Trả phí (Paid).

-   **Quyền:** `USER`
-   **Request Body:** `CreateProgramReq`
    ```json
    {
      "planTemplateId": "uuid-của-plan-đã-chọn", // Bắt buộc (Lấy từ Baseline Result)
      "trial": true,       // true = Dùng thử (7 ngày), false = Trả phí ngay
      "coachId": "optional-uuid"
    }
    ```
-   **Success Response (200 OK):** `ProgramRes`
    ```json
    {
      "id": "uuid-của-program-mới",
      "status": "ACTIVE",
      "planDays": 30,
      "startDate": "2025-11-28",
      "currentDay": 1,
      "access": {
        "entState": "ACTIVE",
        "entExp": "2026-11-28T10:00:00Z", // Ngày hết hạn dùng thử (nếu là trial)
        "tier": "BASIC"
      }
    }
    ```

### `GET /v1/programs/active`

Lấy thông tin chương trình đang kích hoạt (Active) của người dùng.

-   **Quyền:** `USER`
-   **Success Response (200 OK):** `ProgramRes` (cấu trúc như trên)
-   **Lưu ý Frontend:** Nếu API trả về lỗi `402 Payment Required`, nghĩa là thời hạn dùng thử (Trial) đã hết. Cần chuyển hướng user sang trang thanh toán/nâng cấp.

### 3.2. Endpoint Quản lý (`/api/programs`)

### `GET /api/programs/{id}/progress`

Lấy chi tiết tiến độ (Dashboard).

-   **Quyền:** `USER` (chủ sở hữu)
-   **Path Parameters:** `id` (UUID của program)
-   **Success Response (200 OK):** `ProgramProgressRes`
    ```json
    {
      "id": "uuid-of-program",
      "status": "ACTIVE",
      "currentDay": 15,
      "planDays": 30,
      "percentComplete": 50.0, // Phần trăm hoàn thành
      "daysRemaining": 15,
      "stepsCompleted": 5,     // Số bước đã xong
      "stepsTotal": 10,        // Tổng số bước
      "streakCurrent": 10,     // Chuỗi ngày cai hiện tại
      "trialRemainingDays": 5  // Số ngày dùng thử còn lại (null nếu không phải trial)
    }
    ```

### `POST /api/programs/{id}/pause`
### `POST /api/programs/{id}/resume`
### `POST /api/programs/{id}/end`

Thay đổi trạng thái chương trình (Tạm dừng / Tiếp tục / Kết thúc sớm).

-   **Quyền:** `USER` (chủ sở hữu)
-   **Success Response (200 OK):** `ProgramRes`

### `GET /api/programs/{id}/trial-status`

Kiểm tra trạng thái dùng thử.

-   **Quyền:** `USER` (chủ sở hữu)
-   **Success Response (200 OK):** `TrialStatusRes`
    ```json
    {
      "isTrial": true,
      "trialStartedAt": "...",
      "trialEndExpected": "...",
      "remainingDays": 2,
      "canUpgradeNow": true // Có thể nâng cấp ngay bây giờ không
    }
    ```

---

## 4. Mẫu Lộ trình & Nội dung (Content)

### 4.1. Plan Templates (`/api/plan-templates`)

### `GET /api/plan-templates`

Danh sách tất cả các mẫu lộ trình có sẵn.

-   **Quyền:** `Authenticated` (Đã đăng nhập)
-   **Success Response (200 OK):** `List<PlanTemplateSummaryRes>`

### `GET /api/plan-templates/{id}`

Xem chi tiết một mẫu lộ trình.

-   **Quyền:** `Authenticated`
-   **Success Response (200 OK):** `PlanTemplateDetailRes`

### `GET /api/plan-templates/by-code/{code}/days`

Lấy lịch trình chi tiết theo mã template.

-   **Quyền:** `Authenticated`
-   **Query Params:** `expand` (boolean - mở rộng nội dung), `lang` (string - ngôn ngữ)
-   **Success Response (200 OK):** `PlanDaysRes`

### 4.2. Content Modules (`/api/modules`)

### `POST /api/modules` (Admin)

Tạo nội dung mới (Bài viết/Video/Audio...).

-   **Quyền:** `ADMIN`
-   **Request Body:** `ContentModuleCreateReq`
    ```json
    {
      "code": "ARTICLE_001",
      "type": "ARTICLE",
      "lang": "vi",
      "payload": { "title": "...", "content": "..." }
    }
    ```
-   **Success Response (201 Created):** `ContentModuleRes`

### `GET /api/modules` (Search)

Tìm kiếm nội dung.

-   **Quyền:** `ADMIN`
-   **Query Params:** `q` (từ khóa), `lang`, `page`, `size`
-   **Success Response:** `Page<ContentModuleRes>`

### `GET /api/modules/by-code/{code}`

Lấy nội dung theo mã (Code).

-   **Quyền:** Public/Authenticated
-   **Success Response:** `ContentModuleRes`

---

## 5. Hoạt động Hằng ngày (Execution)

### 5.1. Steps (`/api/programs/{programId}/steps`)

### `GET /today`

Lấy danh sách nhiệm vụ (steps) của ngày hôm nay.

-   **Quyền:** `USER`
-   **Success Response (200 OK):** `List<StepAssignment>`

### `PATCH /{id}/status`

Cập nhật trạng thái nhiệm vụ (ví dụ: Đã hoàn thành).

-   **Quyền:** `USER`
-   **Request Body:** `UpdateStepStatusReq`
    ```json
    {
      "status": "COMPLETED",
      "note": "Ghi chú tùy chọn (User cảm thấy thế nào...)"
    }
    ```
-   **Success Response (200 OK):** (Empty Body)

### 5.2. Smoke Events (`/api/programs/{programId}/smoke-events`)

### `POST /`

Ghi nhận sự kiện hút thuốc hoặc thèm thuốc.

-   **Quyền:** `USER`
-   **Request Body:** `CreateSmokeEventReq`
    ```json
    {
      "eventType": "SMOKE", // Loại: SMOKE (Hút) hoặc URGE (Thèm)
      "kind": "SLIP",       // Kiểu: SLIP (Lỡ hút), LAPSE, RELAPSE...
      "puffs": 3,           // Số hơi hút (nếu có)
      "reason": "STRESS",   // Lý do
      "note": "...",        // Ghi chú thêm
      "eventAt": "ISO-8601", // Thời gian xảy ra
      "occurredAt": "ISO-8601"
    }
    ```
-   **Success Response (200 OK):** `SmokeEventRes`

### `GET /history`

Lấy lịch sử hút thuốc.

-   **Query Params:** `size` (số lượng bản ghi)
-   **Success Response:** `List<SmokeEventRes>`

### 5.3. Streaks (`/api/programs/{programId}/streak`)

### `GET /`

Lấy thông tin chuỗi cai thuốc (Streak) hiện tại.

-   **Success Response (200 OK):** `StreakView`
    ```json
    {
      "streakId": "uuid",
      "currentStreak": 5,        // Số ngày cai liên tục hiện tại
      "bestStreak": 10,          // Kỷ lục tốt nhất
      "daysWithoutSmoke": 5,     // Số ngày không hút
      "startedAt": "...",        // Thời điểm bắt đầu streak
      "endedAt": null            // Thời điểm kết thúc (null nếu đang duy trì)
    }
    ```

---

## 6. Quiz Engine (Hệ thống Trắc nghiệm)

### 6.1. Admin Quiz (`/v1/admin/quizzes`)

-   `POST /`: Tạo Template Quiz mới.
-   `POST /{id}/publish`: Xuất bản Template.
-   `POST /{id}/questions`: Thêm câu hỏi vào Template.

### 6.2. User Quiz (`/v1/me/quizzes`)

### `GET /` (List Due)

Lấy danh sách các bài kiểm tra đang cần làm (Weekly Check-in, Recovery Quiz...).

-   **Success Response:** `Map<String, Object>`
    ```json
    {
      "success": true,
      "data": [
        { 
          "attemptId": null, // null nghĩa là chưa bắt đầu làm
          "templateId": "...", 
          "type": "WEEKLY", 
          "deadline": "..." 
        }
      ],
      "count": 1
    }
    ```

### `POST /{templateId}/open`

Bắt đầu làm bài (Mở lượt làm bài - Attempt).

-   **Success Response:** `OpenAttemptRes` (Chứa danh sách câu hỏi và các lựa chọn)

### `PUT /{attemptId}/answer`

Lưu câu trả lời cho từng câu hỏi.

-   **Lưu ý Frontend:** Nên gọi API này ngay sau khi user chọn đáp án để lưu nháp, tránh mất dữ liệu.
-   **Request Body:** `AnswerReq`
    ```json
    { "questionNo": 1, "answer": 2 } // Câu số 1, chọn đáp án có giá trị/index là 2
    ```
-   **Success Response:** `{ "success": true, "message": "..." }`

### `POST /{attemptId}/submit`

Nộp bài và chấm điểm.

-   **Success Response:**
    ```json
    {
      "success": true,
      "data": {
        "attemptId": "uuid",
        "totalScore": 10,
        "severity": "LOW" // Kết quả đánh giá
      }
    }
    ```

---

## 7. Đăng ký (Subscription)

### `GET /api/subscriptions/me`

Kiểm tra trạng thái gói đăng ký hiện tại.

-   **Success Response:** `SubscriptionStatusRes` (Hiện tại là dữ liệu giả lập - Mock)

### `POST /api/subscriptions/upgrade`

Nâng cấp hạng thành viên (Tier).

-   **Request Body:** `{ "targetTier": "PREMIUM" }`
-   **Success Response:** `SubscriptionStatusRes`
