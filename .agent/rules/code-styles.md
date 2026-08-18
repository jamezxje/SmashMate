---
description: Enforce SmashMate code conventions — agent MUST read and follow these rules before writing any code for this project
alwaysApply: true
---

# SmashMate Code Convention

<HARD-GATE>
Before writing, modifying, or generating ANY code in this project, you MUST follow every rule defined below.
The full convention reference is at: docs/CODE_CONVENTION.md
</HARD-GATE>

## General

- All code, variable names, comments, and function names: **English**
- No `System.out.println`, no `console.log` left in committed code
- No magic numbers/strings — use constants or enums
- One responsibility per class/file

## Backend (Java + Spring Boot)

- **Naming**: `PascalCase` for classes, `camelCase` for methods/variables, `SCREAMING_SNAKE_CASE` for constants
- **Package**: `com.smashmate.<feature>` (e.g., `com.smashmate.session`)
- **Money**: Always `BigDecimal` — never `double` or `float`
- **Time**: Always `LocalDateTime` — never `java.util.Date`
- **Entities**: Use `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor` — never `@Data`
- **Relations**: `FetchType.LAZY` by default — `EAGER` only when explicitly justified
- **Transactions**: `@Transactional` on service class, `@Transactional(readOnly = true)` on read-only methods
- **Controllers**: Thin — only call service methods, no business logic
- **Exceptions**: Custom exception classes only — never throw `RuntimeException` directly
- **API responses**: Always wrap in `{ success, data, message, timestamp }` envelope

## Frontend (React + Vite + Tailwind)

- **Components**: Named exports only — never `export default`
- **Props**: Each component must have a `<ComponentName>Props` TypeScript interface
- **No `any`**: Use `unknown` or specific types
- **API calls**: Centralized in `src/api/<feature>.ts` — never call axios directly in components
- **State**: Zustand stores in `src/stores/use<Feature>Store.ts`
- **Money**: Never calculate financial amounts in the frontend — display only what the API returns
- **Currency display**: Use `formatVND()` utility from `src/utils/formatCurrency.ts`

## Database (MySQL)

- **Tables**: `snake_case`, plural (e.g., `shuttle_batches`, `expense_items`)
- **Columns**: `snake_case` (e.g., `unit_price`, `session_id`)
- **Money columns**: `DECIMAL(12, 2)` — never `FLOAT` or `DOUBLE`
- **Time columns**: `DATETIME` — never `TIMESTAMP`
- **Every table must have**: `created_at DATETIME NOT NULL`, `updated_at DATETIME NOT NULL`
- **Soft delete**: `deleted_at DATETIME NULL` — never hard-delete financial records
- **Migrations**: Flyway, named `V{n}__{description}.sql` — never modify a committed migration file

## Commit Messages

Follow Conventional Commits:
```
feat(session): add RSVP endpoint
fix(expense): correct FIFO shuttle cost calculation
```
Types: `feat | fix | docs | style | refactor | test | chore`
Scopes: `auth | member | session | expense | shuttle | report`
