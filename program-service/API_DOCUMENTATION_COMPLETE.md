# Program Service API Documentation

**Version:** 2.1.0
**Last Updated:** 2025-11-28
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
| `409` | Conflict               | The request could not be completed due to a conflict with the current state of the resource (e.g., creating a duplicate item). |
| `501` | Not Implemented        | The server does not support the functionality required to fulfill the request. |

---

## 2. Onboarding & Enrollment

Handles the initial user onboarding and program enrollment flows.

### `POST /api/onboarding/baseline`

Submits the user's baseline assessment answers and receives a program recommendation.

-   **Permissions:** `USER`
-   **Request Body:** `QuizAnswerReq`
    ```json
    {
      "answers": [
        { "q": 1, "score": 4 }, // Question number and score (1-5)
        { "q": 2, "score": 3 }
        // ... up to 10 answers
      ]
    }
    ```
-   **Success Response (200 OK):** `BaselineResultRes`
    ```json
    {
      "totalScore": 35,
      "severity": "HIGH", // Calculated severity level (LOW, MODERATE, HIGH)
      "recommendedTemplateId": "uuid-of-high-severity-plan",
      "recommendedTemplateCode": "PLAN_HIGH_30D",
      "options": [ // List of available plans, with one marked as recommended
        {
          "id": "uuid-of-low-severity-plan",
          "code": "PLAN_LOW_30D",
          "name": "30-Day Light Program",
          "totalDays": 30,
          "recommended": false
        },
        {
          "id": "uuid-of-high-severity-plan",
          "code": "PLAN_HIGH_30D",
          "name": "30-Day Intensive Program",
          "totalDays": 30,
          "recommended": true
        }
      ]
    }
    ```

### `POST /v1/programs/{planTemplateId}/join`

Enrolls a user into a new program based on a selected plan template.

-   **Permissions:** `USER`
-   **Path Parameters:**
    | Parameter         | Type   | Description                         |
    | ----------------- | ------ | ----------------------------------- |
    | `planTemplateId`  | `UUID` | The ID of the `PlanTemplate` to join. |
-   **Request Body (Optional):**
    ```json
    {
      "trial": true // Defaults to true if body is omitted. Set to false for an immediate paid program.
    }
    ```
-   **Success Response (200 OK):** `EnrollmentRes`
    ```json
    {
      "id": "uuid-of-new-program",
      "userId": "uuid-of-user",
      "planTemplateId": "uuid-of-plan-template",
      "planCode": "PLAN_HIGH_30D",
      "status": "ACTIVE",
      "startAt": "2025-11-28T10:00:00Z",
      "endAt": null, // Not currently implemented
      "trialUntil": "2025-12-05T10:00:00Z" // Null if not a trial program
    }
    ```

---

## 3. Programs

Endpoints for managing user programs.

### 3.1. User-Facing Endpoints (`/v1/programs`)

### `POST /v1/programs`

Creates a new program for the user. Clones steps from a plan template and auto-assigns system quizzes.

-   **Permissions:** `USER`
-   **Request Body:** `CreateProgramReq`
    ```json
    {
      "planDays": 30 // Specifies the desired duration of the program
    }
    ```
-   **Success Response (200 OK):** `ProgramRes`
    ```json
    {
      "id": "uuid-of-new-program",
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

Retrieves the user's currently active program.

-   **Permissions:** `USER`
-   **Success Response (200 OK):** `ProgramRes` (same structure as above)
-   **Error Response:** `402 Payment Required` if the user's trial has expired.

### `GET /v1/programs`

Lists all programs belonging to the user.

-   **Permissions:** `USER`
-   **Success Response (200 OK):** `List<ProgramRes>`

### 3.2. Management Endpoints (`/api/programs`)

### `GET /api/programs/{id}/progress`

Retrieves the progress details for a specific program.

-   **Permissions:** `USER` (owner), `COACH`, `ADMIN`
-   **Path Parameters:**
    | Parameter | Type   | Description                |
    | --------- | ------ | -------------------------- |
    | `id`      | `UUID` | The ID of the program.     |
-   **Success Response (200 OK):** `ProgramProgressRes`
    ```json
    {
      "id": "uuid-of-program",
      "status": "ACTIVE",
      "currentDay": 15,
      "planDays": 30,
      "percentComplete": 50.0,
      "daysRemaining": 15,
      "stepsCompleted": 0, // Note: Placeholder, not yet implemented
      "stepsTotal": 0,     // Note: Placeholder, not yet implemented
      "streakCurrent": 10,
      "trialRemainingDays": 5 // Null if not a trial
    }
    ```

### `GET /api/programs/{id}/trial-status`

Checks the trial status of a program.

-   **Permissions:** `USER` (owner), `COACH`, `ADMIN`
-   **Path Parameters:**
    | Parameter | Type   | Description                |
    | --------- | ------ | -------------------------- |
    | `id`      | `UUID` | The ID of the program.     |
-   **Success Response (200 OK):** `TrialStatusRes`
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

Changes the status of a program.

-   **Permissions:** `USER` (owner), `ADMIN`
-   **Path Parameters:**
    | Parameter | Type   | Description                |
    | --------- | ------ | -------------------------- |
    | `id`      | `UUID` | The ID of the program.     |
-   **Success Response (200 OK):** `ProgramRes`

### `POST /api/programs/{id}/upgrade-from-trial`

Upgrades a program from trial to paid. Intended to be called by the Payment Service.

-   **Permissions:** `ADMIN`, `PAYMENT_SERVICE`
-   **Path Parameters:**
    | Parameter | Type   | Description                |
    | --------- | ------ | -------------------------- |
    | `id`      | `UUID` | The ID of the program.     |
-   **Success Response (200 OK):** `ProgramRes`

### `POST /api/programs/{id}/extend-trial`

Extends the trial period for a program.

-   **Permissions:** `ADMIN`
-   **Path Parameters:**
    | Parameter | Type   | Description                |
    | --------- | ------ | -------------------------- |
    | `id`      | `UUID` | The ID of the program.     |
-   **Request Body:** `ExtendTrialReq`
    ```json
    {
      "additionalDays": 7
    }
    ```
-   **Success Response (200 OK):** `TrialStatusRes`

### `PATCH /api/programs/{id}/current-day`

Manually updates the current day of a program.

-   **Permissions:** `ADMIN`
-   **Path Parameters:**
    | Parameter | Type   | Description                |
    | --------- | ------ | -------------------------- |
    | `id`      | `UUID` | The ID of the program.     |
-   **Request Body:** `UpdateCurrentDayReq`
    ```json
    {
      "currentDay": 10
    }
    ```
-   **Success Response (200 OK):** `ProgramRes`

---

## 4. Content & Plan Templates

Endpoints for managing content modules and plan templates.

### 4.1. Plan Templates (`/api/plan-templates`)

### `GET /api/plan-templates`

Lists all available plan templates.

-   **Permissions:** `ADMIN`, `COACH`, `USER`
-   **Success Response (200 OK):** `List<PlanTemplateSummaryRes>`
    ```json
    [
      {
        "id": "uuid-of-plan",
        "code": "PLAN_LOW_30D",
        "name": "30-Day Light Program",
        "description": "A gentle start to your smoke-free journey.",
        "totalDays": 30
      }
    ]
    ```

### `GET /api/plan-templates/{id}`

Retrieves the full details of a single plan template, including its steps.

-   **Permissions:** `ADMIN`, `COACH`, `USER`
-   **Path Parameters:**
    | Parameter | Type   | Description                |
    | --------- | ------ | -------------------------- |
    | `id`      | `UUID` | The ID of the plan template. |
-   **Success Response (200 OK):** `PlanTemplateDetailRes`
    ```json
    {
      "id": "uuid-of-plan",
      "code": "PLAN_LOW_30D",
      // ...other fields
      "steps": [
        {
          "stepNo": 1,
          "title": "Welcome!",
          "details": "Introduction to the program.",
          "dayOffset": 1,
          "type": "ARTICLE"
        }
      ]
    }
    ```

### `GET /api/plan-templates/{id}/days`
### `GET /api/plan-templates/by-code/{code}/days`

Retrieves the daily schedule for a plan template.

-   **Permissions:** `ADMIN`, `COACH`, `USER`
-   **Query Parameters:**
    | Parameter | Type      | Default | Description                                            |
    | --------- | --------- | ------- | ------------------------------------------------------ |
    | `expand`  | `boolean` | `false` | If `true`, includes the full payload of content modules. |
    | `lang`    | `string`  | `vi`    | The desired language for the content.                  |
-   **Success Response (200 OK):** `PlanDaysRes`

### `GET /api/plan-templates/recommendation`

Recommends a plan template based on a severity score.

-   **Permissions:** `ADMIN`, `COACH`, `USER`
-   **Query Parameters:**
    | Parameter  | Type     | Description                               |
    | ---------- | -------- | ----------------------------------------- |
    | `severity` | `string` | `LOW`, `MODERATE`, `HIGH`, or a numeric score. |
-   **Success Response (200 OK):** `PlanRecommendationRes`

### 4.2. Content Modules (`/api/modules`)

### `POST /api/modules`

Creates a new content module.

-   **Permissions:** `ADMIN`
-   **Request Body:** `ContentModuleCreateReq`
-   **Success Response (201 Created):** `ContentModuleRes`

### `PUT /api/modules/{id}`

Updates an existing content module.

-   **Permissions:** `ADMIN`
-   **Path Parameters:**
    | Parameter | Type   | Description                   |
    | --------- | ------ | ----------------------------- |
    | `id`      | `UUID` | The ID of the content module. |
-   **Request Body:** `ContentModuleUpdateReq`
-   **Success Response (200 OK):** `ContentModuleRes`

### `DELETE /api/modules/{id}`

Deletes a content module.

-   **Permissions:** `ADMIN`
-   **Path Parameters:**
    | Parameter | Type   | Description                   |
    | --------- | ------ | ----------------------------- |
    | `id`      | `UUID` | The ID of the content module. |
-   **Success Response (204 No Content):**

### `GET /api/modules`

Searches for content modules.

-   **Permissions:** `ADMIN`
-   **Query Parameters:**
    | Parameter | Type      | Default | Description                               |
    | --------- | --------- | ------- | ----------------------------------------- |
    | `q`       | `string`  | `null`  | Search query string.                      |
    | `lang`    | `string`  | `null`  | Filter by language.                       |
    | `page`    | `integer` | `0`     | The page number to retrieve.              |
    | `size`    | `integer` | `20`    | The number of items per page.             |
-   **Success Response (200 OK):** `Page<ContentModuleRes>`

### `GET /api/modules/{id}`
### `GET /api/modules/by-code/{code}`

Retrieves the latest version of a content module by its ID or code.

-   **Permissions:** `ADMIN`, `COACH` (read), Public lookup by code.
-   **Query Parameters:**
    | Parameter | Type     | Default | Description           |
    | --------- | -------- | ------- | --------------------- |
    | `lang`    | `string` | `null`  | Filter by language.   |
-   **Success Response (200 OK):** `ContentModuleRes`

### `GET /api/modules/by-code/{code}/versions`

Lists all versions of a content module.

-   **Permissions:** `ADMIN`
-   **Success Response (200 OK):** `List<ContentModuleRes>`

---

## 5. Program Execution

Endpoints for interacting with a running program.

### 5.1. Steps (`/api/programs/{programId}/steps`)

Manages the daily tasks or "steps" within a program.

-   **Permissions:** `USER`/`ADMIN` (write), `COACH` (read-only)
-   **Path Parameters:**
    | Parameter   | Type   | Description            |
    | ----------- | ------ | ---------------------- |
    | `programId` | `UUID` | The ID of the program. |

-   **`GET /`**: Lists all step assignments for the program.
-   **`GET /{id}`**: Retrieves a single step assignment.
-   **`GET /today`**: Retrieves steps scheduled for the current day.
-   **`POST /`**: Creates a new step assignment. (Body: `CreateStepAssignmentReq`)
-   **`DELETE /{id}`**: Deletes a step assignment.
-   **`PATCH /{id}/status`**: Updates the status of a step. (Body: `UpdateStepStatusReq {status, note}`)
-   **`POST /{id}/skip`**: Skips a step (sets status to `SKIPPED`).
-   **`PATCH /{id}/reschedule`**: Reschedules a step. (Body: `RescheduleStepReq {newScheduledAt}`)

### 5.2. Smoke Events (`/api/programs/{programId}/smoke-events`)

Logs smoking incidents.

-   **Permissions:** `USER`/`ADMIN` (write), `COACH` (read-only)
-   **Path Parameters:**
    | Parameter   | Type   | Description            |
    | ----------- | ------ | ---------------------- |
    | `programId` | `UUID` | The ID of the program. |

-   **`POST /`**: Creates a new smoke event. (Body: `CreateSmokeEventReq`)
-   **`GET /history`**: Retrieves a list of recent smoke events. (Query: `?size=`)
-   **`GET /stats`**: Retrieves statistics on smoke events. (Query: `?period=DAY|WEEK|MONTH`)
    -   **Note:** The `trend[]` field in the response is currently a placeholder and will be empty.

### 5.3. Streaks (`/api/programs/{programId}/streak`)

Manages smoke-free streaks.

-   **Permissions:** `USER`/`ADMIN` (write), `COACH` (read-only)
-   **Path Parameters:**
    | Parameter   | Type   | Description            |
    | ----------- | ------ | ---------------------- |
    | `programId` | `UUID` | The ID of the program. |

-   **`GET /`**: Retrieves the current streak.
-   **`POST /start`**: Starts a new streak (or returns the existing open one).
-   **`POST /break`**: Breaks the current streak. (Body: `BreakStreakReq`)
-   **`GET /history`**: Retrieves a list of past streaks. (Query: `?size=`)
-   **`GET /breaks`**: Retrieves a list of all streak breaks. (Query: `?size=`)

---

## 6. Quizzes

Endpoints for creating, managing, and taking quizzes.

### 6.1. Quiz Administration (`/v1/admin/quizzes`)

-   **Permissions:** `ADMIN`

-   **`POST /`**: Creates a new quiz template. (Body: `CreateTemplateReq`)
-   **`PUT /{id}`**: Updates a quiz template's metadata. (Body: `UpdateTemplateReq`)
-   **`PUT /{id}/archive`**: Archives a quiz template.
-   **`POST /{id}/publish`**: Publishes a quiz template, making it available.
-   **`POST /{id}/questions`**: Adds a question to a quiz template. (Body: `AddQuestionReq`)
-   **`POST /{id}/questions/{qNo}/choices`**: Adds a choice to a question. (Body: `AddChoiceReq`)

### 6.2. User Quiz Flow (`/v1/me/quizzes`)

-   **Permissions:** `USER` (owner)

-   **`GET /`**: Lists all quizzes that are currently due for the user.
-   **`POST /{templateId}/open`**: Starts a new attempt for a quiz. Returns the questions and choices.
-   **`PUT /{attemptId}/answer`**: Saves the user's answer for a specific question. (Body: `AnswerReq`)
-   **`POST /{attemptId}/submit`**: Submits the quiz for scoring. The `severity` in the response is calculated via `SeverityRuleService`.

### 6.3. Placeholder Endpoints

The following endpoints are planned but not yet implemented. They will return `501 Not Implemented`.

-   `/me/quiz/{templateId}/attempts`
-   `/attempts/{attemptId}`
-   `/retry`

---

## 7. Dashboard & Subscription

Endpoints for user-specific overviews and subscription management.

### `GET /api/me`

Retrieves a consolidated dashboard view for the current user.

-   **Permissions:** `USER`
-   **Success Response (200 OK):** `DashboardRes`
    -   **Note:** The `subscription` object is currently mocked to return a `BASIC` tier.

### `GET /api/subscriptions/me`

Retrieves the current user's subscription status.

-   **Permissions:** `USER`
-   **Success Response (200 OK):** `SubscriptionStatusRes`
    -   **Note:** This is currently mocked.

### `POST /api/subscriptions/upgrade`

Upgrades the user's subscription tier.

-   **Permissions:** `USER`
-   **Request Body:** `UpgradeReq {targetTier}`
-   **Success Response (200 OK):** `SubscriptionStatusRes`
    -   **Note:** This is currently mocked to grant a 30-day subscription to the target tier.
