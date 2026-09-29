# 5-minute demo: screen actions + voiceover

**Before recording:** open Factory IO → *Automated Warehouse* → press **Reset** and **Play**
(driver: Modbus TCP/IP Server). Delete `%USERPROFILE%\.smart-warehouse` to start from an empty database.
Open the project in IntelliJ / VS Code on a second screen.

| Time | Do this | Say this |
|---|---|---|
| 0:00 | GitHub → Commits | "This is the Smart Warehouse Management System. The commit history shows the work step by step." |
| 0:15 | IntelliJ: `model/User.java`, `Admin.java`, `BaseEntity.java`, `Validatable.java` | "Users are an abstract class. Admin, Inventory Manager and Finance Manager override getPermissions, which is polymorphism. Every entity extends the abstract BaseEntity and implements the Validatable interface." |
| 0:30 | `repository/AbstractRepository.java` | "The generic AbstractRepository implements create, read, update and delete once. Each of the eight tables only supplies its SQL and row mapping." |
| 0:40 | Start the app. Resize the login window, tick *Show password*, type a wrong password (card shakes), then `admin` / `admin123` | "The login scene is FXML with a StackPane and a PasswordField. The background photo scales with the window and the card width is bound to the window width. Passwords are salted PBKDF2 hashes in SQLite." |
| 1:00 | Dashboard: numbers count up, cards slide in; click through the sidebar | "The dashboard runs seven queries in parallel with CompletableFuture on a thread pool. Pages change with animated transitions and the indicator slides along the sidebar." |
| 1:15 | Settings → Factory IO connection shows RUNNING | "The Factory IO connection is saved to settings.json. At start-up the app connects over Modbus TCP and switches to automatic mode." |
| 1:25 | Products → *New product* → barcode `619659112318` → **Look up** (name, category and photo appear) → cost 40, price 60, units per box 24, reorder 30 → **Save** | "Barcode lookup is an HTTP request to the UPCitemdb product database, with Open Food Facts as a fallback for food. The JSON reply, including the photo link, is parsed with Gson into a record. Saving is the Create part of CRUD." |
| 1:50 | Same product: *Put into inventory* → 2 boxes, first box to Cell 10 → **Store boxes**; switch to Factory IO | "Right after adding a product I can put it into stock. Each box becomes a crane job. The software PLC scans every 100 milliseconds on a scheduled thread, reads sensors as discrete inputs and writes coils and the target cell register." |
| 2:25 | Back in the app: toast + bell badge; Warehouse page: cell 10 pops green, live I/O | "When the crane is done, the app books the stock movement and the purchase expense in one SQL transaction and shows a notification." |
| 2:40 | Orders → *New order* → "Rahim Traders", product, 1 box → **Save**; Factory IO ships the box | "Customer orders are shipped automatically: the crane takes the oldest box to the exit, and a sales income is booked." |
| 3:05 | Edit a product price → Save, then delete a test product | "That's update and delete. Products below their reorder level turn red." |
| 3:15 | Stock movements (IN/OUT radio buttons) → Export CSV | "The ledger shows every box with its cell and order." |
| 3:25 | Reports: change the period, show best sellers, fulfilment, valuation | "Reports aggregate the data in SQL: best sellers, order fulfilment, margins and the inventory valuation, exported as CSV or JSON." |
| 3:40 | Audit log: filter chips, search | "Every sign-in and change is written to the audit_log table, which references the users table." |
| 3:50 | Press **Ctrl+K**, type "cola", Enter; press **Ctrl+D** | "Ctrl+K opens a second stage for quick search. Ctrl+D switches to dark mode; the whole theme is one CSS file with colour variables." |
| 4:05 | Log out → `finance` / `finance123` → Finance → Transactions: add and delete an expense | "The finance manager only sees finance pages. Exchange rates come from open.er-api.com, parsed with Gson's tree model, refreshed every 30 minutes. Transactions have full CRUD." |
| 4:30 | IntelliJ: `schema.sql`, `util/AppExecutors.java`, `ui/FxAsync.java` | "Foreign keys link products, categories, rack cells, orders, movements, transactions and the audit log. Database and HTTP work always runs on the worker pool, never on the JavaFX thread." |
| 4:50 | Press the Emergency stop in Factory IO, then Reset | "The emergency stop halts everything immediately. Thank you." |
