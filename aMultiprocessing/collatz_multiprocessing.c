#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/types.h>

// Function to generate and print Collatz sequence
void generate_collatz_sequence(int n) {
    printf("Child process (PID: %d) generating Collatz sequence for %d:\n", getpid(), n);
    printf("%d", n);
    
    while (n != 1) {
        if (n % 2 == 0) {
            // n is even
            n = n / 2;
        } else {
            // n is odd
            n = 3 * n + 1;
        }
        printf(", %d", n);
    }
    printf("\n");
}

// Function to check if string represents a positive integer
int is_positive_integer(const char *str) {
    if (str == NULL || *str == '\0') {
        return 0;
    }
    
    // Check if first character is '0' (not positive)
    if (*str == '0') {
        return 0;
    }
    
    // Check if all characters are digits
    while (*str != '\0') {
        if (*str < '0' || *str > '9') {
            return 0;
        }
        str++;
    }
    
    return 1;
}

int main(int argc, char *argv[]) {
    pid_t pid;
    int status;
    
    // Check command line arguments
    if (argc != 2) {
        fprintf(stderr, "Usage: %s <positive_integer>\n", argv[0]);
        fprintf(stderr, "Example: %s 35\n", argv[0]);
        exit(EXIT_FAILURE);
    }
    
    // Validate input is a positive integer
    if (!is_positive_integer(argv[1])) {
        fprintf(stderr, "Error: '%s' is not a valid positive integer.\n", argv[1]);
        fprintf(stderr, "Please provide a positive integer greater than 0.\n");
        exit(EXIT_FAILURE);
    }
    
    // Convert string to integer
    int start_number = atoi(argv[1]);
    
    printf("Parent process (PID: %d) starting...\n", getpid());
    printf("Starting number: %d\n", start_number);
    
    // Create child process using fork()
    pid = fork();
    
    if (pid < 0) {
        // Fork failed
        perror("Fork failed");
        exit(EXIT_FAILURE);
    } else if (pid == 0) {
        // Child process
        printf("Child process (PID: %d) created successfully.\n", getpid());
        generate_collatz_sequence(start_number);
        printf("Child process (PID: %d) completed.\n", getpid());
        exit(EXIT_SUCCESS);
    } else {
        // Parent process
        printf("Parent process (PID: %d) waiting for child process (PID: %d) to complete...\n", 
               getpid(), pid);
        
        // Wait for child process to complete
        if (wait(&status) == -1) {
            perror("Wait failed");
            exit(EXIT_FAILURE);
        }
        
        // Check child process exit status
        if (WIFEXITED(status)) {
            printf("Child process (PID: %d) exited with status %d.\n", 
                   pid, WEXITSTATUS(status));
        } else if (WIFSIGNALED(status)) {
            printf("Child process (PID: %d) was terminated by signal %d.\n", 
                   pid, WTERMSIG(status));
        }
        
        printf("Parent process (PID: %d) completed.\n", getpid());
    }
    
    return EXIT_SUCCESS;
}
