# Postman Test Guide - Program Service

**Version:** 2.0 (Refactored)
**Last Updated:** 2025-12-02
**Status:** Verified against Source Code
**Focus:** Complete API Coverage & Business-Standard Data

## 1. General Configuration

### Environment Variables
Thiết lập các biến này trong Postman Environment để tái sử dụng giữa các request.

| Variable | Description | Example |
|----------|-------------|---------|
| `baseUrl` | API Gateway or Service URL | `http://localhost:8080` |
| `userId` | UUID của User hiện tại | `00000000-0000-0000-0000-000000000001` |
| `adminId` | UUID của Admin | `00000000-0000-0000-0000-000000000000` |
| `programId` | UUID của Program đang active | *(Lấy từ response Create Program)* |
| `onboardingTemplateId` | UUID của Quiz Onboarding | *(Lấy từ response Admin Create Quiz)* |
| `attemptId` | UUID của lượt làm bài Quiz | *(Lấy từ response Open Attempt)* |
| `planTemplateId` | UUID của gói lộ trình | *(Lấy từ Baseline Response)* |

### Authentication Headers
Hệ thống hỗ trợ 2 cơ chế Auth chính (tùy môi trường):

1.  **Development / Direct Call:**
    *   `X-User-Id`: `<UUID>` (Bắt buộc)
    *   `X-User-Role`: `CUSTOMER` | `COACH` | `ADMIN` (Optional)
    *   `X-User-Tier`: `BASIC` | `PREMIUM` | `VIP` (Optional)

2.  **Production (via Gateway):**
    *   `Authorization`: `Bearer <JWT_TOKEN>`

---

## 2. Admin Flows (Setup Data)

### 2.1. Create Quiz Template (Onboarding Assessment)
Tạo bộ câu hỏi đánh giá mức độ nghiện (Fagerström) và động lực.
**Endpoint:** `POST /v1/admin/quizzes`
**Auth:** `ROLE_ADMIN` (Header `X-User-Id` + `X-User-Role: ADMIN`)

**Body (Standard Business Data):**
```json
{
  "name": "Đánh giá đầu vào toàn diện",
  "code": "ONBOARDING_ASSESSMENT",
  "version": 1,
  "questions": [
    {
      "orderNo": 1,
      "questionText": "Bạn thường hút điếu thuốc đầu tiên bao lâu sau khi thức dậy?",
      "type": "SINGLE_CHOICE",
      "explanation": "Đánh giá mức độ phụ thuộc vật lý vào nicotine.",
      "choices": [
        { "labelCode": "WITHIN_5_MIN", "labelText": "Trong vòng 5 phút", "isCorrect": false, "weight": 3 },
        { "labelCode": "WITHIN_30_MIN", "labelText": "Từ 6 - 30 phút", "isCorrect": false, "weight": 2 },
        { "labelCode": "WITHIN_60_MIN", "labelText": "Từ 31 - 60 phút", "isCorrect": false, "weight": 1 },
        { "labelCode": "AFTER_60_MIN", "labelText": "Sau 60 phút", "isCorrect": true, "weight": 0 }
      ]
    },
    // ... (các câu hỏi khác giữ nguyên như cũ)
  ]
}
```

---

## 3. User Onboarding Flow

### 3.1. Get Baseline Quiz
**Endpoint:** `GET /api/onboarding/baseline/quiz`

### 3.2. Submit Baseline & Recommendations
Giả lập user trả lời quiz (dựa trên template ở phần 2.1) với mức độ nghiện trung bình-cao.
**Endpoint:** `POST /api/onboarding/baseline`
**Auth:** `X-User-Id`

**Body:**
```json
{
  "templateId": "{{onboardingTemplateId}}",
  "answers": [
    { "q": 1, "score": 3 },
    { "q": 2, "score": 1 }
  ]
}
```
**Important Response:** Lưu `recommendedTemplateId` từ response này để dùng cho bước tạo Program.

### 3.3. Create Program (Start Journey)
**UPDATED:** Sử dụng API tạo program chuẩn, hỗ trợ Trial.
**Endpoint:** `POST /v1/programs`
**Auth:** `X-User-Id`

**Body (Trial Mode):**
```json
{
  "planTemplateId": "{{planTemplateId}}",
  "trial": true,
  "coachId": null
}
```

**Body (Paid Mode):**
```json
{
  "planTemplateId": "{{planTemplateId}}",
  "trial": false
}
```

---

## 4. Program Management (Lifecycle & Progress)

### 4.1. Get Progress (Dashboard Detail)
Xem chi tiết tiến độ, phần trăm hoàn thành.
**Endpoint:** `GET /api/programs/{id}/progress`
**Auth:** `X-User-Id` (Owner)

### 4.2. Check Trial Status
Kiểm tra thời hạn dùng thử.
**Endpoint:** `GET /api/programs/{id}/trial-status`
**Auth:** `X-User-Id` (Owner)

### 4.3. Upgrade from Trial (Admin/Payment)
Mở khóa tính năng trả phí.
**Endpoint:** `POST /api/programs/{id}/upgrade-from-trial`
**Auth:** `ROLE_ADMIN`

---

## 5. Daily User Flow

### 5.1. Dashboard
**Endpoint:** `GET /api/me`

### 5.2. Get Steps Today
**Endpoint:** `GET /api/programs/{programId}/steps/today`

### 5.3. Update Step Status (Hoàn thành nhiệm vụ)
User hoàn thành một bài tập thở hoặc đọc bài viết.
**Endpoint:** `PATCH /api/programs/{programId}/steps/{stepId}/status`
**Auth:** `X-User-Id`

**Body:**
```json
{
  "status": "COMPLETED",
  "note": "Đã đọc xong bài viết và thực hiện bài tập thở trong 5 phút. Cảm thấy nhịp tim ổn định hơn."
}
```

---

## 6. Quiz Execution Flow (Weekly Check-in)

### 6.1. List Due Quizzes
**Endpoint:** `GET /v1/me/quizzes`

### 6.2. Open Attempt
**Endpoint:** `POST /v1/me/quizzes/{templateId}/open`

### 6.3. Answer Question
**Endpoint:** `PUT /v1/me/quizzes/{attemptId}/answer`
**Body:**
```json
{
  "questionNo": 1,
  "answer": 2
}
```

### 6.4. Submit Quiz
**Endpoint:** `POST /v1/me/quizzes/{attemptId}/submit`

---

## 7. Tracking Flow (Smoke Events & Streaks)

### 7.1. Log Smoke Event - SLIP (Lỡ hút)
User báo cáo việc lỡ hút thuốc.
**Endpoint:** `POST /api/programs/{programId}/smoke-events`
**Body:**
```json
{
  "eventType": "SMOKE",
  "kind": "SLIP", 
  "puffs": 3,
  "reason": "SOCIAL",
  "note": "Đi đám cưới bạn cũ, bị mời nhiệt tình quá nên hút vài hơi xã giao.",
  "eventAt": "2025-12-02T20:30:00Z",
  "occurredAt": "2025-12-02T20:30:00Z"
}
```

### 7.2. Get Streak History
Xem lịch sử các chuỗi đã đạt được.
**Endpoint:** `GET /api/programs/{programId}/streak/history`

---

## 8. Subscription Flow

### 8.1. Get My Subscription
Xem trạng thái gói hiện tại.
**Endpoint:** `GET /api/subscriptions/me`

### 8.2. Upgrade to PREMIUM
**Endpoint:** `POST /api/subscriptions/upgrade`
**Body:**
```json
{
  "targetTier": "PREMIUM"
}
```
