CREATE TABLE program.programs
(
    id                           UUID         NOT NULL,
    user_id                      UUID         NOT NULL,
    coach_id                     UUID,
    chatroom_id                  UUID,
    plan_days                    INTEGER      NOT NULL,
    status                       VARCHAR(255) NOT NULL,
    start_date                   date         NOT NULL,
    current_day                  INTEGER      NOT NULL,
    total_score                  INTEGER,
    severity                     VARCHAR(255),
    entitlement_tier_at_creation VARCHAR(255),
    trial_started_at             TIMESTAMP WITHOUT TIME ZONE,
    trial_end_expected           TIMESTAMP WITHOUT TIME ZONE,
    created_at                   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at                   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted_at                   TIMESTAMP WITHOUT TIME ZONE,
    streak_current               INTEGER      NOT NULL,
    streak_best                  INTEGER      NOT NULL,
    last_smoke_at                TIMESTAMP WITHOUT TIME ZONE,
    streak_frozen_until          TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_programs PRIMARY KEY (id)
);

CREATE TABLE program.quiz_answers
(
    answer      INTEGER,
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    attempt_id  UUID    NOT NULL,
    question_no INTEGER NOT NULL,
    CONSTRAINT pk_quiz_answers PRIMARY KEY (attempt_id, question_no)
);

CREATE TABLE program.quiz_assignments
(
    id          UUID                     NOT NULL,
    scope       PROGRAM.ASSIGNMENT_SCOPE NOT NULL,
    expires_at  TIMESTAMP WITHOUT TIME ZONE,
    template_id UUID,
    program_id  UUID,
    every_days  INTEGER,
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    created_by  UUID,
    CONSTRAINT pk_quiz_assignments PRIMARY KEY (id)
);

CREATE TABLE program.quiz_attempts
(
    id           UUID NOT NULL,
    program_id   UUID,
    template_id  UUID,
    user_id      UUID,
    opened_at    TIMESTAMP WITHOUT TIME ZONE,
    submitted_at TIMESTAMP WITHOUT TIME ZONE,
    status       VARCHAR(255),
    CONSTRAINT pk_quiz_attempts PRIMARY KEY (id)
);

CREATE TABLE program.quiz_choice_labels
(
    label       VARCHAR(255),
    template_id UUID    NOT NULL,
    question_no INTEGER NOT NULL,
    score       INTEGER NOT NULL,
    CONSTRAINT pk_quiz_choice_labels PRIMARY KEY (template_id, question_no, score)
);

CREATE TABLE program.quiz_results
(
    id           UUID NOT NULL,
    program_id   UUID,
    template_id  UUID,
    quiz_version INTEGER,
    total_score  INTEGER,
    severity     VARCHAR(255),
    created_at   TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_quiz_results PRIMARY KEY (id)
);

CREATE TABLE program.quiz_template_questions
(
    text        VARCHAR(255),
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    template_id UUID    NOT NULL,
    question_no INTEGER NOT NULL,
    CONSTRAINT pk_quiz_template_questions PRIMARY KEY (template_id, question_no)
);

CREATE TABLE program.quiz_templates
(
    id            UUID        NOT NULL,
    name          VARCHAR(255),
    version       INTEGER,
    status        VARCHAR(255),
    language_code VARCHAR(255),
    scope         VARCHAR(20) NOT NULL,
    owner_id      UUID,
    published_at  TIMESTAMP WITHOUT TIME ZONE,
    archived_at   TIMESTAMP WITHOUT TIME ZONE,
    created_at    TIMESTAMP WITHOUT TIME ZONE,
    updated_at    TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_quiz_templates PRIMARY KEY (id)
);

CREATE TABLE program.smoke_events
(
    id         UUID                     NOT NULL,
    program_id UUID                     NOT NULL,
    event_at   TIMESTAMP WITHOUT TIME ZONE,
    event_type PROGRAM.SMOKE_EVENT_TYPE NOT NULL,
    note       VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_smoke_events PRIMARY KEY (id)
);

CREATE TABLE program.step_assignments
(
    id           UUID         NOT NULL,
    program_id   UUID         NOT NULL,
    step_no      INTEGER      NOT NULL,
    planned_day  INTEGER      NOT NULL,
    status       VARCHAR(255) NOT NULL,
    scheduled_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    note         VARCHAR(255),
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by   UUID,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_step_assignments PRIMARY KEY (id)
);

CREATE TABLE program.streak_breaks
(
    id               UUID    NOT NULL,
    streak_id        UUID    NOT NULL,
    program_id       UUID    NOT NULL,
    smoke_event_id   UUID    NOT NULL,
    broke_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    prev_streak_days INTEGER NOT NULL,
    note             VARCHAR(255),
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_streak_breaks PRIMARY KEY (id)
);

CREATE TABLE program.streaks
(
    id          UUID NOT NULL,
    program_id  UUID NOT NULL,
    started_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    ended_at    TIMESTAMP WITHOUT TIME ZONE,
    length_days INTEGER,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_streaks PRIMARY KEY (id)
);

ALTER TABLE program.quiz_templates
    ADD CONSTRAINT uq_quiz_template_name_scope_owner_version UNIQUE (name, scope, owner_id, version);

CREATE INDEX idx_quiz_template_scope_owner ON program.quiz_templates (scope, owner_id);

ALTER TABLE program.quiz_answers
    ADD CONSTRAINT FK_QUIZ_ANSWERS_ON_ATTEMPT FOREIGN KEY (attempt_id) REFERENCES program.quiz_attempts (id);

ALTER TABLE program.quiz_choice_labels
    ADD CONSTRAINT FK_QUIZ_CHOICE_LABELS_ON_TEIDQUNO FOREIGN KEY (template_id, question_no) REFERENCES program.quiz_template_questions (template_id, question_no);

ALTER TABLE program.quiz_template_questions
    ADD CONSTRAINT FK_QUIZ_TEMPLATE_QUESTIONS_ON_TEMPLATE FOREIGN KEY (template_id) REFERENCES program.quiz_templates (id);