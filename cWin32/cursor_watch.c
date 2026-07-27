#define _WIN32_WINNT 0x0600
#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>

static volatile BOOL g_running = TRUE;

BOOL WINAPI CtrlHandler(DWORD ctrlType) {
    // Handle Ctrl+C / close / logoff gracefully
    switch (ctrlType) {
        case CTRL_C_EVENT:
        case CTRL_BREAK_EVENT:
        case CTRL_CLOSE_EVENT:
        case CTRL_LOGOFF_EVENT:
        case CTRL_SHUTDOWN_EVENT:
            g_running = FALSE;
            return TRUE;
        default:
            return FALSE;
    }
}

int main(int argc, char** argv) {
    // Update rate (milliseconds). Default: 50 ms
    DWORD interval_ms = 50;
    if (argc >= 2) {
        long v = strtol(argv[1], NULL, 10);
        if (v > 0 && v <= 10000) interval_ms = (DWORD)v;
    }

    // Make UTF-8 output nicer (optional)
    SetConsoleOutputCP(CP_UTF8);

    // IMPORTANT: Disable Quick Edit so the console never suspends the process
    // when the user clicks/selects text. This helps ensure we keep updating
    // even if the console loses focus or the user clicks inside it.
    HANDLE hIn = GetStdHandle(STD_INPUT_HANDLE);
    if (hIn != INVALID_HANDLE_VALUE) {
        DWORD mode = 0;
        if (GetConsoleMode(hIn, &mode)) {
            // Quick Edit & mouse input can suspend responsiveness; turn them off.
            mode &= ~(ENABLE_QUICK_EDIT_MODE | ENABLE_MOUSE_INPUT);
            SetConsoleMode(hIn, mode);
        }
    }

    // Handle Ctrl+C etc.
    SetConsoleCtrlHandler(CtrlHandler, TRUE);

    printf("Cursor Watch (Win32) — update every %lu ms\n", (unsigned long)interval_ms);
    printf("Press Ctrl+C to exit.\n");

    // Print once per interval, overwriting the same line.
    while (g_running) {
        POINT pt;
        if (GetCursorPos(&pt)) {
            // GetCursorPos returns screen (virtual desktop) pixel coordinates.
            // Works regardless of which app has focus.
            printf("\rX: %ld   Y: %ld   ", pt.x, pt.y);
            fflush(stdout);
        } else {
            // If it ever failed (rare), show the error code.
            DWORD err = GetLastError();
            printf("\rGetCursorPos failed (error %lu)     ", (unsigned long)err);
            fflush(stdout);
        }

        Sleep(interval_ms); // Throttle CPU use and set update rate
    }

    printf("\nExiting.\n");
    return 0;
}
