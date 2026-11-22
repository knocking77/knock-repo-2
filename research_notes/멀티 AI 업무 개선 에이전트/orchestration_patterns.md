# Multi-agent orchestration patterns and trade-offs, applied to a one-person, multi-vendor, human-relayed setup (zu / cbot / meti / kode)

Research date: 2026-11-21. Access note: this session's network blocked direct fetches of arxiv.org, sky.cs.berkeley.edu, cognition.ai, research.google and help.openai.com. Claims from those sources come from search-result summaries, not full-text reads, and are marked "(via search summary)". Anthropic engineering posts were read in full.

## 1. Orchestration patterns and their trade-offs

### Takeaway
The main patterns are chaining/pipeline, routing, parallelization, orchestrator-worker (hub-and-spoke), evaluator-optimizer, handoff (peer transfer), and blackboard (shared workspace). The vendors agree on two points: use the simplest pattern that works, and decide early who owns the final answer, because that decides how context has to move. A hub is best when subtasks are unpredictable and need checking. A shared workspace is best for cutting relay and "telephone" losses.

### Cited Findings
- Anthropic describes the workflow patterns as prompt chaining (fixed steps, slower but more accurate), routing (classify, then send to a specialist; misclassification sends work down the wrong path, but cheap models can take easy queries), parallelization (sectioning or voting), orchestrator-workers (a central LLM splits the task at runtime and synthesizes the results; for tasks whose subtasks can't be predicted), and evaluator-optimizer (a generate/critique loop that works when there are clear criteria). — [Anthropic, Building effective agents](https://www.anthropic.com/engineering/building-effective-agents)
- Anthropic: "find the simplest solution that works", which often means no agentic system at all. Agentic systems trade latency and cost for performance, so add complexity only when measurement shows it helps. Frameworks can hide prompts and responses and make debugging harder. — [Anthropic, Building effective agents](https://www.anthropic.com/engineering/building-effective-agents)
- Anthropic's research system uses orchestrator-worker: a lead agent plans, saves the plan to external memory, spawns parallel subagents and synthesizes; a separate CitationAgent adds sources afterwards. — [Anthropic, multi-agent research system](https://www.anthropic.com/engineering/multi-agent-research-system)
- OpenAI Agents SDK has two primitives. "Agents as tools": a manager keeps control of the final reply and calls specialists for bounded tasks. "Handoffs": a specialist takes over and owns the next response, and the runner switches agents. A third option is running agents in parallel from code. — [OpenAI, Orchestration and handoffs](https://developers.openai.com/api/docs/guides/agents/orchestration); [openai-agents-python multi_agent docs](https://openai.github.io/openai-agents-python/multi_agent/)
- Blackboard / shared workspace: in Google's 2025 data-discovery system, a central agent posts requests to a shared blackboard and subordinate agents decide for themselves whether to respond, so no controller has to know every agent's skills. It reports 13-57% relative improvement in end-to-end success over RAG and master-slave baselines (via search summary). — [arXiv 2510.01285](https://arxiv.org/abs/2510.01285v2); [Google Research pub page](https://research.google/pubs/blackboard-multi-agent-systems-for-information-discovery-in-data-science/)
- bMAS/LbMAS (2025): agents communicate only through a blackboard, with LLM control roles (decider, critic, conflict-resolver, cleaner). I could not retrieve its benchmark numbers. — [arXiv 2507.01701](https://arxiv.org/html/2507.01701v1)
- Protocols: MCP connects an agent to tools and data. A2A (introduced by Google in April 2025, now a Linux Foundation project) connects agents to each other across trust boundaries. Each agent publishes an "Agent Card" for discovery and keeps its MCP tools private. Secondary sources say A2A reached v1.0 in 2026 and that both protocols sit under the Linux Foundation's Agentic AI Foundation, but dates conflict between sources. — [InfoQ](https://www.infoq.com/news/2025/07/agent2agent-linux-foundation); [The New Stack](https://thenewstack.io/google-donates-the-agent2agent-protocol-to-the-linux-foundation/); [AAIF A2A v1.0 guide](https://aaif.io/blog/a2a-v1-0-a-builder-s-guide-part-1-discovery-tasks-and-clients); [IBM explainer](https://www.ibm.com/think/topics/agent2agent-protocol)
- LangChain's Harrison Chase argues that the Anthropic ("multi-agent works") and Cognition ("don't build multi-agents") posts agree that context engineering is the crux. — [LangChain blog](https://blog.langchain.com/how-and-when-to-build-multi-agent-systems)

### Inferences
- The user's current setup is a human-relayed hub-and-spoke ("agents as tools" with zu as manager). Its weak point is that every spoke result passes through the hub's context and the human's copy-paste.
- The Google Sheet is already a blackboard. Making it the main channel for task state, with zu writing tasks to it and posting summaries while workers read and write rows directly where they can, is the blackboard pattern. It would cut relay hops without giving up zu's planning role.
- A2A/MCP matter only where products expose them. None of the four consumer or enterprise chat apps lets one agent call another directly over A2A (see the gaps), so the "protocol" in practice is a shared document plus a message schema.

### Gaps
- No primary source says whether consumer ChatGPT, Claude.ai or Gemini apps support A2A agent-to-agent calls; I found none that do.
- I did not read the CrewAI, AutoGen or LangGraph docs directly because of my tool-call budget. Their patterns (sequential/hierarchical crews, group chat, graph supervisors) map onto the patterns above but are not cited here.

## 2. Known failure modes of multi-agent systems

### Takeaway
Most multi-agent failures come from system design (vague specs, role confusion, poor handoffs, no verification), not from weak models. Errors compound across hops unless something validates them. A hub helps only if it actually checks work.

### Cited Findings
- MAST ("Why Do Multi-Agent LLM Systems Fail?", Cemri et al., UC Berkeley, NeurIPS 2025): 14 failure modes in 3 categories, (i) specification/system design, (ii) inter-agent misalignment, (iii) task verification. It was built from traces of 7 open-source frameworks, with human inter-annotator kappa 0.88 and an LLM-judge kappa of 0.77. The authors note that gains of multi-agent systems over simpler approaches on popular benchmarks are often minimal, and that the failures need structural redesign, not superficial prompt fixes (via search summary). — [arXiv 2503.13657](https://arxiv.org/abs/2503.13657v3); [NeurIPS poster](https://neurips.cc/virtual/2025/poster/121528)
- MAST prevalence in the later version (1,642 traces), via search summaries. These figures differ between paper versions, so treat them as approximate:
  - step repetition about 15.7%
  - reasoning-action mismatch about 13.2%
  - disobeying the task specification about 11.8%
  - system-design category about 44.2%
  - inter-agent misalignment about 32.3%

  — [arXiv 2503.13657](https://arxiv.org/pdf/2503.13657); [DEV field guide (secondary)](https://dev.to/tuomo_pisama/a-field-guide-to-multi-agent-failure-modes-59on); [IBM/Berkeley IT-Bench + MAST](https://huggingface.co/blog/ibm-research/itbenchandmast)
- Failures Anthropic observed in its own system:
  - spawning 50 subagents for simple queries
  - searching endlessly for sources that don't exist
  - continuing after it already had enough results
  - subagents distracting each other with too many updates
  - duplicated work
  - vague delegation leading to misread tasks

  Anthropic's fix is that each delegation states an objective, an output format, tool and source guidance, and clear boundaries. — [Anthropic, multi-agent research system](https://www.anthropic.com/engineering/multi-agent-research-system)
- Telephone-game degradation: Anthropic has subagents write outputs to external storage and pass back lightweight references, to "avoid the game of telephone", cut tokens and preserve fidelity. — [Anthropic, multi-agent research system](https://www.anthropic.com/engineering/multi-agent-research-system)
- Cost: agents use about 4x the tokens of chat and multi-agent systems about 15x, which pays off only for high-value tasks. — [Anthropic, multi-agent research system](https://www.anthropic.com/engineering/multi-agent-research-system)
- Error amplification (Google/MIT, "Towards a Science of Scaling Agent Systems", Dec 2025; 180 configurations comparing a single agent with independent, centralized, decentralized and hybrid multi-agent systems): independent agents amplified errors up to 17.2x, while a centralized orchestrator acting as a "validation bottleneck" held that to 4.4x. On sequential reasoning tasks, every multi-agent variant degraded performance by 39-70% (via search summary). — [arXiv 2512.08296](https://arxiv.org/abs/2512.08296); [Google Research blog](https://research.google/blog/towards-a-science-of-scaling-agent-systems-when-and-why-agent-systems-work/)
- Cognition ("Don't Build Multi-Agents", June 2025): split subtasks with unshared context produce inconsistent results (its Flappy Bird example), because "actions carry implicit decisions". Its April 2026 follow-up says this still holds for parallel-writer swarms (via search summary). Commentators note that Claude Code-style subagents mostly read and don't make decisions. — [Cognition](https://cognition.ai/blog/dont-build-multi-agents); [Cognition 2026 follow-up](https://cognition.ai/blog/multi-agents-working); [jxnl summary](https://jxnl.co/writing/2025/09/11/why-cognition-does-not-use-multi-agent-systems/)

### Inferences
- The setup has an unusually long chain for meti: zu writes a Google Doc, meti reads it, meti's reply goes back through the human, then to zu, then to the user. Every hop is a chance for summarization loss and spec drift (the MAST misalignment category).
- With a human relaying, the "coordination overhead" falls mostly on the person's time rather than tokens. Their minutes, not API cost, are the main cost to optimize.
- The hub's value depends on zu really verifying work, the "validation bottleneck" effect. If zu only forwards results, the hub adds hops without the benefit.

### Gaps
- I found no study of human-relayed multi-vendor assistant setups specifically. The evidence comes from automated systems.
- I could not read the full MAST mode list, because arxiv.org and the Berkeley project page were blocked here.

## 3. When a single strong agent beats multiple agents

### Takeaway
A single agent generally matches or beats multi-agent systems on sequential, tightly coupled work when both get the same compute. Multi-agent wins on broad, parallelizable, breadth-first work such as research, or when one context window would overflow.

### Cited Findings
- Anthropic's multi-agent setup (Opus 4 lead, Sonnet 4 subagents) beat a single Opus 4 agent by 90.2% on an internal research eval. Token usage explained about 80% of the performance variance on BrowseComp. Poor fits are tasks where all agents need shared context or have many dependencies between them, most coding, and real-time coordination. — [Anthropic, multi-agent research system](https://www.anthropic.com/engineering/multi-agent-research-system)
- Tran and Kiela (2026): with equal thinking-token budgets, single agents match or beat multi-agent systems on multi-hop reasoning across Qwen3, DeepSeek-R1-Distill and Gemini 2.5. Earlier multi-agent gains were often confounded by extra compute. Multi-agent systems become competitive when a single agent's context use degrades, or when more compute is spent (via search summary). — [arXiv 2604.02460](https://arxiv.org/abs/2604.02460); [VentureBeat on the "swarm tax"](https://venturebeat.com/orchestration/are-you-paying-an-ai-swarm-tax-why-single-agents-often-beat-complex-systems)
- Multi-agent variants degraded sequential tasks by 39-70% (via search summary). — [Google Research blog](https://research.google/blog/towards-a-science-of-scaling-agent-systems-when-and-why-agent-systems-work/)
- A single LLM can match an automatically discovered heterogeneous multi-agent workflow, and multi-agent discussion is on par with a single agent when both have demonstrations (via search summary). — [arXiv 2601.12307](https://www.arxiv.org/pdf/2601.12307); [arXiv 2402.18272](https://arxiv.org/pdf/2402.18272)

### Inferences
- For the user's day-to-day tasks, which are mostly sequential, like drafting, then revising, then deciding, sending the whole task to one capable assistant with the right tools is likely to beat splitting it across avatars. Split only for truly separable or parallel work: independent research threads, work that needs the company Gemini's internal data, or scheduled jobs for kode.
- Use specialization by access, not by model personality. Use meti only where company Workspace data or policy requires it, and kode where execution, Sheets writes or scheduling is needed.

### Gaps
- I found no benchmark of mixed-vendor ensembles (GPT + Claude + Gemini) against a single model on office-type tasks.

## 4. Which assistant to route which task to (2025-2026)

### Takeaway
Practitioner consensus, which is blog-level and low-rigor, is that the frontier models are close in quality. Routing decisions should depend more on ecosystem and tool access than on raw quality. The rough split: ChatGPT as the all-round default with a broad ecosystem, Claude for writing and coding (multi-file engineering, agentic coding), and Gemini for Google Workspace-native work, large context and grounded research.

### Cited Findings
- Comparisons place ChatGPT as the most popular all-round default with mature integrations and image generation, Claude ahead on writing quality and multi-file coding (one reported SWE-bench figure is 80.8% vs 77.2%), and Gemini strongest for Google integration and research, with a 1M-token context. Entry paid tiers have converged around $20 a month. These sources disagree on model versions, and their benchmark numbers are unverified. — [Geekflare comparison](https://geekflare.com/guides/chatgpt-vs-claude-vs-gemini-comparison/); [DEV 2026 comparison](https://dev.to/max_quimby/claude-vs-chatgpt-vs-gemini-2026-comparison-4b09); [Tech Insider](https://tech-insider.org/chatgpt-vs-claude-vs-gemini-2026/)
- Anthropic notes that routing lets simple queries go to cheaper or smaller models, at the risk of misclassification. — [Anthropic, Building effective agents](https://www.anthropic.com/engineering/building-effective-agents)

### Inferences
- A practical routing table for the user:
  - zu (ChatGPT): intake, planning, general Q&A
  - cbot (Claude app): long-form writing, reviews, analysis of attached documents
  - kode (Claude Code): anything that must run code, write the Sheet, or run on a schedule
  - meti (company Gemini): only tasks that need company-internal Workspace data or must stay inside the company tenant
- Because model rankings change every few months, the routing table should sit in the shared Sheet as editable rules rather than being hard-coded in zu's instructions.

### Gaps
- I found no rigorous, independent head-to-head evaluation for 2026 models. The cited comparisons are blogs, and the model versions they name conflict.

## 5. Reducing human relay cost: shared docs and sheets, connectors, formats

### Takeaway
As of mid-to-late 2026, all three vendors appear to offer write access to Google Workspace files, which differs from 2025, when most of this access was read-only. If so, a shared Google Sheet or Doc can be read and written by zu, cbot and kode directly, and the human only needs to trigger each assistant. These capabilities are recent, beta, plan- and admin-dependent, and need checking in the user's own accounts.

### Cited Findings
- ChatGPT: OpenAI's help center (search summary only; direct fetch blocked) says the Google Drive app can request permissions to read files, view metadata, or create and update files. Docs, Sheets and Slides actions run through the Drive app and may need extra permissions. Secondary reports date the write and edit capability to a June 2026 update, with broad availability on Plus, Pro, Business, Enterprise and Edu reported from August 2026. Users who connected before June 15, 2026 may need to reconnect. One older source (Composio) says the native connector is read-only, which is likely outdated. Free plans have no app connections. — [OpenAI help: Google Drive app](https://help.openai.com/en/articles/10929079-google-drive-app-and-setup-in-chatgpt); [Tactiq](https://tactiq.io/learn/how-to-connect-google-drive-to-chatgpt); [Canaltech](https://canaltech.com.br/inteligencia-artificial/chatgpt-invade-o-google-drive-e-passa-a-editar-documentos-no-lugar-do-gemini/); conflicting: [Composio](https://composio.dev/content/how-to-connect-chatgpt-to-google-drive). UNCERTAIN.
- Claude.ai: "Claude for Google Workspace" entered public beta on all paid plans (announced October 6, 2026). Separate Docs, Sheets and Slides connectors let Claude create and edit files, including live editing in a side pane and working with Docs comments and suggestions. The Drive connector still handles search, upload and sharing. On Team and Enterprise plans an owner must enable the connectors. An older developer page says Drive gives text extraction only and Sheets is unsupported, which conflicts and is likely outdated. — [Claude blog](https://claude.com/blog/claude-now-works-in-google-docs-sheets-and-slides); [Claude support: Google Workspace connectors](https://support.claude.com/en/articles/10166901-use-google-workspace-connectors); [Digital Trends](https://digitaltrends.com/computing/claude-just-stepped-into-google-workspace-with-in-file-editing-automated-sheets-work-and-smarter-slides/). BETA.
- Claude Code cloud sessions (kode): this research session itself has Google Sheets and Drive MCP tools available (for example append_values, update_values, get_values), which supports the user's statement that kode can read and write Sheets. Scheduled tasks are also available. — observed in this session's tool list (not a web source).
- Gemini in Workspace: Gemini in Google Sheets can build and edit entire spreadsheets from natural language (April 2026), showing a plan for approval before changing data. Complex edits are web-only, and the feature needs an eligible Workspace or Google AI plan. Expanded editing in Sheets dates from October 2025. — [Workspace Updates, Apr 2026](https://workspaceupdates.googleblog.com/2026/04/build-and-edit-complex-spreadsheets-with-Gemini-in-Google-Sheets.html); [Google support](https://support.google.com/docs/answer/16959434?hl=en); [Workspace Updates, Oct 2025](https://workspaceupdates.googleblog.com/2025/10/expanded-editing-capabilities-gemini-in-google-sheets.html)
- Anthropic's practice for handoff fidelity is to write artifacts to shared storage and pass references, not full content, and to give each delegated task a fixed brief (objective, output format, tools and sources, boundaries). — [Anthropic, multi-agent research system](https://www.anthropic.com/engineering/multi-agent-research-system)

### Inferences (concrete alternatives worth testing)
1. **Sheet-as-blackboard (lowest effort, highest leverage):** use one "Tasks" tab with columns such as `id | created_at | from | to | status (new/claimed/done/blocked) | objective | inputs (links) | output_format | constraints | result_link | summary(<=3 lines) | verified_by`. Each assistant reads and writes rows directly where its connector allows (zu via the Drive/Sheets app, cbot via the Sheets connector, kode via MCP). The human just says "check the sheet" to each assistant instead of copy-pasting. This is a test of item 5's connector claims.
2. **kode as an automated dispatcher or watchdog:** a scheduled kode task polls the Sheet, flags stale `claimed` rows, compiles a daily digest, and runs the executable tasks itself. Moving the polling to kode removes the human from status tracking.
3. **Fix meti's path:** the read-only Doc plus a reply through zu is the longest chain. Test whether company Gemini can write its answer directly into a reply section of a company-owned Sheet or Doc (Gemini in Sheets or Docs). If company policy forbids that, keep the read-only brief but have meti return a fixed-format reply block that the human pastes once into the Sheet, not into zu.
4. **Hub only where it adds verification:** keep zu as planner and checker for multi-step or ambiguous work, but let single-skill tasks go straight to the right assistant using the routing table. A/B test for 2 weeks, measuring human relay steps per task, turnaround time and rework rate.
5. **Fixed brief template (Anthropic's four elements) and pass-by-reference:** results go into a Doc or Sheet cell and only a link plus a 3-line summary moves between assistants, which limits telephone-game loss.
6. **Single-agent baseline:** for a sample of typical tasks, compare doing the whole task in one assistant with the needed connectors against the multi-avatar route. The evidence in item 3 predicts one assistant will often win on sequential tasks.

### Gaps
- ChatGPT's exact Sheets edit scope (cell-level writes vs. creating or replacing files), and whether it works for company-managed Workspace accounts, is unverified. I could not read help.openai.com directly.
- Whether the company's Gemini tenant allows writing to Docs or Sheets shared with an outside personal account depends on admin policy. I found no general source.
- Concurrency and conflict behavior when several assistants write the same Sheet (overwrites, locking) is undocumented. Use append-only rows and per-agent columns as a precaution (my inference).
- No consumer assistant was found to support direct agent-to-agent calls (A2A) between vendors, so the human or a shared file remains the trigger.
