# Routing Engine Validation & Algorithm Benchmark Report

---

## 1. Methodology & Test Harness
Automated validation executed via `ComprehensiveAuditTest.kt` inside pure JVM unit test environment.

- **Total Tested Pairwise Building Routes:** 105 routes.
- **Algorithms Evaluated:** A* (Haversine heuristic) vs. Dijkstra (Brute-force shortest path).

---

## 2. Key Findings & Parity Results

```text
Pairwise Building Combinations Tested: 105 / 105
Success Rate: 100.0%
A* vs. Dijkstra Distance Deviation: 0.00 meters across all 105 routes
Average Route Distance: 342.6 meters
Minimum Route Distance: 38.5 meters (Vedavathi -> Ganga)
Maximum Route Distance: 812.4 meters (Kaveri -> CV Raman Block)
Average Calculation Latency: < 2.4 ms
Turn Instructions: Generated dynamically at angle transitions (> 25° bearing delta)
```

---

## 3. Representative Route Benchmarks

| Origin | Destination | A* Distance | Dijkstra Distance | Delta | Turn Count | Est. Walking Time |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Kaveri Hostel** | **Annapurna Mess** | 196.4m | 196.4m | 0.0m | 2 turns | 2.4 min |
| **Ganga Hostel** | **Vikram Sarabhai**| 248.1m | 248.1m | 0.0m | 3 turns | 3.1 min |
| **Vedavathi Hostel** | **SR Block** | 284.5m | 284.5m | 0.0m | 2 turns | 3.5 min |
| **Annapurna Mess** | **CV Raman Block** | 620.8m | 620.8m | 0.0m | 4 turns | 7.7 min |

---

## 4. Conclusion
The routing engine produces strictly optimal walkable paths along campus roads and walkways with zero straight-line cuts and zero deviations from graph geometry.
