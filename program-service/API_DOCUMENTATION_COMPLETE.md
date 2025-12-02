# Program Service API Documentation

**Version:** 1.1.0
**Last Updated:** 2025-12-02
**Charset:** UTF-8

---

## 1. Overview

This document provides a complete reference for the Program Service API. The service is responsible for managing user programs, content, progress tracking, and related functionalities.

### 1.1. Base URL

All API endpoints are relative to the service's base URL.

-   **Production:** `https://api.smokefree.app/program`
-   **Development:** `http://localhost:8080`

### 1.2. Authentication

All requests must be authenticated via the API Gateway. The following headers are required on all incoming requests:

| Header          | Description                                     | Example                                |
| --------------- | ----------------------------------------------- | -------------------------------------- |
| `Authorization` | Bearer token for authentication.                | `Bearer <JWT_TOKEN>`                   |
| `X-User-Id`     | The UUID of the authenticated user.             | `a1b2c3d4-e5f6-7890-1234-567890abcdef` |
| `X-User-Role`   | The role of the authenticated user.             | `USER`, `COACH`, or `ADMIN`            |
| `X-User-Tier`   | The tier of the authenticated user (Optional).  | `BASIC`, `PREMIUM`, or `VIP`           |

### 1.3. User Roles

| Role    | Permissions                                                              |
| ------- | ------------------------------------------------------------------------ |
| `ADMIN` | Full access to all resources for management and administration.          |
| `COACH` | Read-only access to data of assigned users/programs.                     |
| `USER`  | Access to their own data only (programs, steps, quizzes, etc.).          |

### 1.4. Common HTTP Status Codes

| Code  | Status                 | Description                                                                 |
| ----- | ---------------------- | --------------------------------------------------------------------------- |
| `200` | OK                     | The request was successful.                                                 |
| `201` | Created                | The resource was successfully created.                                      |
| `400` | Bad Request            | The request was malformed (e.g., invalid JSON, missing parameters).         |
| `401` | Unauthorized           | Authentication failed or was not provided.                                  |
| `402` | Payment Required       | The requested action requires a subscription (e.g., trial has expired).     |
| `403` | Forbidden              | The authenticated user does not have permission to access this resource.    |
| `404` | Not Found              | The requested resource could not be found.                                  |
| `409` | Conflict               | The request could not be completed due to a conflict (e.g., duplicate item).|

---

## 2. Onboarding & Enrollment

Handles the initial user onboarding and program enrollment flows.

### `POST /api/onboarding/baseline`

Submits the user's baseline assessment answers and receives a program recommendation.

-   **Permissions:** `USER`
-   **Request Body:** `QuizAnswerReq`
    ```json
    {
      "templateId": "uuid-of-template",
      "answers": [
        { "q": 1, "score": 4 }, // Question number and score
        { "q": 2, "score": 3 }
      ]
    }
    ```
-   **Success Response (200 OK):** `BaselineResultRes`
    ```json
    {
      "totalScore": 35,
      "severity": "HIGH", // LOW, MODERATE, HIGH
      "recommendedTemplateId": "uuid-of-high-severity-plan",
      "recommendedTemplateCode": "PLAN_HIGH_30D",
      "options": [ 
        {
          "id": "uuid-of-low-severity-plan",
          "code": "PLAN_LOW_30D",
          "name": "30-Day Light Program",
          "totalDays": 30,
          "recommended": false
        }
      ]
    }
    ```

### `GET /api/onboarding/baseline/quiz`

Retrieves the questions for the onboarding assessment.

-   **Permissions:** `USER`
-   **Success Response (200 OK):** `OpenAttemptRes`
    ```json
    {
      "attemptId": null, // Null for baseline preview
      "templateId": "uuid-of-template",
      "version": 1,
      "questions": [
        {
          "questionNo": 1,
          "questionText": "How soon...",
          "choices": {
            "A": "Within 5 min",
            "B": "6-30 min"
          }
        }
      ]
    }
    ```

---

## 3. Programs

Endpoints for managing user programs.

### 3.1. User-Facing Endpoints (`/v1/programs`)

### `POST /v1/programs`

Creates a new program for the user.

-   **Permissions:** `USER`
-   **Request Body:** `CreateProgramReq`
    ```json
    {
      "planDays": 30,
      "coachId": "optional-uuid"
    }
    ```
-   **Success Response (200 OK):** `ProgramRes`
    ```json
    {
      "id": "uuid-of-new-program",
      "status": "ACTIVE", // ACTIVE, PAUSED, COMPLETED, CANCELLED
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

Retrieves the user's currently active program.

-   **Permissions:** `USER`
-   **Success Response (200 OK):** `ProgramRes` (same structure as above)
-   **Error Response:** `402 Payment Required` if the user's trial has expired.

### 3.2. Management Endpoints (`/api/programs`)

### `GET /api/programs/{id}/progress`

Retrieves detailed progress metrics.

-   **Permissions:** `USER` (owner)
-   **Path Parameters:** `id` (UUID)
-   **Success Response (200 OK):** `ProgramProgressRes`
    ```json
    {
      "id": "uuid-of-program",
      "status": "ACTIVE",
      "currentDay": 15,
      "planDays": 30,
      "percentComplete": 50.0,
      "daysRemaining": 15,
      "stepsCompleted": 5,
      "stepsTotal": 10,
      "streakCurrent": 10,
      "trialRemainingDays": 5 // Null if not trial
    }
    ```

### `POST /api/programs/{id}/pause`
### `POST /api/programs/{id}/resume`
### `POST /api/programs/{id}/end`

Changes program status.

-   **Permissions:** `USER` (owner)
-   **Success Response (200 OK):** `ProgramRes`

### `GET /api/programs/{id}/trial-status`

Checks if the program is in trial period.

-   **Permissions:** `USER` (owner)
-   **Success Response (200 OK):** `TrialStatusRes`
    ```json
    {
      "isTrial": true,
      "trialStartedAt": "...",
      "trialEndExpected": "...",
      "remainingDays": 2,
      "canUpgradeNow": true
    }
    ```

---

## 4. Plan Templates & Content

### 4.1. Plan Templates (`/api/plan-templates`)

### `GET /api/plan-templates`

Lists all plan templates.

-   **Permissions:** `Authenticated`
-   **Success Response (200 OK):** `List<PlanTemplateSummaryRes>`

### `GET /api/plan-templates/{id}`

Gets details of a plan template.

-   **Permissions:** `Authenticated`
-   **Success Response (200 OK):** `PlanTemplateDetailRes`

### `GET /api/plan-templates/by-code/{code}/days`

Gets the daily schedule for a template code.

-   **Permissions:** `Authenticated`
-   **Query Params:** `expand` (boolean), `lang` (string)
-   **Success Response (200 OK):** `PlanDaysRes`

### 4.2. Content Modules (`/api/modules`)

### `POST /api/modules` (Admin)

Creates content (Article/Video/etc).

-   **Permissions:** `ADMIN`
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
    ```json
    {
      "id": "uuid",
      "code": "ARTICLE_001",
      "type": "ARTICLE",
      "lang": "vi",
      "version": 1,
      "payload": { ... },
      "updatedAt": "...",
      "etag": "..."
    }
    ```

### `GET /api/modules` (Search)

-   **Permissions:** `ADMIN`
-   **Query Params:** `q`, `lang`, `page`, `size`
-   **Success Response:** `Page<ContentModuleRes>`

### `GET /api/modules/by-code/{code}`

-   **Permissions:** Public/Authenticated
-   **Success Response:** `ContentModuleRes`

---

## 5. Daily Execution

### 5.1. Steps (`/api/programs/{programId}/steps`)

### `GET /today`

Gets steps for the current day.

-   **Permissions:** `USER`
-   **Success Response (200 OK):** `List<StepAssignment>`

### `PATCH /{id}/status`

Updates step status (e.g., COMPLETED).

-   **Permissions:** `USER`
-   **Request Body:** `UpdateStepStatusReq`
    ```json
    {
      "status": "COMPLETED",
      "note": "Optional note"
    }
    ```
-   **Success Response (200 OK):** (Empty Body)

### 5.2. Smoke Events (`/api/programs/{programId}/smoke-events`)

### `POST /`

Logs a smoke event.

-   **Permissions:** `USER`
-   **Request Body:** `CreateSmokeEventReq`
    ```json
    {
      "eventType": "SMOKE", // SMOKE, URGE
      "kind": "SLIP",       // SLIP, LAPSE, RELAPSE, STRONG...
      "puffs": 3,
      "reason": "STRESS",
      "note": "...",
      "eventAt": "ISO-8601",
      "occurredAt": "ISO-8601"
    }
    ```
-   **Success Response (200 OK):** `SmokeEventRes`

### `GET /history`

-   **Query Params:** `size` (int)
-   **Success Response:** `List<SmokeEventRes>`

### 5.3. Streaks (`/api/programs/{programId}/streak`)

### `GET /`

Gets current streak info.

-   **Success Response (200 OK):** `StreakView`
    ```json
    {
      "streakId": "uuid",
      "currentStreak": 5,        // Days
      "bestStreak": 10,
      "daysWithoutSmoke": 5,
      "startedAt": "...",
      "endedAt": null
    }
    ```

---

## 6. Quiz Engine

### 6.1. Admin Quiz (`/v1/admin/quizzes`)

-   `POST /`: Create Template
-   `POST /{id}/publish`: Publish Template
-   `POST /{id}/questions`: Add Question

### 6.2. User Quiz (`/v1/me/quizzes`)

### `GET /` (List Due)

Lists quizzes assigned to the user (Weekly, Assessment, Recovery).

-   **Success Response:** `Map<String, Object>`
    ```json
    {
      "success": true,
      "data": [
        { "attemptId": null, "templateId": "...", "type": "WEEKLY", "deadline": "..." }
      ],
      "count": 1
    }
    ```

### `POST /{templateId}/open`

Starts a quiz attempt.

-   **Success Response:** `OpenAttemptRes` (Contains questions)

### `PUT /{attemptId}/answer`

Saves an answer.

-   **Request Body:** `AnswerReq`
    ```json
    { "questionNo": 1, "answer": 2 }
    ```
-   **Success Response:** `{ "success": true, "message": "..." }`

### `POST /{attemptId}/submit`

Submits and scores the quiz.

-   **Success Response:**
    ```json
    {
      "success": true,
      "data": {
        "attemptId": "uuid",
        "totalScore": 10,
        "severity": "LOW"
      }
    }
    ```

---

## 7. Subscription

### `GET /api/subscriptions/me`

Gets current subscription status.

-   **Success Response:** `SubscriptionStatusRes` (Mocked)

### `POST /api/subscriptions/upgrade`

Upgrades tier.

-   **Request Body:** `{ "targetTier": "PREMIUM" }`
-   **Success Response:** `SubscriptionStatusRes`