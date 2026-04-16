# CineBook CLI

A concurrent movie ticket booking engine for the terminal. Built with Java 17, file-based persistence (JSON/CSV), and thread-safe seat booking.

## Prerequisites

- **Java 17+** (OpenJDK or Oracle)
- No other dependencies needed -- the Gradle wrapper is included.

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
./gradlew run -q --console=plain
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

## Sample Session

```
================================
       CineBook  v1.0
================================
 [1] Browse Showtimes
 [2] My Bookings
 [3] Account
 [Q] Quit
--------------------------------
 Not logged in
 > 3

================================
       Account Access
================================
 [1] Login
 [2] Register
 [B] Back
--------------------------------
 > 1
 Username: saken
 Password: password123
 Welcome back, saken!

 > 1

================================
       Showtimes
================================
 [1] Dune Part Two        | 2026-04-20 14:00 | Hall H1 | $80.00
 [2] Dune Part Two        | 2026-04-20 18:00 | Hall H1 | $100.00
 ...
 [B] Back
--------------------------------
 > 1

 Dune Part Two | 2026-04-20 14:00

        SCREEN THIS WAY
        ------------------------
      1  2  3  4  5  6  7  8
 A  [V][V][V][V][V][V][V][V]
 B  [V][V][V][V][V][V][V][V]
 C  [ ][ ][ ][ ][ ][ ][ ][ ]
 D  [ ][ ][ ][ ][ ][ ][ ][ ]
 E  [ ][ ][ ][ ][ ][ ][ ][ ]

 Legend: [ ] available  [X] booked  [H] held  [V] VIP available

 Select seats (e.g. D6 D7) or [B] back:
 > D6 D7
 Seats held for 5 minutes.
 Discount code (or Enter to skip):
================================
       BOOKING SUMMARY
================================
 Movie   : Dune Part Two
 Hall    : H1
 Time    : 2026-04-20 14:00
 Seats   : D6, D7
--------------------------------
 Confirm? [Y/N] > Y
 Booking confirmed! Code: BK-20260420-9F3A
```

## Project Structure

```
src/main/java/com/cinebook/
  App.java                  # Entry point, wires all layers, WAL recovery
  cli/
    MainMenu.java           # Main menu loop
    AuthController.java     # Login/register UI
    BookingController.java  # Browse, seat selection, booking UI
    AdminController.java    # Admin panel UI
    AsciiSeatRenderer.java  # Seat map ASCII rendering
    InputReader.java        # Scanner wrapper with retry support
  service/
    AuthService.java        # Registration, login, session management
    BookingService.java     # Hold, confirm, cancel, refund with locking
    BookingManager.java     # Singleton lock registry (concurrency layer 1)
    PricingService.java     # Strategy composition engine
    ReportService.java      # Occupancy and revenue reports
    HoldExpiryScheduler.java # Background TTL scanner (concurrency layer 2)
    SeatStatusNotifier.java # Observer for seat release events
  domain/
    Movie.java              # Film catalog entity
    Hall.java               # Hall layout with VIP row config
    Showtime.java           # Screening with inline seat grid
    Seat.java               # Seat with hold/book/release state machine
    Booking.java            # Booking with confirm/cancel/refund lifecycle
    Ticket.java             # Individual ticket (seat + price)
    HoldToken.java          # Temporary hold reference with TTL
    User.java               # User with hashed credentials
    enums/                  # Role, SeatStatus, SeatTier, BookingStatus
  pricing/
    PricingStrategy.java    # Strategy interface
    StandardPricing.java    # Base + 1.5x VIP
    PeakHourPricing.java    # 1.25x for 18:00-21:00
    WeekendPricing.java     # 1.20x for Sat/Sun
    DiscountCodePricing.java # STUDENT20, SENIOR15, WELCOME10
  repository/
    Repository.java         # Generic CRUD interface
    MovieRepository.java    # Movie-specific queries
    ShowtimeRepository.java # findByMovieId
    BookingRepository.java  # findByUserId
    UserRepository.java     # findByUsername
  infra/
    JsonFileAdapter.java    # Atomic JSON read/write (tmp+fsync+move)
    CsvAppendAdapter.java   # Append-only CSV with quoted field parsing
    JsonMovieRepository.java
    JsonShowtimeRepository.java
    JsonUserRepository.java
    CsvBookingRepository.java
    PasswordHasher.java     # Salted SHA-256
    TransactionLog.java     # Write-ahead log for crash recovery
    FileLockManager.java    # Cross-process file locking
  exception/
    CineBookException.java  # Root checked exception
    SeatUnavailableException.java
    HoldExpiredException.java
    PaymentFailedException.java
    AuthenticationException.java
    InvalidInputException.java
data/
  movies.json               # 3 movies
  halls.json                # 2 halls
  showtimes.json            # 5 showtimes with full seat grids
  users.json                # 2 seed users
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

| Pattern    | File                                          | Why                                              |
|------------|-----------------------------------------------|--------------------------------------------------|
| Strategy   | `pricing/PricingStrategy.java` + 4 impls      | Pricing rules vary (peak, weekend, discount). Swap at runtime, no if/else chains. |
| Singleton  | `service/BookingManager.java`                 | Single source of truth for per-showtime locks across all threads. |
| Observer   | `service/SeatStatusNotifier.java`             | CLI sessions refresh seat map when HoldExpiryScheduler releases expired holds. |
| Repository | `repository/*` interfaces, `infra/*` impls    | Services never see JSON/CSV. Swap to DB without touching business logic. |
| Decorator  | `PeakHourPricing(StandardPricing)` chaining   | Pricing strategies wrap each other, multipliers stack naturally. |

## Concurrency Model

Three-layer defense against double-booking:

1. **In-process lock** -- `BookingManager` holds a `ConcurrentHashMap<showtimeId, ReentrantLock>`. All seat mutations acquire the lock with 2s timeout.
2. **Hold TTL** -- Seats go HELD for 5 minutes. `HoldExpiryScheduler` runs every 30s on a daemon thread, releases expired holds, publishes events via `SeatStatusNotifier`.
3. **Optimistic versioning** -- `Booking.version` increments on every state change. Append-only CSV means all versions are preserved.

Proven by `BookingServiceConcurrencyTest`: 100 threads racing for 1 seat, exactly 1 wins.

## Crash Recovery

On startup, `App.main` runs WAL recovery:
1. Scan `wal.log` for HOLD entries without a matching ROLLBACK
2. For each, release any seats still in HELD state
3. Log a ROLLBACK entry

This guarantees that after a crash, bookings are either fully present or fully absent -- never half-committed.

## Implementation Status

- [x] Phase 1: Scaffold
- [x] Phase 2: Domain entities with invariants
- [x] Phase 3: Infrastructure (atomic writes, CSV, password hashing, WAL)
- [x] Phase 4: Repositories (JSON + CSV backed)
- [x] Phase 5: Pricing (Strategy pattern, 4 implementations)
- [x] Phase 6: Auth (register, login, role checks)
- [x] Phase 7: Single-threaded booking (hold/confirm/cancel/refund)
- [x] Phase 8: Concurrency (ReentrantLock, 100-thread test)
- [x] Phase 9: Hold TTL + Observer
- [x] Phase 10: CLI (MainMenu, seat renderer, booking flow)
- [x] Phase 11: Admin (reports, CSV export, add movie/showtime)
- [x] Phase 12: WAL crash recovery
- [x] Phase 13: Polish
