Filtering tests
===============

Weaver supports two types of test filtering: pattern-based filtering and tag-based filtering. Both can be used independently or combined together.

## Pattern-based filtering

When using the IOSuite variants, you can filter tests by their qualified name using the `-o` or `--only` flag:

```
> testOnly -- -o *foo*
> testOnly -- --only *foo*
```

This filter will prevent the execution of any test that doesn't contain the string "foo" in its qualified name. For a test labeled "foo" in a "FooSuite" object, in the package "fooPackage", the qualified name of a test is:

```
fooPackage.FooSuite.foo
```

You can also filter by line number:

```
> testOnly -- -o FooSuite.line://42
```

This will only run the test at line 42 in FooSuite.

## Tag-based filtering

Weaver supports powerful tag-based filtering using GitHub-style tag expressions with the `-t` or `--tags` flag:

```
> testOnly -- -t bug-123
> testOnly -- --tags "bug-* !bug-wontfix"
```

### Tag expression syntax

Tag expressions use GitHub's label filtering syntax:

- **`,` (comma)** - OR operator: matches tests with any of the specified tags
- **` ` (space)** - AND operator: matches tests with all of the specified tags
- **`!` (exclamation)** - NOT operator: excludes tests with the specified tag
- **`()` (parentheses)** - grouping to control precedence

### Examples

**Match tests with either tag:**
```
> testOnly -- -t "bug,feature"
```
Matches tests tagged with `bug` OR `feature`.

**Match tests with both tags:**
```
> testOnly -- -t "bug critical"
```
Matches tests tagged with both `bug` AND `critical`.

**Exclude specific tags:**
```
> testOnly -- -t "!slow"
```
Matches all tests EXCEPT those tagged `slow`.

**Complex expressions:**
```
> testOnly -- -t "bug,feature !wontfix"
```
Matches tests tagged with (`bug` OR `feature`) AND NOT `wontfix`.

**Grouping with parentheses:**
```
> testOnly -- -t "(bug,feature) critical"
```
Matches tests tagged with (`bug` OR `feature`) AND `critical`.

### Wildcard patterns

Tag expressions support wildcards for flexible matching:

- **`*` (asterisk)** - matches zero or more characters
- **`?` (question mark)** - matches exactly one character

**Examples:**

**Prefix matching:**
```
> testOnly -- -t "bug-*"
```
Matches tags like `bug-123`, `bug-critical`, `bug-feature-x`, etc.

**Suffix matching:**
```
> testOnly -- -t "*-prod"
```
Matches tags like `env-prod`, `db-prod`, etc.

**Single character:**
```
> testOnly -- -t "test-?"
```
Matches tags like `test-1`, `test-a`, but not `test-12`.

**Infix matching:**
```
> testOnly -- -t "first*bug"
```
Matches tags like `firstbug`, `first-critical-bug`, etc.

**Complex wildcards:**
```
> testOnly -- -t "bug-*,feature-* !bug-wontfix"
```
Matches all bug or feature tags except `bug-wontfix`.

### Precedence rules

Operators have the following precedence (highest to lowest):

1. `!` (NOT)
2. ` ` (AND - space)
3. `,` (OR - comma)

Use parentheses to override the default precedence:

```
> testOnly -- -t "tag1,tag2 tag3"      # (tag1) OR (tag2 AND tag3)
> testOnly -- -t "(tag1,tag2) tag3"    # (tag1 OR tag2) AND (tag3)
```

## Combining filters

You can combine pattern-based (`-o`/`--only`) and tag-based (`-t`/`--tags`) filtering in the same command. When both are specified, **BOTH filters must match** for a test to run (AND logic):

```
> testOnly -- -t "bug-*" -o "*integration*"
```

This will only run tests that:
- Are tagged with a bug tag (like `bug-123`, `bug-critical`), **AND**
- Have "integration" in their qualified name

**More examples:**

```
> testOnly -- -t "slow" -o "MySuite.*"
```
Runs only slow tests from MySuite.

```
> testOnly -- -t "!flaky" -o "*database*"
```
Runs all database-related tests that are NOT tagged as flaky.

```
> testOnly -- -t "(bug,feature) !wontfix" -o "FooSuite.line://42"
```
Runs the test at line 42 only if it's tagged as bug or feature, but not wontfix.
