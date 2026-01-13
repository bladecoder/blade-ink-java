# Conformance Tests

This subproject contains integration tests that verify the correct behavior of both the **compiler** and **runtime** components working together.

## Purpose

These tests:
- Compile `.ink` source files using the `Compiler`
- Execute the compiled stories using the `Runtime` (Story class)
- Verify the output matches expected behavior

## Test Coverage

The tests cover various Ink language features including:
- Basic text output
- Choices and branching
- Conditionals and sequences
- Diverts and knots
- Functions (internal and external)
- Gathers and stitches
- Glue
- Lists
- Tags
- Threads and multi-flow
- Tunnels
- Variables
- And more...

## Running Tests

```bash
# Run all conformance tests
./gradlew :conformance-tests:test

# Run tests for the entire project
./gradlew test
```

## Test Structure

- `src/test/java/` - JUnit test classes
- `src/test/resources/inkfiles/` - `.ink` source files and their expected JSON output
