CC = gcc
CFLAGS = -Wall -Wextra -std=c99 -pedantic
TARGET = collatz_multiprocessing
SOURCE = collatz_multiprocessing.c

all: $(TARGET)

$(TARGET): $(SOURCE)
	$(CC) $(CFLAGS) -o $(TARGET) $(SOURCE)

clean:
	rm -f $(TARGET)

test: $(TARGET)
	@echo "Testing with various inputs:"
	@echo "================================"
	@echo "Test 1: ./$(TARGET) 8"
	@./$(TARGET) 8
	@echo ""
	@echo "Test 2: ./$(TARGET) 35"
	@./$(TARGET) 35
	@echo ""
	@echo "Test 3: ./$(TARGET) 1"
	@./$(TARGET) 1
	@echo ""
	@echo "Test 4: ./$(TARGET) 27"
	@./$(TARGET) 27

.PHONY: all clean test
