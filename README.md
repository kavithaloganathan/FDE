# Docs Q&A Assistant

Ask plain-English questions about the Solstice docs. Answers are extracted from the docs, and the app says "I don't have an answer" when the docs don't cover a question. See [SOLUTION_DESIGN.md](SOLUTION_DESIGN.md) for the design.

## Layout

- `backend/` Spring Boot 3.5 API (Java 25, Maven)
- `frontend/` Angular 22 UI (Angular CLI 22)
- `starter_kit/` original data and example questions

## Run the backend

Requires JDK 25 and Maven 3.10+.

```
cd backend
mvn test
mvn spring-boot:run
```

The API listens on http://localhost:8080.

Example request:

```
curl -X POST http://localhost:8080/api/ask -H "Content-Type: application/json" -d "{\"question\":\"How do I reset my password?\"}"
```

## Run the frontend

Requires Node 24+.

```
cd frontend
npm install
npm start
```

Open http://localhost:4200. The Angular dev server proxies `/api` requests to `http://localhost:8080`, where the backend runs.

## Evaluate

```
cd backend
mvn test -Peval
```

Prints correct-doc rate, refusal rate, and wrong answers over `backend/src/test/resources/data/eval_questions.json`. Excluded from the normal `mvn test`.

## Feedback

`POST /api/feedback` appends each rating to `backend/data/feedback.jsonl`. Review these and add real questions to the eval set.

## Known limitations

- Lexical matching with a curated synonym list (`backend/src/main/resources/data/synonyms.json`). Unlisted synonyms are not matched.
- Thresholds were tuned by hand against the 5 example questions. Not validated on a larger set.
- Answers are extracted sentences, not generated prose.
- The docs are loaded from `backend/src/main/resources/data/docs.json` at startup. Changes require a restart.
- The backend and frontend were written without a local compile in this session. Run `mvn test` first and fix any issues.
