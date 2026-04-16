# CineBook CLI

A concurrent movie ticket booking engine for the terminal. Built with Java 17, file-based persistence (JSON/CSV), and thread-safe seat booking.

## Prerequisites

- **Java 17+** (OpenJDK or Oracle)
- No other dependencies needed — the Gradle wrapper is included.

## Build

```bash
./gradlew build
```

## Run

```bash
java -jar build/libs/cinebook.jar
```

Or directly:

```bash
./gradlew run
```

## Test

```bash
./gradlew test
```

Coverage report (HTML):

```bash
./gradlew jacocoTestReport
# open build/reports/jacoco/test/html/index.html
```

## Seed Accounts

| Username | Password    | Role     |
|----------|-------------|----------|
| saken    | password123 | CUSTOMER |
| admin    | admin123    | ADMIN    |

## Project Structure

```
src/main/java/com/cinebook/
  App.java                  # Entry point
  cli/                      # Terminal UI controllers
  service/                  # Business logic
  domain/                   # Pure domain entities and enums
  pricing/                  # Strategy pattern for pricing
  repository/               # Repository interfaces
  infra/                    # File I/O, hashing, WAL
  exception/                # Checked exception hierarchy
data/
  movies.json               # Movie catalog
  halls.json                # Hall layouts
  showtimes.json            # Showtimes with inline seat grids
  users.json                # Registered users
  bookings.csv              # Append-only booking log
  wal.log                   # Write-ahead log for crash recovery
```

## Architecture

```
CLI  --->  SERVICE  --->  DOMAIN
               |---->  REPOSITORY  --->  INFRASTRUCTURE
```

Strict layering: CLI never touches infra. Domain has no I/O.

## Design Patterns

| Pattern    | Location                  | Purpose                                    |
|------------|---------------------------|--------------------------------------------|
| Strategy   | `pricing/PricingStrategy` | Swappable pricing rules                    |
| Repository | `repository/*`            | Abstracts file persistence from services   |
| Singleton  | (Phase 8)                 | BookingManager lock registry               |
| Factory    | (Phase 8)                 | TicketFactory for Regular vs VIP           |
| Observer   | (Phase 9)                 | SeatStatusNotifier for hold expiry events  |
| Command    | (Phase 9)                 | BookingCommand with execute/undo for refund|

*Patterns marked with a phase number are not yet implemented.*

## Implementation Status

- [x] Phase 1: Scaffold
- [ ] Phase 2: Domain
- [ ] Phase 3: Infrastructure
- [ ] Phase 4: Repositories
- [ ] Phase 5: Pricing
- [ ] Phase 6: Auth
- [ ] Phase 7: Single-threaded booking
- [ ] Phase 8: Concurrency
- [ ] Phase 9: Hold TTL & Observer
- [ ] Phase 10: CLI
- [ ] Phase 11: Admin
- [ ] Phase 12: WAL + crash recovery
- [ ] Phase 13: Polish
