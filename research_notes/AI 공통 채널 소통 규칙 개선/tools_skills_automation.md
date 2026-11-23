# Tools, skills and automation to enforce the "AI 공통 채널" protocol

Research date: 2026-11-23. Note on method: developers.google.com could not be fetched directly from this research session (the network allowlist blocked it), so the Google facts below come from search-result extracts of official Google pages plus community sources. Each item says which kind of source it came from. Claude Code routines docs were fetched in full from code.claude.com.

## 1. Google Apps Script on the sheet (triggers, auto-id, server timestamp, protection, digests, quotas)

### Takeaway
An installable **On change** trigger plus `LockService` is the cheapest way to assign ids and server timestamps on the server side, whoever wrote the row. Whether it fires for Sheets-API/connector appends is **not confirmed by Google**: community reports conflict, so the user must test it with each agent's connector. A **time-driven trigger** that runs every few minutes, scans for rows with a blank id and stamps them, is the robust fallback and needs no event delivery. Protected ranges will **not** stop the agents if they all write as the owner account.

### Cited Findings
- Official `changeType` values for change events: EDIT, INSERT_ROW, INSERT_COLUMN, REMOVE_ROW, REMOVE_COLUMN, INSERT_GRID, REMOVE_GRID, FORMAT, OTHER. `triggerUid` is populated only for installable triggers. — [Apps Script Event Objects (official, via search extract)](https://developers.google.com/apps-script/guides/triggers/events?hl=ko)
- On change fires for structural changes such as adding a row. Inside it `getActiveSheet()` returns the first sheet, and formula recalculation can arrive as `OTHER` even when no values changed. `e.range` is not available in onChange. — [simple/installable triggers guide (third-party)](https://sites.google.com/view/appsscripts/learning-development/simple-installable-triggers); [Google Groups thread](https://groups.google.com/g/google-apps-script-community/c/9a0vdqKiIUY)
- **Conflicting evidence on API writes.** Some community posts say onEdit/onChange do not fire when values are added through the API. A 2021 reply and a 2022 post report onChange **does** fire for Sheets API writes (anecdotally under the V8 runtime). Google publishes no official confirmation. — [Google Apps Script community: Triggering onChange with Sheets API](https://groups.google.com/g/google-apps-script-community/c/Hjhhe1NtWYQ); [onEdit not triggered from AppSheet](https://groups.google.com/g/google-apps-script-community/c/p1VPAi-1dZM); [Make community workaround](https://community.make.com/t/google-sheets-watch-changes-workaround/67787)
- Simple `onEdit` is widely understood to fire only for edits a user makes in the UI, not for script or API writes (consistent with the reports above). The official triggers page could not be fetched to quote, so this is **unverified here**.
- `LockService` prevents concurrent access to shared code sections. Google's own example uses it to generate unique ticket numbers without duplicates. The pattern is: get a script lock, `waitLock`/`tryLock`, append or number the row, increment a counter in Script Properties, then release the lock in `finally`. Lock waits can time out and throw. — [Class Lock (official)](https://developers.google.com/apps-script/reference/lock/lock); [Concurrency and Google Apps Script (Google blog)](https://gsuite-developers.googleblog.com/2011/10/concurrency-and-google-apps-script.html); [bettersheets LockService](https://bettersheets.co/glossary/lock-service)
- One working showcase uses an onChange trigger that finds new rows with no number and assigns auto-incremented ids inside a lock. It survived simultaneous row creation. — [Glide community showcase](https://community.glideapps.com/t/showcase-concurrent-auto-row-number-preserving-increment-even-with-deletion-of-past-rows-gs-apps-script-rowid/31361)
- Formula ids (`=ROW()-1`, COUNTA, MAX+1) shift or are reused when rows are deleted or sorted. The recommendation is a static value written once by a script. — [bulldo-gs guide](https://bulldo-gs.pages.dev/generate-a-sequential-id-for-new-rows-in-google-sheets/)
- Consumer-account quotas: 100 email recipients per day (1,500 on Workspace), triggers total runtime 90 min/day (6 h on Workspace), 6 min per execution. Google says quotas can change without notice. — [Apps Script quotas (official, via search extract)](https://developers.google.com/apps-script/guides/services/quotas?hl=en); [dev.to 2026 quota guide](https://dev.to/stack_c285afb2fa0bef/google-apps-script-quota-limits-2026-every-error-every-fix-2p87)
- Protected ranges: with "Only you", only you **and the owner** can edit. The owner always keeps full control over protections. — [Mount Marty KB reproducing Google help](https://support.mountmarty.edu/TDClient/336/Portal/KB/Article/10569/Protect-a-Range-or-Sheet-in-Google-Sheets); [U. Michigan KB](https://teamdynamix.umich.edu/TDClient/30/Portal/KB/PrintArticle?ID=10164)

### Inferences
- **Protection is unlikely to help as-is.** cbot (Claude.ai connector), 캐드/캐대 (Claude Code connector) and probably zu (ChatGPT Drive app) all authenticate as the user's personal Google account, which owns the sheet, so a protected range cannot block them. Protection would only work with separate Google accounts per agent, or with a "write-only through an Apps Script web app" design (the script runs as owner and the agents only call its URL). Neither design was verified for every agent.
- **Recommended Apps Script design (low cost, about 1 to 2 hours of setup):**
  1. Agents append rows with **id and time left blank** (or put a client draft id in a separate column).
  2. An installable On change trigger, plus a time-driven trigger every 1 to 5 minutes as a safety net, takes a script lock, fills the missing ids from a Script Properties counter (or a UUID) and writes `Utilities.formatDate(new Date(), "Asia/Seoul", ...)` as the server time. This fixes **id collisions and guessed timestamps**.
  3. The same pass validates `from`, `to`, `status` and the reply-to fields. It flags bad rows in an `error` column and copies each row's original body to a hidden audit tab, so an overwritten row can be detected and restored. This mitigates **rows overwritten**, since the history tab is append-only by script.
  4. A time-driven digest (hourly or daily) emails the human the open items: `status=new` older than X, results without `done`, and broadcasts not acknowledged by every recipient. That fits easily inside 100 recipients/day and 90 min/day of trigger runtime.
  5. Optionally the script calls `UrlFetchApp` to POST to a Claude Code routine `/fire` endpoint when a row addressed to 캐드/캐대 arrives (see section 5). That gives near-real-time **notification**, at least for the Claude Code agents.
- A Google Apps Script **web app** (doGet/doPost) could become the only write path ("append message" endpoint, server-assigned id). That would remove collisions outright, but each LLM would need to call a URL. Claude Code can do that with curl if the host is allowed. Whether ChatGPT or the Claude.ai app can call an arbitrary URL is unverified, so this mainly helps 캐드/캐대.

### Gaps
- No official Google statement on whether installable onChange fires for `spreadsheets.values.append` from the Sheets API. Some reports say yes, some say no. Test it: install the trigger, have cbot, 캐드 and zu each append one row, and check the executions log.
- Whether a `values.append` fires one INSERT_ROW or an EDIT event is not confirmed.
- UrlFetchApp daily quota for consumer accounts was not captured (the official page could not be fetched).

## 2. Built-in Sheets features (formula ids, Tables, notification rules, filter views, checkboxes)

### Takeaway
The built-in features help with **display and read tracking** but not with enforcement. Conditional notification rules are limited to certain work or school accounts, and the classic "Notification settings" are email-only and batched. Use Apps Script for anything that must be enforced.

### Cited Findings
- Conditional notifications fire when a chosen cell or column changes. They are available only to certain work or school accounts, emails are consolidated rather than instant, and changes from external sources such as Connected Sheets don't trigger them. — [Use conditional notifications (Google Docs Editors Help)](https://support.google.com/docs/answer/14099459); [Workspace Updates blog, June 2024](https://workspaceupdates.googleblog.com/2024/06/conditional-notifications-google-sheets.html)
- Marketplace add-ons such as "Sheet Notifications" (free tier with up to 2 rules) and "Check Sheet" offer any-change alerts. They are third-party and their terms were not checked. — [Sheet Notifications](https://workspace.google.com/marketplace/app/sheet_notifications/135225883697); [Check Sheet](https://workspace.google.com/marketplace/app/check_sheet_notifications/239755856136)
- `ROW()-1`-style formula ids renumber when rows are deleted or sorted. — [bulldo-gs guide](https://bulldo-gs.pages.dev/generate-a-sequential-id-for-new-rows-in-google-sheets/)

### Inferences
- Notifications go to the human's email, and no AI agent is woken by them, so this does not solve "agents only check on schedule".
- **Broadcast read tracking:** add one checkbox column per agent (`ack_zu`, `ack_cbot`, `ack_캐드`, `ack_캐대`, `ack_meti`), or one "acks" tab with one row per (message id, agent). The tab form is safer for concurrent writes because each agent appends its own row instead of editing a shared row. The script digest then lists broadcasts with missing acks. A per-recipient ack also replaces "ack-only message rows" in the main tab.
- Filter views per agent (`to` contains the agent name OR `to = all`, `status != done`) help the human review but do not affect agents that read through the API.
- Data validation (dropdowns for status/from/to) applies to UI entry. Whether it rejects API writes was not verified. Assume it does not, and have the Apps Script pass flag violations.

### Gaps
- Could not confirm whether the consumer "Tools > Notification settings" (any change, email) is still offered for personal accounts in 2026.
- Could not check whether Sheets "Tables" (2024 feature) enforce column types for API writes.

## 3. Shared "channel protocol" Skill (Claude.ai / Claude Code) and ChatGPT equivalents

### Takeaway
A single SKILL.md can be uploaded as a custom skill in Claude.ai (for cbot) and committed into the GitHub repo under `.claude/skills/` (for 캐드/캐대, whose cloud sessions and routines load repo skills). ChatGPT now also supports SKILL.md skills, but only on Business, Enterprise, Edu and Healthcare plans. On a personal plan, zu would use Custom GPT or Project instructions instead. A skill improves consistency but cannot enforce anything. Server-side checks (section 1) are still needed.

### Cited Findings
- Claude.ai: Customize > Skills > "+" > Upload a skill (a ZIP of the skill folder). Uploaded custom skills are private to your account. The description is limited to 200 characters, and the folder name must match the skill name. — [Use skills in Claude (Claude Help Center)](https://support.claude.com/en/articles/12512180-using-skills-in-claude-ai)
- Organization sharing and provisioning exists on Team/Enterprise (owner toggles, "Requires review"). — [Provision and manage skills for your organization](https://support.claude.com/en/articles/13119606-provision-and-manage-skills-for-your-organization)
- Agent Skills overview: a folder holding SKILL.md (name, description, instructions) plus optional scripts and resources, loaded on demand. — [Agent Skills docs](https://docs.claude.com/en/docs/agents-and-tools/agent-skills); [Introducing Agent Skills](https://www.anthropic.com/research/skills)
- Claude Code scheduled tasks (routines) "run as full Claude Code cloud sessions... use skills... including the ones committed to the cloned repository". — [Claude Code docs: scheduled tasks](https://code.claude.com/docs/en/routines)
- OpenAI: ChatGPT and Codex support SKILL.md skills that follow the Agent Skills open standard, so skills are portable between products. Personal skills don't sync between desktop and web/mobile. Skills are available to eligible ChatGPT **Business, Enterprise, Healthcare and Edu** users. — [Skills in ChatGPT (OpenAI Help)](https://help.openai.com/en/articles/20001066-skills-in-chatgpt); [OpenAI build skills docs](https://developers.openai.com/ja-JP/docs/build-skills); [Simon Willison on OpenAI skills, Dec 2025](https://simonwillison.net/2025/Dec/12/openai-skills/)
- A single user report (May 2026) says Business skills appeared enabled but were not available at runtime. Unconfirmed. — [OpenAI community thread](https://community.openai.com/t/chatgpt-business-skills-appear-installed-enabled-in-ui-but-are-not-available-at-runtime/1380754)
- An existing skill to reuse or adapt: **mcp_agent_mail** ships a SKILL.md for agent messaging etiquette (inbox check, ack, threads). — [SKILL.md on jsDelivr](https://cdn.jsdelivr.net/gh/dicklesworthstone/mcp_agent_mail@main/SKILL.md); [Smithery listing](https://smithery.ai/skills/dicklesworthstone/agent-mail)

### Inferences
- Suggested "ai-channel-protocol" skill contents:
  - read inbox (`to` = me or `all`, status new) before acting;
  - append only, never edit other agents' rows, change only your own ack/status;
  - leave id and time blank for the script to fill, or use the server time from a cell such as `=NOW()` read back;
  - a reply always sets `reply-to` and the `subject` request ID;
  - close the loop with `status=done` or `canceled`;
  - acknowledge with an ack cell, not a new row;
  - re-read the row after writing to verify it.
- Keep one canonical SKILL.md in the GitHub repo. cbot's copy must be re-uploaded as a ZIP whenever it changes, which is a manual sync risk. zu's copy has to be pasted into Custom GPT or Project instructions.
- meti (company Gemini, reads one Google Doc only) cannot run a skill. An Apps Script export of the "messages to meti" rows into that Doc (time-driven) would keep meti in the loop. That is feasible with DocumentApp, though not separately verified.

### Gaps
- Did not verify whether Claude.ai personal (Pro/Max) custom skills apply inside **Claude.ai scheduled tasks** (cbot's hourly task). This is likely but unconfirmed.
- No dedicated Anthropic-official "team comms/kanban" skill was found. The searches focused on agent-mail.
- Custom GPT or Project instructions limits for zu were not re-checked for 2026.

## 4. MCP servers / platforms for agent-to-agent messaging: stay on the Sheet or move?

### Takeaway
Agent-native mailboxes such as **mcp_agent_mail** solve ids, threads, acks and inboxes properly, but they are self-hosted HTTP servers aimed at coding agents on one machine. ChatGPT and the Claude.ai app could reach them only as remote MCP connectors (ChatGPT full MCP/developer mode is Business/Enterprise/Edu only), so they don't fit the current mix. **GitHub Issues** is the strongest alternative that all Claude surfaces can reach, with server ids, timestamps, threads, close state and notifications. ChatGPT's access to write to GitHub could not be confirmed. Linear and Notion now have write-capable ChatGPT apps. meti still needs a Doc mirror in any option.

### Cited Findings
- **MCP Agent Mail** (Dicklesworthstone): an HTTP FastMCP server giving agents identities, inbox/outbox, searchable threaded Markdown messages, acknowledgements, file-reservation leases, contact policies and "Human Overseer" messages, backed by Git and SQLite. It runs locally on port 8765. A Rust port exposes 40 tools and 25 resources. — [GitHub repo](https://github.com/Dicklesworthstone/mcp_agent_mail); [PyPI](https://pypi.org/project/mcp-agent-mail/); [Rust port](https://github.com/Dicklesworthstone/mcp_agent_mail_rust); [Glama listing](https://glama.ai/mcp/servers/@Dicklesworthstone/mcp_agent_mail)
- Similar but unrelated: `agent-inbox` (a SQLite mailbox MCP for local agents), and the `mcp-mail` fork. — [agent-inbox PyPI](https://pypi.org/project/agent-inbox/0.2.1/); [mcp-mail PyPI](https://pypi.org/project/mcp-mail/)
- ChatGPT: full MCP support including write actions (developer mode) is in beta for **Business, Enterprise and Edu**, enabled by admins. Write actions may ask for confirmation, and some risky actions are blocked. — [Developer mode and full MCP connectors in ChatGPT](https://help.openai.com/en/articles/12584461-developer-mode-and-full-mcp-connectors-in-chatgpt)
- Linear and Notion ChatGPT apps were updated (March 2026) with write capabilities where supported. App availability depends on plan, region and workspace. The app directory moved to a "Plugin directory" on 2026-07-09. By default ChatGPT asks before consequential actions. — [OpenAI community: Updated Box, Notion, Linear, Dropbox apps](https://community.openai.com/t/updated-box-notion-linear-and-dropbox-apps/1378101); [Apps in ChatGPT (OpenAI Help)](https://help.openai.com/en/articles/11487775-apps-in-chatgpt)
- ChatGPT event-triggered tasks can react to Gmail, Slack or GitHub activity (Plus, Pro, Business, Enterprise, Edu; not Free or Go). — [Tasks in ChatGPT (OpenAI Help)](https://help.openai.com/en/articles/10291617-scheduled-tasks-in-chatgpt)
- Claude Code scheduled tasks can use any claude.ai connector (Slack, Linear and others are cited as examples), and GitHub triggers start a session on repository events. — [Claude Code docs: scheduled tasks](https://code.claude.com/docs/en/routines)

### Inferences
- **GitHub Issues as the channel:**
  - Strengths: one issue per request, so the subject/request id is the issue number, assigned server-side with no collisions; comments are replies with server timestamps; labels or assignees replace `to`; close/reopen replaces status; history cannot be silently overwritten because edits are logged.
  - 캐드/캐대 already have GitHub, and a routine **GitHub trigger** can wake them on new issues or comments, which would fix the notification problem for them.
  - ChatGPT event-triggered tasks list GitHub activity as a trigger, which may let zu react too. Whether ChatGPT can *write* issues/comments on a personal plan was not verified.
  - cbot (Claude.ai) has a GitHub connector for reading. Write support was not verified.
  - meti cannot reach it, so a Doc mirror is still needed.
- **Slack:** both ChatGPT event-triggered tasks and Claude connectors cover Slack, so it could serve as the notification layer while the Sheet stays the record. That adds a workspace to maintain.
- **Recommendation for this setup:** keep the Sheet (every agent already reaches it, and meti's Doc mirror is simple). Add the Apps Script enforcement layer and, if needed, use GitHub issues or routine fires only as the *wake-up* channel for Claude Code agents. A full migration has a higher switching cost and uncertain ChatGPT write access.
- A2A (Agent2Agent) SDKs need each agent to run as a server endpoint. None of these consumer chat products expose that, so A2A is not practical here (inference; not researched in depth).

### Gaps
- Not verified: whether the Claude.ai app's GitHub connector can create issues or comments on a personal plan; whether ChatGPT Plus has a GitHub write app as of late 2026.
- No reliable source found for a hosted multi-vendor "agent mailbox" that ChatGPT (personal plan), Claude.ai and Claude Code can all reach without developer mode.

## 5. Workflow automation and wake-ups (n8n, Zapier, Make, Claude Code routine fire, ChatGPT tasks)

### Takeaway
Real-time "wake the recipient" is fully possible only for the **Claude Code agents**, through the routine API trigger: Apps Script, Zapier, Make or n8n can POST to `/fire` when a row addressed to them appears. ChatGPT scheduled tasks run at most hourly, and the Claude.ai app has no inbound webhook found. External automation platforms poll Sheets every 5 to 15 minutes. Apps Script calling `/fire` directly is the cheapest path ($0).

### Cited Findings
- Claude Code scheduled tasks (formerly routines) have three trigger types: Schedule (minimum hourly, custom cron allowed down to hourly), **API** (HTTP POST to the task's endpoint with a bearer token) and GitHub events. They are available on Pro, Max, Ultra, Team and Enterprise, and in research preview. — [Claude Code docs: scheduled tasks](https://code.claude.com/docs/en/routines)
- How `/fire` works:
  - The endpoint is `POST https://api.anthropic.com/v1/claude_code/routines/{routine_id}/fire` with `Authorization: Bearer <token>` and the beta header `experimental-cc-routine-2026-04-01`.
  - The optional `text` field is delivered wrapped as an untrusted `<routine-fire-payload>`, so the saved prompt must explicitly tell Claude to act on it.
  - The token is shown once. Regenerating it revokes the old one.
  - There is no idempotency key, so a retrying caller can start duplicate sessions.
  
  — [Claude Code docs: scheduled tasks](https://code.claude.com/docs/en/routines); [Trigger a routine through the API (Claude Platform docs)](https://platform.claude.com/docs/en/api/claude-code/routines-fire)
- Limits (no overage on these):
  - 100 scheduled runs per hour per account.
  - Run-now plus API fires: 30 per hour per task.
  - API fires: 100 per hour per account.
  - Runs consume subscription usage.
  - Connectors (for example Google Sheets) are included by default and work without network allowlisting.
  
  — [Claude Code docs: scheduled tasks](https://code.claude.com/docs/en/routines)
- Scheduled-task actions through connectors appear as the user. — [Claude Code docs: scheduled tasks](https://code.claude.com/docs/en/routines)
- ChatGPT Tasks:
  - Active task caps: Free/Go 3, Plus 5, Business/Edu 10, Pro/Enterprise 15. Older help pages give different Business numbers.
  - A task cannot run more than once per hour.
  - Tasks can use connected apps.
  - Event-triggered tasks (Gmail, Slack, GitHub) are available on Plus and above.
  
  — [Tasks in ChatGPT (OpenAI Help)](https://help.openai.com/en/articles/10291617-scheduled-tasks-in-chatgpt)
- n8n's Google Sheets Trigger polls, with no push. Polling every minute causes Google API rate-limit errors, and 5 to 10 minutes is advised. Workato's minimum is 5 minutes. — [n8n community](https://community.n8n.io/t/google-sheets-trigger-not-checking-for-new-data/30353); [Workato docs](https://docs.workato.com/connectors/google-sheets/trigger-new-row-my-drive.html)
- Pricing:
  - Zapier Free is $0 for 100 tasks per month with two-step Zaps and 15-minute polling.
  - Make Free gives 1,000 credits per month with 2 active scenarios.
  - Make Core is quoted at $9 or $12 per month depending on the source.
  
  — [Zapier pricing](https://zapier.com/pricing); [Latenode on Zapier free plan](https://latenode.com/blog/zapier-free-plan); [Zapier blog: Make pricing](https://zapier.com/blog/make-com-pricing/)
- A Zapier template emails a daily digest of Google Sheets updates (Digest by Zapier). — [Zapier template](https://zapier.com/templates/details/8b93a3fb441e462090b212a791cb0b69dfe8ddb97185ac9626aca3295897d811)

### Inferences
- **Recommended wake-up wiring (about $0):**
  1. In the Apps Script On change or time-driven pass, for each new row whose `to` includes 캐드 or 캐대 (or `all`), call `UrlFetchApp.fetch(fireUrl, {method: "post", headers: {Authorization: "Bearer " + token, "anthropic-beta": "experimental-cc-routine-2026-04-01"}, payload: JSON.stringify({text: "new message id=..."})})`.
  2. Store tokens in Script Properties.
  3. Mark the row as `notified_at` so a retry does not double-fire, since there is no idempotency key.
  4. The routine prompt says: "read message id in routine-fire-payload from the sheet, handle it, set status".
- cbot and zu remain schedule-driven: Claude.ai hourly scheduled task, and ChatGPT Task hourly at most (5 active on Plus). The mitigation is the email digest to the human plus strict "close the loop" rules.
- ChatGPT event-triggered tasks on Gmail mean the Apps Script could **email** a message to the user's Gmail with a recognizable subject (for example `[AI채널→zu]`) to wake zu. This is plausible but **unverified**: it depends on the event-triggered task's filter options and plan availability, and it uses the 100 recipients/day quota.
- Zapier or Make add little over Apps Script here. The free tiers (100 tasks, 15-minute polling; 1,000 credits) would be exhausted quickly by busy traffic, and they add another account holding Google OAuth. n8n self-hosted is free but needs a server.
- Risks:
  - Routines are research preview, so the API and beta header can change.
  - A leaked fire token lets anyone start runs, although its payload is marked as untrusted.
  - Each fire consumes Claude subscription usage.
  - Ping-pong loops are possible between agents that reply to each other. Add a rule that no wake fires for rows with `status=done` or for ack rows, and cap fires per hour in the script.

### Gaps
- Not verified: whether Claude.ai (non-Code) scheduled tasks have any API or webhook trigger. None was found, so cbot is assumed schedule-only.
- Not verified: the exact filter options of ChatGPT Gmail event-triggered tasks.
- No official n8n doc on the minimum poll interval was retrieved.
