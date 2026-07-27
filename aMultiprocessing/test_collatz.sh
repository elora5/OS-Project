#!/bin/bash

echo "=========================================="
echo "Collatz Conjecture Multiprocessing Tests"
echo "=========================================="
echo

# Test 1: Basic functionality with small number
echo "Test 1: Basic functionality (input: 8)"
echo "Expected: 8, 4, 2, 1"
echo "Output:"
./collatz_multiprocessing 8
echo

# Test 2: Larger number (from problem statement)
echo "Test 2: Larger number (input: 35)"
echo "Expected: 35, 106, 53, 160, 80, 40, 20, 10, 5, 16, 8, 4, 2, 1"
echo "Output:"
./collatz_multiprocessing 35
echo

# Test 3: Edge case - number 1
echo "Test 3: Edge case (input: 1)"
echo "Expected: 1"
echo "Output:"
./collatz_multiprocessing 1
echo

# Test 4: Error handling - no arguments
echo "Test 4: Error handling - no arguments"
echo "Expected: Usage error message"
echo "Output:"
./collatz_multiprocessing
echo

# Test 5: Error handling - invalid input (zero)
echo "Test 5: Error handling - invalid input (0)"
echo "Expected: Error message about invalid positive integer"
echo "Output:"
./collatz_multiprocessing 0
echo

# Test 6: Error handling - invalid input (negative)
echo "Test 6: Error handling - invalid input (-5)"
echo "Expected: Error message about invalid positive integer"
echo "Output:"
./collatz_multiprocessing -5
echo

# Test 7: Error handling - invalid input (non-numeric)
echo "Test 7: Error handling - invalid input (abc)"
echo "Expected: Error message about invalid positive integer"
echo "Output:"
./collatz_multiprocessing abc
echo

# Test 8: Another valid number
echo "Test 8: Another valid number (input: 12)"
echo "Expected: 12, 6, 3, 10, 5, 16, 8, 4, 2, 1"
echo "Output:"
./collatz_multiprocessing 12
echo

echo "=========================================="
echo "All tests completed!"
echo "=========================================="
