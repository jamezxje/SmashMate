# SmashMate Phase 1 — Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scaffold the SmashMate monorepo, set up the full database schema with Flyway migrations, implement JWT authentication, and deliver a fully-working Members CRUD API with role-based access control.

**Architecture:** Spring Boot 3.x backend with layered architecture (Controller → Service → Repository → Entity). MySQL 8 with Flyway migrations. React + Vite frontend scaffolded and connected via Axios. JWT stored as Bearer token (access) + HttpOnly cookie (refresh).

**Tech Stack:** Java 21, Spring Boot 3.x, Spring Security 6, JJWT 0.12.x, JPA/Hibernate, Flyway, MySQL 8, MapStruct, Lombok, ReactJS 18, Vite 5, Tailwind CSS 3, Zustand 4, Axios 1.x

**Spec:** `docs/superpowers/specs/2026-08-18-smashmate-design.md`

**Code Convention:** `docs/CODE_CONVENTION.md` — **MANDATORY**. Every task must comply with all rules in that file. The constraints below are a summary of the most critical rules; the full file is the source of truth.

## Global Constraints

> All items below are derived from `docs/CODE_CONVENTION.md`. Read the full file before starting any task.

### General
- All code, comments, variable names, method names: **English**
- No `System.out.println` / `console.log` left in committed code
- No commented-out dead code
- No magic numbers/strings — use **constants or enums**
- Each class/file has **one responsibility** (SRP)

### Backend — Java
- Java 21 (LTS), Spring Boot 3.x minimum
- Package root: `com.smashmate`; sub-packages follow convention exactly:
  ```
  com.smashmate
  ├── config/       # Spring + Security config
  ├── controller/   # Thin controllers — only call service
  ├── service/      # Business logic
  │   └── impl/     # Service implementations (if interface used)
  ├── repository/   # JPA Repositories
  ├── entity/       # JPA Entities
  ├── dto/
  │   ├── request/
  │   └── response/
  ├── mapper/       # MapStruct mappers
  ├── exception/    # Custom exceptions + GlobalExceptionHandler
  ├── security/     # JWT, UserDetails, filters
  └── util/         # Utility classes (DateUtil, CostCalculator...)
  ```
- Naming: `PascalCase` class, `camelCase` method/variable, `SCREAMING_SNAKE_CASE` constant/enum value
- Entity rules: use `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor` — **never** `@Data`
- Money: `BigDecimal` always — **never** `double`/`float`
- Time: `LocalDateTime` (UTC) — **never** `java.util.Date`
- All JPA relations: `FetchType.LAZY` by default
- Service class: annotate `@Transactional` at class level; add `@Transactional(readOnly = true)` on query methods
- Throw **custom exceptions** only — never throw `RuntimeException` directly
- Custom exception message format: `"ResourceName not found with id: {id}"`

### API
- Base URL: `/api/v1`; noun plural paths (e.g. `/sessions`, `/members`)
- HTTP verbs: GET list, GET detail, POST create, PUT full update, PATCH partial, DELETE remove, POST for complex actions (e.g. `/sessions/{id}/close`)
- Every response wrapped in envelope: `{ "success", "data", "message", "timestamp" }`
- Every error wrapped in: `{ "success": false, "error": "ERROR_CODE", "message", "timestamp" }`

### Database
- Table names: `snake_case`, plural (e.g. `shuttle_batches`)
- Column names: `snake_case`
- FK column naming: `<table_singular>_id` (e.g. `session_id`, `member_id`)
- Index naming: `idx_<table>_<column(s)>` (e.g. `idx_sessions_date`)
- Unique constraint naming: `uq_<table>_<column(s)>` (e.g. `uq_members_email`)
- FK constraint naming: `fk_<table>_<ref_table>` (e.g. `fk_shuttle_batches_session`)
- Every table must have `created_at DATETIME` and `updated_at DATETIME`
- Money: `DECIMAL(12,2)` — never `FLOAT`/`DOUBLE`
- Soft delete: `deleted_at DATETIME NULL` on financially-sensitive tables
- Flyway files: each file changes **one related group of entities**; always add a comment at top describing intent
- Never modify a migration file that has already been applied

### Frontend — React + TypeScript
- All source in `frontend/src/`; folder structure:
  ```
  src/
  ├── api/          # axios.ts + one file per feature (auth.ts, members.ts...)
  ├── components/
  │   ├── common/   # Button, Modal, Badge, Table, Input...
  │   └── <feature>/# Feature-specific components
  ├── pages/        # Route-level pages
  ├── hooks/        # Custom hooks (prefix: use)
  ├── stores/       # Zustand stores (prefix: use<Feature>Store)
  ├── types/        # TypeScript types and interfaces
  ├── utils/        # Pure utility functions (e.g. formatCurrency.ts)
  ├── constants/    # App-wide constants (SCREAMING_SNAKE_CASE)
  └── assets/
  ```
- Named exports only — **no `export default`**
- No TypeScript `any` — use `unknown` or specific types
- Props must have a TypeScript interface named `<ComponentName>Props`
- Separate logic (custom hooks) from UI (JSX)
- Zustand store interface must declare all state fields and actions with proper types
- Currency display: always use `formatVND()` from `src/utils/formatCurrency.ts` — **no financial calculations on frontend**; backend is source of truth
- `src/utils/formatCurrency.ts` must exist with: `export function formatVND(amount: number): string { return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount); }`

### Before Completing Any Task (PR Checklist)
- [ ] Naming convention followed per `docs/CODE_CONVENTION.md`
- [ ] No `System.out.println` / `console.log` remaining
- [ ] API response matches envelope format
- [ ] Money: `BigDecimal` (Java) / `DECIMAL(12,2)` (DB) / no calculations on frontend
- [ ] Migration files named correctly, no edits to previously applied files
- [ ] Every entity/table has `created_at` and `updated_at`
- [ ] No TypeScript `any`
- [ ] Only custom exceptions thrown, not raw `RuntimeException`
- [ ] No financial calculation logic in frontend code

---

---

## Task 1: Backend Project Scaffolding + Full DB Schema

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/smashmate/SmashMateApplication.java`
- Create: `backend/src/main/resources/db/migration/V1__create_initial_schema.sql`
- Create: `backend/src/main/resources/db/migration/V2__seed_data.sql`

**Interfaces:**
- Produces: Running Spring Boot app on port 8080; Flyway applies V1+V2 migrations on startup

- [x] **Step 1: Generate Spring Boot project**

  ```bash
  curl https://start.spring.io/starter.zip \
    -d type=maven-project \
    -d language=java \
    -d bootVersion=3.3.0 \
    -d groupId=com.smashmate \
    -d artifactId=smashmate-backend \
    -d name=smashmate-backend \
    -d packageName=com.smashmate \
    -d javaVersion=21 \
    -d dependencies=web,data-jpa,mysql,security,validation,flyway,lombok \
    -o backend.zip
  unzip backend.zip -d backend
  ```

- [x] **Step 2: Add missing dependencies to `backend/pom.xml`**

  Add inside `<dependencies>`:
  ```xml
  <!-- JWT -->
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
  </dependency>
  <!-- MapStruct -->
  <dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
  </dependency>
  <dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
    <scope>provided</scope>
  </dependency>
  ```

  Add to `<build><plugins>` the `maven-compiler-plugin` with annotationProcessorPaths:
  ```xml
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
      <annotationProcessorPaths>
        <path>
          <groupId>org.projectlombok</groupId>
          <artifactId>lombok</artifactId>
          <version>${lombok.version}</version>
        </path>
        <path>
          <groupId>org.mapstruct</groupId>
          <artifactId>mapstruct-processor</artifactId>
          <version>1.5.5.Final</version>
        </path>
      </annotationProcessorPaths>
    </configuration>
  </plugin>
  ```

- [x] **Step 3: Create `backend/src/main/resources/application.yml`**

  ```yaml
  spring:
    datasource:
      url: jdbc:mysql://localhost:3306/smashmate?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      username: ${DB_USERNAME:root}
      password: ${DB_PASSWORD:root}
      driver-class-name: com.mysql.cj.jdbc.Driver
    jpa:
      hibernate:
        ddl-auto: validate
      show-sql: false
      properties:
        hibernate:
          dialect: org.hibernate.dialect.MySQL8Dialect
    flyway:
      enabled: true
      locations: classpath:db/migration
      baseline-on-migrate: true

  server:
    port: 8080

  app:
    jwt:
      secret: ${JWT_SECRET:smashmate-dev-secret-key-must-be-at-least-256-bits-long-for-hs256}
      access-token-expiration-ms: 900000
      refresh-token-expiration-ms: 604800000
    upload:
      qr-dir: ${UPLOAD_QR_DIR:uploads/qr}
      max-file-size-bytes: 2097152
  ```

- [x] **Step 4: Create `V1__create_initial_schema.sql`**

  Save to `backend/src/main/resources/db/migration/V1__create_initial_schema.sql`:
  ```sql
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
      id          BIGINT       NOT NULL AUTO_INCREMENT,
      day_of_week TINYINT      NOT NULL,
      start_time  TIME         NOT NULL,
      end_time    TIME         NOT NULL,
      venue_name  VARCHAR(200) NULL,
      is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
      created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
      id          BIGINT  NOT NULL AUTO_INCREMENT,
      session_id  BIGINT  NOT NULL,
      member_id   BIGINT  NOT NULL,
      rsvp_status ENUM('ATTENDING','ABSENT','PENDING') NOT NULL DEFAULT 'PENDING',
      checked_in  BOOLEAN NOT NULL DEFAULT FALSE,
      created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
  ```

- [x] **Step 5: Create `V2__seed_data.sql`**

  Save to `backend/src/main/resources/db/migration/V2__seed_data.sql`:
  ```sql
  INSERT INTO expense_categories (code, name, description, is_system, is_active, sort_order) VALUES
  ('COURT_FEE',   'Tien san', 'Chi phi thue san tap', TRUE,  TRUE, 1),
  ('SHUTTLE_FEE', 'Tien cau', 'Chi phi cau long',     TRUE,  TRUE, 2),
  ('FOOD',        'An uong',  'Chi phi an uong',      FALSE, TRUE, 3),
  ('OTHER',       'Khac',     'Chi phi khac',         FALSE, TRUE, 4);

  INSERT INTO club_settings (setting_key, setting_value, description) VALUES
  ('club_name',             'SmashMate', 'Ten CLB hien thi'),
  ('payment_qr_image_path', NULL,        'Duong dan file anh QR code ngan hang');

  -- Default ADMIN: password = Admin@123 (BCrypt strength 12)
  INSERT INTO members (full_name, email, password, role, status, joined_date) VALUES
  ('Admin', 'admin@smashmate.local',
   '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
   'ADMIN', 'ACTIVE', CURDATE());
  ```

- [x] **Step 6: Create MySQL database, run app, verify migrations**

  ```sql
  CREATE DATABASE smashmate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```

  ```bash
  cd backend && ./mvnw spring-boot:run
  ```
  Expected: `Successfully applied 2 migrations to schema "smashmate"` in logs, no errors.

---

## Task 2: API Response Infrastructure

**Files:**
- Create: `backend/src/main/java/com/smashmate/common/ApiResponse.java`
- Create: `backend/src/main/java/com/smashmate/exception/ResourceNotFoundException.java`
- Create: `backend/src/main/java/com/smashmate/exception/BusinessException.java`
- Create: `backend/src/main/java/com/smashmate/exception/GlobalExceptionHandler.java`

**Interfaces:**
- Produces:
  - `ApiResponse.success(data)` / `ApiResponse.success(data, message)` / `ApiResponse.error(code, message)`
  - `GlobalExceptionHandler` catches `ResourceNotFoundException`, `BusinessException`, validation errors, `AccessDeniedException`, generic `Exception`

- [x] **Step 1: Create `ApiResponse.java`**

  ```java
  package com.smashmate.common;

  import com.fasterxml.jackson.annotation.JsonInclude;
  import lombok.Builder;
  import lombok.Getter;
  import java.time.Instant;

  @Getter
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public class ApiResponse<T> {
      private final boolean success;
      private final T data;
      private final String message;
      private final String error;
      @Builder.Default
      private final String timestamp = Instant.now().toString();

      public static <T> ApiResponse<T> success(T data, String message) {
          return ApiResponse.<T>builder().success(true).data(data).message(message).build();
      }
      public static <T> ApiResponse<T> success(T data) { return success(data, null); }
      public static ApiResponse<Void> error(String errorCode, String message) {
          return ApiResponse.<Void>builder().success(false).error(errorCode).message(message).build();
      }
  }
  ```

- [x] **Step 2: Create exception classes**

  `ResourceNotFoundException.java`:
  ```java
  package com.smashmate.exception;
  public class ResourceNotFoundException extends RuntimeException {
      public ResourceNotFoundException(String resourceName, Long id) {
          super(resourceName + " not found with id: " + id);
      }
      public ResourceNotFoundException(String message) { super(message); }
  }
  ```

  `BusinessException.java`:
  ```java
  package com.smashmate.exception;
  import lombok.Getter;
  import org.springframework.http.HttpStatus;

  @Getter
  public class BusinessException extends RuntimeException {
      private final String errorCode;
      private final HttpStatus status;
      public BusinessException(String errorCode, String message, HttpStatus status) {
          super(message); this.errorCode = errorCode; this.status = status;
      }
      public BusinessException(String errorCode, String message) {
          this(errorCode, message, HttpStatus.BAD_REQUEST);
      }
  }
  ```

- [x] **Step 3: Create `GlobalExceptionHandler.java`**

  ```java
  package com.smashmate.exception;

  import com.smashmate.common.ApiResponse;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.http.*;
  import org.springframework.security.access.AccessDeniedException;
  import org.springframework.validation.FieldError;
  import org.springframework.web.bind.MethodArgumentNotValidException;
  import org.springframework.web.bind.annotation.*;
  import java.util.stream.Collectors;

  @Slf4j
  @RestControllerAdvice
  public class GlobalExceptionHandler {

      @ExceptionHandler(ResourceNotFoundException.class)
      public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(ApiResponse.error("RESOURCE_NOT_FOUND", ex.getMessage()));
      }

      @ExceptionHandler(BusinessException.class)
      public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
          return ResponseEntity.status(ex.getStatus())
              .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
      }

      @ExceptionHandler(MethodArgumentNotValidException.class)
      public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
          String msg = ex.getBindingResult().getFieldErrors().stream()
              .map(FieldError::getDefaultMessage).collect(Collectors.joining(", "));
          return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_ERROR", msg));
      }

      @ExceptionHandler(AccessDeniedException.class)
      public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
          return ResponseEntity.status(HttpStatus.FORBIDDEN)
              .body(ApiResponse.error("ACCESS_DENIED", "You do not have permission"));
      }

      @ExceptionHandler(Exception.class)
      public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
          log.error("Unhandled exception", ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
      }
  }
  ```

- [x] **Step 4: Write and run smoke test**

  ```java
  // backend/src/test/java/com/smashmate/exception/GlobalExceptionHandlerTest.java
  @SpringBootTest
  @AutoConfigureMockMvc
  class GlobalExceptionHandlerTest {
      @Autowired MockMvc mockMvc;

      @Test
      void unauthenticated_request_returns_401() throws Exception {
          mockMvc.perform(get("/api/v1/members"))
              .andExpect(status().isUnauthorized());
      }
  }
  ```

  Run: `./mvnw test -pl backend -Dtest=GlobalExceptionHandlerTest`
  Expected: PASS

---

## Task 3: JWT Authentication

**Files:**
- Create: `backend/src/main/java/com/smashmate/security/JwtProperties.java`
- Create: `backend/src/main/java/com/smashmate/security/JwtService.java`
- Create: `backend/src/main/java/com/smashmate/security/JwtAuthFilter.java`
- Create: `backend/src/main/java/com/smashmate/security/UserDetailsServiceImpl.java`
- Create: `backend/src/main/java/com/smashmate/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/smashmate/controller/AuthController.java`
- Create: `backend/src/main/java/com/smashmate/dto/request/LoginRequest.java`
- Create: `backend/src/main/java/com/smashmate/dto/response/TokenResponse.java`

**Interfaces:**
- Consumes: `ApiResponse<T>` from Task 2, `MemberRepository` from Task 4 (created alongside this)
- Produces:
  - `JwtService.generateAccessToken(email, role) -> String`
  - `JwtService.generateRefreshToken(email, role) -> String`
  - `JwtService.extractEmail(token) -> String`
  - `JwtService.extractRole(token) -> String`
  - `POST /api/v1/auth/login -> ApiResponse<TokenResponse>`
  - `POST /api/v1/auth/refresh -> ApiResponse<TokenResponse>`

- [x] **Step 1: Create `JwtProperties.java`**

  ```java
  package com.smashmate.security;
  import lombok.Getter; import lombok.Setter;
  import org.springframework.boot.context.properties.ConfigurationProperties;
  import org.springframework.stereotype.Component;

  @Getter @Setter @Component
  @ConfigurationProperties(prefix = "app.jwt")
  public class JwtProperties {
      private String secret;
      private long accessTokenExpirationMs;
      private long refreshTokenExpirationMs;
  }
  ```

- [x] **Step 2: Create `JwtService.java`**

  ```java
  package com.smashmate.security;
  import io.jsonwebtoken.*;
  import io.jsonwebtoken.security.Keys;
  import lombok.RequiredArgsConstructor;
  import org.springframework.stereotype.Service;
  import javax.crypto.SecretKey;
  import java.nio.charset.StandardCharsets;
  import java.util.Date;

  @Service @RequiredArgsConstructor
  public class JwtService {
      private final JwtProperties props;

      private SecretKey key() {
          return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
      }

      public String generateAccessToken(String email, String role) {
          return build(email, role, props.getAccessTokenExpirationMs());
      }
      public String generateRefreshToken(String email, String role) {
          return build(email, role, props.getRefreshTokenExpirationMs());
      }
      private String build(String subject, String role, long expirationMs) {
          return Jwts.builder()
              .subject(subject).claim("role", role)
              .issuedAt(new Date())
              .expiration(new Date(System.currentTimeMillis() + expirationMs))
              .signWith(key()).compact();
      }
      public Claims validateToken(String token) {
          return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
      }
      public String extractEmail(String token) { return validateToken(token).getSubject(); }
      public String extractRole(String token) { return validateToken(token).get("role", String.class); }
  }
  ```

- [x] **Step 3: Write and run unit test for JwtService**

  ```java
  // backend/src/test/java/com/smashmate/security/JwtServiceTest.java
  @SpringBootTest
  class JwtServiceTest {
      @Autowired JwtService jwtService;

      @Test
      void generateAndValidate_accessToken() {
          String token = jwtService.generateAccessToken("admin@smashmate.local", "ADMIN");
          assertThat(jwtService.extractEmail(token)).isEqualTo("admin@smashmate.local");
          assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
      }
  }
  ```

  Run: `./mvnw test -pl backend -Dtest=JwtServiceTest`
  Expected: PASS

- [x] **Step 4: Create `UserDetailsServiceImpl.java`**

  > Note: This file references `MemberRepository` which is created in Task 4. Create both tasks in the same session, or stub `MemberRepository` as an interface first.

  ```java
  package com.smashmate.security;
  import com.smashmate.repository.MemberRepository;
  import lombok.RequiredArgsConstructor;
  import org.springframework.security.core.authority.SimpleGrantedAuthority;
  import org.springframework.security.core.userdetails.*;
  import org.springframework.stereotype.Service;
  import java.util.List;

  @Service @RequiredArgsConstructor
  public class UserDetailsServiceImpl implements UserDetailsService {
      private final MemberRepository memberRepository;

      @Override
      public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
          var member = memberRepository.findByEmailAndDeletedAtIsNull(email)
              .orElseThrow(() -> new UsernameNotFoundException("Member not found: " + email));
          if (member.getPassword() == null) {
              throw new UsernameNotFoundException("Member has no account: " + email);
          }
          return new User(member.getEmail(), member.getPassword(),
              List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())));
      }
  }
  ```

- [x] **Step 5: Create `JwtAuthFilter.java`**

  ```java
  package com.smashmate.security;
  import jakarta.servlet.*;
  import jakarta.servlet.http.*;
  import lombok.RequiredArgsConstructor;
  import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
  import org.springframework.security.core.context.SecurityContextHolder;
  import org.springframework.security.core.userdetails.UserDetailsService;
  import org.springframework.stereotype.Component;
  import org.springframework.web.filter.OncePerRequestFilter;
  import java.io.IOException;

  @Component @RequiredArgsConstructor
  public class JwtAuthFilter extends OncePerRequestFilter {
      private final JwtService jwtService;
      private final UserDetailsService userDetailsService;

      @Override
      protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                      FilterChain chain) throws ServletException, IOException {
          String header = req.getHeader("Authorization");
          if (header == null || !header.startsWith("Bearer ")) { chain.doFilter(req, res); return; }
          try {
              String token = header.substring(7);
              String email = jwtService.extractEmail(token);
              if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                  var userDetails = userDetailsService.loadUserByUsername(email);
                  var auth = new UsernamePasswordAuthenticationToken(
                      userDetails, null, userDetails.getAuthorities());
                  SecurityContextHolder.getContext().setAuthentication(auth);
              }
          } catch (Exception ignored) { }
          chain.doFilter(req, res);
      }
  }
  ```

- [x] **Step 6: Create `SecurityConfig.java`**

  ```java
  package com.smashmate.config;
  import com.smashmate.security.JwtAuthFilter;
  import lombok.RequiredArgsConstructor;
  import org.springframework.context.annotation.*;
  import org.springframework.security.authentication.AuthenticationManager;
  import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
  import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
  import org.springframework.security.config.annotation.web.builders.HttpSecurity;
  import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
  import org.springframework.security.config.http.SessionCreationPolicy;
  import org.springframework.security.crypto.bcrypt.*;
  import org.springframework.security.crypto.password.PasswordEncoder;
  import org.springframework.security.web.SecurityFilterChain;
  import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
  import org.springframework.web.cors.*;
  import java.util.List;

  @Configuration @EnableWebSecurity @EnableMethodSecurity @RequiredArgsConstructor
  public class SecurityConfig {
      private final JwtAuthFilter jwtAuthFilter;

      @Bean
      public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
          http.csrf(c -> c.disable())
              .cors(c -> c.configurationSource(corsConfigurationSource()))
              .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
              .authorizeHttpRequests(auth -> auth
                  .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                  .requestMatchers("/api/v1/settings/payment-qr", "/api/v1/settings/payment-qr/image").permitAll()
                  .anyRequest().authenticated())
              .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
          return http.build();
      }

      @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

      @Bean
      public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
          return cfg.getAuthenticationManager();
      }

      @Bean
      public CorsConfigurationSource corsConfigurationSource() {
          var config = new CorsConfiguration();
          config.setAllowedOrigins(List.of("http://localhost:5173"));
          config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
          config.setAllowedHeaders(List.of("*"));
          config.setAllowCredentials(true);
          var source = new UrlBasedCorsConfigurationSource();
          source.registerCorsConfiguration("/**", config);
          return source;
      }
  }
  ```

- [x] **Step 7: Create `LoginRequest.java`, `TokenResponse.java`, and `AuthController.java`**

  `LoginRequest.java`:
  ```java
  package com.smashmate.dto.request;
  import jakarta.validation.constraints.*; import lombok.*;

  @Getter @Setter
  public class LoginRequest {
      @NotBlank @Email private String email;
      @NotBlank private String password;
  }
  ```

  `TokenResponse.java`:
  ```java
  package com.smashmate.dto.response;
  import lombok.*;

  @Getter @Builder
  public class TokenResponse {
      private String accessToken;
      private String tokenType;
      private long expiresIn;
  }
  ```

  `AuthController.java`:
  ```java
  package com.smashmate.controller;
  import com.smashmate.common.ApiResponse;
  import com.smashmate.dto.request.*;
  import com.smashmate.dto.response.TokenResponse;
  import com.smashmate.security.*;
  import com.smashmate.service.MemberService;
  import jakarta.servlet.http.*;
  import jakarta.validation.Valid;
  import lombok.RequiredArgsConstructor;
  import org.springframework.http.ResponseEntity;
  import org.springframework.security.authentication.*;
  import org.springframework.security.core.Authentication;
  import org.springframework.web.bind.annotation.*;

  @RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor
  public class AuthController {
      private final AuthenticationManager authManager;
      private final JwtService jwtService;
      private final JwtProperties jwtProperties;
      private final MemberService memberService;

      @PostMapping("/login")
      public ResponseEntity<ApiResponse<TokenResponse>> login(
              @Valid @RequestBody LoginRequest req, HttpServletResponse res) {
          Authentication auth = authManager.authenticate(
              new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
          String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
          String accessToken = jwtService.generateAccessToken(req.getEmail(), role);
          String refreshToken = jwtService.generateRefreshToken(req.getEmail(), role);
          Cookie cookie = new Cookie("refreshToken", refreshToken);
          cookie.setHttpOnly(true);
          cookie.setPath("/api/v1/auth/refresh");
          cookie.setMaxAge((int)(jwtProperties.getRefreshTokenExpirationMs() / 1000));
          res.addCookie(cookie);
          return ResponseEntity.ok(ApiResponse.success(
              TokenResponse.builder().accessToken(accessToken).tokenType("Bearer")
                  .expiresIn(jwtProperties.getAccessTokenExpirationMs() / 1000).build(),
              "Login successful"));
      }

      @PostMapping("/refresh")
      public ResponseEntity<ApiResponse<TokenResponse>> refresh(
              @CookieValue(name = "refreshToken", required = false) String refreshToken) {
          if (refreshToken == null) return ResponseEntity.status(401)
              .body(ApiResponse.error("MISSING_REFRESH_TOKEN", "Refresh token not found"));
          String email = jwtService.extractEmail(refreshToken);
          String role = jwtService.extractRole(refreshToken);
          return ResponseEntity.ok(ApiResponse.success(
              TokenResponse.builder().accessToken(jwtService.generateAccessToken(email, role))
                  .tokenType("Bearer").expiresIn(jwtProperties.getAccessTokenExpirationMs() / 1000).build(),
              "Token refreshed"));
      }

      @PatchMapping("/change-password")
      public ResponseEntity<ApiResponse<Void>> changePassword(
              @Valid @RequestBody ChangePasswordRequest req,
              java.security.Principal principal) {
          memberService.changePassword(principal.getName(), req);
          return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
      }
  }
  ```

- [x] **Step 8: Write and run integration test for login**

  ```java
  // backend/src/test/java/com/smashmate/controller/AuthControllerTest.java
  @SpringBootTest @AutoConfigureMockMvc
  class AuthControllerTest {
      @Autowired MockMvc mockMvc;
      @Autowired ObjectMapper objectMapper;

      @Test
      void login_validCredentials_returnsAccessToken() throws Exception {
          mockMvc.perform(post("/api/v1/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of(
                      "email", "admin@smashmate.local", "password", "Admin@123"))))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.success").value(true))
              .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
      }

      @Test
      void login_wrongPassword_returns401() throws Exception {
          mockMvc.perform(post("/api/v1/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of(
                      "email", "admin@smashmate.local", "password", "wrong"))))
              .andExpect(status().isUnauthorized());
      }
  }
  ```

  Run: `./mvnw test -pl backend -Dtest=AuthControllerTest`
  Expected: PASS

---

## Task 4: Member Entity, Repository, Service, Controller

**Files:**
- Create: `backend/src/main/java/com/smashmate/entity/enums/Role.java`
- Create: `backend/src/main/java/com/smashmate/entity/enums/MemberStatus.java`
- Create: `backend/src/main/java/com/smashmate/entity/Member.java`
- Create: `backend/src/main/java/com/smashmate/repository/MemberRepository.java`
- Create: `backend/src/main/java/com/smashmate/dto/request/CreateMemberRequest.java`
- Create: `backend/src/main/java/com/smashmate/dto/request/UpdateMemberRequest.java`
- Create: `backend/src/main/java/com/smashmate/dto/request/AssignAccountRequest.java`
- Create: `backend/src/main/java/com/smashmate/dto/request/ChangePasswordRequest.java`
- Create: `backend/src/main/java/com/smashmate/dto/response/MemberResponse.java`
- Create: `backend/src/main/java/com/smashmate/mapper/MemberMapper.java`
- Create: `backend/src/main/java/com/smashmate/service/MemberService.java`
- Create: `backend/src/main/java/com/smashmate/controller/MemberController.java`

**Interfaces:**
- Consumes: `ApiResponse<T>` from Task 2
- Produces:
  - `MemberRepository.findByEmailAndDeletedAtIsNull(String) -> Optional<Member>`
  - `MemberService.createMember(CreateMemberRequest) -> MemberResponse`
  - `MemberService.createGuest(String fullName) -> MemberResponse`
  - `MemberService.assignAccount(Long id, AssignAccountRequest) -> MemberResponse`
  - `MemberService.changePassword(String email, ChangePasswordRequest)`
  - All CRUD endpoints under `/api/v1/members`

- [x] **Step 1: Create enum classes**

  ```java
  // com/smashmate/entity/enums/Role.java
  package com.smashmate.entity.enums;
  public enum Role { ADMIN, MEMBER, GUEST }
  ```

  ```java
  // com/smashmate/entity/enums/MemberStatus.java
  package com.smashmate.entity.enums;
  public enum MemberStatus { ACTIVE, INACTIVE, LEFT }
  ```

- [x] **Step 2: Create `Member.java` entity**

  ```java
  package com.smashmate.entity;
  import com.smashmate.entity.enums.*;
  import jakarta.persistence.*;
  import lombok.*;
  import org.hibernate.annotations.*;
  import java.math.BigDecimal;
  import java.time.*;

  @Entity @Table(name = "members")
  @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
  public class Member {
      @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
      @Column(name = "full_name", nullable = false, length = 100) private String fullName;
      @Column(length = 20) private String phone;
      @Column(length = 150, unique = true) private String email;
      @Column(length = 255) private String password;
      @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private Role role;
      @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private MemberStatus status;
      @Column(nullable = false, precision = 12, scale = 2) private BigDecimal balance;
      @Column(name = "joined_date", nullable = false) private LocalDate joinedDate;
      @CreationTimestamp @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
      @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
      @Column(name = "deleted_at") private LocalDateTime deletedAt;
  }
  ```

- [x] **Step 3: Create `MemberRepository.java`**

  ```java
  package com.smashmate.repository;
  import com.smashmate.entity.Member;
  import com.smashmate.entity.enums.Role;
  import org.springframework.data.jpa.repository.JpaRepository;
  import java.util.*;

  public interface MemberRepository extends JpaRepository<Member, Long> {
      Optional<Member> findByEmailAndDeletedAtIsNull(String email);
      boolean existsByEmailAndDeletedAtIsNull(String email);
      List<Member> findAllByDeletedAtIsNull();
      List<Member> findAllByRoleAndDeletedAtIsNull(Role role);
  }
  ```

- [x] **Step 4: Create DTO classes**

  `CreateMemberRequest.java`:
  ```java
  package com.smashmate.dto.request;
  import jakarta.validation.constraints.*; import lombok.*;
  import java.time.LocalDate;

  @Getter @Setter
  public class CreateMemberRequest {
      @NotBlank(message = "Full name is required") private String fullName;
      private String phone;
      @Email(message = "Invalid email format") private String email;
      private String password;
      private LocalDate joinedDate;
  }
  ```

  `UpdateMemberRequest.java`:
  ```java
  package com.smashmate.dto.request;
  import lombok.*;
  @Getter @Setter
  public class UpdateMemberRequest { private String fullName; private String phone; }
  ```

  `AssignAccountRequest.java`:
  ```java
  package com.smashmate.dto.request;
  import jakarta.validation.constraints.*; import lombok.*;
  @Getter @Setter
  public class AssignAccountRequest {
      @NotBlank @Email private String email;
      @NotBlank private String password;
  }
  ```

  `ChangePasswordRequest.java`:
  ```java
  package com.smashmate.dto.request;
  import jakarta.validation.constraints.*; import lombok.*;
  @Getter @Setter
  public class ChangePasswordRequest {
      @NotBlank private String currentPassword;
      @NotBlank @Size(min = 8, message = "New password must be at least 8 characters") private String newPassword;
  }
  ```

  `MemberResponse.java`:
  ```java
  package com.smashmate.dto.response;
  import com.smashmate.entity.enums.*;
  import lombok.*;
  import java.math.BigDecimal; import java.time.*;

  @Getter @Builder
  public class MemberResponse {
      private Long id;
      private String fullName;
      private String phone;
      private String email;
      private Role role;
      private MemberStatus status;
      private BigDecimal balance;
      private LocalDate joinedDate;
      private LocalDateTime createdAt;
      private boolean hasAccount;
  }
  ```

- [x] **Step 5: Create `MemberMapper.java`**

  ```java
  package com.smashmate.mapper;
  import com.smashmate.dto.response.MemberResponse;
  import com.smashmate.entity.Member;
  import org.mapstruct.*;

  @Mapper(componentModel = "spring")
  public interface MemberMapper {
      @Mapping(target = "hasAccount",
               expression = "java(member.getEmail() != null && member.getPassword() != null)")
      MemberResponse toResponse(Member member);
  }
  ```

- [x] **Step 6: Create `MemberService.java`**

  ```java
  package com.smashmate.service;
  import com.smashmate.dto.request.*;
  import com.smashmate.dto.response.MemberResponse;
  import com.smashmate.entity.Member;
  import com.smashmate.entity.enums.*;
  import com.smashmate.exception.*;
  import com.smashmate.mapper.MemberMapper;
  import com.smashmate.repository.MemberRepository;
  import lombok.RequiredArgsConstructor;
  import org.springframework.security.crypto.password.PasswordEncoder;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;
  import java.math.BigDecimal; import java.time.*;
  import java.util.List;

  @Service @RequiredArgsConstructor @Transactional
  public class MemberService {
      private final MemberRepository memberRepository;
      private final MemberMapper memberMapper;
      private final PasswordEncoder passwordEncoder;

      @Transactional(readOnly = true)
      public List<MemberResponse> getAllMembers(Role roleFilter) {
          var members = roleFilter != null
              ? memberRepository.findAllByRoleAndDeletedAtIsNull(roleFilter)
              : memberRepository.findAllByDeletedAtIsNull();
          return members.stream().map(memberMapper::toResponse).toList();
      }

      @Transactional(readOnly = true)
      public MemberResponse getMemberById(Long id) {
          return memberMapper.toResponse(findActiveById(id));
      }

      public MemberResponse createMember(CreateMemberRequest req) {
          if (req.getEmail() != null && req.getPassword() == null
              || req.getEmail() == null && req.getPassword() != null) {
              throw new BusinessException("INVALID_ACCOUNT",
                  "Email and password must both be provided or both be absent");
          }
          if (req.getEmail() != null && memberRepository.existsByEmailAndDeletedAtIsNull(req.getEmail())) {
              throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email is already in use");
          }
          var member = Member.builder()
              .fullName(req.getFullName()).phone(req.getPhone())
              .email(req.getEmail())
              .password(req.getPassword() != null ? passwordEncoder.encode(req.getPassword()) : null)
              .role(Role.MEMBER).status(MemberStatus.ACTIVE).balance(BigDecimal.ZERO)
              .joinedDate(req.getJoinedDate() != null ? req.getJoinedDate() : LocalDate.now())
              .build();
          return memberMapper.toResponse(memberRepository.save(member));
      }

      public MemberResponse createGuest(String fullName) {
          var guest = Member.builder().fullName(fullName)
              .role(Role.GUEST).status(MemberStatus.ACTIVE)
              .balance(BigDecimal.ZERO).joinedDate(LocalDate.now()).build();
          return memberMapper.toResponse(memberRepository.save(guest));
      }

      public MemberResponse updateMember(Long id, UpdateMemberRequest req) {
          var member = findActiveById(id);
          if (req.getFullName() != null) member.setFullName(req.getFullName());
          if (req.getPhone() != null) member.setPhone(req.getPhone());
          return memberMapper.toResponse(memberRepository.save(member));
      }

      public MemberResponse assignAccount(Long id, AssignAccountRequest req) {
          var member = findActiveById(id);
          if (member.getRole() == Role.GUEST) {
              throw new BusinessException("CANNOT_ASSIGN_ACCOUNT_TO_GUEST",
                  "GUEST members cannot have an account");
          }
          if (memberRepository.existsByEmailAndDeletedAtIsNull(req.getEmail())) {
              throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email is already in use");
          }
          member.setEmail(req.getEmail());
          member.setPassword(passwordEncoder.encode(req.getPassword()));
          return memberMapper.toResponse(memberRepository.save(member));
      }

      public MemberResponse updateStatus(Long id, MemberStatus newStatus) {
          var member = findActiveById(id);
          member.setStatus(newStatus);
          return memberMapper.toResponse(memberRepository.save(member));
      }

      public void softDelete(Long id) {
          var member = findActiveById(id);
          member.setDeletedAt(LocalDateTime.now());
          memberRepository.save(member);
      }

      public void changePassword(String email, ChangePasswordRequest req) {
          var member = memberRepository.findByEmailAndDeletedAtIsNull(email)
              .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + email));
          if (!passwordEncoder.matches(req.getCurrentPassword(), member.getPassword())) {
              throw new BusinessException("WRONG_CURRENT_PASSWORD", "Current password is incorrect");
          }
          member.setPassword(passwordEncoder.encode(req.getNewPassword()));
          memberRepository.save(member);
      }

      private Member findActiveById(Long id) {
          return memberRepository.findById(id)
              .filter(m -> m.getDeletedAt() == null)
              .orElseThrow(() -> new ResourceNotFoundException("Member", id));
      }
  }
  ```

- [x] **Step 7: Create `MemberController.java`**

  ```java
  package com.smashmate.controller;
  import com.smashmate.common.ApiResponse;
  import com.smashmate.dto.request.*;
  import com.smashmate.dto.response.MemberResponse;
  import com.smashmate.entity.enums.*;
  import com.smashmate.service.MemberService;
  import jakarta.validation.Valid;
  import lombok.RequiredArgsConstructor;
  import org.springframework.http.*;
  import org.springframework.security.access.prepost.PreAuthorize;
  import org.springframework.web.bind.annotation.*;
  import java.util.*;

  @RestController @RequestMapping("/api/v1/members") @RequiredArgsConstructor
  public class MemberController {
      private final MemberService memberService;

      @GetMapping
      public ResponseEntity<ApiResponse<List<MemberResponse>>> getAll(
              @RequestParam(required = false) Role role) {
          return ResponseEntity.ok(ApiResponse.success(memberService.getAllMembers(role)));
      }

      @PostMapping @PreAuthorize("hasRole('ADMIN')")
      public ResponseEntity<ApiResponse<MemberResponse>> create(
              @Valid @RequestBody CreateMemberRequest req) {
          return ResponseEntity.status(HttpStatus.CREATED)
              .body(ApiResponse.success(memberService.createMember(req), "Member created"));
      }

      @PostMapping("/guests") @PreAuthorize("hasRole('ADMIN')")
      public ResponseEntity<ApiResponse<MemberResponse>> createGuest(
              @RequestBody Map<String, String> body) {
          String fullName = body.get("fullName");
          if (fullName == null || fullName.isBlank())
              return ResponseEntity.badRequest()
                  .body(ApiResponse.error("VALIDATION_ERROR", "fullName is required"));
          return ResponseEntity.status(HttpStatus.CREATED)
              .body(ApiResponse.success(memberService.createGuest(fullName), "Guest created"));
      }

      @GetMapping("/{id}")
      public ResponseEntity<ApiResponse<MemberResponse>> getById(@PathVariable Long id) {
          return ResponseEntity.ok(ApiResponse.success(memberService.getMemberById(id)));
      }

      @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
      public ResponseEntity<ApiResponse<MemberResponse>> update(
              @PathVariable Long id, @RequestBody UpdateMemberRequest req) {
          return ResponseEntity.ok(ApiResponse.success(memberService.updateMember(id, req), "Updated"));
      }

      @PatchMapping("/{id}/account") @PreAuthorize("hasRole('ADMIN')")
      public ResponseEntity<ApiResponse<MemberResponse>> assignAccount(
              @PathVariable Long id, @Valid @RequestBody AssignAccountRequest req) {
          return ResponseEntity.ok(ApiResponse.success(memberService.assignAccount(id, req), "Account assigned"));
      }

      @PatchMapping("/{id}/status") @PreAuthorize("hasRole('ADMIN')")
      public ResponseEntity<ApiResponse<MemberResponse>> updateStatus(
              @PathVariable Long id, @RequestBody Map<String, String> body) {
          return ResponseEntity.ok(ApiResponse.success(
              memberService.updateStatus(id, MemberStatus.valueOf(body.get("status"))), "Status updated"));
      }

      @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
      public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
          memberService.softDelete(id);
          return ResponseEntity.ok(ApiResponse.success(null, "Member deleted"));
      }
  }
  ```

- [x] **Step 8: Write and run service tests**

  ```java
  // backend/src/test/java/com/smashmate/service/MemberServiceTest.java
  @SpringBootTest @Transactional
  class MemberServiceTest {
      @Autowired MemberService memberService;

      @Test
      void createMember_offline_noEmailPassword() {
          var req = new CreateMemberRequest();
          req.setFullName("Nguyen Van A");
          var result = memberService.createMember(req);
          assertThat(result.isHasAccount()).isFalse();
          assertThat(result.getRole()).isEqualTo(Role.MEMBER);
      }

      @Test
      void createGuest_roleIsGuest() {
          var result = memberService.createGuest("Khach 1");
          assertThat(result.getRole()).isEqualTo(Role.GUEST);
          assertThat(result.isHasAccount()).isFalse();
      }

      @Test
      void assignAccount_toGuest_throws() {
          var guest = memberService.createGuest("Khach 2");
          var req = new AssignAccountRequest();
          req.setEmail("khach@test.com"); req.setPassword("pass12345");
          assertThatThrownBy(() -> memberService.assignAccount(guest.getId(), req))
              .isInstanceOf(BusinessException.class);
      }

      @Test
      void softDelete_memberNotFoundAfterDelete() {
          var req = new CreateMemberRequest(); req.setFullName("To Delete");
          var created = memberService.createMember(req);
          memberService.softDelete(created.getId());
          assertThatThrownBy(() -> memberService.getMemberById(created.getId()))
              .isInstanceOf(ResourceNotFoundException.class);
      }

      @Test
      void createMember_onlyEmail_withoutPassword_throws() {
          var req = new CreateMemberRequest();
          req.setFullName("Email Only"); req.setEmail("test@test.com");
          assertThatThrownBy(() -> memberService.createMember(req))
              .isInstanceOf(BusinessException.class)
              .hasMessageContaining("both");
      }
  }
  ```

  Run: `./mvnw test -pl backend -Dtest=MemberServiceTest`
  Expected: PASS

- [x] **Step 9: Write and run controller integration tests**

  ```java
  // backend/src/test/java/com/smashmate/controller/MemberControllerTest.java
  @SpringBootTest @AutoConfigureMockMvc
  class MemberControllerTest {
      @Autowired MockMvc mockMvc;
      @Autowired ObjectMapper objectMapper;
      private String adminToken;

      @BeforeEach
      void setUp() throws Exception {
          var resp = mockMvc.perform(post("/api/v1/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(
                      Map.of("email","admin@smashmate.local","password","Admin@123"))))
              .andReturn();
          adminToken = objectMapper.readTree(resp.getResponse().getContentAsString())
              .path("data").path("accessToken").asText();
      }

      @Test
      void createMember_asAdmin_returns201() throws Exception {
          mockMvc.perform(post("/api/v1/members")
                  .header("Authorization", "Bearer " + adminToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of("fullName","Test Member"))))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.data.fullName").value("Test Member"));
      }

      @Test
      void createGuest_asAdmin_roleIsGuest() throws Exception {
          mockMvc.perform(post("/api/v1/members/guests")
                  .header("Authorization", "Bearer " + adminToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of("fullName","Khach VL"))))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.data.role").value("GUEST"));
      }

      @Test
      void createMember_noAuth_returns401() throws Exception {
          mockMvc.perform(post("/api/v1/members")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of("fullName","No Auth"))))
              .andExpect(status().isUnauthorized());
      }
  }
  ```

  Run: `./mvnw test -pl backend -Dtest=MemberControllerTest`
  Expected: PASS

---

## Task 5: Frontend Scaffolding + Login Page

**Files:**
- Create: `frontend/` (Vite project)
- Create: `frontend/src/types/index.ts`
- Create: `frontend/src/api/axios.ts`
- Create: `frontend/src/api/auth.ts`
- Create: `frontend/src/stores/useAuthStore.ts`
- Create: `frontend/src/pages/LoginPage.tsx`
- Create: `frontend/src/App.tsx`

**Interfaces:**
- Produces: React app on port 5173; login POSTs to `/api/v1/auth/login`; JWT in Zustand store; refresh token interceptor on 401

- [ ] **Step 1: Scaffold Vite + React + Tailwind**

  ```bash
  npm create vite@latest frontend -- --template react-ts
  cd frontend
  npm install
  npm install axios zustand react-router-dom
  npm install -D tailwindcss postcss autoprefixer
  npx tailwindcss init -p
  ```

  In `tailwind.config.js`:
  ```js
  export default {
    content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
    theme: { extend: {} },
    plugins: [],
  };
  ```

  In `src/index.css`:
  ```css
  @tailwind base;
  @tailwind components;
  @tailwind utilities;
  ```

- [ ] **Step 2: Create `src/types/index.ts`**

  ```typescript
  export interface ApiResponse<T> {
    success: boolean;
    data: T;
    message?: string;
    error?: string;
    timestamp: string;
  }

  export type Role = 'ADMIN' | 'MEMBER' | 'GUEST';
  export type MemberStatus = 'ACTIVE' | 'INACTIVE' | 'LEFT';

  export interface MemberResponse {
    id: number;
    fullName: string;
    phone?: string;
    email?: string;
    role: Role;
    status: MemberStatus;
    balance: number;
    joinedDate: string;
    createdAt: string;
    hasAccount: boolean;
  }

  export interface TokenResponse {
    accessToken: string;
    tokenType: string;
    expiresIn: number;
  }
  ```

- [ ] **Step 3: Create `src/stores/useAuthStore.ts`**

  ```typescript
  import { create } from 'zustand';
  import type { Role } from '../types';

  interface AuthState {
    accessToken: string | null;
    role: Role | null;
    email: string | null;
    setAccessToken: (token: string) => void;
    logout: () => void;
  }

  function parseJwtClaim(token: string, claim: string): string | null {
    try {
      return JSON.parse(atob(token.split('.')[1]))[claim] ?? null;
    } catch { return null; }
  }

  export const useAuthStore = create<AuthState>((set) => ({
    accessToken: null, role: null, email: null,
    setAccessToken: (token) => set({
      accessToken: token,
      role: parseJwtClaim(token, 'role') as Role | null,
      email: parseJwtClaim(token, 'sub'),
    }),
    logout: () => set({ accessToken: null, role: null, email: null }),
  }));
  ```

- [ ] **Step 4: Create `src/api/axios.ts`**

  ```typescript
  import axios from 'axios';
  import { useAuthStore } from '../stores/useAuthStore';

  export const api = axios.create({
    baseURL: 'http://localhost:8080/api/v1',
    withCredentials: true,
  });

  api.interceptors.request.use((config) => {
    const token = useAuthStore.getState().accessToken;
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  });

  api.interceptors.response.use(
    (res) => res,
    async (error) => {
      if (error.response?.status === 401) {
        try {
          const { data } = await axios.post(
            'http://localhost:8080/api/v1/auth/refresh', {}, { withCredentials: true });
          useAuthStore.getState().setAccessToken(data.data.accessToken);
          error.config.headers.Authorization = `Bearer ${data.data.accessToken}`;
          return api(error.config);
        } catch {
          useAuthStore.getState().logout();
          window.location.href = '/login';
        }
      }
      return Promise.reject(error);
    }
  );
  ```

- [ ] **Step 5: Create `src/api/auth.ts`**

  ```typescript
  import { api } from './axios';
  import type { ApiResponse, TokenResponse } from '../types';

  export const authApi = {
    login: (email: string, password: string) =>
      api.post<ApiResponse<TokenResponse>>('/auth/login', { email, password }),
    refresh: () =>
      api.post<ApiResponse<TokenResponse>>('/auth/refresh'),
    changePassword: (currentPassword: string, newPassword: string) =>
      api.patch<ApiResponse<void>>('/auth/change-password', { currentPassword, newPassword }),
  };
  ```

- [ ] **Step 6: Create `src/pages/LoginPage.tsx`**

  ```tsx
  import { useState } from 'react';
  import { useNavigate } from 'react-router-dom';
  import { authApi } from '../api/auth';
  import { useAuthStore } from '../stores/useAuthStore';

  export function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const setAccessToken = useAuthStore((s) => s.setAccessToken);
    const navigate = useNavigate();

    async function handleSubmit(e: React.FormEvent) {
      e.preventDefault();
      setError('');
      setLoading(true);
      try {
        const { data } = await authApi.login(email, password);
        setAccessToken(data.data.accessToken);
        navigate('/dashboard');
      } catch {
        setError('Email hoac mat khau khong dung');
      } finally {
        setLoading(false);
      }
    }

    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="w-full max-w-md bg-white rounded-2xl shadow-lg p-8">
          <h1 className="text-2xl font-bold text-center mb-6 text-green-700">SmashMate</h1>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input id="login-email" type="email" required value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-green-500"
                placeholder="admin@smashmate.local" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Mat khau</label>
              <input id="login-password" type="password" required value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-green-500" />
            </div>
            {error && <p className="text-red-500 text-sm">{error}</p>}
            <button id="login-submit" type="submit" disabled={loading}
              className="w-full bg-green-600 text-white font-semibold py-2 rounded-lg hover:bg-green-700 disabled:opacity-50">
              {loading ? 'Dang dang nhap...' : 'Dang nhap'}
            </button>
          </form>
        </div>
      </div>
    );
  }
  ```

- [ ] **Step 7: Create `src/App.tsx`**

  ```tsx
  import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
  import { LoginPage } from './pages/LoginPage';
  import { useAuthStore } from './stores/useAuthStore';

  function ProtectedRoute({ children }: { children: React.ReactNode }) {
    const token = useAuthStore((s) => s.accessToken);
    return token ? <>{children}</> : <Navigate to="/login" replace />;
  }

  export function App() {
    return (
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <div className="p-8"><h1 className="text-2xl font-bold">Dashboard (coming soon)</h1></div>
            </ProtectedRoute>
          } />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    );
  }
  ```

  Update `src/main.tsx` to use named export:
  ```tsx
  import React from 'react';
  import ReactDOM from 'react-dom/client';
  import { App } from './App';
  import './index.css';
  ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode><App /></React.StrictMode>);
  ```

- [ ] **Step 8: Verify end-to-end**

  ```bash
  cd frontend && npm run dev
  ```
  Open http://localhost:5173 → redirected to /login → login with `admin@smashmate.local` / `Admin@123` → redirected to /dashboard.
  Expected: No console errors.

---

## Task 6: Frontend Members Page

**Files:**
- Create: `frontend/src/api/members.ts`
- Create: `frontend/src/pages/MembersPage.tsx`
- Create: `frontend/src/components/members/MemberFormModal.tsx`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- Consumes: `MemberResponse`, `Role`, `api` from Task 5
- Produces: `/members` route with list, add member, add guest, edit, delete

- [ ] **Step 1: Create `src/api/members.ts`**

  ```typescript
  import { api } from './axios';
  import type { ApiResponse, MemberResponse, Role } from '../types';

  export const memberApi = {
    getAll: (role?: Role) =>
      api.get<ApiResponse<MemberResponse[]>>('/members', { params: role ? { role } : {} }),
    create: (data: { fullName: string; phone?: string; email?: string; password?: string }) =>
      api.post<ApiResponse<MemberResponse>>('/members', data),
    createGuest: (fullName: string) =>
      api.post<ApiResponse<MemberResponse>>('/members/guests', { fullName }),
    update: (id: number, data: { fullName?: string; phone?: string }) =>
      api.put<ApiResponse<MemberResponse>>(`/members/${id}`, data),
    assignAccount: (id: number, email: string, password: string) =>
      api.patch<ApiResponse<MemberResponse>>(`/members/${id}/account`, { email, password }),
    updateStatus: (id: number, status: string) =>
      api.patch<ApiResponse<MemberResponse>>(`/members/${id}/status`, { status }),
    delete: (id: number) => api.delete<ApiResponse<void>>(`/members/${id}`),
  };
  ```

- [ ] **Step 2: Create `src/components/members/MemberFormModal.tsx`**

  ```tsx
  import { useState } from 'react';
  import { memberApi } from '../../api/members';
  import type { MemberResponse } from '../../types';

  interface Props {
    member: MemberResponse | null;
    onClose: () => void;
    onSaved: () => void;
  }

  export function MemberFormModal({ member, onClose, onSaved }: Props) {
    const [fullName, setFullName] = useState(member?.fullName ?? '');
    const [phone, setPhone] = useState(member?.phone ?? '');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);

    async function handleSubmit(e: React.FormEvent) {
      e.preventDefault();
      setLoading(true);
      try {
        if (member) {
          await memberApi.update(member.id, { fullName, phone });
        } else {
          await memberApi.create({ fullName, phone, email: email || undefined, password: password || undefined });
        }
        onSaved();
      } finally { setLoading(false); }
    }

    return (
      <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
        <div className="bg-white rounded-2xl p-6 w-full max-w-md shadow-xl">
          <h2 className="text-lg font-bold mb-4">{member ? 'Sua thanh vien' : 'Them thanh vien'}</h2>
          <form onSubmit={handleSubmit} className="space-y-3">
            <input id="member-fullname" required value={fullName} onChange={(e) => setFullName(e.target.value)}
              placeholder="Ho ten *" className="w-full border rounded-lg px-3 py-2" />
            <input id="member-phone" value={phone} onChange={(e) => setPhone(e.target.value)}
              placeholder="So dien thoai" className="w-full border rounded-lg px-3 py-2" />
            {!member && (
              <>
                <input id="member-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                  placeholder="Email (tuy chon)" className="w-full border rounded-lg px-3 py-2" />
                <input id="member-password" type="password" value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Mat khau (tuy chon)" className="w-full border rounded-lg px-3 py-2" />
              </>
            )}
            <div className="flex gap-2 pt-2">
              <button type="button" onClick={onClose}
                className="flex-1 border rounded-lg py-2 hover:bg-gray-50">Huy</button>
              <button id="member-save-btn" type="submit" disabled={loading}
                className="flex-1 bg-green-600 text-white rounded-lg py-2 hover:bg-green-700 disabled:opacity-50">
                {loading ? 'Dang luu...' : 'Luu'}
              </button>
            </div>
          </form>
        </div>
      </div>
    );
  }
  ```

- [ ] **Step 3: Create `src/pages/MembersPage.tsx`**

  ```tsx
  import { useEffect, useState } from 'react';
  import { memberApi } from '../api/members';
  import type { MemberResponse } from '../types';
  import { MemberFormModal } from '../components/members/MemberFormModal';

  export function MembersPage() {
    const [members, setMembers] = useState<MemberResponse[]>([]);
    const [showModal, setShowModal] = useState(false);
    const [editTarget, setEditTarget] = useState<MemberResponse | null>(null);

    async function load() {
      const { data } = await memberApi.getAll();
      setMembers(data.data);
    }

    useEffect(() => { load(); }, []);

    async function handleDelete(id: number) {
      if (!confirm('Xoa thanh vien nay?')) return;
      await memberApi.delete(id);
      load();
    }

    async function handleAddGuest() {
      const name = prompt('Ten khach:');
      if (name?.trim()) { await memberApi.createGuest(name.trim()); load(); }
    }

    const roleBadge: Record<string, string> = {
      ADMIN: 'bg-purple-100 text-purple-700',
      MEMBER: 'bg-blue-100 text-blue-700',
      GUEST: 'bg-orange-100 text-orange-700',
    };

    return (
      <div className="p-6">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-2xl font-bold">Thanh vien</h1>
          <div className="flex gap-2">
            <button id="add-guest-btn" onClick={handleAddGuest}
              className="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300 text-sm">
              + Them khach
            </button>
            <button id="add-member-btn"
              onClick={() => { setEditTarget(null); setShowModal(true); }}
              className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 text-sm">
              + Them thanh vien
            </button>
          </div>
        </div>
        <table className="w-full text-sm border-collapse">
          <thead>
            <tr className="bg-gray-100 text-left">
              {['Ten', 'SDT', 'Role', 'Trang thai', 'Tai khoan', 'Thao tac'].map((h) => (
                <th key={h} className="p-3">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {members.map((m) => (
              <tr key={m.id} className="border-b hover:bg-gray-50">
                <td className="p-3">{m.fullName}</td>
                <td className="p-3">{m.phone ?? '—'}</td>
                <td className="p-3">
                  <span className={`px-2 py-1 rounded text-xs font-medium ${roleBadge[m.role]}`}>
                    {m.role}
                  </span>
                </td>
                <td className="p-3">
                  <span className={`px-2 py-1 rounded text-xs font-medium ${
                    m.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                  }`}>{m.status}</span>
                </td>
                <td className="p-3">{m.hasAccount ? 'Co' : '—'}</td>
                <td className="p-3 flex gap-2">
                  <button onClick={() => { setEditTarget(m); setShowModal(true); }}
                    className="text-blue-600 hover:underline text-xs">Sua</button>
                  <button onClick={() => handleDelete(m.id)}
                    className="text-red-600 hover:underline text-xs">Xoa</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {showModal && (
          <MemberFormModal member={editTarget}
            onClose={() => setShowModal(false)}
            onSaved={() => { setShowModal(false); load(); }} />
        )}
      </div>
    );
  }
  ```

- [ ] **Step 4: Add `/members` route to `App.tsx`**

  In `src/App.tsx`, add:
  ```tsx
  import { MembersPage } from './pages/MembersPage';
  // Inside <Routes>:
  <Route path="/members" element={<ProtectedRoute><MembersPage /></ProtectedRoute>} />
  ```

- [ ] **Step 5: Verify end-to-end**

  Backend + frontend running. Login, navigate to http://localhost:5173/members.
  - Seeded Admin row visible
  - "Them thanh vien" → modal opens, fill name, save → row appears
  - "Them khach" → prompt for name → GUEST row appears with GUEST badge
  - Delete → row disappears

---

## Self-Review

### Spec Coverage

| Spec requirement | Task |
|-----------------|------|
| All 11 DB tables (V1 migration) | Task 1 |
| Seed: expense_categories, club_settings, admin user | Task 1 |
| JWT login + refresh token (HttpOnly cookie) | Task 3 |
| Change password | Task 4 (MemberService) + Task 3 (AuthController) |
| ADMIN/MEMBER/GUEST roles | Task 4 |
| Offline MEMBER (nullable email/password) | Task 4 |
| GUEST always NULL email/password (enforced) | Task 4 |
| Assign account to offline MEMBER | Task 4 |
| Soft delete members | Task 4 |
| `@PreAuthorize` role enforcement | Task 4 |
| `ApiResponse` envelope on all responses | Task 2 |
| Error codes on all error responses | Task 2 |
| BCrypt strength 12 | Task 3 (SecurityConfig) |
| CORS (localhost:5173) | Task 3 (SecurityConfig) |
| Frontend login page | Task 5 |
| Token refresh interceptor (401 retry) | Task 5 |
| Frontend members list + CRUD | Task 6 |

### Type Consistency

- `Role` enum values: `ADMIN/MEMBER/GUEST` — consistent in Java entity, Spring Security, frontend types ✅
- `MemberResponse.hasAccount: boolean` — backend MapStruct expression returns `boolean`; frontend type declares `boolean` ✅
- `ApiResponse<T>` — `success/data/message/error/timestamp` consistent between Java builder and TypeScript interface ✅
- `BigDecimal balance` on backend — serialized as number in JSON; frontend `balance: number` ✅
