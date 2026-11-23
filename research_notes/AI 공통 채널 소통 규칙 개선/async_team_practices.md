# Async Team Communication and Work-Tracking Practices (for the "AI 공통 채널" sheet)

Method note: in this session WebFetch could not reach any host (handbook.gitlab.com, support.atlassian.com, adr.github.io and hbr.org did not resolve), so every finding below comes from WebSearch result summaries of the cited pages, not from reading the full pages. URLs are the pages the search engine attributed each claim to. Exact wording should be spot-checked before quoting it to humans.

## 1. Ticket/issue workflow state machines: standard states, who transitions, resolved vs closed, auto-close, requester confirmation

### Takeaway
Mature ticket systems split "work finished" (Resolved, set by the worker) from "loop closed" (Closed, set by the requester's confirmation or by a timer after a few days). They also have an explicit inbox state (New/Triage) and a cancel path with a reason (Canceled / "not planned"). This split fixes the sheet's "results never closed" and "original request left open" failures. Today the sheet's one `done` value has to mean both things, and nobody owns the final close.

### Cited Findings
- Common ITIL-style incident lifecycle across vendor implementations: New (logged, not yet worked) -> Assigned (an owner takes it) -> In Progress -> On Hold (waiting on someone else) -> Resolved -> Closed. No official ITIL publication appeared in the results; the lifecycle comes from vendor docs, which differ in detail. — [ServiceNow incident life cycle](https://www.servicenow.com/docs/r/QykF2iRSIs6VXztArCSo3w/wx13CEpLt~8B1elAmArNyw); [ManageEngine glossary](https://www.manageengine.com/products/service-desk/it-incident-management/it-incident-glossary-terms.html); [Octopus ITSM](https://docs.octopus-itsm.com/en/node/750)
- Resolved means a fix was applied but the caller has not confirmed it. One implementation allows reopening for 5 days. Closure follows confirmation, or a set time in Resolved: "only the caller has the right to close the incident ticket by accepting its solution"; otherwise it is "Rejected by user". — [Octopus ITSM](https://wiki.octopus-itsm.com/en/node/750); [ManageEngine ITIL guide](https://www.manageengine.com/uk/products/service-desk/itil-incident-management-guide.html)
- Jira Service Management's ITSM template includes an SLA and automation that auto-close service requests 3 business days after an agent resolves them. The "Time to close after resolution" clock starts when the resolution is set and stops if the resolution is cleared or the request moves to Closed. A 2024 community thread asks whether the default is 3 or 7 days, so check your own instance. — [Atlassian: Auto-close resolved service requests](https://support.atlassian.com/jira-service-management-cloud/docs/auto-close-resolved-service-requests/); [Atlassian KB: inactivity follow-ups and closure](https://confluence.atlassian.com/display/JIRAKB/Implement+Automated+Issue+Inactivity+Follow-ups+and+Closure+in+Jira+Service+Management+Cloud)
- A common Jira pattern: the agent moves the ticket to Resolved, then the customer approves (to Closed) or rejects (back to In Progress). Teams add a 3-day SLA from Resolved to Closed. — [Atlassian Community](https://community.atlassian.com/t5/Jira-Service-Management/How-to-close-a-ticket-when-resolved-by-the-reporter-Multiple/qaq-p/1459734); [Atlassian Community: auto close if no response in 7 days](https://community.atlassian.com/forums/Jira-Service-Management/Auto-close-ticket-if-no-response-in-7-days-and-reopen-if-replied/qaq-p/2845681)
- GitHub Issues (since May 2022) closes an issue with a reason: "completed" (the default) or "not planned", exposed as `state_reason` in the API and webhooks. — [GitHub changelog 2022-05-19](https://github.blog/changelog/2022-05-19-the-new-github-issues-may-19th-update/); [GitHub Docs: closing an issue](https://docs.github.com/en/enterprise-server@3.17/issues/tracking-your-work-with-issues/administering-issues/closing-an-issue)
- The actions/stale bot auto-closes inactive issues. Its close reason changed over time from "completed" to "not_planned" as the default. — [actions/stale PR #807](https://redirect.github.com/actions/stale/pull/807)
- Linear status categories: Triage (a team inbox), Backlog, Unstarted, Started, Completed, Canceled. Declining a triaged issue moves it to Canceled with an optional explanatory comment. — [Linear: configuring workflows](https://linear.app/docs/configuring-workflows); [Linear: triage](https://linear.app/docs/triage)
- Linear auto-close cancels issues not updated within a configurable period. Auto-archive hides issues that have been closed for months. — [Linear changelog 2020-08-19](https://linear.app/changelog/2020-08-19-auto-close-and-auto-archive); [Linear docs](https://linear.app/docs/configuring-workflows)

### Inferences
- Map onto the sheet: `new` (Triage/New) -> `read` (Assigned/accepted, meaning "I own it") -> optional `blocked`/`waiting` (On Hold) -> `done` on the **result row** (Resolved) -> the requester closes the **original request row** as `done` (Closed) -> `canceled` with a one-line reason (Not planned / Declined).
- Who changes what: the recipient moves their incoming row (new -> read -> done). The requester owns the final close of their own REQ row once a [결과] row arrives. A [결과] row is itself a message *to* the requester, so the requester marks it `done` once consumed. That one rule clears both "results left open in inbox" and "original requests left open."
- Auto-close by timer, modeled on Jira's 3 business days: if a [결과] row has sat with no objection for N hours (24–72h suits hourly or nightly agents), the nightly retrospective agent (캐드) closes it and its parent REQ and writes "auto-closed" in a note. The sheet has 8 rows older than 24h, which suggests a threshold around 24–48h.
- Keep `canceled` but require a reason, as Linear and GitHub do, so canceled work can't be confused with completed work.

### Gaps
- Official ITIL 4 text on states and the caller-confirmation requirement was not retrieved; the findings come from vendor docs.
- No source compared closure rates of manual vs timer-based closing.

## 2. Kanban: WIP limits, explicit policies, aging work items, definition of done, blocked state

### Takeaway
Kanban's main lessons are to write the rules down (explicit policies), cap work in progress, watch work item age against a service-level expectation, and count blocked items toward WIP. For the sheet, that means a published "how rows move" policy, a per-agent cap on open items, and an aging check run by the nightly retrospective.

### Cited Findings
- The Kanban Guide says members "must explicitly control the number of work items in a workflow from start to finish", which is usually shown as WIP limits. Their duties include ensuring "Work Items do not age unnecessarily, using the Service Level Expectation as a reference" and resolving "impediments that are causing blocked Work". — [Kanban Guide](https://kanbanguides.org/english/)
- Explicit policies cover "how work items can flow through each state from started to finished." — [Kanban Guide 2020 copy](https://www.scribd.com/document/487821094/Kanban-Guide-2020-07-pdf); [Planview: make process policies explicit](https://blog.planview.com/make-process-policies-explicit/)
- Work Item Age is "the amount of elapsed time between when a work item 'started' and the current time." A blocked item still counts against the WIP limit. — [Scrum.org: actively manage items](https://www.scrum.org/resources/blog/how-do-we-actively-manage-items-workflowitems-progress-kanban)
- Anderson's Kanban Method practices: Visualize, Limit WIP, Manage Flow, Make Policies Explicit, Use Feedback Loops, Improve/Evolve Experimentally. — [Wikipedia: Kanban (development)](https://www.Wikipedia.com/wiki/Kanban_(development)); [Kanban University glossary](https://kanban.university/wp-content/uploads/2023/04/The-Official-Kanban-Guide_Glossary.pdf)
- Caveat: the Kanban Guide (Vacanti and Coleman) and Anderson's Kanban University method are separate documents. — [Wikipedia](https://www.Wikipedia.com/wiki/Kanban_(development))

### Inferences
- Explicit policies: keep a "Rules" tab with a version number, as the single place where row transitions are defined. Every agent prompt links to it.
- WIP limit per agent, for example at most 3 open `read` (accepted) REQs. zu, who distributes work, checks the limit before assigning more.
- Aging service-level expectation: any row in `new` for more than 2 of the recipient's check cycles, or `read` for more than 24h with no result or blocked note, gets flagged in the nightly retrospective. Because the hourly agent (cbot) and the nightly agents check at different rates, set the expectation per recipient.
- Add a `blocked` status (or a "blocked:" prefix in a note) instead of leaving rows silently in `read`; blocked rows still count toward WIP.
- Definition of done for a request: a [결과] row exists that references the REQ id in reply-to, states the deliverable or link, and the requester has closed both rows.

### Gaps
- The full primary text of Anderson's book or the current Kanban Guide was not retrieved. A Scrum Guide "Definition of Done" source was not retrieved; the search returned nothing on it.

## 3. Async communication handbooks: request templates, response times, decision logs, announcements, triage habits, single source of truth

### Takeaway
Async-first companies converge on a single written source of truth, a stated response window (often 24h) rather than immediate replies, batched check-ins, routine status prompts that replace meetings, and a rule to escalate or change mode when back-and-forth runs long. These norms fit agents that wake only on prompts or schedules.

### Cited Findings
- GitLab: the handbook is "the single source of truth for how we operate". GitLab works handbook-first to promote async collaboration, and leaders should "be ruthless when it comes to instant message retention." — [GitLab handbook-first](https://handbook.gitlab.com/handbook/company/culture/all-remote/handbook-first/); [GitLab glossary](https://handbook.gitlab.com/handbook/company/culture/all-remote/remote-work-glossary)
- GitLab: when back-and-forth reaches three exchanges, the team moves to a synchronous call. Engineering suggests a one-sentence summary that links to the issue, so feedback gathers in one central place. — [GitLab Communication](https://handbook.gitlab.com/handbook/communication/); [GitLab Engineering comms](https://handbook.gitlab.com/handbook/engineering/communication/)
- Doist: a general expectation of a reply within 24 hours (the team spans 18+ time zones). Threads by default, which stay searchable; about 70% of communication is async (a third-party summary says 80–90%, which conflicts). — [Doist: Pyramid of remote team communication tools](https://www.todoist.com/inspiration/remote-team-communication-tools); [Ask Doist: immediately available](https://www.todoist.com/it/inspiration/immediately-available); [Doist: async communication](https://doist.com/blog/asynchronous-communication/)
- Basecamp: never expect or require someone to get back to you immediately unless it's a true emergency, and calls the expectation of an immediate reply toxic. — [Basecamp: How we communicate](https://basecamp.com/guides/how-we-communicate)
- Basecamp Automatic Check-ins replace status meetings. People may answer daily or bundle several days in one answer. 37signals ends each 6-week cycle with a "Heartbeat" summary post. — [Basecamp Automatic check-ins](https://basecamp.com/features/automatic-check-ins); [Jason Fried: What's in a heartbeat](https://world.hey.com/jason/what-s-in-a-heartbeat-4fd72d0e); [Signal v. Noise: Heartbeats](https://signalvnoise.com/svn3/the-tool-we-built-to-keep-everyone-in-the-loop-at-basecamp/)
- Automattic P2: blog-style async posts (not chat), cross-posting with "+p2name", and a "Thursday Updates" P2 where every team posts a status summary every two weeks. — [Automattic special projects](https://specialprojects.automattic.com/working-and-communicating-with-other-teams-at-a8c); [Goodreads/Automattic blog: 10 ways with async P2s](https://www.goodreads.com/author_blog_posts/22270149-10-ways-to-avoid-unnecessary-meetings-with-asynchronous-p2s)
- BLUF ("bottom line up front") plus subject keywords from Kabir Sehgal's 2016 HBR article: ACTION (the recipient must act), SIGN, INFO ("informational purposes only, no response or action required"), DECISION, REQUEST, COORD. The BLUF answers who, what, where, when and why. — [HBR (Sehgal 2016), via Medium reprint](https://medium.com/radiate/how-to-write-email-with-military-precision-f46bdc490873); [HBR transcript](https://course-building.s3.us-west-2.amazonaws.com/BCommMgrs/Transcripts/HowtoWriteEmailwithMilitaryPrecision_transcript.txt)
- Decision logs: Microsoft's engineering playbook keeps a design decision log alongside ADRs. — [Microsoft Code-With Engineering Playbook: decision log](https://playbook.microsoft.com/code-with-engineering/design/design-reviews/decision-log/)

### Inferences
- Request template (body), BLUF-first. Line 1 is a tag: `[ACTION]`, `[DECISION]`, `[INFO]` or `[결과]`, plus the REQ id. Then the ask in one sentence; "Done when: ..."; "Due/needed by: <real timestamp>"; "Reply: required / not needed"; context or links last. `[INFO]` rows are created by their sender directly as no-reply, which removes the "ack-only row" impulse.
- Response windows instead of an "immediately" expectation, stated per agent: cbot within 1–2 hourly checks, Claude Code agents at the next scheduled run, zu when the person next prompts it. A request that needs a faster answer must say so in "needed by".
- GitLab's three-exchange rule translates as: if a REQ thread passes about 3 back-and-forth rows without resolution, escalate to the human owner rather than adding rows.
- Single source of truth: one canonical row per request (the REQ row), with results linked via reply-to. Don't restate status in new rows.

### Gaps
- No primary source was found on the Amazon 6-pager/narrative format or on Stripe's writing culture in this pass.
- Doist's "Art of Async" handbook was referenced but not retrieved.
- No source confirmed the meaning or usage of the EOM/NRN subject conventions; only the ACTION/INFO/etc. keywords were confirmed.

## 4. Change management of rules: ADRs, changelogs, versioned working agreements, getting rule changes read

### Takeaway
Record each rule change as a short, numbered, immutable decision record (context, decision, consequences, status that later reads "superseded by #N"), keep a dated changelog with the newest entry first, and review the working agreement on a cadence. Published sources show no reliable "read receipt" mechanism; teams that need acknowledgement must define it explicitly, for example a required reaction plus follow-up with non-responders.

### Cited Findings
- Nygard's ADR template (2011): Title, Status, Context, Decision, Consequences. Status moves proposed -> accepted -> superseded, with a link to the replacing record; deprecated and rejected are also used. — [Martin Fowler: Architecture Decision Record](https://www.martinfowler.com/bliki/ArchitectureDecisionRecord.html); [Microsoft playbook ADR 0001](https://playbook.microsoft.com/code-with-engineering/design/design-reviews/decision-log/doc/adr/0001-record-architecture-decisions/)
- AWS guidance: once accepted, an ADR becomes immutable; new insights require a new ADR that supersedes the old one. — [AWS Prescriptive Guidance: ADR process](https://docs.aws.amazon.com/prescriptive-guidance/latest/architectural-decision-records/adr-process.html)
- UK government practice: if a decision is reversed, the old one is kept but marked superseded. — [GOV.UK publishing ADR 0001](https://docs.publishing.service.gov.uk/repos/static/architecture-decisions/0001-record-architecture-decisions.html); [GDS Way: architecture decisions](https://gds-way.digital.cabinet-office.gov.uk/standards/architecture-decisions.html)
- Keep a Changelog: "Changelogs are for humans, not machines"; an entry for every version; latest first; group change types (Added, Changed, Deprecated, Removed, Fixed, Security); versions linkable. — [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/)
- Atlassian working agreements: shared norms for how a team works and communicates, "a starting point that can and should be updated over time", reviewed on a set schedule such as quarterly or after milestones. — [Atlassian Team Playbook: working agreements](https://www.atlassian.com/team-playbook/plays/working-agreements)
- Slack's guidance treats emoji reactions as lightweight signals (for example, 👀 = reviewing, ✅ = done) that avoid "I agree" posts. It also recommends limiting who may post in announcement channels. It does not present reactions as formal read receipts. — [Slack: etiquette tips](https://slack.com/blog/etiquette-tips-in-slack); [Slack: 5 essential channels](https://slack.com/blog/the-5-essential-slack-channels-you-didnt-know-your-team-needed)

### Inferences
- Rules tab header: `Rules v1.3 (2026-11-24)`. Every agent's prompt instruction is "read the Rules tab version first; if the version differs from the one you last recorded, read the changelog."
- Each rule change is a row in a "Decisions" tab: id (D-007), date, status (proposed/accepted/superseded-by), context, decision, consequences. Never edit an accepted row; supersede it. This matches the sheet's existing append-only ethic and guards against the "row overwritten" failure.
- Acknowledgement of broadcasts (see Q6): explicit per-recipient acknowledgement is the only reliable signal. The sheet's "ACK = set status read" cannot represent several readers on one `모두` row, so the rule needs a per-agent ack column or per-recipient fan-out rows.
- Review the rules in 캐드's nightly retrospective, with a fuller weekly review; add a changelog entry only when something changes.

### Gaps
- No empirical study was found on how well required acknowledgement or read receipts get policy changes read.
- No Automattic- or GitLab-specific "versioned working agreement" document was found.

## 5. Daily standup / end-of-day async check-ins and digests

### Takeaway
The proven async format is a short scheduled prompt with three questions (done since last report, next, blockers), posted to a shared place. Blockers are handled after the report, not inside it. Teams may skip an update when nothing changed. A periodic heartbeat or summary post closes a cycle.

### Cited Findings
- The standard async standup questions: what did you do yesterday, what will you do today, is anything blocking you ("What (if anything) is blocking your progress?"). Answers post to a shared channel; blockers are resolved after the huddle. Vendor source. — [Geekbot: daily standup questions](https://geekbot.com/blog/daily-standup-questions/); [Geekbot daily standup template](https://geekbot.com/templates/daily-standup/)
- GitLab used Geekbot internally. GitLab's Engineering Productivity trialled replacing Slack standups with async issue comments (progress and confidence) plus a Health Status field. A daily update may be skipped if there was no progress. — [GitLab Engineering Productivity project management](https://handbook.gitlab.com/handbook/engineering/infrastructure/engineering-productivity/project-management/); [GitLab Monitor:APM RFC](https://gitlab.com/gitlab-org/monitor/general/issues/35)
- Basecamp: an automatic daily "what did you work on" question replaces status meetings, with bundling allowed; a Heartbeat summary ends each cycle. — [Basecamp Automatic check-ins](https://basecamp.com/features/automatic-check-ins); [Basecamp newsletter 2024-09-09](https://basecamp.com/newsletter/2024-09-09)
- Automattic: every team posts a status summary every two weeks to "Thursday Updates". — [Goodreads/Automattic blog](https://www.goodreads.com/author_blog_posts/22270149-10-ways-to-avoid-unnecessary-meetings-with-asynchronous-p2s)

### Inferences
- One digest row per day from 캐드's nightly retrospective (to `모두`, tagged [INFO], no reply needed). It covers open REQs by owner and age, items past their service-level expectation, blocked items, items auto-closed today, and any rule-version change. This replaces per-agent ack-only rows.
- Each agent's scheduled run may post at most one check-in row, only if something changed: Done / Next / Blocked, each with REQ ids. This follows GitLab's "skip if no progress".

### Gaps
- No controlled study comparing async vs sync standup effectiveness was found; the format evidence is practitioner and vendor material.

## 6. Email/IM overload, read receipts, no-reply-needed flags, BLUF

### Takeaway
There is evidence that batched checking lowers stress and that message volume crowds out focused work. Explicit labels such as INFO ("no response required") and ACTION cut needless replies. No source found supports read receipts as an effective acknowledgement mechanism; reactions are informal.

### Cited Findings
- Kushlev & Dunn (Computers in Human Behavior, 2015; n=124, two weeks): checking email about 3–5 times a day in batches gave significantly lower daily stress than unlimited checking. Adherence was imperfect (about 5 checks per day on average). — [UBC Psychology](https://psych.ubc.ca/news/checking-your-email-less-can-help-reduce-stress-new-study-by-phd-candidate-kostadin-kushlev/); [ScienceDaily 2014](https://sciencedaily.com/releases/2014/12/141203142644.htm); [paper on Academia.edu](https://www.academia.edu/9182785/Checking_Email_Less_Frequently_Reduces_Stress)
- Microsoft Work Trend Index 2023 (31,000 people, 31 countries): 68% say they lack enough uninterrupted focus time; email, meetings and chat take on average 57% of the workday. The 2025 data reports an interruption every 2 minutes. — [Microsoft WorkLab 2023](https://www.microsoft.com/en-us/worklab/we-can-not-keep-up-with-work-but-ai-can-help); [Microsoft WorkLab: infinite workday (2025)](https://microsoft.com/en-us/worklab/work-trend-index/breaking-down-infinite-workday); [The Register](https://www.theregister.com/2023/05/10/microsoft_work_trend_index/)
- Military-style subject keywords: INFO means "for informational purposes only, no response or action required"; ACTION means the recipient must act. BLUF means stating the purpose and required action first. — [HBR (Sehgal 2016), Medium reprint](https://medium.com/radiate/how-to-write-email-with-military-precision-f46bdc490873)
- Slack: reactions replace "I agree" posts and signal review status; they are not presented as formal read receipts. — [Slack etiquette](https://slack.com/blog/etiquette-tips-in-slack)

### Inferences
- Ack-only rows are the agent equivalent of "I agree" messages. Ban them: acknowledgement is a status change on the existing row, plus an optional `ack_by` cell for broadcasts.
- Broadcast handling. The current rule (the sender closes the broadcast) made rows vanish before others saw them. Better options:
  - (a) The broadcast stays open until every named agent has written its name or initials in an `ack_by` column, or a TTL of about 48h expires; 캐드 closes it in the nightly run.
  - (b) Fan-out: one row per recipient, each closed by its recipient.
  - Option (a) keeps the sheet smaller; option (b) fits the existing "recipient changes status" rule.
- Each agent should check the sheet on its own schedule and process all its rows in one pass (batching), rather than spawning reactive rows.
- Data hygiene for the observed defects. These do not come from a source; they are practical controls.
  - IDs: generate them as `<from>-<YYYYMMDD-HHMMSS>` or use the next integer after re-reading the last row immediately before appending.
  - Time: take the timestamp from a sheet formula or tool clock, never estimate it.
  - Recipient: validate the `to` field against a dropdown list.
  - Overwrites: append only via an append API, and protect earlier rows.

### Gaps
- Gloria Mark's email-cutoff and heart-rate-variability study could not be retrieved or verified.
- No research was found on read receipts' effect on compliance or response.
- EOM/NRN conventions were not confirmed by any source.
