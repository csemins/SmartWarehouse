# Smart Warehouse Management System

JavaFX desktop application for the Advanced Programming lab (KUET).
Products, stock and users live in SQLite; the **Factory IO "Automated Warehouse"** scene is driven over **Modbus TCP**.

## Features

- **Dashboard**: products, units in stock, rack cells used, open orders, money, stock value per category, units in/out per day, restock list.
- **Products**: CRUD, barcode lookup (UPCitemdb, Open Food Facts as fallback for food), photo URL, *Put into inventory*.
- **Warehouse**: 54-cell rack, target cell (manual or first free), automatic inbound, ship oldest box, event log.
- **Orders**: open orders are shipped automatically (FIFO) and booked as income.
- **Stock movements**: every box stored or shipped, CSV export.
- **Finance**: income, expenses, profit, monthly chart, live exchange rates and transaction CRUD.
- **Audit log** (admin) and **Settings** (admin): Factory IO connection, scene wiring, manual crane control, rack correction.

## Automations

| Automation | How it works |
|---|---|
| Auto connect and start | On start-up the app connects to Factory IO and switches to automatic mode. The connection (IP, port, *Save and connect*, *Disconnect*, live status) is in **Settings**. |
| Put into inventory from Products | Products → save or select a product → *Put into inventory*: number of boxes and the cell for the first box → *Store boxes*. One crane job is queued per box. |
| Inbound (manual or automatic) | Warehouse → choose the product and a **target cell** (or *First free cell*; clicking an empty cell in the rack also selects it) → *Store box*, or tick *Store every arriving box automatically*. |
| Automatic order shipping | Orders → create an order. Whenever the crane is free, it ships the oldest box of that product (FIFO). The order becomes DONE when all its boxes have left, and missing boxes are shipped as soon as they are stored. |
| Automatic bookkeeping | Storing a box books a *Purchase* expense, and shipping books a *Sales* income linked to the stock movement (one SQL transaction). |
| Restock alerts | Products at or below their reorder level turn red, and the top bar and dashboard list them. |
| Audit | Every sign-in and change is written to the audit log. |
| Live exchange rates | Finance loads the rates from open.er-api.com when it is opened. |
| Safety | The Factory IO Emergency stop halts all motion immediately. Reset aborts the job and restores the rack cell. A lost connection reconnects and resumes on its own. |

## Run

```bat
mvnw.cmd javafx:run
```

Or run `build-exe.bat` to get `dist\SmartWarehouse\SmartWarehouse.exe` with a bundled Java runtime.

Data lives in `%USERPROFILE%\.smart-warehouse\`: `warehouse.db` (SQLite) and `settings.json`.

First start creates `admin / admin123`, `inventory / inventory123` and `finance / finance123`.

## Factory IO

Scene **Automated Warehouse** → *Drivers → Modbus TCP/IP Server*: Digital Inputs 15, Digital Outputs 10,
Register Outputs 1 (offsets 0), default tag order. Put the IP shown there into Settings (default `192.168.0.111`).

| Inputs | Coils |
|---|---|
| 0 At Entry, 1 At Load, 2 At Left, 3 At Middle, 4 At Right, 5 At Unload, 6 At Exit, 7 Moving X, 8 Moving Z, 9 Start, 10 Reset, 11 Stop, 12 Emergency stop, 13 Auto, 14 FACTORY I/O (Running) | 0 Entry Conveyor, 1 Load Conveyor, 2 Forks Left, 3 Forks Right, 4 Lift, 5 Unload Conveyor, 6 Exit Conveyor, 7 Start light, 8 Reset light, 9 Stop light; **holding register 0** = Target Position |

Notes from testing on the real scene: forks LEFT reach the load conveyor, forks RIGHT reach the rack and the
unload conveyor, conveyor sensors are TRUE when clear, cell 55 is the rest position.
