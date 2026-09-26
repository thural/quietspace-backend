# ADR 005: Feed-Visibility Read Model (Event-Driven)

## Status
Accepted

## Context
`PostSpecifications.visibleToUser()` filtered feeds by joining `post → user →
profile_settings` in SQL. That join is the last cross-module SQL dependency blocking
JPA decoupling, but moving the filter to application code breaks pagination density
(filtered-out rows leave sparse pages) and risks leaking private-account posts.

## Decision
Denormalize visibility into `domain-post`-local tables, maintained by a projector
(`PostVisibilityProjector`) from user lifecycle events:

- `post_author_visibility(author_id PK, is_private)` ← `UserPrivacyChangedEvent`,
  `UserRegisteredEvent` (default public).
- `viewer_author_access(viewer_id, author_id)` ← `UserFollowedEvent` /
  `UserUnfollowedEvent`.
- `visibleToUser()` = `author NOT IN (private authors)` OR `author = viewer`
  OR `EXISTS (access edge)` — all inside SQL, pagination intact.
- All projector handlers are idempotent via `processed_events` (at-least-once safe).
- Schema + backfill ship as Flyway `V6__post-visibility-readmodel.sql` (privacy from
  `profile_settings`, edges from `user_followings`).

## Consequences / Accepted Trade-off
**Eventual consistency window on a security boundary, by product sign-off.**
A privacy flip or (un)follow takes effect after outbox propagation (typically
sub-second). A post may remain visible to a non-follower for that window. This is
the industry-standard trade-off for paginated feeds at scale (live joins against
the author aggregate do not survive high read load) and is documented here as a
deliberate decision, not a flaw. Authors without a visibility row default to public.
