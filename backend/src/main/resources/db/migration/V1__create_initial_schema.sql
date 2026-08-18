-- SmashMate initial schema migration
-- Stores all timestamps in DATETIME (UTC), money in DECIMAL(12,2)

CREATE TABLE members (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    full_name   VARCHAR(100)    NOT NULL,
    phone       VARCHAR(20)     NULL,
    email       VARCHAR(150)    NULL,
    password    VARCHAR(255)    NULL,
    role        ENUM('ADMIN','MEMBER','GUEST') NOT NULL DEFAULT 'MEMBER',
    status      ENUM('ACTIVE','INACTIVE','LEFT') NOT NULL DEFAULT 'ACTIVE',
    balance     DECIMAL(12, 2)  NOT NULL DEFAULT 0,
    joined_date DATE            NOT NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_members_email (email)
);

CREATE TABLE recurring_schedules (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    day_of_week TINYINT         NOT NULL,
    start_time  TIME            NOT NULL,
    end_time    TIME            NOT NULL,
    venue_name  VARCHAR(200)    NULL,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE sessions (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    schedule_id  BIGINT          NULL,
    session_date DATE            NOT NULL,
    start_time   TIME            NOT NULL,
    end_time     TIME            NULL,
    venue_name   VARCHAR(200)    NULL,
    status       ENUM('UPCOMING','IN_PROGRESS','CLOSED','CANCELLED') NOT NULL DEFAULT 'UPCOMING',
    notes        TEXT            NULL,
    created_by   BIGINT          NOT NULL,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_sessions_date (session_date),
    INDEX idx_sessions_status (status),
    CONSTRAINT fk_sessions_schedule   FOREIGN KEY (schedule_id) REFERENCES recurring_schedules(id),
    CONSTRAINT fk_sessions_created_by FOREIGN KEY (created_by)  REFERENCES members(id)
);

CREATE TABLE session_attendees (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    session_id  BIGINT          NOT NULL,
    member_id   BIGINT          NOT NULL,
    rsvp_status ENUM('ATTENDING','ABSENT','PENDING') NOT NULL DEFAULT 'PENDING',
    checked_in  BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_session_attendees (session_id, member_id),
    CONSTRAINT fk_sa_session FOREIGN KEY (session_id) REFERENCES sessions(id),
    CONSTRAINT fk_sa_member  FOREIGN KEY (member_id)  REFERENCES members(id)
);

CREATE TABLE session_tasks (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    session_id  BIGINT          NOT NULL,
    title       VARCHAR(200)    NOT NULL,
    assigned_to BIGINT          NULL,
    is_done     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_session_tasks_session (session_id),
    CONSTRAINT fk_tasks_session FOREIGN KEY (session_id) REFERENCES sessions(id),
    CONSTRAINT fk_tasks_member  FOREIGN KEY (assigned_to) REFERENCES members(id)
);

CREATE TABLE shuttle_batches (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    purchased_by  BIGINT          NOT NULL,
    quantity      INT             NOT NULL,
    remaining     INT             NOT NULL,
    unit_price    DECIMAL(12, 2)  NOT NULL,
    total_price   DECIMAL(12, 2)  NOT NULL,
    purchase_date DATE            NOT NULL,
    notes         VARCHAR(500)    NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_shuttle_batches_remaining (remaining),
    CONSTRAINT fk_batches_member FOREIGN KEY (purchased_by) REFERENCES members(id)
);

CREATE TABLE session_shuttle_usages (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    session_id    BIGINT          NOT NULL,
    batch_id      BIGINT          NOT NULL,
    quantity_used INT             NOT NULL,
    unit_price    DECIMAL(12, 2)  NOT NULL,
    total_cost    DECIMAL(12, 2)  NOT NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_ssu_session (session_id),
    CONSTRAINT fk_ssu_session FOREIGN KEY (session_id) REFERENCES sessions(id),
    CONSTRAINT fk_ssu_batch   FOREIGN KEY (batch_id)   REFERENCES shuttle_batches(id)
);

CREATE TABLE expense_categories (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)     NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    description VARCHAR(300)    NULL,
    is_system   BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order  INT             NOT NULL DEFAULT 0,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_expense_categories_code (code)
);

CREATE TABLE expense_items (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    session_id   BIGINT          NOT NULL,
    category_id  BIGINT          NOT NULL,
    description  VARCHAR(300)    NOT NULL,
    total_amount DECIMAL(12, 2)  NOT NULL,
    paid_by      BIGINT          NULL,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_expense_items_session (session_id),
    INDEX idx_expense_items_category (category_id),
    CONSTRAINT fk_ei_session  FOREIGN KEY (session_id)  REFERENCES sessions(id),
    CONSTRAINT fk_ei_category FOREIGN KEY (category_id) REFERENCES expense_categories(id),
    CONSTRAINT fk_ei_paid_by  FOREIGN KEY (paid_by)     REFERENCES members(id)
);

CREATE TABLE expense_participants (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    expense_item_id BIGINT          NOT NULL,
    member_id       BIGINT          NOT NULL,
    share_amount    DECIMAL(12, 2)  NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_expense_participant (expense_item_id, member_id),
    CONSTRAINT fk_ep_expense FOREIGN KEY (expense_item_id) REFERENCES expense_items(id),
    CONSTRAINT fk_ep_member  FOREIGN KEY (member_id)       REFERENCES members(id)
);

CREATE TABLE payment_debts (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    session_id   BIGINT          NOT NULL,
    member_id    BIGINT          NOT NULL,
    gross_owed   DECIMAL(12, 2)  NOT NULL,
    balance_used DECIMAL(12, 2)  NOT NULL DEFAULT 0,
    amount_owed  DECIMAL(12, 2)  NOT NULL,
    amount_paid  DECIMAL(12, 2)  NOT NULL DEFAULT 0,
    is_settled   BOOLEAN         NOT NULL DEFAULT FALSE,
    settled_at   DATETIME        NULL,
    confirmed_by BIGINT          NULL,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_debt_session_member (session_id, member_id),
    INDEX idx_debts_member (member_id),
    CONSTRAINT fk_debts_session   FOREIGN KEY (session_id)   REFERENCES sessions(id),
    CONSTRAINT fk_debts_member    FOREIGN KEY (member_id)    REFERENCES members(id),
    CONSTRAINT fk_debts_confirmed FOREIGN KEY (confirmed_by) REFERENCES members(id)
);

CREATE TABLE member_balance_logs (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    member_id     BIGINT          NOT NULL,
    amount        DECIMAL(12, 2)  NOT NULL,
    balance_after DECIMAL(12, 2)  NOT NULL,
    reason        VARCHAR(300)    NOT NULL,
    ref_type      ENUM('DEBT_SETTLE','BALANCE_DEDUCT','MANUAL') NOT NULL,
    ref_id        BIGINT          NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_balance_logs_member (member_id),
    CONSTRAINT fk_bl_member FOREIGN KEY (member_id) REFERENCES members(id)
);

CREATE TABLE club_settings (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    setting_key   VARCHAR(100)    NOT NULL,
    setting_value TEXT            NULL,
    description   VARCHAR(300)    NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_club_settings_key (setting_key)
);
