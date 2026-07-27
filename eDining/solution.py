import threading
import time
import random
from dataclasses import dataclass, field

# ===============================
# I. Problem Definition constants
# ===============================
N_PHIL = 5  # Five philosophers at a circular table

# ======================
# II. Simulation Setup
# ======================
# Timings (seconds)
THINK_RANGE = (0.3, 1.0)
EAT_RANGE   = (0.3, 0.9)

# Timeout for acquiring the second chopstick before releasing and retrying
SECOND_CHOPSTICK_TIMEOUT = 0.8

# Backoff after a timeout before retrying (jitter helps avoid livelock)
BACKOFF_RANGE = (0.05, 0.2)

# Evaluation thresholds
DEADLOCK_STALL_SECONDS   = 3.0  # if no one eats for this long while hungry -> deadlock suspected
STARVATION_WAIT_SECONDS  = 2.5  # if any single hungry period exceeds this -> starvation risk

LEFT  = lambda i: i
RIGHT = lambda i: (i + 1) % N_PHIL

@dataclass
class Stats:
    eats: list = field(default_factory=lambda: [0]*N_PHIL)
    max_wait: list = field(default_factory=lambda: [0.0]*N_PHIL)
    starvation_flags: list = field(default_factory=lambda: [False]*N_PHIL)
    time_last_progress: float = field(default_factory=time.monotonic)
    deadlock_detected: bool = False

class DiningTable:
    """
    Implements:
      - Threads: one per philosopher
      - Resources: chopsticks as mutexes (threading.Lock)
      - States: thinking, hungry, eating
      - Synchronization: locks + asymmetric pick order + timeout release
      - Deadlock & starvation detection
    """
    def __init__(self):
        # One mutex per chopstick; initially available (unlocked)
        self.chopsticks = [threading.Lock() for _ in range(N_PHIL)]

        # Shared state
        self.state_lock = threading.Lock()
        self.states = ["thinking"] * N_PHIL  # ["thinking" | "hungry" | "eating"]
        self.hungry_since = [None] * N_PHIL  # timestamps when a philosopher becomes hungry

        self.stats = Stats()

        # For clean shutdown
        self.stop_event = threading.Event()

    # --------------- Helpers for logging & state ----------------
    def _ts(self):
        return time.strftime("%H:%M:%S")

    def set_state(self, i, new_state):
        with self.state_lock:
            self.states[i] = new_state

    def mark_hungry(self, i):
        with self.state_lock:
            self.states[i] = "hungry"
            self.hungry_since[i] = time.monotonic()
            print(f"[{self._ts()}] Philosopher {i} is HUNGRY.")

    def mark_eating(self, i):
        with self.state_lock:
            self.states[i] = "eating"
            # compute wait time for evaluation
            if self.hungry_since[i] is not None:
                waited = time.monotonic() - self.hungry_since[i]
                self.stats.max_wait[i] = max(self.stats.max_wait[i], waited)
                if waited >= STARVATION_WAIT_SECONDS:
                    self.stats.starvation_flags[i] = True
            self.stats.eats[i] += 1
            self.stats.time_last_progress = time.monotonic()
            print(f"[{self._ts()}] Philosopher {i} starts EATING.")

    def mark_thinking(self, i):
        with self.state_lock:
            self.states[i] = "thinking"
            self.hungry_since[i] = None
            print(f"[{self._ts()}] Philosopher {i} finishes EATING → THINKING.")

    # ---------- III. Implementing the Simulation ----------
    # Initialization: threads are created in run()

    def philosopher(self, i: int, rounds: int):
        """
        Philosopher logic, exactly as requested:
          - THINKING
          - After random time → HUNGRY
          - Tries to acquire both adjacent chopsticks (mutexes)
              * If both available → EATING
              * If one/both unavailable → waits (with timeout), releases if timeout, retries
          - Eats for random time
          - Releases both chopsticks
          - Returns to THINKING
        Deadlock Prevention:
          - Asymmetric approach: even i picks LEFT first, odd i picks RIGHT first
          - Timeout: releases held chopstick if second cannot be acquired in time
        """
        # Asymmetric pick order
        first, second = (LEFT(i), RIGHT(i)) if i % 2 == 0 else (RIGHT(i), LEFT(i))

        for r in range(rounds):
            # THINK
            think_for = random.uniform(*THINK_RANGE)
            print(f"[{self._ts()}] Philosopher {i} is THINKING for {think_for:.2f}s (round {r+1}/{rounds}).")
            time.sleep(think_for)

            # HUNGRY
            self.mark_hungry(i)

            while not self.stop_event.is_set():
                # Pick up FIRST chopstick (blocking wait satisfies "must wait" if only one is held)
                self.chopsticks[first].acquire()
                # Try to pick SECOND with timeout; if fails, put down first and back off
                got_second = self.chopsticks[second].acquire(timeout=SECOND_CHOPSTICK_TIMEOUT)

                if got_second:
                    # Both acquired → EATING
                    self.mark_eating(i)
                    eat_for = random.uniform(*EAT_RANGE)
                    time.sleep(eat_for)

                    # Release both and go THINKING
                    self.chopsticks[second].release()
                    self.chopsticks[first].release()
                    self.mark_thinking(i)
                    break  # proceed to next round
                else:
                    # Could not get second within timeout → release first and retry later
                    self.chopsticks[first].release()
                    backoff = random.uniform(*BACKOFF_RANGE)
                    print(f"[{self._ts()}] Philosopher {i} timed out waiting for second chopstick; "
                          f"releasing and backing off {backoff:.2f}s.")
                    time.sleep(backoff)

        print(f"[{self._ts()}] Philosopher {i} is DONE.")

    # ---------------- IV. Evaluating the Simulation ----------------
    def watchdog(self):
        """
        Deadlock Detection:
          - If nobody has eaten for DEADLOCK_STALL_SECONDS while at least one philosopher is hungry
            and none are eating, flag deadlock.
        Starvation Detection:
          - Max per-episode hungry wait time is tracked; flag if exceeded STARVATION_WAIT_SECONDS.
        """
        while not self.stop_event.is_set():
            time.sleep(0.2)
            now = time.monotonic()
            with self.state_lock:
                any_hungry = any(s == "hungry" for s in self.states)
                any_eating = any(s == "eating" for s in self.states)
                stalled = (now - self.stats.time_last_progress) >= DEADLOCK_STALL_SECONDS

                if any_hungry and not any_eating and stalled and not self.stats.deadlock_detected:
                    self.stats.deadlock_detected = True
                    print(f"[{self._ts()}] ⚠️ DEADLOCK SUSPECTED: "
                          f"no progress for {DEADLOCK_STALL_SECONDS:.1f}s while hungry philosophers exist.")

                # Ongoing starvation risk notifications (optional, verbose)
                for i in range(N_PHIL):
                    if self.states[i] == "hungry" and self.hungry_since[i] is not None:
                        waited = now - self.hungry_since[i]
                        if waited >= STARVATION_WAIT_SECONDS and not self.stats.starvation_flags[i]:
                            self.stats.starvation_flags[i] = True
                            print(f"[{self._ts()}] ⚠️ STARVATION RISK: Philosopher {i} has waited "
                                  f"{waited:.2f}s without eating.")

    def run(self, rounds_per_philosopher: int = 5):
        # Create threads for philosophers (Initialization)
        philos = [
            threading.Thread(target=self.philosopher, args=(i, rounds_per_philosopher), daemon=True)
            for i in range(N_PHIL)
        ]
        guard = threading.Thread(target=self.watchdog, daemon=True)

        # Start
        guard.start()
        for t in philos:
            t.start()
        for t in philos:
            t.join()

        # Stop watchdog and print evaluation
        self.stop_event.set()
        time.sleep(0.3)  # let watchdog exit

        self.print_summary()

    def print_summary(self):
        print("\n========== Simulation Summary ==========")
        print(f"Eats per philosopher: {self.stats.eats}")
        print(f"Max hungry wait (s): {[round(x, 3) for x in self.stats.max_wait]}")
        if any(self.stats.starvation_flags):
            risky = [i for i, f in enumerate(self.stats.starvation_flags) if f]
            print(f"Starvation risk flagged for philosopher(s): {risky}")
        else:
            print("No starvation risk flagged (by configured threshold).")
        print(f"Deadlock detected: {self.stats.deadlock_detected}")
        print("========================================")

# ----------------- Entry point -----------------
if __name__ == "__main__":
    random.seed()  # nondeterministic runs
    table = DiningTable()
    # You can tweak rounds to run longer/shorter
    table.run(rounds_per_philosopher=5)
