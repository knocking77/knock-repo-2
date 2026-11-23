# Reliability and safety failure modes for LLM agents coordinating through a shared append-only sheet

Source-access note: in this session only anthropic.com could be fetched in full. arxiv.org, research.google, sky.cs.berkeley.edu, simonwillison.net, developers.google.com and genai.owasp.org were all blocked by the network allowlist. Everything else below comes from **search-result snippets or search-engine summaries** and is tagged **[snippet]**. Items tagged **[fetched]** were read on the page itself. Mark anything **[inference]** as my own reasoning.

## (a) Multi-agent failure taxonomies: MAST, Google/MIT scaling study, Anthropic lessons

### Takeaway
The observed problems map closely onto MAST's three categories. Broadcasts closed by the sender and requests left open are verification and termination failures. Acting outside one's role (overwriting a fixed row) is a design failure. Missed broadcasts and withheld results are inter-agent misalignment. Both the Google/MIT study and Anthropic say a central validating coordinator, explicit task specs, and durable external state reduce error propagation.

### Cited Findings
- MAST ("Why Do Multi-Agent LLM Systems Fail?", Cemri, Pan, Yang et al., UC Berkeley, arXiv 2503.13657). v3 (Oct 2025) defines 14 failure modes in 3 categories: system design issues, inter-agent misalignment, and task verification. It is built from 1,600+ annotated traces across 7 MAS frameworks. Inter-annotator agreement is κ = 0.88, falling to 0.79 on an unseen domain. The authors call it a first step that is not exhaustive. — [arXiv v3 HTML](https://arxiv.org/html/2503.13657v3) [snippet]; [MAST project page](https://sky.cs.berkeley.edu/project/mast/) [snippet]
- Named modes include 1.2 Disobey Role Specification, 1.3 Step Repetition, 1.5 Unaware of Termination Conditions, 2.4 Information Withholding, and 2.6 Reasoning-Action Mismatch, plus task derailment, premature termination, and failing to recognize task completion. — [arXiv v2](https://arxiv.org/html/2503.13657v2) [snippet]
- The failure rates across 7 open-source MAS were 41%–86.7%. Category shares in v3 are System Design 41.8%, Inter-Agent Misalignment 36.9%, and Task Verification 21.3%. Secondary sources cite 44.2 / 32.3 / 23.5, so the numbers changed between versions. — [arXiv v3 PDF](https://arxiv.org/pdf/2503.13657v3) [snippet]; contradicted by [FutureAGI write-up](https://futureagi.substack.com/p/why-do-multi-agent-llm-systems-fail) [snippet]
- The MAST authors argue that improving role definitions and orchestration alone is not enough to fix MAS failures. Structural changes and verification are also needed. — [arXiv v3 PDF](https://arxiv.org/pdf/2503.13657v3) [snippet]
- Earlier versions used different names (v1 "MASFT"; "specification issues" in place of "system design"), so cite the version. — [arXiv v1](https://arxiv.org/html/2503.13657v1) [snippet]
- Google Research / DeepMind / MIT, "Towards a Science of Scaling Agent Systems" (arXiv 2512.08296; blog Jan 28 2026). The study covers 180 agent configurations. Independent agents working in parallel without communicating amplified errors 17.2× versus a single agent. A centralized orchestrator held this to 4.4× by acting as a "validation bottleneck". A predictive model picked the best architecture for 87% of unseen configurations. Multi-agent coordination helps on parallelizable tasks but can degrade sequential ones. — [Google Research blog](https://research.google/blog/towards-a-science-of-scaling-agent-systems-when-and-why-agent-systems-work/) [snippet]; [arXiv 2512.08296](https://arxiv.org/html/2512.08296v2) [snippet]; [MIT Media Lab](https://www.media.mit.edu/projects/towards-a-science-of-scaling-agent-systems-when-and-why-agent-systems-work/overview/) [snippet]
- Anthropic, "How we built our multi-agent research system" [fetched]:
  - "Without detailed task descriptions, agents duplicate work, leave gaps, or fail to find necessary information." Each delegation should state an objective, an output format, guidance on tools and sources, and clear task boundaries. — [Anthropic engineering](https://www.anthropic.com/engineering/multi-agent-research-system)
  - "Agents are stateful and errors compound." Anthropic built resumable execution and has the lead agent save its plan to external memory, because context beyond 200k tokens gets truncated. — same source
  - Subagents store outputs in external systems and pass lightweight references back to the coordinator. This avoids a "game of telephone" and "prevents information loss during multi-stage processing." — same source
  - Synchronous execution means "the lead agent can't steer subagents, subagents can't coordinate." Early versions spawned 50 subagents for simple queries. — same source
  - Evaluation should judge the end state ("evaluate whether it achieved the correct final state"), starting with small samples. LLM judges help with scale, but "Human evaluation catches what automation misses." Full production tracing was needed to diagnose failures. — same source

### Inferences
- [inference] Mapping the observed problems onto MAST:
  - Broadcast closed by its sender, so scheduled readers never see it: a termination/verification failure (1.5 / premature termination) plus information withholding (2.4).
  - Results and requests left open: "unaware of termination conditions" and no task verification.
  - Agent writing to a fixed row and overwriting another's message: disobey role specification (1.2), and also a concurrency bug (see b).
  - Guessed timestamps: reasoning-action mismatch / fabrication.
- [inference] In this setup, "zu" is the orchestrator. The Google/MIT result suggests zu should do validation: confirm closure, check that replies match their requests. Routing alone adds no value.
- [inference] Anthropic's "store outputs externally, pass references" pattern maps to this design: results go in a separate tab or doc, and the message row carries a link plus request ID.

### Gaps
- I could not read the per-mode prevalence table or the paper's intervention case studies (ChatDev/AG2 role and topology fixes and their measured gains), because arxiv.org could not be fetched. Per-mode percentages from secondary blogs are inconsistent and should not be quoted.
- I could not verify the scaling paper's exact numbers for the tool-heavy coordination penalty or the capability-saturation threshold.
- A 2026 paper titled "Understanding Communication Failures in Multi-Agent LLM Systems" (arXiv 2610.32060) appeared in search results. Its content is unverified.

## (b) Concurrency and consistency on a shared Google Sheet, plus blackboard control

### Takeaway
Concurrent `values.append` calls can land on the same cells and overwrite each other. Google staff have called this a known limitation. Use INSERT_ROWS, have a single writer or a lock, make ids collision-proof without reading the sheet first (agent prefix plus timestamp or sequence), and make writes idempotent. Blackboard designs add explicit rules for who may write where and how readers find new items.

### Cited Findings
- A Google staff reply on the Sheets API group says `append` writes into existing cells rather than creating new ones, so concurrent appends can overwrite each other: "this is a known limitation of the append requests." The suggested workarounds are `insertDataOption=INSERT_ROWS` and/or application-level locking. — [google-spreadsheets-api mail archive](https://www.mail-archive.com/google-spreadsheets-api@googlegroups.com/msg02682.html) [snippet]; [narkive mirror](https://google-spreadsheets-api.narkive.com/4gPznOKn/concurrent-rows-appending-in-google-sheets-api-v4) [snippet]. Caveat: the thread is about 10 years old, and I could not check the current docs.
- A recent Google Developer forum report describes misplaced row offsets when appends run right after `deleteDimension`. The delete may still be processing on the backend even though it returned success. — [discuss.google.dev](https://discuss.google.dev/t/row-offsets-occur-when-append-requests-run-immediately-after-deletedimension-suggesting-the-delete-isn-t-fully-applied-yet/345218) [snippet]
- Community and blog reports (n8n, DEV) describe silently dropped or missing rows during bursts of concurrent appends. Their fix is to serialize writes through a queue. — [n8n community](https://community.n8n.io/t/google-sheets-missing-rows-race-condition-during-bulk-webhook-triggers/295991?tl=en) [snippet]; [DEV Community](https://dev.to/devansh365/your-google-sheets-backend-is-silently-dropping-rows-heres-why-3o74) [snippet]. These are secondary sources.
- Read-modify-write is the general race pattern: two writers read the same state, then both write. — [Wikipedia: Read–modify–write](https://en.wikipedia.org/wiki/Read%E2%80%93modify%E2%80%93write) [snippet]
- Idempotency keys (Stripe):
  - The client generates a unique key, and the server uses it to recognize retries.
  - A reused key returns the saved first result.
  - The same key with different parameters is an error.
  - Persist the key across process restarts.
  - Don't retry with a new key until the original outcome is reconciled.
  — [Stripe docs: idempotent requests](https://docs.stripe.com/api/idempotent_requests) [snippet]; [Stripe blog](https://stripe.com/en-cn/blog/idempotency) [snippet]
- Blackboard LLM systems:
  - bMAS/LbMAS (arXiv 2507.01701): the selected agent reads the whole blackboard as its prompt and writes its output back. — [arXiv 2507.01701](https://arxiv.org/html/2507.01701v1) [snippet]
  - A Google-authored blackboard system (arXiv 2510.01285) broadcasts requests on the blackboard and lets each agent decide autonomously whether to participate. It reports 13–57% relative end-to-end success gains over master-slave baselines. — [arXiv 2510.01285](https://arxiv.org/pdf/2510.01285) [snippet]
  - AgentsCAD uses a single shared-state object read and written by independent agents in a fixed phase order. — [arXiv 2607.02448](https://arxiv.org/pdf/2607.02448) [snippet]

### Inferences
- [inference] Id collisions come from agents computing "max id + 1" or minute-resolution timestamps: a classic read-modify-write race. The fix is to build ids without reading the sheet, for example `<agent>-<YYYYMMDDHHMMSS>-<random4>`, or `<agent>-<local sequence>` combined with the row number assigned after append. Treat the sheet's row position as the physical order, not the agent's clock.
- [inference] Guessed timestamps: an agent's own sense of "now" is unreliable. Prefer a server-filled timestamp, such as an Apps Script onEdit/onChange stamp or a formula column. If that is impossible, require each agent to state that its clock source is real (from a tool), never inferred. Lamport-style ordering by reply-to id gives causal order without trusting clocks. This is classic distributed-systems practice; no source was fetched here.
- [inference] Rule "append-only, never update another agent's row": only the addressee may change `status`, and only on rows addressed to them. A "fixed row" overwrite is prevented by a protected range or by forbidding `values.update` on message rows entirely. Status changes can themselves be appended as event rows (`ack`/`done` with reply-to), which makes the log fully append-only.
- [inference] Readers find new items by keeping a cursor: each scheduled agent records "last processed id/row" in its own heartbeat row (see e). That is more robust than relying on `status=new`, which other agents can change.
- [inference] Broadcasts ("모두"): a single shared `status` cell cannot represent per-recipient delivery. Use per-recipient ack rows, or never let the sender close a broadcast. A broadcast closes only by expiry or after all recipients acknowledge.
- [inference] Idempotency: include a request ID in each write so a re-run scheduled agent (e.g., cbot retrying after a timeout) can check "did I already append a row for request X?" before appending.

### Gaps
- I could not verify the current official `values.append` documentation on concurrency, or whether `INSERT_ROWS` is truly race-free. developers.google.com was blocked.
- The MCP Sheets tools that the agents use may not expose `insertDataOption`. That needs checking in the actual tool schemas.
- I did not fetch the classic blackboard literature (Hearsay-II, Nii 1986) on control knowledge sources.

## (c) Prompt injection and authority rules for agent-to-agent messages

### Takeaway
Every major source treats text arriving via tools or other agents as untrusted data that must not override the human principal. cbot's refusal to apply sheet-borne "operational changes" without in-chat human confirmation is the recommended behavior, not a bug. Authority should come from a channel the agents cannot forge: a human-only approval column or protected range, or the human's own chat. A "from: human" field that any agent can type is not enough.

### Cited Findings
- Willison's "lethal trifecta": private data access, exposure to untrusted content, and an exfiltration channel. LLMs "are unable to reliably distinguish the importance of instructions based on where they came from." He argues that the only safe approach is to avoid combining all three, and says vendor guardrails that stop 95% of attacks are a failing grade. — [Simon Willison, Jun 16 2025](https://simonwillison.net/2025/Jun/16/the-lethal-trifecta/) [snippet]
- "Design Patterns for Securing LLM Agents against Prompt Injections" (Beurer-Kellner, Tramèr, Debenedetti et al., arXiv 2506.08837) presents six patterns: plan-then-execute, dual LLM, code-then-execute/CaMeL, and others. The claim is that system design can keep agents secure even if the model is vulnerable. Plan-then-execute fixes the action sequence before reading untrusted data, but untrusted data can still influence arguments. In the dual-LLM pattern, a quarantined LLM without tools handles untrusted text and returns symbolic variables to the privileged LLM. The quarantined model can still be manipulated. — [arXiv 2506.08837](https://arxiv.org/html/2506.08837v3) [snippet]; [Willison summary](https://simonwillison.net/2025/Jun/13/prompt-injection-design-patterns/) [snippet]
- OpenAI's instruction hierarchy (arXiv 2404.13208): models often treat system prompts at the same priority as untrusted third-party text. Training teaches them to ignore lower-privileged instructions. The ordering is System > developer > user > tool, and "If a tool output contains malicious instructions, the model should ignore them rather than treat them as commands." — [arXiv 2404.13208](https://arxiv.org/html/2404.13208v1) [snippet]; [OpenAI: the instruction hierarchy](https://openai.com/index/the-instruction-hierarchy/) [snippet]
- Anthropic developer docs advise putting untrusted content only in tool results and telling Claude that tool, document, and search content is untrusted data that must never override the system prompt or the user's request. — [Claude docs: mitigating jailbreaks & prompt injections](https://platform.claude.com/docs/en/docs/mitigating-jailbreaks-prompt-injections) [snippet]
- Anthropic on browser agents: it trains against injected content and classifies untrusted context. A ~1% attack success rate "still represents meaningful risk." Its 2026 trustworthy-agents post says safeguards are not a guarantee and defenses are needed at every level. — [Anthropic: prompt injection defenses](https://www.anthropic.com/research/prompt-injection-defenses) [snippet]; [Anthropic: trustworthy agents](https://anthropic.com/research/trustworthy-agents) [snippet]
- OWASP LLM Top 10 (2025):
  - LLM01 Prompt Injection mitigations include least-privilege data access, privilege control on backend access, and segregating external content from user prompts.
  - LLM06 Excessive Agency stems from excessive functionality, permissions, or autonomy. The fix is to minimize tools and permissions and to add oversight.
  - Per CSA, the 2026 edition keeps prompt injection at #1 and moves excessive agency from #6 to #3.
  — [HackerOne on OWASP 2025](https://www.hackerone.com/blog/owasp-top-10-llms-2025-how-genai-risks-are-evolving) [snippet]; [CSA on OWASP 2026](https://cloudsecurityalliance.org/articles/the-owasp-top-10-for-llm-applications-2026-what-changed-and-why-it-matters) [snippet]

### Inferences
- [inference] The sheet is "untrusted content" for every agent, because any agent, or anything an agent pasted from the web or email, can write any cell. Each agent should treat a sheet body as a request to consider, never as an instruction that changes its standing rules.
- [inference] Authority model for a no-server setup:
  1. Standing rules live in a separate "rules" tab that only the human can edit (protected range). Agents read it but cannot change it.
  2. An `approved_by_human` column is protected so only the human's account can edit it. A sheet message counts as "operational" only if that cell is filled.
  3. Otherwise, the agent asks the human in its own chat, which is cbot's current behavior and should become the norm.
- [inference] Protected ranges only work if the agents write as an account different from the human's. If all agents use the human's Google credentials, protection cannot tell them apart, and an approval column is forgeable. This needs checking.
- [inference] Classify message types: `info`, `request` (task within an existing mandate), and `ops-change` (changes rules, credentials, schedules, or external sends). Only `ops-change` needs human approval. This keeps cbot's caution from blocking ordinary work.
- [inference] Agents should quote other agents' text as data in their replies (e.g., in a quoted block) and should never forward one agent's body as a fresh instruction to a third agent ("meti" off-sheet included).

### Gaps
- I could not fetch the OWASP pages or the Anthropic/OpenAI docs directly for exact wording on human approval gates.
- I found no published study specifically on agent-to-agent injection through a shared spreadsheet.

## (d)/(e) Liveness, escalation, evaluation, and rule-following limits

### Takeaway
Few sources were found specifically on heartbeats for LLM schedulers. The design can borrow from cron "dead man's switch" monitoring, which was not fetched here. Rule compliance degrades as rule count grows: even frontier models reach only about 68% at 500 simultaneous instructions, with a bias toward earlier instructions. Keep the protocol short and checkable, and measure adherence directly from the sheet.

### Cited Findings
- IFScale ("How Many Instructions Can LLMs Follow at Once?", Jaroslawicz et al., arXiv 2507.11538, NeurIPS 2025) tested 20 models with 10–500 simultaneous instructions. The best reached only 68% at 500. It found three degradation patterns and a primacy bias toward earlier instructions. Reasoning models do better but show higher variance at extreme densities. — [arXiv 2507.11538](https://arxiv.org/abs/2507.11538) [snippet]; [NeurIPS page](https://neurips.cc/virtual/2025/122417) [snippet]
- ManyIFEval (arXiv 2509.21051): multi-instruction performance falls predictably with instruction count. A logistic regression on count predicts it within about 10% error. — [arXiv 2509.21051](https://arxiv.org/html/2509.21051v1) [snippet]
- Anthropic: evaluate whether the system reached the correct final state, start with small samples, and keep humans in the loop. Tracing of decision patterns was key to fixing failures. — [Anthropic engineering](https://www.anthropic.com/engineering/multi-agent-research-system) [fetched]
- Anthropic: resumable execution plus external memory let agents recover from failures without restarting. — same source [fetched]

### Inferences
- [inference] Heartbeat design:
  - Add a `heartbeat` tab with one row per agent: agent, last_run_at, last_seen_id (cursor), open_items_count, note. Each agent overwrites only its own row, the one exception to append-only, enforced by protecting each row per writer where possible.
  - Liveness rule: if cbot's last_run_at is older than 2× its schedule (hourly, so 2h), any agent that notices, or zu, appends an `alert` to the human.
- [inference] Dead-letter and escalation:
  - Any `request` row open longer than an SLA (e.g., 24h, or 2 missed scheduled runs of the addressee) is escalated: the next agent to see it appends `escalate → human`.
  - Rows addressed to an unknown or off-sheet agent ("meti") go to the human by default.
- [inference] Metrics computable from the sheet itself, with a formula or Apps Script:
  - adherence rate: % of rows with valid id format, request ID, correct status transitions, and no edits to other agents' rows
  - open-age distribution and count of requests open past SLA
  - round-trip time, from request to first ack and to done
  - broadcast coverage: % of recipients that acked
  - heartbeat gaps
  - human-approval bypass attempts
  Review these weekly, as Anthropic recommends small-sample human review.
- [inference] Given IFScale/ManyIFEval, compress the protocol to about 5–10 hard rules at the top of every agent's prompt (primacy bias). Put the full spec in the rules tab. Enforce what you can mechanically (formula validation, protected ranges, data validation dropdowns for `status`/`to`) rather than relying on LLM memory. A fresh scheduled run has no memory except the sheet, so the rules tab must be re-read on every run.

### Gaps
- I found no sourced material on heartbeat or dead-man's-switch monitoring of scheduled LLM agents. That search was not run within budget, and the general cron-monitoring literature (e.g., healthchecks.io-style services) was not fetched.
- I found no published benchmark of rule adherence in multi-agent message protocols. Adherence would need to be measured locally.
- Instruction drift over long single sessions for the Claude Code agents ("캐드", "캐대") was not researched specifically.
