# Design Document Template

---
**Purpose**: A design that a human reviewer can read top to bottom in one sitting, and that an implementer can follow without interpretation drift.

**Rules**:
- Target 200 lines or fewer for the whole document. MUST NOT exceed 250 lines; if it would, simplify the design or split the spec.
- Sections up to "Related ADRs" are for human review. Sections after the separator are implementation references.
- Do not restate steering or ADRs. Technology choices live in `.kiro/steering/tech.md` and `docs/adr/`; reference them as `ADR-NNNN`.
- Keep the heading names below unchanged; write the body in the language set in `spec.json`.
- Prefer tables and Mermaid diagrams over prose. Omit an optional section entirely instead of writing "N/A".

---

## Overview
<!-- 2-3 sentences: what this delivers and to whom. -->

### Goals
- 

### Non-Goals
- 

## Boundary Commitments

### This Spec Owns
- 

### Out of Boundary
- 

### Allowed Dependencies
- 

### Revalidation Triggers
- 

## Architecture
<!-- One Mermaid diagram of components and dependency direction. Then state the dependency direction in one line (e.g. Web → Application → Domain ← Infrastructure) and the pattern with its ADR number. No technology stack table. -->

```mermaid
```

## Data Models
<!-- Tables, columns, types, keys and constraints in one table per entity. Add a Mermaid ER diagram only if relationships are non-trivial. -->

## API Contract
<!-- One table: Method | Path | Request | Success response | Error responses. Detail only what the table cannot carry (e.g. content types, size limits). -->

| Method | Path | Request | Success | Errors |
|--------|------|---------|---------|--------|

## System Flows
<!-- Optional. Sequence diagrams only for flows with branching, external processes or partial-failure handling. State the decisions (ordering, cleanup on failure) in bullets after the diagram. -->

## Error Handling
<!-- One table mapping each failure condition to its response and to the requirement it satisfies. -->

| Condition | Response | Requirements |
|-----------|----------|--------------|

## Requirements Traceability

| Requirement | Summary | Components | Tests |
|-------------|---------|------------|-------|

## Testing Strategy
<!-- Derive each item from acceptance criteria. Name what is verified and the requirement IDs; no generic items. Split into unit and integration. -->

## Related ADRs
<!-- Bullet list: ADR-NNNN — one-line reason it matters to this design. -->

---
<!-- Implementation references below. Reviewers may skim. -->

## Components and Interfaces
<!-- Summary table first. Full blocks only for components that introduce a boundary (ports, adapters, persistence, external processes): responsibility, dependencies (Inbound/Outbound/External with P0-P2), and the interface signature. No implementation code. -->

| Component | Layer | Intent | Requirements | Dependencies |
|-----------|-------|--------|--------------|--------------|

## File Structure Plan
<!-- Concrete paths with one responsibility each. Every component above must appear here. Describe repeated patterns once. -->

```
```
