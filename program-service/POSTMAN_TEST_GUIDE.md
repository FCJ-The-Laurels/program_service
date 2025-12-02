# Tài liệu API Toàn diện - Program Service

**Phiên bản:** 1.0
**Ngày cập nhật:** 2025-12-06

Tài liệu này cung cấp phân tích chi tiết và hướng dẫn sử dụng cho toàn bộ các API của `program-service`. Nội dung được cấu trúc theo từng Controller để dễ dàng tra cứu và thực hiện kiểm thử.

---

### **Phần A: Chuẩn bị Môi trường**

#### **1. Biến môi trường Postman**

| Tên Biến      | Giá trị Mẫu                               | Mô tả                                            |
| :------------ | :---------------------------------------- | :----------------------------------------------- |
| `baseUrl`     | `http://localhost:8080`                   | URL cơ sở của `program-service`.                 |
| `userId`      | `a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11`    | UUID của một người dùng test (vai trò `CUSTOMER`).|
| `adminId`     | `a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12`    | UUID của một người dùng test (vai trò `ADMIN`).   |
| `authToken`   | `Bearer <YOUR_JWT_TOKEN>`                 | Token xác thực lấy từ dịch vụ `auth-service`.    |
| `programId`   |                                           | Sẽ được set tự động sau khi tạo chương trình.    |

---

## **I. Luồng Người dùng (End-User APIs)**

### **1. OnboardingFlowController**
*   **Mục đích:** Xử lý luồng ban đầu cho người dùng mới.
*   **Path:** `/api/onboarding`

#### `POST /baseline`
*   **Mô tả:** Người dùng nộp bài quiz ban đầu (baseline). Hệ thống sẽ tính điểm, xác định mức độ phụ thuộc (`severity`), và trả về danh sách các kế hoạch (`PlanOption`) phù hợp, trong đó có một kế hoạch được đề xuất.
*   **Authorization:** `isAuthenticated()`
*   **Request Body (`QuizAnswerReq`):**
    ```json
    {
      "templateId": "UUID_CUA_BASELINE_QUIZ_TEMPLATE",
      "answers": [
        { "q": 1, "score": 3 },
        { "q": 2, "score": 2 }
      ]
    }
    ```
*   **Response (200 OK - `BaselineResultRes`):**
    ```json
    {
      "totalScore": 5,
      "severity": "MODERATE",
      "recommendedTemplateId": "f0e9d8c7-b6a5-4321-fedc-ba9876543210",
      "recommendedTemplateCode": "L2_45D",
      "options": [
        { "id": "c1d2e3f4-...", "code": "L1_30D", "name": "Chương trình 30 ngày", "totalDays": 30, "recommended": false },
        { "id": "f0e9d8c7-...", "code": "L2_45D", "name": "Chương trình 45 ngày", "totalDays": 45, "recommended": true }
      ]
    }
    ```
*   **Luồng xử lý:** `OnboardingFlowController` -> `OnboardingFlowService.submitBaselineAndRecommend` -> `QuizService.submitAnswers` & `SeverityRuleService.recommendPlanCode`.
*   **Entity liên quan:** `QuizTemplate`, `PlanTemplate`.

---

### **2. ProgramController**
*   **Mục đích:** Quản lý vòng đời cơ bản của chương trình cai thuốc.
*   **Path:** `/v1/programs`

#### `POST /`
*   **Mô tả:** Tạo một chương trình mới cho người dùng dựa trên `planDays` đã chọn. API này sẽ tự động tìm `PlanTemplate` phù hợp, tạo `Program`, và khởi tạo các `StepAssignment` (nhiệm vụ hàng ngày) và `QuizAssignment` (lịch hẹn làm quiz) tương ứng.
*   **Headers:** `X-User-Id`, `X-User-Tier` (optional).
*   **Request Body (`CreateProgramReq`):**
    ```json
    {
      "planDays": 45,
      "coachId": "UUID_CUA_COACH"
    }
    ```
*   **Response (200 OK - `ProgramRes`):**
    ```json
    {
        "id": "b1c2d3e4-...",
        "status": "ACTIVE",
        "planDays": 45,
        "startDate": "2023-10-27",
        "currentDay": 1,
        ...
    }
    ```
*   **Luồng xử lý:** `ProgramController` -> `ProgramService.createProgram` -> `ProgramCreationService.createPaidProgram`, `stepAssignmentService.createForProgramFromTemplate`, `assignSystemQuizzes`.
*   **Entity liên quan:** `Program`, `PlanTemplate`, `StepAssignment`, `QuizAssignment`.

#### `GET /active`
*   **Mô tả:** Lấy thông tin chi tiết về chương trình đang ở trạng thái `ACTIVE` của người dùng.
*   **Headers:** `X-User-Id`, `X-User-Tier` (optional), etc.
*   **Response (200 OK - `ProgramRes`):** Tương tự `POST /`.
*   **Luồng xử lý:** `ProgramController` -> `ProgramService.getActive`.
*   **Entity liên quan:** `Program`.

---

### **3. MeController**
*   **Mục đích:** Cung cấp một endpoint tổng hợp để người dùng lấy thông tin dashboard.
*   **Path:** `/api/me`

#### `GET /`
*   **Mô tả:** Trả về một đối tượng JSON lớn chứa thông tin về gói đăng ký, chương trình đang hoạt động, các bài quiz đến hạn, và thông tin chuỗi streak.
*   **Authorization:** `isAuthenticated()`
*   **Response (200 OK - `DashboardRes`):**
    ```json
    {
      "userId": "...",
      "subscription": { "tier": "PREMIUM", "status": "ACTIVE", ... },
      "activeProgram": { "id": "...", "currentDay": 1, ... },
      "dueQuizzes": [ { "templateId": "...", "templateName": "...", ... } ],
      "streakInfo": { "currentStreak": 1, "longestStreak": 5, ... }
    }
    ```
*   **Luồng xử lý:** `MeController` -> `MeService.dashboard`. Service này sẽ tổng hợp dữ liệu từ nhiều service/repository khác.
*   **Entity liên quan:** `Program`, `QuizAssignment`, `Streak`.

---

### **4. SmokeEventController & StreakController**
*   **Mục đích:** Quản lý các sự kiện hút thuốc và chuỗi ngày không hút.

#### `POST /api/programs/{programId}/smoke-events`
*   **Mô tả:** Ghi lại một sự kiện liên quan đến hút thuốc. Nếu `kind` là `SLIP` hoặc `RELAPSE`, nó sẽ kích hoạt luồng ngắt chuỗi và có thể gán nhiệm vụ phục hồi.
*   **Authorization:** `isAuthenticated()`
*   **Request Body (`CreateSmokeEventReq`):**
    ```json
    {
      "eventType": "SMOKE",
      "kind": "SLIP",
      "note": "Căng thẳng quá.",
      "puffs": 2,
      "reason": "STRESS"
    }
    ```
*   **Response (200 OK - `SmokeEventRes`):** Trả về đối tượng `SmokeEvent` vừa được tạo.
*   **Luồng xử lý:** `SmokeEventController` -> `SmokeEventService.create` -> `StreakService.breakStreakAndLog` -> `handleRecoveryAssignment`.
*   **Entity liên quan:** `SmokeEvent`, `Program`, `Streak`, `StreakBreak`, `QuizAssignment` (nếu có), `StepAssignment` (nếu có).

#### `GET /api/programs/{programId}/streak`
*   **Mô tả:** Lấy thông tin về chuỗi streak hiện tại của chương trình.
*   **Authorization:** `isAuthenticated()`
*   **Response (200 OK - `StreakView`):**
    ```json
    {
        "streakId": "...",
        "currentStreak": 5,
        "bestStreak": 10,
        "daysWithoutSmoke": 5,
        "startedAt": "...",
        "endedAt": null
    }
    ```
*   **Luồng xử lý:** `StreakController` -> `StreakService.current`.
*   **Entity liên quan:** `Program`, `Streak`.

---

### **5. StepController**
*   **Mục đích:** Quản lý các nhiệm vụ (`Step`) hàng ngày của người dùng.
*   **Path:** `/api/programs/{programId}/steps`

#### `GET /today`
*   **Mô tả:** Lấy danh sách tất cả các nhiệm vụ được lên lịch cho ngày hôm nay.
*   **Authorization:** `isAuthenticated()`
*   **Response (200 OK - `List<StepAssignment>`):** Trả về danh sách các entity `StepAssignment`.
*   **Luồng xử lý:** `StepController` -> `StepAssignmentService.listByProgramAndDate`.
*   **Entity liên quan:** `StepAssignment`, `Program`.

#### `PATCH /{id}/status`
*   **Mô tả:** Cập nhật trạng thái của một nhiệm vụ (ví dụ: `COMPLETED`). Khi tất cả nhiệm vụ trong ngày hoàn thành, nó sẽ kích hoạt logic cập nhật streak.
*   **Authorization:** `hasAnyRole('USER','ADMIN')`
*   **Request Body (`UpdateStepStatusReq`):**
    ```json
    { "status": "COMPLETED" }
    ```
*   **Response (200 OK):** (Không có body)
*   **Luồng xử lý:** `StepController` -> `StepAssignmentService.updateStatus` -> `handleDayCompletion` -> `StreakService`.
*   **Entity liên quan:** `StepAssignment`, `Program`, `Streak`.

---

### **6. MeQuizController**
*   **Mục đích:** Cung cấp API cho người dùng làm các bài quiz được giao.
*   **Path:** `/v1/me/quizzes`

#### `GET /`
*   **Mô tả:** Lấy danh sách các bài quiz đến hạn hoặc quá hạn.
*   **Headers:** `X-User-Id`
*   **Response (200 OK):**
    ```json
    {
      "success": true,
      "data": [ { "templateId": "...", "templateName": "...", ... } ],
      "count": 1
    }
    ```
*   **Luồng xử lý:** `MeQuizController` -> `QuizFlowService.listDue`.
*   **Entity liên quan:** `QuizAssignment`, `Program`.

#### `POST /{templateId}/open`
*   **Mô tả:** Bắt đầu một lượt làm bài quiz mới.
*   **Headers:** `X-User-Id`
*   **Response (201 Created - `OpenAttemptRes`):** Trả về cấu trúc câu hỏi và lựa chọn của bài quiz.
*   **Luồng xử lý:** `MeQuizController` -> `QuizFlowService.openAttempt`.
*   **Entity liên quan:** `QuizAttempt`, `QuizTemplate`.

#### `POST /{attemptId}/submit`
*   **Mô tả:** Nộp bài quiz đã hoàn thành và nhận kết quả.
*   **Headers:** `X-User-Id`
*   **Response (200 OK - `SubmitRes`):**
    ```json
    {
      "success": true,
      "data": { "attemptId": "...", "totalScore": 15, "severity": "HIGH" }
    }
    ```
*   **Luồng xử lý:** `MeQuizController` -> `QuizFlowService.submit`.
*   **Entity liên quan:** `QuizAttempt`, `QuizAnswer`, `QuizResult`, `Program`.

---

## **II. Luồng Quản trị (Admin APIs)**

### **7. AdminQuizController**
*   **Mục đích:** Cung cấp API cho quản trị viên để CRUD các `QuizTemplate`.
*   **Path:** `/v1/admin/quizzes`

#### `POST /`
*   **Mô tả:** Tạo một `QuizTemplate` hoàn chỉnh, bao gồm câu hỏi và lựa chọn.
*   **Authorization:** `hasRole('ADMIN')` (Dự kiến)
*   **Request Body (`CreateFullQuizReq`):**
    ```json
    {
      "name": "Recovery Quiz 1",
      "code": "RECOVERY_QUIZ_1",
      "questions": [
        {
          "orderNo": 1,
          "questionText": "Lý do chính khiến bạn hút thuốc lần này là gì?",
          "type": "SINGLE_CHOICE",
          "choices": [
            { "labelCode": "STRESS", "labelText": "Do căng thẳng", "weight": 1 },
            { "labelCode": "SOCIAL", "labelText": "Do giao tiếp xã hội", "weight": 1 }
          ]
        }
      ]
    }
    ```
*   **Response (201 Created):** `{ "id": "...", "message": "..." }`
*   **Luồng xử lý:** `AdminQuizController` -> `AdminQuizService.createFullQuiz`.
*   **Entity liên quan:** `QuizTemplate`, `QuizTemplateQuestion`, `QuizChoiceLabel`.

---

### **8. ModuleController**
*   **Mục đích:** Quản lý các "module" nội dung có thể tái sử dụng.
*   **Path:** `/api/modules`

#### `POST /`
*   **Mô tả:** Tạo một module nội dung mới (ví dụ: một bài viết, video).
*   **Authorization:** `hasRole('ADMIN')`
*   **Request Body (`ContentModuleCreateReq`):**
    ```json
    {
        "code": "RECOVERY_TASK_3",
        "type": "ARTICLE",
        "lang": "vi",
        "payload": {
            "title": "Bài đọc củng cố ý chí",
            "content": "Nội dung bài đọc..."
        }
    }
    ```
*   **Response (201 Created - `ContentModuleRes`):** Trả về module vừa tạo.
*   **Luồng xử lý:** `ModuleController` -> `ContentModuleService.create`.
*   **Entity liên quan:** `ContentModule`.

---

### **9. ProgramManagementController**
*   **Mục đích:** Cung cấp các API quản trị cho một chương trình cụ thể.
*   **Path:** `/api/programs`

#### `PATCH /{id}/current-day`
*   **Mô tả:** Cập nhật ngày hiện tại của chương trình. Đây là một API quan trọng để giả lập việc "qua ngày mới" trong môi trường test.
*   **Authorization:** `hasRole('ADMIN')`
*   **Request Body (`UpdateCurrentDayReq`):**
    ```json
    { "currentDay": 2 }
    ```
*   **Response (200 OK - `ProgramRes`):** Trả về trạng thái mới của chương trình.
*   **Luồng xử lý:** `ProgramManagementController` -> `ProgramRepository.findById` -> `program.setCurrentDay` -> `programRepository.save`.
*   **Entity liên quan:** `Program`.

---

## **III. Luồng Test Admin: Tạo và Lên lịch Quiz**

Phần này mô tả một luồng test end-to-end, trong đó Admin sẽ tạo các loại quiz khác nhau và lên lịch chúng cho một chương trình. Sau đó, chúng ta sẽ kiểm tra từ phía Customer để xác nhận rằng các quiz đã được gán đúng.

**Kịch bản:**
1.  **Admin** tạo một bài quiz "Đánh giá ban đầu" (`ONBOARDING_ASSESSMENT`).
2.  **Admin** tạo một bài quiz "Kiểm tra hàng tuần" (`WEEKLY_CHECKIN`).
3.  **Admin** lên lịch cho quiz "Đánh giá ban đầu" sẽ diễn ra **một lần duy nhất** vào **Ngày 1** của chương trình.
4.  **Admin** lên lịch cho quiz "Kiểm tra hàng tuần" sẽ diễn ra **lặp lại mỗi 7 ngày**, bắt đầu từ **Ngày 7**.
5.  **Customer** đăng ký một chương trình mới.
6.  **Customer** kiểm tra và thấy quiz "Đánh giá ban đầu" đến hạn.
7.  **Admin** giả lập thời gian trôi qua, chuyển chương trình của Customer sang **Ngày 7**.
8.  **Customer** kiểm tra và thấy quiz "Kiểm tra hàng tuần" đến hạn.

---

### **Bước 1: (Admin) Tạo Quiz Đánh giá ban đầu**

*   **API:** `POST /api/management/quiz-templates`
*   **Mục đích:** Tạo template cho bài quiz sẽ được làm ngay khi đăng ký.
*   **Headers:**
    *   `X-User-Id`: `{{adminId}}`
    *   `X-User-Role`: `ADMIN`
*   **Request Body:**
    ```json
    {
      "name": "Đánh giá tình trạng ban đầu",
      "code": "ONBOARDING_ASSESSMENT",
      "questions": [
        {
          "orderNo": 1,
          "questionText": "Bạn có cảm thấy căng thẳng khi không hút thuốc không?",
          "type": "SINGLE_CHOICE",
          "choices": [
            { "labelCode": "YES", "labelText": "Có", "weight": 2 },
            { "labelCode": "NO", "labelText": "Không", "weight": 0 }
          ]
        }
      ]
    }
    ```
*   **Ghi chú:** Lưu lại `id` của template quiz vừa tạo từ response, ví dụ: `onboardingQuizId`.

### **Bước 2: (Admin) Tạo Quiz Kiểm tra hàng tuần**

*   **API:** `POST /api/management/quiz-templates`
*   **Mục đích:** Tạo template cho bài quiz lặp lại hàng tuần.
*   **Headers:**
    *   `X-User-Id`: `{{adminId}}`
    *   `X-User-Role`: `ADMIN`
*   **Request Body:**
    ```json
    {
      "name": "Kiểm tra tiến độ hàng tuần",
      "code": "WEEKLY_CHECKIN",
      "questions": [
        {
          "orderNo": 1,
          "questionText": "Tuần qua, bạn có gặp khó khăn gì trong việc cai thuốc không?",
          "type": "SINGLE_CHOICE",
          "choices": [
            { "labelCode": "A_LOT", "labelText": "Rất nhiều khó khăn", "weight": 3 },
            { "labelCode": "SOME", "labelText": "Một vài khó khăn", "weight": 1 },
            { "labelCode": "NONE", "labelText": "Không có khó khăn nào", "weight": 0 }
          ]
        }
      ]
    }
    ```
*   **Ghi chú:** Lưu lại `id` của template quiz vừa tạo, ví dụ: `weeklyQuizId`.

### **Bước 3: (Admin) Lên lịch cho Quiz Đánh giá ban đầu**

*   **API:** `POST /api/management/plan-templates/{{planTemplateId}}/quiz-schedules`
*   **Mục đích:** Gán quiz "Đánh giá ban đầu" vào chương trình.
*   **Headers:**
    *   `X-User-Id`: `{{adminId}}`
    *   `X-User-Role`: `ADMIN`
*   **Path Variables:**
    *   `planTemplateId`: ID của một `PlanTemplate` có sẵn (ví dụ: chương trình 60 ngày).
*   **Request Body:**
    ```json
    {
      "quizTemplateId": "{{onboardingQuizId}}",
      "startOffsetDay": 1,
      "everyDays": 0
    }
    ```

### **Bước 4: (Admin) Lên lịch cho Quiz Hàng tuần**

*   **API:** `POST /api/management/plan-templates/{{planTemplateId}}/quiz-schedules`
*   **Mục đích:** Gán quiz "Kiểm tra hàng tuần" vào chương trình.
*   **Headers:**
    *   `X-User-Id`: `{{adminId}}`
    *   `X-User-Role`: `ADMIN`
*   **Path Variables:**
    *   `planTemplateId`: Sử dụng cùng `planTemplateId` như ở Bước 3.
*   **Request Body:**
    ```json
    {
      "quizTemplateId": "{{weeklyQuizId}}",
      "startOffsetDay": 7,
      "everyDays": 7
    }
    ```

### **Bước 5: (Customer) Đăng ký chương trình**

*   **API:** `POST /v1/programs`
*   **Mục đích:** Một người dùng mới đăng ký chương trình.
*   **Headers:**
    *   `X-User-Id`: `{{userId}}`
*   **Request Body:**
    ```json
    {
      "planDays": 60 // Phải khớp với PlanTemplate đã được cấu hình ở trên
    }
    ```
*   **Ghi chú:** Lưu lại `id` của chương trình vừa tạo vào biến `programId`.

### **Bước 6: (Customer) Kiểm tra Quiz đến hạn (Ngày 1)**

*   **API:** `GET /v1/me/quizzes`
*   **Mục đích:** Kiểm tra xem quiz "Đánh giá ban đầu" có xuất hiện không.
*   **Headers:**
    *   `X-User-Id`: `{{userId}}`
*   **Kết quả mong đợi:** Response trả về sẽ chứa một bài quiz có `templateName` là "Đánh giá tình trạng ban đầu".

### **Bước 7: (Admin) Giả lập thời gian trôi qua**

*   **API:** `PATCH /api/programs/{{programId}}/current-day`
*   **Mục đích:** "Tua nhanh" chương trình của customer đến ngày thứ 7.
*   **Headers:**
    *   `X-User-Id`: `{{adminId}}`
    *   `X-User-Role`: `ADMIN`
*   **Path Variables:**
    *   `programId`: ID của chương trình mà customer đã tạo.
*   **Request Body:**
    ```json
    { "currentDay": 7 }
    ```

### **Bước 8: (Customer) Kiểm tra Quiz đến hạn (Ngày 7)**

*   **API:** `GET /v1/me/quizzes`
*   **Mục đích:** Kiểm tra xem quiz "Kiểm tra hàng tuần" có xuất hiện không.
*   **Headers:**
    *   `X-User-Id`: `{{userId}}`
*   **Kết quả mong đợi:** Response trả về sẽ chứa một bài quiz có `templateName` là "Kiểm tra tiến độ hàng tuần".
