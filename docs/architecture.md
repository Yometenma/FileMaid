# FileMaid architecture

FileMaid is a modular monolith. Modules have one-way dependencies:

```text
server -> infrastructure -> application -> core
   |                              ^
   +------------------------------+

legacy-engine (isolated compatibility module)
```

## Modules

- `core`: framework-free domain types and invariants.
- `application`: use cases and ports. It coordinates work without knowing HTTP, databases, or operating-system details.
- `infrastructure`: adapters for local filesystems, SQLite, metadata providers, and media tools.
- `server`: Spring Boot composition root and HTTP API.
- `legacy-engine`: the existing FileMaid engine. New modules must access it through explicit adapters rather than importing UI or CLI classes directly.

## Safety boundary

Clients identify files with a configured storage-root ID and a relative path. Absolute paths are not accepted by the application API. Paths are normalized and must remain below the configured root before filesystem access occurs.

Mutating operations will use a two-step protocol: create an immutable preview plan, then execute that exact plan after revalidation and explicit confirmation. The initial HTTP API is read-only.
