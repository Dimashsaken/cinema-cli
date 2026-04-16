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

## Testing & Demo Guide

### Running the Test Suite

```bash
# Run all 238 tests
./gradlew test

# Run only a specific test class
./gradlew test --tests "com.cinebook.service.BookingServiceConcurrencyTest"
./gradlew test --tests "com.cinebook.service.WalRecoveryTest"
./gradlew test --tests "com.cinebook.cli.MainMenuSmokeTest"

# Run all tests in a package
./gradlew test --tests "com.cinebook.domain.*"
./gradlew test --tests "com.cinebook.pricing.*"
./gradlew test --tests "com.cinebook.service.*"

# View test results in the browser
open build/reports/tests/test/index.html

# Generate and view coverage report
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

### Key Tests to Highlight

| Test class | What it proves | Command |
|---|---|---|
| `BookingServiceConcurrencyTest` | 100 threads race for 1 seat -- exactly 1 wins, 99 get `SeatUnavailableException` | `./gradlew test --tests "*ConcurrencyTest"` |
| `WalRecoveryTest` | Simulates crash mid-booking: seats stuck in HELD are auto-released on restart | `./gradlew test --tests "*WalRecoveryTest"` |
| `MainMenuSmokeTest` | End-to-end CLI with scripted input/output -- login, browse, book, confirm | `./gradlew test --tests "*SmokeTest"` |
| `SeatTest` | 25 tests covering seat state machine (hold/book/release invariants) | `./gradlew test --tests "*SeatTest"` |
| `PricingServiceTest` | 16 tests for strategy composition: peak + weekend + VIP + discount stacking | `./gradlew test --tests "*PricingServiceTest"` |
| `BookingServiceTest` | 25 happy-path and failure tests for hold/confirm/cancel/refund | `./gradlew test --tests "*BookingServiceTest"` |

### Coverage Targets (all met)

| Package | Line coverage | Requirement |
|---|---|---|
| `service/` | 89% | >= 80% |
| `domain/` | 84% | >= 80% |
| `pricing/` | 100% | -- |
| `infra/` | 91% | -- |

### Demo Script: Customer Flow

Launch the app and walk through these steps:

```bash
java -jar build/libs/cinebook.jar
```

**Step 1: Login as customer**
```
 > 3                    (Account)
 > 1                    (Login)
 Username: saken
 Password: password123
 Welcome back, saken!
```

**Step 2: Browse showtimes and view seat map**
```
 > 1                    (Browse Showtimes)
 > 1                    (Select "Dune Part Two" at 14:00)
```
You will see the ASCII seat map with `[V]` VIP, `[ ]` available, `[X]` booked, `[H]` held.

**Step 3: Book two seats**
```
 > D6 D7                (Select seats)
 Seats held for 5 minutes.
 Discount code: STUDENT20    (or press Enter to skip)
 Confirm? [Y/N] > Y
 Booking confirmed! Code: BK-20260416-XXXX
```

**Step 4: View booking history**
```
 > 2                    (My Bookings)
```

**Step 5: Refund the booking**
```
 > 2                    (My Bookings)
 > R                    (Refund)
 > 1                    (Select booking #1)
 Booking BK-... refunded.
```

**Step 6: Verify seat is available again**
```
 > 1                    (Browse Showtimes)
 > 1                    (Same showtime)
```
Seats D6 and D7 should show `[ ]` again.

### Demo Script: Admin Flow

```
 > 3                    (Account -- logout first if logged in as customer)
 > 1                    (Logout, if shown)
 > 3                    (Account)
 > 1                    (Login)
 Username: admin
 Password: admin123
 > 4                    (Admin Panel)
```

**Add a movie**
```
 > 1
 Title: Oppenheimer
 Duration (min): 180
 Rating: R
 Genre: Drama
 Movie added: MXXX
```

**Add a showtime**
```
 > 2
 Select movie: (pick the new movie number)
 Hall ID: H1
 Rows: 5
 Cols: 8
 VIP rows: 0,1
 Start time: 2026-05-01 19:00
 Base price: 90
 Showtime added: STXXX
```

**View occupancy report**
```
 > 3
 Select showtime: 1
 Showtime: ST001
 Total: 40 | Booked: 2 | Held: 0 | Available: 38
 Occupancy: 5.0%
```

**View revenue report**
```
 > 4
 Revenue Report
 Confirmed: 1 | Refunded: 0
 Total Revenue: $160.0
```

**Export bookings CSV**
```
 > 5
 bookingId,userId,showtimeId,seats,totalPrice,status,createdAt,version
 BK-20260416-2608,U001,ST001,"D6;D7",160.0,CONFIRMED,2026-04-16T06:52:24.408418Z,2
```

### Demo Script: Discount Codes

Available codes to demonstrate pricing strategy:

| Code | Discount | Example: base $80 regular seat |
|---|---|---|
| `STUDENT20` | 20% off | $64.00 |
| `SENIOR15` | 15% off | $68.00 |
| `WELCOME10` | 10% off | $72.00 |

During booking, enter the code at the "Discount code" prompt.

### Verifying Architecture Constraints

```bash
# CLI never imports infra -- should return nothing
grep -r "import com.cinebook.infra" src/main/java/com/cinebook/cli/

# Domain has no I/O -- should return nothing
grep -r "import java.io\|import java.nio\|import org.slf4j" src/main/java/com/cinebook/domain/

# Domain doesn't import service or CLI -- should return nothing
grep -r "import com.cinebook.service\|import com.cinebook.cli" src/main/java/com/cinebook/domain/
```

### Verifying Concurrency Safety

Run the concurrency test multiple times to prove it's deterministic:

```bash
for i in 1 2 3 4 5; do
  echo "Run $i:"
  ./gradlew test --tests "*ConcurrencyTest" --rerun 2>&1 | grep -E "tests completed|BUILD"
done
```

Every run should show `3 tests completed, 0 failed`.

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
