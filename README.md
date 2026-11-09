# ServiceFlow · Deliverable 3 First Prototype

CS2114 Project 3 · Joey, Jovany, and Noah

ServiceFlow is a Java console app for small landscaping, cleaning, and repair businesses. This prototype lets an owner add requests, check the waiting line, start jobs in first-in, first-out order, issue bills, record payments, and view history and totals. One job can be active at a time. Records live in memory for the current session.

## How to run

### Interactive console in VS Code on Windows

1. Clone or download this repository. If you download a ZIP, extract it first.
2. In VS Code, choose **File → Open Folder** and select the folder containing `README.md`, `src`, and `run-serviceflow.cmd`.
3. Press **Ctrl+Shift+B** or choose **Terminal → Run Task → ServiceFlow: Run**.
4. When `Choice:` appears in the terminal, enter a menu number. The first run needs internet to download the tools into the project folder. The VS Code task handles that setup automatically.

For the lab demo, choose `1` and enter `Ana`, `Mow lawn`, `2.5`, `40`, and `15`. Choose `4` to start the job and `5` to finish it. Choose `8` to see **$115.00 unpaid**. Choose `6`, enter job ID `1`, and then choose `8` to see **$115.00 collected** and **$0.00 unpaid**. Choose `0` to exit.

### Standalone model driver

From the repo root, run these commands in PowerShell with JDK 17 or newer. This compiles only the model and demo packages.

```powershell
$modelSources = @(Get-ChildItem -Path 'src/main/java/serviceflow/model','src/main/java/serviceflow/demo' -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac --release 17 -encoding UTF-8 -d model-out $modelSources
java -cp model-out serviceflow.demo.ModelDriver
```

The driver adds two requests, checks FIFO order, completes the first job, and verifies that recording payment moves the bill from unpaid to collected. It exits automatically.

### JUnit tests

From the repo root, use PowerShell with JDK 17 or newer.

```powershell
$mainSources = @(Get-ChildItem src/main/java -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac --release 17 -encoding UTF-8 -d out $mainSources

$junit = 'junit-platform-console-standalone-1.11.4.jar'
Invoke-WebRequest -UseBasicParsing -Uri "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/1.11.4/$junit" -OutFile $junit
$testSources = @(Get-ChildItem src/test/java -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac --release 17 -encoding UTF-8 -cp "out;$junit" -d test-out $testSources
java -jar $junit execute --class-path "out;test-out" --scan-class-path
```

### Lab demo

1. Run the model driver. It adds Ana and Luis, verifies that Ana starts first, and prints the status and payment transitions. It exits automatically.
2. Run the console and choose `1`. Enter `Ana`, `Mow lawn`, `2.5`, `40`, and `15`. Add a second request for `Luis`, `Trim hedges`, `1`, `35`, and `0`.
3. Choose `2` to see the waiting line. Choose `4` to start Ana, then `5` to finish her job. Choose `8` to see `$115.00` unpaid.
4. Choose `6` and enter job ID `1`. Choose `7` to see the paid job in history and `8` to see `$115.00` collected and `$0.00` unpaid. Luis is still waiting. Choose `0` to exit.



## Reflection

### What changed from the specification

| Area | What Deliverable 2 specified | What the prototype does and why |
| --- | --- | --- |
| Classes and entry points | The model uses `Job`, `LinkedQueue<T>`, its nested `Node<T>`, and `ServiceManager`. The console starts in `ServiceController.main()` and uses `ConsoleView`. | Those classes and roles carry over in separate packages. `ModelDriver.main()` is an extra entry point, so the user can run the full queue and payment path with fixed inputs and no view. |
| File methods and menu | `saveJobs(String)` and `loadJobs(String)` belong to the manager. `saveQueue()` and `loadQueue()` are controller actions at menu choices 9 and 10. | The prototype leaves those methods and menu choices for Deliverable 4. The original scope made persistence the change feature, and deferring it lets this checkpoint establish the in-session workflow first. |
| Active-job display | The menu listed waiting jobs under option 2 and showed the active job when starting work. | Option 2 also displays the active job so an owner can check it again without starting or completing anything. `jobList()` still formats only waiting jobs. |
| Validation and recovery | The model enforces request and stus rules. The controller handles input parsing and lets the owner correct an invalid field without losing other entries. | `Job` enforces request rules, `ServiceManager` enforces ID and stus rules, and the controller retries blank text and malformed numbers at their prompts. Model-level request errors such as a negative rate return to the menu with an explanation, so the owner can retry the request. This keeps business limits in one place while the first prototype establishes the add flow. Payment retries an invalid or unknown ID in the payment prompt. |
| Tests | The specification proposed individual method, validation, state-transition, and file tests. | Four JUnit files cover the queue, jobs, manager, and console. The console tests drive the actual menu with input lines so they can catch controller, view, and model wiring errors that model tests miss. The file cases wait for persistence. |

### Did the model stand alone?

Yes. `ModelDriver` imports only model classes and calls the same `ServiceManager` methods as the controller. The model and driver compile together with the entire view and controller excluded. The queue, job, and manager JUnit tests also exercise the model directly. Billing and status transitions stay in the model. The controller parses text and sends snapshots or totals to the view. `ConsoleView` formats those results using `Job` getters and has no manager reference. `Job.toString()` is available for model diagnostics and the driver.

### What happened to the original unknowns?

| Question from Deliverable 1 | Result |
| --- | --- |
| How do front and rear behave when the final request leaves? | Removing the final node clears the rear reference as well as the front. A queue test removes the only item and adds another. |
| How can statuses prevent invalid transitions? | The manager is the only class that changes stored status. It creates replacement snapshots for `WAITING → ACTIVE → COMPLETED → PAID` and rejects a second start or an early or repeated payment. |
| How should charges be represented and rounded? | The public API keeps the specified doubles. Decimal arithmetic calculates the quote and rounds it once to cents with `HALF_UP`. Totals add the stored bills using decimal arithmetic. Tests cover a half-cent and a multi-bill total. |
| How can tests drive the model without console input? | JUnit tests instantiate the queue or manager directly. The driver separately demonstrates the complete workflow with fixed data. |
| How can a saved file safely restore queue order? | Still open in code. Deliverable 4 will validate a temporary session before replacing the live one, and test that the original session survives a rejected load. |

### What remains open

The full in-session MVP runs from adding a job through payment, history, and totals. All 20 current JUnit tests pass. Save and load and their tests are still pending. For rejected requests that pass parsing but fail a model rule, the console currently returns to the menu instead of retaining the completed form and re-prompting the affected field.

## Next Steps

### Order of work and owners

These owners follow the responsibilities in Deliverable 2 and are proposed for the next checkpoint.

| Order | Owner | Remaining work |
| --- | --- | --- |
| 1 | Jovany, with Joey on the model contract | Finish the remaining in-session recovery polish. Make `ServiceController.addJob()` retain valid fields and ask again for the field rejected by the model. Joey will keep the field validation rules in `Job`. |
| 2 | Joey | Implement `ServiceManager.saveJobs(String)` and `loadJobs(String)`, along with model tests. Preserve the waiting order, active job, history, bills, and next ID. |
| 3 | Noah | Extend `ConsoleView.showMenu()` with the save and restore choices and verify the prompts, confirmation, and messages in a console session. |
| 4 | Jovany | Connect the save and restore commands to the model, handle cancellation and errors, add controller tests, and run the final demo from a fresh checkout. |

### Deliverable 4 change feature · Save and restore a session

This feature extends the original in-session MVP. The owner will save the current session to a file and restore it later, even after restarting ServiceFlow. Loading rebuilds the waiting queue, active job, history, bills, and ID counter. The model will build and validate a temporary session before replacing the live session. A failed load leaves the live session intact.
ServiceFlow will automatically save the session after every successful change, including adding a request, starting or finishing a job, recording a payment, and clearing the waiting queue. The save file will preserve the waiting order, active job, job history, payment statuses, and next job ID. When ServiceFlow opens again, it will restore the last saved session so the owner can pick up where they left off.

The controller will trigger the save after each successful change, and `ServiceManager` will handle reading and writing the file. Rejected actions will leave the save file untouched. If saving fails, the console will tell the owner that the latest change is still in memory and has not been saved.

| Class | Method-level prediction |
| --- | --- |
| `ServiceManager` (model) | Add `saveJobs(String)` and `loadJobs(String)`. Save the version header and records in creation order, then replace the destination from a temporary file. Load checks IDs, status, one-active-job limit, bill amounts, and the next-ID counter before committing state. |
| `ServiceController` (controller) | Add `saveQueue()` and `loadQueue()`. Update `run()` to dispatch choices 9 and 10. Ask for a file path, confirm replacing the current session, and display file errors through the view. |
| `ConsoleView` (view) | Update `showMenu()` to list the new actions. Reuse `readText(String)` and `showMessage(String)` for prompts and results. `showJob(Job)` and `showTotal(double, double)` should stay as written. |
| `Job`, `LinkedQueue<T>`, nested `Node<T>`, and `ModelDriver` | No source changes expected for persistence. The manager can rebuild jobs through the constructor and its existing package-visible `Job.withStatus(...)` helper, then rebuild the line with `LinkedQueue.enqueue(T)`. The driver stays an in-session demonstration. |

**Stretch goals.** Commit to manual save and restore. Drop recurring-service templates and a graphical interface for Deliverable 4.

**New tests.** Joey will test `saveJobs()` and `loadJobs()` for a round trip with two waiting jobs and an active, completed, and paid job. Cases include a blank path, missing file, failed save, unsupported header, duplicate IDs, multiple active jobs, inconsistent bills, and an invalid next-ID counter. Tests will check that a failed load preserves the prior session. Jovany will test `addJob()` field correction and preserved inputs, plus save and restore menu dispatch, declined restore, EOF at a path or confirmation prompt, and recovery after a file error. Noah will verify that menu and result messages match the actions during the demo.

## Attribution

The table records the initial prototype work and separately lists proposed owners. The team should record its actual coding, revision, and testing work in the middle column before submitting.

| Class or test file | Recorded contribution | Proposed owner |
| --- | --- | --- |
| `Job.java` | drafted the immutable snapshot, request validation, and billing calculation. | Joey |
| `LinkedQueue.java` | implemented the FIFO methods and snapshot behavior. | Jovany |
| `LinkedQueue.Node<T>` (nested in `LinkedQueue.java`) |  implemented the queue node and linking behavior. | Joey |
| `ServiceManager.java` | implemented the workflow, ID checks, history, and totals. | Noah|
| `ConsoleView.java` |  drafted the menus, prompts, and displays. | Noah |
| `ServiceController.java` |  integrated the view and model and implemented command and input handling. | Jovany |
| `ModelDriver.java` |  drafted and ran the model-only workflow. | Jovany |
| `JobTest.java` |  wrote and ran the request and billing tests. | Joey |
| `LinkedQueueTest.java` |  wrote and ran the core queue tests. | Noah |
| `ServiceManagerTest.java` |  wrote and ran the model workflow and state-validation tests. | Noah|
| `ServiceControllerTest.java` | wrote and ran the console workflow and recovery tests. | Jovany, with Noah on console output checks |

**GenAI note.** ChatGPT used the team's Deliverable 1 scope, Deliverable 2 specification, and Deliverable 3 rubric to help draft this Java prototype and README and run the driver, console, and tests. For the change feature, the design question was “Given `Job`, `LinkedQueue`, `ServiceManager`, `ConsoleView`, and `ServiceController`, which classes and methods would have to change to save and restore a session?” The resulting prediction keeps file handling in the manager, command routing in the controller, and the new menu labels in the view.
