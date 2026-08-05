# Agent instructions

## Gitea issues

When a task refers to `issue#<number>` or `issue #<number>`, read the issue with:

```bash
curl -fsS \
  "https://git.r35157.com/api/v1/repos/r35157/com_r35157_nenjim-hubd-impl_ref/issues/<number>"
```

Treat the issue title and body as the authoritative task description.
Gitea access is read-only unless the user explicitly requests otherwise.

## ValueTags

Text enclosed by `Ω`, such as `ΩRaydiumLiquidityΩ`, is a ValueTag.

ValueTags provide semantic type safety without requiring a separate Java
record or wrapper class for every domain value.

Values with the same Java backing type may represent different domain
concepts and must not necessarily be interchangeable. ValueTags help prevent
accidentally swapped arguments and express knowledge about what a value means,
for example whether a price includes tax.

Choose ValueTags according to their domain meaning and the contract of the
method, not merely according to their generated Java backing type.

The ValueTag hierarchy in `conf/detag.conf` determines its Java backing type.
For example, `ΩRaydiumLiquidityΩ` is generated as `BigInteger`.

Imports required by the generated Java type must remain in the `.tjava`
source, even when the backing type name does not appear directly in that file.

Edit files under `src/main/tjava`.
Do not edit generated files under `build/generated/sources/detag`.

## Detag configuration

Detagging is the process of preprocessing `.tjava` files into `.java` files
by resolving ValueTags to their Java backing types.

The Detag configuration is available at:
`conf/detag.conf`

This file is a symbolic link to the shared configuration outside the repository.

Detag processing is integrated into the normal Gradle build and runs
automatically when the project is built.

Read `conf/detag.conf` before working with `.tjava` files or ValueTags.

Do not run Detag manually unless explicitly required.
Do not modify `conf/detag.conf` unless the user explicitly requests it.

When writing new `.tjava` code, prefer an existing ValueTag from
`conf/detag.conf` when it accurately describes the value.

Use the most specific appropriate ValueTag. Do not use a semantically
incorrect ValueTag merely because it has the desired Java backing type.

When no suitable ValueTag exists, propose a new ValueTag with:

- its name
- its parent in the ValueTag hierarchy
- its Java backing type
- the reason it is needed

Do not add or change ValueTags in `conf/detag.conf` without explicit user
approval.