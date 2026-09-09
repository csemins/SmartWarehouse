# Smart Warehouse Management System

JavaFX desktop application for the Advanced Programming lab (KUET).
Products, stock and users live in SQLite; the **Factory IO "Automated Warehouse"** scene is driven over **Modbus TCP**.

## Run

```bat
mvnw.cmd javafx:run
```

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
