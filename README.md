# ServiceFlow — Deliverable 4: Second Prototype + Tests

CS2114 Project 3 · Joey, Jovany, and Noah

ServiceFlow is a Java console application for a small service business. Users add requests, inspect the FIFO waiting queue, start and complete jobs, record payment, and check history and totals. **The D4 feature is autosave after successful changes and restore on startup**, as named in D3.

**Before submission:** complete the confirmed-human-contribution column in Attribution. Proposed ownership below is a plan, not evidence of completed work. Each member must actually contribute substantively to at least one class or test file and be ready to explain it. This draft does not claim those contributions happened.

## How to compile and run

### VS Code on Windows

1. Extract the entire ZIP. Open the `cs2114-project3-group63` folder in VS Code, not the ZIP or its parent. This folder contains `src`, `lib`, `.vscode`, and `run-serviceflow.cmd`.
2. Use **Terminal → Run Task → ServiceFlow: Run** (or Ctrl+Shift+B).
3. Use **Terminal → Run Task → ServiceFlow: Test** to compile the source and run JUnit. Expect **47 tests successful, 0 failed**.
4. Use **Terminal → Run Task → ServiceFlow: Model Demo** for the original standalone model demonstration.

Equivalent commands in the VS Code PowerShell terminal:

```powershell
.\run-serviceflow.cmd
.\run-serviceflow.cmd test
.\run-serviceflow.cmd model
```

The Windows launcher retains the D3 local Java runtime download with checksum verification; its first run needs internet and 64-bit Windows. The ECJ compiler and JUnit console runner are included in `lib`. No Maven installation is required. Do not delete `lib`. The VS Code Java Extension Pack is useful for editing and the Testing sidebar, but the tasks run independently of it.

If you already have Java 17 or newer on PATH, these PowerShell commands avoid the runtime download:

```powershell
$mainSources = @(Get-ChildItem src/main/java -Recurse -Filter *.java | ForEach-Object { $_.FullName })
$testSources = @(Get-ChildItem src/test/java -Recurse -Filter *.java | ForEach-Object { $_.FullName })
$junit = 'lib/junit-platform-console-standalone-1.11.4.jar'
java -jar lib/ecj-3.40.0.jar -17 -proc:none -encoding UTF-8 -d out $mainSources
java -jar lib/ecj-3.40.0.jar -17 -proc:none -encoding UTF-8 -cp "out;$junit" -d test-out $testSources
java -jar $junit execute --class-path "out;test-out" --scan-class-path --fail-if-no-tests
java -cp out serviceflow.controller.ServiceController
```

### Linux/macOS with Java 17+

```bash
bash test-serviceflow.sh
java -cp out serviceflow.controller.ServiceController
java -cp out serviceflow.demo.ModelDriver
```

The script compiles all main and test sources using the included ECJ compiler, then executes JUnit. To run only model tests after compilation:

```bash
java -jar lib/junit-platform-console-standalone-1.11.4.jar execute --class-path out:test-out --select-package serviceflow.model --fail-if-no-tests
```

Use `out;test-out` instead of `out:test-out` on Windows. The 38 model test invocations instantiate models directly without a controller, view, or console input. Nine additional integration tests check user flows and persistence wiring; they supplement the model tests.

### Save location and recovery

The application uses `serviceflow-data.tsv` in the working folder. Successful add, start, complete, payment, and confirmed clear operations trigger autosave. Read-only choices and rejected operations do not write the file. Clearing an already empty queue is a valid no-op and may save an equivalent snapshot.

A first launch with no file starts empty. Restarting restores waiting order, active work, history, bills, payment statuses, and the next ID. The application can use a different file:

```powershell
java -cp out serviceflow.controller.ServiceController demo-session.tsv
```

The model writes UTF-8 tab-separated records with a version and next-ID header. It writes a temporary sibling file and uses atomic replacement. If the filesystem cannot perform atomic replacement, saving fails with a message; there is no unsafe delete-and-replace fallback. Parents must already exist. Only one app instance should use a save file at a time.

If an existing file cannot be loaded, the console explains the problem and disables autosave for that run to protect the file. Changes can still be made in memory. Preserve a copy, repair or move the bad file, and restart. If a later save fails, the changed state remains in memory and the app warns that the previous save remains unchanged. It retries on the next successful mutation. Do not exit expecting unsaved changes to be restored.

## Design and class responsibilities

| Class | Responsibility |
| --- | --- |
| `Job` | Immutable request/status snapshot; validates fields and calculates the rounded quote. |
| `LinkedQueue<T>` / private `Node<T>` | FIFO linked structure with snapshot iteration. |
| `ServiceManager` | Owns jobs, waiting order, active job, transitions, bills, totals, and validated save/load. |
| `ServiceController` | Reads actions, parses input, invokes model operations, triggers persistence, and passes results to the view. |
| `ConsoleView` | Prompts and formats supplied results; owns no job collection, status, total, or manager reference. |
| `ModelDriver` | Demonstrates the model without the controller or view. |

Flow: user input → controller → manager → current job snapshots/totals → controller → view. The controller briefly creates a display list; neither it nor the view retains a second business-state collection.

## Tests and validation

Verified on November 16, 2026 using Java 21, ECJ targeting Java 17, and JUnit Platform 1.11.4: **47 executed, 47 passed, none skipped**. Parameterized malformed-file cases count separately. Windows launcher commands were reviewed but could not be executed in the Linux verification environment. Run the VS Code Test task on your Windows computer before lab.

| Model method or contract | Normal coverage | Invalid input or boundary coverage |
| --- | --- | --- |
| `Job` constructor, getters, cost, `toString` | Trimmed fields, all getters, rounding, zero costs, upper limits | Invalid ID, missing/long/unsafe text, negative/zero hours, invalid ranges, excess decimals, nonfinite numbers |
| `enqueue`, `peek`, `dequeue` | FIFO, refill, interleaved enqueue/dequeue order | Null entries, empty peek/dequeue, unchanged queue after rejection |
| Queue `size`, `isEmpty`, `clear`, `toArray`, `iterator`, `toString` | Populated and cleared states, snapshot isolation | Empty/repeated clear, exhausted iterator, rejected iterator mutation |
| Both `addJob` overloads | Automatic and explicit IDs | Null, non-waiting job, duplicate/reused ID, invalid fields, exhausted IDs |
| Manager `peek`, `startNextJob`, `removeJob` | FIFO and active status | Empty queue and second start while active |
| `completeActiveJob` | Bill and history created | No active job and repeated completion |
| `markPaid`, `getJob` | Lookup and payment totals | Unknown/nonpositive IDs, premature/repeated payment |
| `jobCount`, `isEmpty`, `iterator`, `getActiveJob`, `getHistory`, totals | Waiting/active/completed/paid states and decimal totals | Empty state, immutable history, exhausted/read-only iterator, preserved state after errors |
| `clearWaitingJobs` | Removes only waiting work, keeps active/history/ID sequence | Empty/repeated clear and rejected reuse of removed ID |
| `saveJobs` | Empty/mixed sessions, replace previous snapshot | Null/blank/invalid paths, missing parent, failed replacement with preserved destination and temp-file cleanup |
| `loadJobs` | Complete round trip and next ID after clear | Missing file/directory, bad paths, bad version/header/row, duplicate IDs, invalid status/bill/fields, multiple active jobs, invalid next ID; old model stays intact |

No-argument queries do not accept bad input; empty/boundary states and snapshot mutation attempts are their meaningful edge cases. Private helpers and `Job.withStatus` are exercised through model operations. Model validation failures are expected exceptions in JUnit; the interactive controller catches the applicable failures and keeps the session running.

The console tests additionally cover invalid menu text, blank fields, nonnumeric/nonfinite amounts, invalid IDs, negative/over-limit values, premature/repeated transitions, interrupted input, startup corruption, save failure, restart, and unchanged save contents/timestamps after rejected or read-only actions.

## Reflection

### How the design absorbed the change

The implemented change is the same **autosave and restore** feature predicted in D3. The production classes actually modified were **`ServiceManager` and `ServiceController`**.

| D3 prediction | Actual result |
| --- | --- |
| Manager adds `saveJobs(String)` and `loadJobs(String)`, validates before applying, and replaces saves safely | Matched. Loading constructs a temporary manager before committing its state. Saving writes a sibling temporary file and atomically replaces the destination. |
| Controller restores at startup and saves after successful mutations | Matched. A new constructor accepts the save path; `main` supplies the default or a command-line path. `saveSession` handles save messages. |
| View reuses `showMessage`; job display methods stay unchanged | Matched. `ConsoleView` source is unchanged. |
| `Job`, `LinkedQueue`, `Node`, and `ModelDriver` need no persistence changes | Matched. Their source is unchanged from the D3 archive. |

Existing `LinkedQueueTest` and `ServiceManagerTest` were expanded. New `PersistenceTest` and `AutosaveTest` files cover the feature. Build tasks, runner scripts, dependencies, and this README were also updated. The original `JobTest` and `ServiceControllerTest` remain as regression checks.

A detail the D3 plan did not fully resolve was what happens *after* a bad startup load: simply catching the exception would let the next successful action overwrite the damaged file. The controller now blocks autosave for that run and explicitly reports memory-only changes. Another detail was preserving the next ID when every waiting job was cleared; the header records it independently of remaining jobs.

### Model/view boundary

The change did not move business logic into the view. The view learned no file format, statuses, billing rules, or save-path rules and has no copied model state. The manager owns serialization and whole-session validation; the controller decides when to invoke save/load and displays errors through the existing view method. File errors cross the boundary as exceptions/messages, not as view-side business rules.

### Differences from the D3 plan

- The feature was not switched. `saveJobs(String)` and `loadJobs(String)` now declare `IOException`; invalid path arguments use `IllegalArgumentException`. The controller handles both.
- A three-argument controller constructor allows isolated file paths for integration tests. The two-argument constructor remains an explicitly in-memory mode so older tests never read or overwrite a real save.
- The format is now specified: versioned UTF-8 TSV with increasing unique IDs. Record order preserves the waiting queue because requests are added with increasing IDs. Unknown versions are rejected rather than guessed.
- Persistence tests live in separate files for readability. Model tests stay independent of the UI; console tests only verify wiring and user recovery.
- Field-by-field correction after model validation was deferred. Blank text and malformed numbers re-prompt as before; a completed form rejected by a model rule returns to the menu without adding a partial job. D3's richer correction behavior needs a structured field-validation contract to avoid copying limits into the controller.

### What does not work yet

The in-session MVP and the planned persistence feature are implemented; no tests currently fail. Field-level correction is still missing. There is no GUI, recurring template system, partial payment, multi-crew scheduling, concurrency control, or automatic repair of corrupt saves. Permission-denied behavior and unsupported atomic moves are handled by exception paths but were not independently induced under this environment's privileged account/filesystem. Windows execution and the team's lab walkthrough still need local verification.

### Design improvement for the next feature

Before adding more storage formats, extract persistence behind a model-side `SessionStore` interface so `ServiceManager` can focus on service rules and file failures can be tested with an injected store. Before field-level correction, give validation failures a field identifier so the controller can retry the correct field without duplicating `Job` limits. Neither refactor is necessary for this checkpoint's working feature.

## Next Steps

These owners follow the D3 proposed roles; the team must confirm acceptance and record actual work in Attribution.

| Order | Proposed owner | Remaining item |
| --- | --- | --- |
| 1 | Joey | Review and explain `ServiceManager` persistence and `PersistenceTest`; verify failed loads preserve live state and perform permission/atomic-move checks on the target machine. Record actual revisions. |
| 2 | Jovany | Run the VS Code program/test tasks on Windows; review controller autosave and `AutosaveTest`; fix any platform issues. |
| 3 | Jovany with Joey | Add model-owned field identification and controller field retries, with tests retaining previously valid input. |
| 4 | Noah | Substantively review/revise `ServiceControllerTest` and queue edge-case tests; add a justified missing case, then verify messages and the restart workflow. Record the specific contribution. |
| 5 | All three | Fill in truthful per-file attribution, run all tests again after any edits, rehearse the demo, and upload the full source/test/README project to the group repository. |

**Committed stretch goal:** autosave and startup restore, implemented here and retained for the final build. **Dropped from the final-build scope:** GUI, recurring templates, priority queue, extra crews/statuses, partial payments, notifications, exports, and analytics. Prioritize validation and reliability over new features.

### Timed three-minute demo

Before the demo, compile, run the tests, and use a fresh demo file; preserve any real business data. Have the successful test summary ready.

| Time | Demo action |
| --- | --- |
| 0:00–0:20 | Explain controller/model/view roles; show the completed test summary and queue interleaving test. |
| 0:20–0:55 | Add Ana: Lawn, 2.5 hours, $40/hour, $15 materials. Add Luis: Hedges, 1 hour, $35/hour, $0 materials. Option 2 shows FIFO order. |
| 0:55–1:20 | Option 4 starts Ana. Try 4 again to show the handled error. Option 5 completes Ana; option 8 shows $115 unpaid. |
| 1:20–1:50 | Exit with 0 and restart with the same save path. Show restored Luis waiting and Ana's unpaid bill/history. This demonstrates the new feature. |
| 1:50–2:15 | Option 6, ID 1 marks Ana paid. Option 8 shows $115 collected and $0 unpaid. Try paying ID 1 again to show rejection. |
| 2:15–2:40 | Show `loadJobs` validating into a temporary manager and the malformed-file test preserving the old state. |
| 2:40–3:00 | State which classes changed, the deferred field-correction work, and each member's actual contribution. |

## Attribution



| Class or test file | GenAI assistance / current change | Confirmed human contributor(s) and work |
| --- | --- | --- |
| `Job.java` | D3 draft; unchanged in D4 | Joey |
| `LinkedQueue.java` | D3 draft; unchanged in D4 | Joey |
| `LinkedQueue.Node<T>` (nested class) | D3 draft; unchanged in D4 | Joey |
| `ServiceManager.java` | D3 draft; D4 save/load, validation, atomic replacement drafted | Jovany |
| `ServiceController.java` | D3 draft; D4 startup restore, autosave, protected failure mode drafted | Noah |
| `ConsoleView.java` | D3 draft; unchanged in D4 | Noah |
| `ModelDriver.java` | D3 draft; unchanged in D4 | Jovany |
| `JobTest.java` | D3 test draft; retained and executed | Joey |
| `LinkedQueueTest.java` | D3 draft; D4 interleaved FIFO test added and executed | Joey |
| `ServiceManagerTest.java` | D3 draft; D4 empty-state, alias, and invalid-input cases added and executed | Jovany |
| `ServiceControllerTest.java` | D3 test draft; retained and executed | Noah |
| `PersistenceTest.java` | D4 model-only save/load tests drafted and executed | Joey |
| `AutosaveTest.java` | D4 console/persistence integration tests drafted and executed | Jovany |

**GenAI note:** ChatGPT drafted the D3 prototype, tests, and setup. For D4, ChatGPT used the prior D3 project ZIP and the later D3 README plan, drafted persistence and controller changes, added tests, updated VS Code/build scripts, and drafted this README. ChatGPT compiled the code and executed all 47 JUnit test invocations in a Linux environment. Human review, understanding, local execution, substantive contributions, and the attribution names must be recorded by the team; they are not asserted here. The team should retain only code and tests it understands and can defend during grading.

## Submission

Upload the source, tests, scripts, dependency setup, and completed README to the group's GitHub repository, then submit its URL in Canvas. This package does not itself update GitHub or submit Canvas. Include `src/main/java`, `src/test/java`, `.vscode`, `lib`, scripts, README, and third-party notices; do not upload generated class directories, downloaded runtimes, or personal session files.
