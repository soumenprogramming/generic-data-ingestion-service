# Generic Data Ingestion Service

Config-driven service that pulls data from arbitrary HTTP APIs and persists it — without being written for any one source. Built as the IntentWise take-home assignment.

## Quick start

### Option A — Docker Compose (recommended for reviewers)

```bash
docker compose up --build
```

Service listens on [http://localhost:8080](http://localhost:8080).

### Option B — Local (H2, no Docker)

Requires JDK 17+.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Trigger ingestion

```bash
# List configured sources
curl -s http://localhost:8080/api/v1/sources | jq

# Ingest both structurally different demos
curl -s -X POST http://localhost:8080/api/v1/ingest \
  -H 'Content-Type: application/json' \
  -d '{"sourceIds":["nasa-apod","rickandmorty-characters"]}' | jq

# Or one at a time
curl -s -X POST http://localhost:8080/api/v1/ingest/nasa-apod | jq
curl -s -X POST http://localhost:8080/api/v1/ingest/rickandmorty-characters | jq

# Inspect jobs and stored records
curl -s http://localhost:8080/api/v1/jobs | jq
curl -s 'http://localhost:8080/api/v1/records?collection=nasa_apod&limit=5' | jq
curl -s 'http://localhost:8080/api/v1/records?collection=characters&limit=5' | jq
```

## Public APIs used

| Source id | API | Auth | Pagination | Response shape |
|-----------|-----|------|------------|----------------|
| `nasa-apod` | [NASA APOD](https://api.nasa.gov/) | API key query param (`DEMO_KEY`) | none | JSON **array** at root |
| `rickandmorty-characters` | [Rick and Morty](https://rickandmortyapi.com/) | none | `next_url` via `$.info.next` | nested `$.results` |
| `jsonplaceholder-posts` | [JSONPlaceholder](https://jsonplaceholder.typicode.com/) | none | none | JSON array (extra sanity source) |

These two primary sources differ in authentication **and** pagination/response layout, which is the point of the exercise.

## Architecture

```
HTTP trigger  →  IngestionPipeline  →  AuthHandler + PaginationHandler
                                   →  HttpDataFetcher (retries/timeouts)
                                   →  JsonRecordExtractor (JsonPath)
                                   →  DataSink (DatabaseSink today; S3-shaped stub ready)
                                   →  Postgres / H2
```

**Design idea:** sources are data, not code. Adding a new API means adding a YAML block under `ingestion.sources`. Auth and pagination are strategy registries — new styles are new handler beans, not pipeline rewrites. Persistence goes through a `DataSink` interface so a future S3/object-storage destination plugs in without touching fetch/extract logic.

### Key packages

| Package | Role |
|---------|------|
| `config` | Declarative `IngestionProperties` bound from YAML |
| `auth` | `none`, `api_key`, `bearer`, `basic` |
| `pagination` | `none`, `page`, `offset`, `next_url` |
| `fetch` | Java 11+ `HttpClient` with timeouts + retry on 429/5xx |
| `extract` | JsonPath record extraction + external id resolution |
| `sink` | `DatabaseSink` + unimplemented `ObjectStorageSink` sketch |
| `pipeline` | Orchestrates one job end-to-end |
| `api` | REST surface to trigger and observe |

### API surface

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/v1/health` | Liveness + configured source ids |
| GET | `/api/v1/sources` | Source catalog |
| POST | `/api/v1/ingest/{sourceId}` | Run one source |
| POST | `/api/v1/ingest` | Run several (`{"sourceIds":[...]}`) |
| GET | `/api/v1/jobs` / `/api/v1/jobs/{id}` | Job status |
| GET | `/api/v1/records?collection=&sourceId=&limit=` | Inspect persisted payloads |

## Tradeoffs and assumptions

- **Config over auto-discovery.** “Work out how to pull data” is interpreted as a declarative connector model (auth, pagination, JsonPath), not LLM/schema scraping. That matches how production ingestion platforms usually grow in two days.
- **Sync HTTP jobs.** Triggers run inline and return the finished job. Fine for a demo; production would enqueue async workers and stream progress.
- **Schema-flexible storage.** Records are stored as JSON text with `sourceId` / `collection` / `externalId`. Avoids per-source tables; queryability of fields is weaker than typed columns.
- **Replace-by-default.** Each successful run can wipe the prior rows for that source/collection (`replace-existing: true`) so demos stay deterministic. Real pipelines usually upsert/CDC.
- **Bounded pagination.** `max-pages` caps crawl depth so a misconfigured source cannot loop forever.
- **NASA `DEMO_KEY`.** Suitable for light demo traffic; replace with a real key via `NASA_API_KEY` for heavier use.

## What I would do with more time

- Async job queue (e.g. Spring + DB/Redis) with progress events
- Upsert / incremental sync using cursors or `updated_at`
- Rate-limit tokens per source and respect `Retry-After`
- Wire a real S3 sink (JSONL / Parquet) behind `DataSink`
- OpenAPI spec + auth on the control plane
- Metrics/tracing (Micrometer, job duration, bytes, error rates)
- Contract tests against recorded WireMock fixtures for CI without live APIs

## How I used AI tools

I used Cursor to scaffold the Spring project layout, draft the strategy registries, and iterate on YAML source definitions and README wording. I reviewed and edited every generated piece — especially pagination edge cases and transaction boundaries (HTTP I/O must not sit inside a DB transaction).

**One place AI got it wrong:** an early draft put `@Transactional` around the entire `IngestionPipeline.execute` method (fetch + parse + persist). That would hold a DB connection open across slow external HTTP calls and retries. I caught it while thinking through failure modes, removed the pipeline-level transaction, and kept `@Transactional` only on `DatabaseSink.persist`.

## Tests

```bash
./gradlew test
```

Unit tests cover JsonPath extraction and pagination handlers; a Spring context test checks the health/sources endpoints with the `local` (H2) profile.
