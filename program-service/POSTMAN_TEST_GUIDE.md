# Hướng dẫn Kiểm thử API Dịch vụ Chương trình bằng Postman

**Phiên bản:** 2.0
**Cập nhật lần cuối:** 28-11-2025
**Bảng mã:** UTF-8

Tài liệu này hướng dẫn cách sử dụng bộ sưu tập Postman `smoke-free-complete-test.postman_collection.json` để kiểm thử các chức năng của Dịch vụ Chương trình (Program Service).

---

## 1. Cài đặt Môi trường

### 1.1. Tạo Environment

Tạo một môi trường mới trong Postman với tên `Program Service Dev`.

### 1.2. Cấu hình Biến Môi trường (Environment Variables)

Các biến này cho phép tái sử dụng và dễ dàng chuyển đổi giữa các môi trường khác nhau.

| Biến              | Giá trị Mẫu                            | Mô tả                                                              |
| ----------------- | -------------------------------------- | ------------------------------------------------------------------ |
| `baseUrl`         | `http://localhost:8080`                | URL gốc của dịch vụ đang chạy local.                               |
| `accessToken`     | `your_jwt_token_here`                  | Token xác thực (nếu có). Có thể để trống khi test nội bộ.          |
| `userId`          | `uuid-cua-user-A`                      | UUID của một người dùng có vai trò `USER`.                          |
| `coachId`         | `uuid-cua-coach-B`                     | UUID của một người dùng có vai trò `COACH`.                         |
| `adminId`         | `uuid-cua-admin-C`                     | UUID của một người dùng có vai trò `ADMIN`.                         |
| `planTemplateId`  | `uuid-cua-plan-template`               | ID của một `PlanTemplate` có sẵn trong DB để test ghi danh.        |
| `programId`       | (Để trống)                             | Sẽ được tự động cập nhật bởi các request tạo chương trình.          |
| `stepId`          | (Để trống)                             | Sẽ được tự động cập nhật bởi các request tạo bài học.              |
| `moduleId`        | (Để trống)                             | Sẽ được tự động cập nhật bởi các request tạo module.               |
| `attemptId`       | (Để trống)                             | Sẽ được tự động cập nhật bởi request "Open Quiz Attempt".          |

---

## 2. Xác thực & Headers

Hệ thống sử dụng các header để xác định người dùng và vai trò, cho phép mô phỏng các kịch bản phân quyền khác nhau.

-   **Headers Bắt buộc cho mọi Request:**
    -   `Authorization`: `Bearer {{accessToken}}` (Có thể không cần thiết khi gateway chưa tích hợp).
    -   `X-User-Id`: `{{userId}}` (Thay đổi giá trị thành `{{coachId}}` hoặc `{{adminId}}` tùy theo vai trò cần test).
    -   `X-User-Role`: `USER`, `COACH`, hoặc `ADMIN`.

-   **Ví dụ Kịch bản Phân quyền:**
    -   **Test với vai trò USER:** Đặt `X-User-Id: {{userId}}` và `X-User-Role: USER`.
    -   **Test với vai trò ADMIN:** Đặt `X-User-Id: {{adminId}}` và `X-User-Role: ADMIN`.
    -   **Test với vai trò COACH:** Đặt `X-User-Id: {{coachId}}` và `X-User-Role: COACH`. Lưu ý rằng chương trình đang được kiểm thử phải được gán cho coach này.

---

## 3. Các Kịch bản Kiểm thử Chính (Test Cases)

Dưới đây là các kịch bản kiểm thử được nhóm theo chức năng.

### 3.1. Onboarding & Enrollment

| Kịch bản                               | Endpoint & Method                  | Vai trò | Body / Params                                       | Kết quả Mong muốn (Assertions)                                     |
| --------------------------------------- | ---------------------------------- | ------ | --------------------------------------------------- | ------------------------------------------------------------------ |
| **Gửi đánh giá ban đầu**                | `POST /api/onboarding/baseline`    | `USER` | `QuizAnswerReq` với 10 câu trả lời.                  | `200 OK`, trả về `BaselineResultRes` chứa `severity` và `recommendedTemplateId`. |
| **Ghi danh chương trình (Dùng thử)**     | `POST /v1/programs/{{planTemplateId}}/join` | `USER` | `{"trial": true}` hoặc không có body.             | `200 OK`, trả về `EnrollmentRes` với `trialUntil` khác null.        |
| **Ghi danh chương trình (Trả phí)**      | `POST /v1/programs/{{planTemplateId}}/join` | `USER` | `{"trial": false}`                                  | `200 OK`, trả về `EnrollmentRes` với `trialUntil` là null.          |
| **Ghi danh khi đã có chương trình**      | `POST /v1/programs/{{planTemplateId}}/join` | `USER` | (Thực hiện sau khi đã tạo thành công một chương trình) | `409 Conflict`.                                                    |

### 3.2. Programs

| Kịch bản                               | Endpoint & Method                  | Vai trò | Body / Params                                       | Kết quả Mong muốn (Assertions)                                     |
| --------------------------------------- | ---------------------------------- | ------ | --------------------------------------------------- | ------------------------------------------------------------------ |
| **Lấy chương trình đang hoạt động**      | `GET /v1/programs/active`          | `USER` | -                                                   | `200 OK`.                                                          |
| **Lấy chương trình khi trial hết hạn**   | `GET /v1/programs/active`          | `USER` | (Yêu cầu mock DB `trialEndExpected` < now)          | `402 Payment Required`.                                            |
| **Lấy danh sách chương trình**           | `GET /v1/programs`                 | `USER` | -                                                   | `200 OK`, trả về một danh sách.                                     |
| **Tạm dừng/Tiếp tục/Kết thúc**          | `POST /api/programs/{{programId}}/{action}` | `USER` | `action` là `pause`, `resume`, `end`.               | `200 OK`, trạng thái chương trình được cập nhật.                    |
| **Gia hạn trial**                        | `POST /api/programs/{{programId}}/extend-trial` | `ADMIN`| `{"additionalDays": 7}`                           | `200 OK`, `trialEndExpected` được cập nhật.                         |
| **Lấy tiến độ chương trình**             | `GET /api/programs/{{programId}}/progress` | `USER` | -                                                   | `200 OK`, `percentComplete` được tính đúng, `stepsCompleted` là 0. |

### 3.3. Content & Plan Templates

| Kịch bản                               | Endpoint & Method                  | Vai trò | Body / Params                                       | Kết quả Mong muốn (Assertions)                                     |
| --------------------------------------- | ---------------------------------- | ------ | --------------------------------------------------- | ------------------------------------------------------------------ |
| **Lấy danh sách mẫu kế hoạch**           | `GET /api/plan-templates`          | `USER` | -                                                   | `200 OK`.                                                          |
| **Lấy chi tiết lịch trình (expand)**     | `GET /api/plan-templates/by-code/{code}/days` | `USER` | `expand=true`                                       | `200 OK`, `moduleBrief` trong response phải chứa `payload`.        |
| **CRUD Module Nội dung**                | `POST`, `PUT`, `DELETE /api/modules` | `ADMIN`| Body tương ứng.                                     | `201` (POST), `200` (PUT), `204` (DELETE).                          |
| **Thử CRUD Module với vai trò USER**    | `POST /api/modules`                | `USER` | Body bất kỳ.                                        | `403 Forbidden`.                                                   |

### 3.4. Program Execution (Steps, Smoke Events, Streaks)

| Kịch bản                               | Endpoint & Method                  | Vai trò | Body / Params                                       | Kết quả Mong muốn (Assertions)                                     |
| --------------------------------------- | ---------------------------------- | ------ | --------------------------------------------------- | ------------------------------------------------------------------ |
| **Lấy bài học hôm nay**                 | `GET /api/programs/{{programId}}/steps/today` | `USER` | -                                                   | `200 OK`.                                                          |
| **Hoàn thành một bài học**              | `PATCH /api/programs/{{programId}}/steps/{{stepId}}/status` | `USER` | `{"status": "COMPLETED"}`                         | `200 OK`.                                                          |
| **Coach thử hoàn thành bài học**        | `PATCH /api/programs/{{programId}}/steps/{{stepId}}/status` | `COACH`| `{"status": "COMPLETED"}`                         | `403 Forbidden`.                                                   |
| **Ghi nhận một lần hút thuốc (Slip)**   | `POST /api/programs/{{programId}}/smoke-events` | `USER` | `{"kind": "SLIP", ...}`                           | `200 OK`. Streak của chương trình phải được reset.                  |
| **Bắt đầu một chuỗi ngày**              | `POST /api/programs/{{programId}}/streak/start` | `USER` | -                                                   | `200 OK`, trả về `StreakView`.                                      |
| **Coach thử phá vỡ chuỗi ngày**         | `POST /api/programs/{{programId}}/streak/break` | `COACH`| Body bất kỳ.                                        | `403 Forbidden`.                                                   |

### 3.5. Quizzes

| Kịch bản                               | Endpoint & Method                  | Vai trò | Body / Params                                       | Kết quả Mong muốn (Assertions)                                     |
| --------------------------------------- | ---------------------------------- | ------ | --------------------------------------------------- | ------------------------------------------------------------------ |
| **Admin tạo câu hỏi cho Quiz**          | `POST /v1/admin/quizzes/{{templateId}}/questions` | `ADMIN`| `AddQuestionReq`                                    | `200 OK`, trả về `questionId`.                                      |
| **Lấy danh sách Quiz đến hạn**          | `GET /v1/me/quizzes`               | `USER` | -                                                   | `200 OK`, trả về danh sách `DueItem`.                              |
| **Mở một bài Quiz**                     | `POST /v1/me/quizzes/{{templateId}}/open` | `USER` | -                                                   | `201 Created`, trả về `OpenAttemptRes` chứa câu hỏi.                |
| **Mở Quiz khi trial hết hạn**           | `POST /v1/me/quizzes/{{templateId}}/open` | `USER` | (Yêu cầu mock DB `trialEndExpected` < now)          | `402 Payment Required`.                                            |
| **Lưu và Nộp bài Quiz**                 | 1. `PUT /v1/me/quizzes/{{attemptId}}/answer` <br> 2. `POST /v1/me/quizzes/{{attemptId}}/submit` | `USER` | 1. `AnswerReq` <br> 2. -                            | `200 OK` cho cả hai. Response của Submit phải có `severity` hợp lý. |
| **Truy cập endpoint placeholder**       | `GET /me/quiz/{{templateId}}/attempts` | `USER` | -                                                   | `501 Not Implemented`.                                             |

---

## 4. Luồng Kiểm thử Phức hợp (End-to-End Flows)

### 4.1. Luồng Phân quyền USER vs. COACH

1.  **Vai trò USER:** Đặt header `X-User-Id: {{userId}}`, `X-User-Role: USER`.
2.  **Thực hiện:** Gọi `POST /api/programs/{{programId}}/smoke-events` (ghi dữ liệu).
3.  **Kiểm tra:** Mong đợi `200 OK`.
4.  **Vai trò COACH:** Đổi header `X-User-Id: {{coachId}}`, `X-User-Role: COACH`.
5.  **Thực hiện:** Gọi lại `POST /api/programs/{{programId}}/smoke-events` với cùng `programId`.
6.  **Kiểm tra:** Mong đợi `403 Forbidden`.
7.  **Thực hiện:** Gọi `GET /api/programs/{{programId}}/smoke-events/history` (đọc dữ liệu).
8.  **Kiểm tra:** Mong đợi `200 OK`.

### 4.2. Luồng Hoàn chỉnh một Bài Quiz

1.  **Lấy Quiz đến hạn:** Gọi `GET /v1/me/quizzes` để lấy `templateId` của một bài quiz.
2.  **Mở bài Quiz:** Gọi `POST /v1/me/quizzes/{{templateId}}/open`. Lưu `attemptId` từ response vào biến môi trường.
3.  **Trả lời câu hỏi:** Lặp qua các câu hỏi trong response, gọi `PUT /v1/me/quizzes/{{attemptId}}/answer` cho mỗi câu.
4.  **Nộp bài:** Gọi `POST /v1/me/quizzes/{{attemptId}}/submit`.
5.  **Xác thực:** Kiểm tra response chứa `totalScore` và `severity` chính xác.

---

## 5. Hướng dẫn Assertions & Troubleshooting

### 5.1. Các Assertions Quan trọng

Trong tab "Tests" của mỗi request, hãy thêm các kiểm thử JavaScript để xác thực:

-   **Status Code:** `pm.test("Status code is 200", () => pm.response.to.have.status(200));`
-   **Quyền truy cập:** `pm.test("Status code is 403 for forbidden access", () => pm.response.to.have.status(403));`
-   **Schema & Kiểu dữ liệu:** `pm.test("ID should be a valid UUID", () => { const res = pm.response.json(); pm.expect(res.id).to.match(/^[...]/); });`
-   **Nội dung Response:** `pm.test("Response should contain a 'severity' field", () => pm.expect(pm.response.json()).to.have.property('severity'));`
-   **Sắp xếp:** Với các endpoint lịch sử, kiểm tra xem item đầu tiên có ngày tạo mới hơn item thứ hai.

### 5.2. Xử lý sự cố (Troubleshooting)

| Mã lỗi | Nguyên nhân & Giải pháp                                                              |
| ------- | ------------------------------------------------------------------------------------ |
| `401`   | **Token sai/thiếu.** Kiểm tra biến `accessToken` và header `Authorization`.           |
| `403`   | **Không có quyền.** Vai trò không phù hợp (ví dụ: COACH ghi dữ liệu) hoặc `programId` không thuộc `userId` đang dùng. |
| `404`   | **Không tìm thấy.** ID trong URL (ví dụ: `{{programId}}`) không tồn tại trong DB.     |
| `409`   | **Xung đột.** Thường do tạo tài nguyên đã tồn tại (ví dụ: tạo program khi đã có một cái `ACTIVE`). |
| `500`   | **Lỗi Server.** Kiểm tra log của service. Thường do thiếu dữ liệu seed (template, module) hoặc lỗi kết nối DB. |
