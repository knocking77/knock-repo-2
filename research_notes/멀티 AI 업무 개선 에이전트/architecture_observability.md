# Architecture & Observability for a Personal Work-Method Improvement Agent (zu / cbot / meti / kode + "AI 공통 채널" sheet)

Research date: 2026-11-21. Network note: in this research session `developers.google.com` and `docs.github.com` could not be fetched directly (host not allowed); facts about Apps Script and GitHub Actions below come from search-result snippets of the official pages plus third-party guides, and are flagged where unverified.

## 1. Which trace/log fields matter, and how to simplify them for a spreadsheet log

### Takeaway
Observability standards (OpenTelemetry GenAI, Langfuse, LangSmith) all come down to the same few things: a trace/conversation id that groups related messages, a named agent per step, start/end times, an outcome, and scores attached to the trace that can come from a person, an LLM judge or code. For a solo user the existing "AI 공통 채널" columns (id, time, sender, recipient, subject, body, reply-to id, status) already cover messages. What they lack is a **task id**, **per-status timestamps**, **rework/handoff markers** and a **quality score**. The cheapest fix is a second "tasks" tab, derived nightly by kode, plus one manual rating column.

### Cited Findings
- OpenTelemetry has GenAI "agent and framework spans" conventions that define `create_agent` and `invoke_agent` spans, in client (remote) and internal (in-process) variants. Their status is **Development**, so names may change — [OTel GenAI agent spans](https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-agent-spans/index.md)
- The GenAI registry lists `gen_ai.agent.id`, `gen_ai.agent.name`, `gen_ai.agent.description`, `gen_ai.agent.version` and `gen_ai.conversation.id`. The core-repo registry page now marks them as moved to a separate `semantic-conventions-genai` repository. A secondary source dates the move to v1.42.0 (June 2026); that date is unverified — [OTel gen-ai registry](https://opentelemetry.io/docs/specs/semconv/registry/attributes/gen-ai/index.md); [semantic-conventions-genai repo doc](https://raw.githubusercontent.com/open-telemetry/semantic-conventions-genai/main/docs/gen-ai/gen-ai-agent-spans.md); [Dash0 explainer](https://www.dash0.com/knowledge/opentelemetry-genai-semantic-conventions-explained)
- Vendors build on these attributes. Honeycomb's Agent Timeline groups spans into conversations and identifies agents from the GenAI attributes. Datadog ingests spans that follow the 1.37+ GenAI conventions — [Honeycomb agents](https://docs.honeycomb.io/send-data/agents); [Datadog OTel instrumentation](https://docs.datadoghq.com/llm_observability/instrumentation/otel_instrumentation/?tab=python)
- Langfuse stores every quality judgment as a "score" attached to traces or observations, whether it comes from a human annotation, an LLM judge, a programmatic check or end-user feedback. It supports four score data types, but text scores cannot be aggregated in experiments, LLM-as-judge or score analytics — [Langfuse evaluation core concepts](https://langfuse.com/docs/evaluation/core-concepts); [Langfuse scores overview](https://langfuse.com/docs/evaluation/scores/overview.md)
- Langfuse annotation queues are recommended for "building ground truth, systematic labeling". Its LLM-as-judge evaluators can run on observations, whole traces or experiments, and the docs advise calibrating judges against human annotation scores — [Langfuse core concepts](https://langfuse.com/docs/evaluation/core-concepts); [Langfuse LLM-as-a-judge](https://langfuse.com/docs/evaluation/evaluation-methods/llm-as-a-judge.md)
- LangSmith annotation queues hold single runs or whole **threads** (a full conversation) scored against a rubric. Pairwise queues compare two outputs side by side. Automation rules can route errors or low user scores into a queue, up to 100 items per action — [LangSmith annotation queues](https://docs.langchain.com/langsmith/annotation-queues)
- LangSmith feedback sources include in-app user feedback, annotation queues, offline automatic evaluators and online evaluators — [LangSmith feedback data format](https://docs.langchain.com/langsmith/feedback-data-format)
- Anthropic's agent-eval guidance separates the **transcript** (what the agent did and said) from the **outcome** (the final state of the environment). Graders are code-based, model-based or human, and grade either the transcript or the outcome. Commentary on the post says to read transcripts in order to validate the graders. Note: the primary text was not fetched; these points come via summaries — [Anthropic: Demystifying evals for AI agents](https://www.anthropic.com/engineering/demystifying-evals-for-ai-agents); [secondary summary](https://idlirshkurti.github.io/tutorials/agent_evals.html)
- OpenAI's evaluation best-practices guide recommends eval-driven development ("evaluate early and often") and lists "not calibrating automated metrics against human evals" as a common mistake. It also warns that eval data should reflect production traffic. The page also says the OpenAI **Evals platform is being deprecated**; a localized version gives read-only from 2026-10-31 and end of service on 2026-11-30 — [OpenAI evaluation best practices](https://developers.openai.com/api/docs/guides/evaluation-best-practices)

### Inferences
- **Mapping to the user's world.** One "task" equals one trace/conversation (`gen_ai.conversation.id`, mapped to `task_id`). Each sheet message is a span-like event. sender/recipient correspond to `gen_ai.agent.name` (zu, cbot, meti, kode, plus "me" for the human). Adopting OTel names in the column headers costs nothing and keeps a later move to Langfuse or Phoenix possible, but a full OTel/Langfuse stack is overkill for a solo user.
- **Proposed minimal schema.** Keep the existing message log ("AI 공통 채널") as the raw event stream, and add only three columns to it:
  - `task_id`: inherited from the root message. Whoever sends a new request generates it, e.g. `T-20261121-03`.
  - `msg_type`: one of request / ack / result / revision_request / question / relay.
  - `relayed_by_human`: Y/N. Set Y when the user copy-pasted the message manually between apps, as with meti, which is read-only, and with ChatGPT/Claude app messages that cannot write to the sheet themselves.
- **"tasks" tab, one row per task, written nightly by kode.** Columns:
  `task_id, title, requester, assignee, created_at, first_ack_at, started_at, done_at, status, handoff_count (distinct sender→recipient hops), rework_count (revision_request messages), human_relay_count, human_relay_wait_min (gap between a message's arrival and its relayed copy), lead_time_min, active_time_min, wait_time_min, quality_human (1–5, user fills), quality_judge (1–5, LLM), judge_rationale, outcome (accepted / accepted-with-edits / abandoned), tags`.
  - The "requested/started/done" timestamps can be inferred from status transitions (new→read→done) if the sheet records when each status changes. Today the sheet only has a single `time`.
  - Recommendation: either add `read_at` and `done_at` columns, or use an Apps Script `onEdit` installable trigger to stamp them automatically. (Apps Script trigger docs were not fetched; `onEdit` is standard, but its behaviour under API/connector writes is a known gap. Edits made via the API may not fire `onEdit`; verify this.)
- **Quality rating.** One human 1–5 score plus a one-line reason on maybe 20–30% of tasks is enough to calibrate the judge. Use numeric or categorical scores, not free text, because Langfuse's design notes text scores can't be aggregated.

### Gaps
- Could not fetch the full current attribute table of the OTel agent spans doc, so the exact list of required vs recommended attributes is not confirmed.
- Arize Phoenix and LangSmith's exact span schema were not researched in detail. Phoenix is OpenInference/OTel-based; that comes from my background knowledge and was not verified here.
- Not verified: whether Google Sheets `onEdit` fires on edits made through the Sheets API or connectors (kode's writes).

## 2. Scheduling options for a nightly run (capabilities and limits, 2025–2026)

### Takeaway
**Claude Code cloud scheduled tasks** (formerly "routines") are the best fit. They run on Anthropic's cloud while the laptop is closed, support a nightly cron with time zone, clone the GitHub repo, and can use the Google Sheets/Drive connectors with read and write access. Their minimum interval is hourly, which is fine for a nightly job. Apps Script time-driven triggers are a good free complement for deterministic, non-LLM work: timestamping, computing metrics and sending a digest email. ChatGPT tasks and Gemini scheduled actions are poor fits as the analysis engine, because they have small task quotas, limited access to the sheet, and meti is a company account. GitHub Actions cron works but requires an API key and has known reliability caveats.

### Cited Findings
**Claude Code scheduled tasks (official docs, research preview)**
- They are saved configurations: a prompt, one or more repos and a set of connectors. They run on Anthropic-managed cloud infrastructure "so they keep working when your laptop is closed". Triggers can be a schedule (hourly, nightly, weekly or one-off), an API POST, or GitHub events. **Status: research preview; behavior, limits and API may change** — [Claude Code scheduled tasks](https://code.claude.com/docs/en/web-scheduled-tasks)
- Available on Pro, Max, Ultra, Team and Enterprise. Tasks are created at claude.ai/code/routines or with `/schedule` in the CLI. They belong to the individual account, and their actions appear as the user (GitHub commits, connector actions) — [same](https://code.claude.com/docs/en/web-scheduled-tasks)
- Custom cron: expressions that run "more often than once per hour are rejected". A five-field cron is read as UTC unless it is prefixed with `CRON_TZ=<zone>` (e.g. `CRON_TZ=Asia/Seoul`). Runs scheduled exactly on the hour may start several minutes late, so the docs suggest e.g. 9:07 — [same](https://code.claude.com/docs/en/web-scheduled-tasks)
- Each run clones the repos, starts from the default branch and pushes to `claude/`-prefixed branches unless the prompt says otherwise — [same](https://code.claude.com/docs/en/web-scheduled-tasks)
- Connectors (claude.ai integrations, e.g. Google Drive) are included by default and can read and write during runs. Connector traffic goes through Anthropic servers, so the cloud network allowlist doesn't need changes for it. Locally added `claude mcp add` servers are not available unless they are re-added as connectors or declared in a committed `.mcp.json` — [same](https://code.claude.com/docs/en/web-scheduled-tasks)
- Runs start in **auto mode**, in which a classifier approves or blocks actions. A run still pauses for "ask" rules or after repeated blocks, waiting for the user to answer in the run's session. Within a Claude Code *project*, a run can continue an existing thread instead of starting a new session — [same](https://code.claude.com/docs/en/web-scheduled-tasks)
- Limits: 100 scheduled runs per hour per account, and 30 "Run now"/API fires per hour per task. Runs draw down normal subscription usage; without usage credits, extra runs are rejected until the window resets. If the GitHub connection lapses, runs are skipped for up to 72 h, after which the task turns off — [same](https://code.claude.com/docs/en/web-scheduled-tasks)
- A scheduled run can publish a private artifact (an HTML page) without asking, and "when a scheduled task's job is to keep a page current, give it an artifact you already published". This allows a persistent "nightly retro dashboard" URL — [same](https://code.claude.com/docs/en/web-scheduled-tasks)
- API trigger: the `/fire` endpoint is under beta header `experimental-cc-routine-2026-04-01` — [same](https://code.claude.com/docs/en/web-scheduled-tasks)

**ChatGPT scheduled tasks**
- Active task caps per the help center: Free/Go 3, Plus 5, Business/Edu 10, Pro/Enterprise 15. At the cap, a task must be paused or deleted before another is created. Tasks count against plan usage limits. Versions of the help page conflict on tiers, and an older version says tasks can't run more than once per hour; unverified whether that still applies — [OpenAI Help: Scheduled tasks in ChatGPT](https://help.openai.com/en/articles/10291617)

**Gemini scheduled actions (company account = meti)**
- Gemini Apps lists "Scheduled actions — up to 10 active actions at a time"; the page did not render which edition this applies to. Limits vary by Workspace edition, "may change frequently" and reset daily. Gemini Spark schedules are a separate feature. Workspace Studio flows have their own cap, typically 400 flow runs per month (German page), with enforcement of some Studio limits starting 2026-10-01 — [Use Gemini Apps with a work or school account](https://support.google.com/gemini/answer/14620100); [Workspace Studio usage limits](https://knowledge.workspace.google.com/ai/usage-limits-workspace-studio); [Gemini Spark schedules](https://support.google.com/gemini/answer/17094710)

**Google Apps Script time-driven triggers**
- Official quotas page, per the search snippet: script runtime is 6 min per execution, and total trigger runtime is **90 min/day for consumer accounts and 6 h/day for Workspace**. Quotas are per user, and exceeding one throws an exception. Older third-party figures are outdated — [Apps Script quotas](https://developers.google.com/apps-script/guides/services/quotas)
- Not fetched: the installable-triggers page, which covers daily "atHour" triggers that fire at a random time within the chosen hour. That behaviour is from background knowledge and was not verified in this session.

**GitHub Actions cron**
- Third-party guides agree: the minimum interval is every 5 min, cron is in UTC, and runs can be delayed under high load, especially at the top of the hour, with some queued runs dropped. Scheduled workflows are auto-disabled after 60 days without repository activity; sources differ on whether this applies to public repos only. Not verified against docs.github.com (fetch blocked) — [cronuru guide](https://cronuru.com/guides/github-actions-scheduled-workflows); [runhooks: unreliable schedules](https://runhooks.app/blog/github-actions-scheduled-workflows-unreliable/); [HackerNoon](https://hackernoon.com/your-github-actions-cron-job-can-disappear-without-failing)

### Inferences
- **Recommended setup (primary).** One Claude Code scheduled task named "nightly-retro":
  - cron `CRON_TZ=Asia/Seoul 37 23 * * *`, off the hour per the docs' advice;
  - fresh session per run;
  - repo: the user's GitHub repo, which holds the prompt/skill, the experiment log and the reports;
  - connectors: Google Sheets/Drive only (remove the others to limit blast radius).
  - A fresh session plus state kept in the repo/sheet is more robust than resuming one long session, whose context would grow and degrade every night.
- **Optional complement.** An Apps Script time-driven trigger (~23:00 KST) that does deterministic work inside Google: stamps status-change times, computes per-task durations into the "tasks" tab and emails a digest. It needs no LLM, stays well within 90 min/day, and lowers kode's token cost. Apps Script cannot call Claude without an API key, so leave the judgement work to kode.
- **Avoid as the main engine:**
  - ChatGPT tasks (zu): the cap is 5 on Plus, it can't write the sheet natively, and connector support inside tasks is uncertain.
  - Gemini scheduled actions (meti): company account; mixing personal workflow data into it raises the privacy issues in section 5.
  - GitHub Actions: needs an Anthropic API key stored as a secret (separate API billing), and has the reliability and 60-day caveats.
- **Delivery options for the morning:**
  - kode appends a row to "AI 공통 채널" with sender=kode, recipient=me (and zu), subject="야간 회고 YYYY-MM-DD", status=new;
  - commits `retros/YYYY-MM-DD.md` to the repo;
  - optionally updates a persistent artifact dashboard.
  - zu, as orchestrator, can be told in its custom instructions to read the latest retro row first each morning (that is a manual paste or a check of the sheet if a ChatGPT connector is available; uncertain).

### Gaps
- Not verified: whether ChatGPT scheduled tasks can use Google Drive/Sheets connectors or apps during a scheduled run.
- Edition-specific Gemini scheduled-action limits for the user's company tenant are unknown, and the company admin may have disabled the feature.
- Exact current GitHub "60-day" scope and Apps Script trigger timing precision were not confirmed from official pages (fetch blocked).

## 3. Memory and state across nights (hypotheses, experiments, results)

### Takeaway
The agent starts fresh each night, so all memory must live outside it. Use **two stores**:
- **the GitHub repo**, for narrative and versioned memory: `retro/STATE.md`, `experiments.md` and daily reports. Git history gives an audit trail, and Claude Code clones the repo every run by design.
- **a sheet tab "experiments"**, for structured tracking the user can edit on a phone.

### Cited Findings
- Claude Code scheduled tasks clone each repository on every run and can commit or push to `claude/` branches, or to another branch if the prompt says so. Skills committed in the repo are loaded in runs — [Claude Code scheduled tasks](https://code.claude.com/docs/en/web-scheduled-tasks)
- OpenAI's eval guidance describes a continuous loop: run evals on every change and grow the eval set over time. OpenAI's primer frames it as "Specify → Measure → Improve" — [OpenAI evaluation best practices](https://developers.openai.com/api/docs/guides/evaluation-best-practices); [OpenAI: Evals drive the next chapter of AI](https://openai.com/index/evals-drive-next-chapter-of-ai)
- Langfuse datasets/experiments allow side-by-side comparison of different experiments on the same data — [Langfuse core concepts](https://langfuse.com/docs/evaluation/core-concepts)

### Inferences
- **Proposed "experiments" tab columns:**
  `exp_id, created_on, hypothesis ("If zu routes coding tasks directly to kode instead of via cbot, lead time drops"), change (the concrete rule/prompt change), metric (e.g. median lead_time_min for tag=coding), baseline_value, baseline_window, target, start_date, review_date (e.g. +7 days), status (proposed/accepted/running/kept/reverted/rejected), result_value, verdict, notes`.
- **Lifecycle:**
  1. The agent proposes at most 1–3 changes per night, with status=proposed.
  2. The user marks them accepted.
  3. The agent records the baseline.
  4. After N days, or once at least ~10 tasks are affected, it computes the result and writes keep/revert.
  - With low task volume, demand a minimum sample before judging, and compare medians, not means.
- **`retro/STATE.md`:** a short rolling summary (≤1–2 pages) of the current working agreements, known bottlenecks, open experiments and rules already tried and rejected. It is read first every night and rewritten last. This keeps the prompt small and avoids re-proposing rejected ideas.
- **`retro/playbook.md` (or the orchestrator's "working agreement"):** the current routing rules (which AI gets which task type). kode proposes edits to it as a PR or diff that the user approves, so nothing changes silently.

### Gaps
- No authoritative source found for a standard "experiment log" format for personal LLM workflows; the schema above is a synthesis.

## 4. Analysis techniques an LLM agent can run nightly

### Takeaway
Do the numbers deterministically, in a script or with sheet formulas, then let the LLM interpret them, classify messages and judge quality.
- **Metrics:** lead time, active time, wait time, flow efficiency, throughput, WIP, rework rate, handoff count, human-relay delay.
- **Bottlenecks:** find which agent or edge (sender→recipient) accumulates wait.
- **Routing mismatches:** find task types that bounce between agents or get reworked.
- **Quality:** an LLM judge on outputs, calibrated against the user's own ratings.

### Cited Findings
- Lead time is the total elapsed time from creation to completion, including queue time. Cycle time is the time in progress. Flow efficiency = active time / lead time × 100%. Throughput is completed items per period. Little's Law relates cycle time to WIP and throughput (cycle time = WIP / throughput) — [Inflectra Kanban metrics](https://inflectra.com/Ideas/Topic/Kanban-Metrics.aspx); [Kanbantool advanced metrics](https://kanbantool.com/kanban-guide/advanced-kanban-metrics); [Zoho Sprints Kanban metrics](https://www.zoho.com/sprints/track-kanban-metrics.html)
- Active, value-adding steps are usually only a fraction of total lead time; low flow efficiency shows where tasks get stuck — [Planview](https://blog.planview.com/creating-continuous-improvement-with-lean-metrics/); [Miro Kanban metrics](https://miro.com/agile/metrics-kanban/)
- LLM-as-judge:
  - MT-Bench found GPT-4 judges reached over 80% agreement with human preferences, comparable to human–human agreement, but showed strong **position bias**, as well as verbosity and self-enhancement biases — [Zheng et al., Judging LLM-as-a-Judge](https://arxiv.org/pdf/2306.05685)
  - Newer 2026 preprints conflict on how big position and verbosity bias are in current models; one finds style bias dominant. Calibration against human labels on the specific task is advised — [The Coin Flip Judge?](https://arxiv.org/pdf/2606.13685); [CaLLM judge](https://arxiv.org/pdf/2610.18108); [DIAL](https://arxiv.org/pdf/2609.31215)
- Langfuse and OpenAI both recommend comparing LLM-judge scores with human annotations on the same items — [Langfuse LLM-as-a-judge](https://langfuse.com/docs/evaluation/evaluation-methods/llm-as-a-judge.md); [OpenAI evaluation best practices](https://developers.openai.com/api/docs/guides/evaluation-best-practices)
- Anthropic: prefer outcome-based checks where possible (did the deliverable actually get used or accepted), and use transcript review to debug — [Anthropic agent evals](https://www.anthropic.com/engineering/demystifying-evals-for-ai-agents) (via secondary summaries)

### Inferences
- **Nightly pipeline for kode:**
  1. **Load** today's and the trailing 14 days of rows from "AI 공통 채널" via the Sheets connector.
  2. **Thread** messages into tasks using reply-to id chains. If task_id is missing, the LLM clusters orphan messages by subject/body; flag low-confidence clusters.
  3. **Classify** each message: msg_type (request / result / revision / question / relay) and task category (coding, research, writing, company-doc lookup, etc.).
  4. **Compute** with a Python script in the repo, which is deterministic and testable:
     - lead_time = done − created;
     - wait = gaps where the next actor hadn't acted, e.g. a status=new message not yet read;
     - human_relay_delay = time between a message addressed to a non-sheet-writing AI and its appearance as handled;
     - rework_count;
     - handoff_count;
     - ping-pong detection (A→B→A→B on the same task);
     - stale items (status new/read for more than 24 h);
     - per-agent and per-edge median wait;
     - throughput and WIP per day.
  5. **Bottlenecks:** rank edges and agents by total wait-minutes. The human ("me") as the relay is likely the top bottleneck; quantify it.
  6. **Routing mismatch:** for each category × assignee, compare rework rate, lead time and quality. Flag categories where one assignee is consistently worse or tasks get re-routed. Example: research tasks that went zu→cbot→kode should have gone straight to kode.
  7. **Quality judge:** for tasks completed today, an LLM judge scores the final deliverable against the original request on a 1–5 rubric (requirement coverage, correctness, needed edits). Use a judge from a different model family than the producer where feasible, to limit self-preference bias. Store the score and rationale. Weekly, compute agreement (e.g. % within ±1, or Spearman) between quality_judge and quality_human. If agreement is low, revise the rubric and note it in STATE.md.
  8. **Recommend:** at most 3 concrete, testable changes, each tied to a metric and written as an experiment row.
  9. **Evaluate** running experiments whose review_date has arrived.
  10. **Write:** retro report, sheet rows, updated STATE.md, commit.
- **Volume caveat.** A solo user may have about 5–30 tasks a day, too few for statistics. Use trailing 7- or 14-day windows, report medians and counts, and avoid over-claiming. The weekly summary may be more meaningful than the nightly one.

### Gaps
- No source found on process-mining tools usable directly on a Google Sheet; standard process-mining software (e.g. PM4Py) exists, but it was not researched here.
- Judge-human agreement thresholds for "good enough" in personal workflows are not established in sources.

## 5. Privacy and security when mixing company (Gemini/work) data with personal tools

### Takeaway
Company Workspace data in meti is covered by enterprise terms (no training without permission, admin-controlled retention). Once it is copied into personal ChatGPT, Claude or a personal Google Sheet or GitHub repo, those enterprise protections no longer apply, and doing so may breach company policy. The improvement agent should log **metadata about** work-account interactions, not their content. Keep the repo private and the connector scope minimal.

### Cited Findings
- Google Workspace: customer data is not used to train models without the customer's permission (the "Training Restriction" in the Workspace Service Specific Terms). Gemini app conversation retention is admin-controlled, 90 days to indefinite — [Generative AI in Google Workspace Privacy Hub](https://support.google.com/a/answer/15706919?hl=en); [How Gemini in Workspace apps protects your data](https://support.google.com/docs/answer/14615114?hl=en-HK)
- In contrast, data shared with Gemini Apps under a **personal** account is processed under consumer terms and "may be used for model training and improvement" — [Privacy Hub](https://knowledge.workspace.google.com/admin/generative-ai/generative-ai-in-google-workspace-privacy-hub?rd=1)
- Claude Code scheduled tasks act as the user. Included connectors can **write** during runs and all connectors are included by default, so the docs advise removing unneeded ones. Cloud network access defaults to a "Limited" allowlist — [Claude Code scheduled tasks](https://code.claude.com/docs/en/web-scheduled-tasks)

### Inferences
- **Data minimization rule for meti traffic.** In "AI 공통 채널", record only `sender=meti/me`, timestamps, subject at a generic level ("사내 규정 조회") and outcome. Do not paste company document content into the personal sheet, the repo or the nightly report.
- **Prompt-injection risk.** Message bodies from any AI, especially content derived from external documents, are untrusted input to the nightly agent. The agent's prompt should treat bodies as data, never follow instructions in them, and only write to the designated tabs and repo paths. Remove unneeded connectors from the scheduled task. Proposals that change routing rules require user approval (PR or "accepted" status), not auto-apply.
- **Repo hygiene.** Use a private GitHub repo, keep no secrets in sheet or repo, and use branch protection if the agent pushes to main. Reports should never contain raw company content.
- **Check company policy.** Many employers prohibit moving work data into personal AI accounts. Confirm whether even metadata logging of work tasks in a personal sheet is allowed.
- Consumer ChatGPT and Claude may use conversations for training depending on account settings. Users should review the data-control settings in each app; not researched in detail here.

### Gaps
- Current consumer data-training defaults for ChatGPT and Claude apps were not researched in this pass.
- The user's specific company AI acceptable-use policy is unknown.
