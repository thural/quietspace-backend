# Rule 02 — Module Internal Structure Rules

> Extracted from: `modular-architecture-overview.md` §7–8, `modular-architecture-review.md` §2, `system-architecture-overview.md` §3/§6, `domain-module-contracts.md`, `adding-domain-module.md`.

## 1. Package layout (flat root + subpackages)

Domains do **not** use a layered `service/repository/model` tree. Standard layout:

```
dev.thural.quietspace.domain.<xxx>/
├── <Agg>.java, <Agg>Service.java, <Agg>ServiceImpl.java, <Agg>Repository.java,
│   <Agg>Mapper.java, ...                                   # flat root
├── api/                # public query contracts: *QueryPort.java + dto/*.java (records)
├── adapter/            # consumer-port implementations (*Adapter.java, @Component)
├── port/               # consumer-owned ports owned by THIS module (if any)
├── controller/         # REST controllers (thin, delegate to service)
├── dto/                # request/response DTOs (mutable Lombok, extend BaseResponse)
├── health/             # *HealthIndicator
└── (module-specific)   # e.g. auth/ in user, security/ in user, specifications in post
```

- `api/` = the only cross-module-readable surface (ports + immutable record DTOs).
- `adapter/` = implementations of **other** modules' ports (provider side).
- `port/` = interfaces **owned** by this module for others to implement (consumer side).

## 2. Aggregate & entity rules

- One aggregate owner per domain module (see contracts doc for the registry).
- Rich domain model: factory method (`Xxx.create(...)`) + behavior methods; **no public setters** for business state.
- Entities extend `BaseEntity` (`core-shared`) via Lombok `@SuperBuilder`. Use `BaseRepository` (`core-data`) as the repository base.
- Response DTOs (`*Response`) are mutable Lombok `@SuperBuilder` hierarchies extending `BaseResponse`, built by hand-written `@Component` mappers. **Records are only for new `api.dto` summaries.** Do not migrate the response hierarchy to records.

## 3. Repository rules

- Repositories stay `public` **within the module** (so `adapter/`, `service/`, `api/`, `health/` subpackages can use them).
- Cross-module repository imports are **forbidden** (ArchUnit `has_no_cross_module_repository_dependency`).
- Same-module access only; every cross-domain read goes through a query port.

## 4. Layering rules

- **Controller → Service → Repository.** Controllers never inject repositories (ArchUnit-enforced).
- Controllers are thin: map request → call service → map to response. Business logic lives in services/domain methods.
- Services are `*ServiceImpl` implementing a same-module `*Service` interface; transactional boundaries (`@Transactional`) at the service layer.
- Mappers resolve cross-domain display data via query ports (e.g. `PostMapper` → `UserQueryPort`, `CommentMapper` → `ReactionQueryPort`), never via foreign repositories or lazy entity navigation.

## 5. JPA decoupling rules (no new cross-aggregate associations)

- **New code must not add `@ManyToOne`/`@OneToMany`/`@ManyToMany` associations to another aggregate's entity.** Use a plain `UUID` FK column + query port instead.
- Established migrations (follow as precedent):
  - `Comment.user` → plain `userId`; `Message.sender/recipient` → `senderId`/`recipientId`.
  - `Post.comments` collection **removed**; replaced by `CommentQueryPort.countCommentsByPostId`, subquery specs, `CommentCommandPort.deleteAllByPostId` explicit cascade.
  - `Post.user` → **transitional dual mapping**: association retained as single writer for feed-privacy SQL joins; read-only `authorId` FK view used by all new code (`PostNotificationAdapter` uses `post.getAuthorId()`).
- Ports return detached snapshots and **cannot** populate associations — remaining joins exist only where SQL pagination/visibility requires them (ADR 005).
- Feed-visibility filtering uses **local denormalized tables** (`post_author_visibility`, `viewer_author_access` maintained by `PostVisibilityProjector`), not cross-module joins. (ADR 005)

## 6. Health & observability per module

- Each domain module exposes a `*HealthIndicator` in `health/`.
- Metrics via Micrometer/Prometheus, tracing via Brave + Zipkin — provided by the domain-conventions plugin; do not redeclare versions.
