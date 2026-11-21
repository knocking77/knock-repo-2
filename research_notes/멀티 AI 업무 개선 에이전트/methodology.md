# Methodology for a Nightly "Work-Method Improvement Agent"

Scope: evidence-based methods for an agent that watches one knowledge worker's way of working, reflects on it, proposes small changes, tests them, and avoids bad metrics. The notes are drawn from web searches done 2026-11-21. Search snippets and abstracts were the main evidence; full texts were not opened unless noted. Figures should be checked against the primary papers before they are published.

## 1. What does research say about measuring knowledge-worker productivity, and which metrics mean something vs. mislead (Goodhart's law)?

### Takeaway
No single metric captures knowledge-work productivity. The SPACE framework (Microsoft/GitHub, 2021) recommends tracking several dimensions together, with at least one perceptual measure such as satisfaction or a "good day" rating next to the activity measures. Activity counts (commits, PRs, messages, hours) are the most common measures and the most often misused. Once a measure becomes a target it gets gamed or drifts away from the real goal, so the agent should use metrics to diagnose, never as scores to optimize.

### Cited Findings
**SPACE framework**
- "The SPACE of Developer Productivity" (Forsgren, Storey, Maddila, Zimmermann, Houck, Butler; ACM Queue 2021, reprinted in CACM June 2021) argues that productivity cannot be reduced to one dimension or one metric. It proposes five dimensions: Satisfaction and well-being, Performance, Activity, Communication and collaboration, and Efficiency and flow. — [ACM Queue](https://queue.acm.org/detail.cfm?id=3454124); [CACM](https://cacm.acm.org/practice/the-space-of-developer-productivity)
- SPACE stresses balancing individual, team, and organizational measures and understanding the trade-offs between them. — [CACM](https://cacm.acm.org/practice/the-space-of-developer-productivity)
- Commentary on SPACE notes that activity is the most common productivity measure and often the most misused. — [GitHub blog](https://github.blog/news-insights/research/measuring-enterprise-developer-productivity/)

**DORA, and AI as a confound**
- DORA 2024: a 25% increase in AI adoption was associated with an estimated 1.5% drop in delivery throughput and a 7.2% drop in delivery stability. It was also associated with a 3.4% rise in code quality and a 3.1% rise in code-review speed. One proposed explanation is that AI produces larger changes, and larger changes are less stable. These are correlational survey estimates. — [Google Cloud blog](https://cloud.google.com/blog/products/devops-sre/announcing-the-2024-dora-report); [DORA 2024 report PDF](https://services.google.com/fh/files/misc/2024_final_dora_report.pdf); [InfoQ](https://www.infoq.com/news/2024/11/2024-dora-report/)
- DORA 2025, as reported by TechTarget: AI coding tool use no longer correlated with throughput slowdowns, but stability problems remained. — [TechTarget](https://www.techtarget.com/searchsoftwarequality/news/366631712/Google-DORA-Software-delivery-caught-up-to-AI-coding-tools)

**Flow, interruptions, and the "good day"**
- Meyer, Barr, Bird, Zimmermann, "Today was a Good Day" (IEEE TSE; about 5,971 Microsoft developer responses): developers spend little of their time on actual development, and they dislike meetings and interruptions. Longer stretches of uninterrupted time were linked to higher perceived workday quality. — [Microsoft Research](https://microsoft.com/en-us/research/?p=578803); [UCL PDF](https://discovery.ucl.ac.uk/10090507/1/Barr_Today%20was%20a%20Good%20Day.%20The%20Daily%20Life%20of%20Software%20Developers_AAM2.pdf)
- GitHub "Good Day Project" (40 engineers, daily self-ratings):
  - Developers had an 82% chance of a good day with minimal interruptions, and a 7% chance when interrupted for most of the day.
  - Going from two to three meetings a day cut the chance of progress toward goals from 74% to 14%.
  - Good days were the days developers reached flow.
  - [GitHub blog](https://github.blog/news-insights/research/octoverse-spotlight-good-day-project/)
- Gloria Mark (UC Irvine):
  - Average attention on one screen fell from about 2.5 minutes (2003–04) to about 75 seconds (around 2012) to about 47 seconds (mean; recent median about 40 seconds).
  - People interrupt themselves about as often as others interrupt them.
  - Returning to a project after an interruption can take up to about 25 minutes. I found no source for the often-quoted "23 minutes."
  - [APA podcast](https://www.apa.org/news/podcasts/speaking-of-psychology/attention-spans); [UC Irvine Informatics](https://www.informatics.uci.edu/?p=7800); [UC news](https://www.universityofcalifornia.edu/news/cant-pay-attention-youre-not-alone)
- Amabile and Kramer's diary study (238 professionals, 7 companies, about 12,000 daily entries; Dataverse lists 11,637): of all positive events, progress in meaningful work had the strongest effect on "inner work life" (emotions, perceptions, motivation). Small wins had a surprisingly large positive effect, and small setbacks a surprisingly large negative one. — [HBS Working Knowledge](https://www.library.hbs.edu/working-knowledge/how-small-wins-unleash-creativity); [Harvard Dataverse](https://dx.doi.org/10.7910/DVN/25463)

**Goodhart's and Campbell's laws**
- Goodhart's 1975 point came from monetary policy. The popular phrasing "when a measure becomes a target, it ceases to be a good measure" is a later paraphrase usually credited to Strathern (1997); Wikipedia credits it to Hoskin. — [Wikipedia](https://en.wikipedia.org/wiki/Goodhart%27s_law)
- Campbell's law (formulated from 1969 on): the more weight a quantitative social indicator carries in decisions, the more it gets corrupted and the more it distorts the process it is meant to monitor. — [NN/g](https://www.nngroup.com/articles/campbells-law/); [Manheim & Garrabrant, MPRA](https://mpra.ub.uni-muenchen.de/98288/1/MPRA_paper_98288.pdf)
- Manheim's paper lists three ways a measure fails:
  1. It is poorly aligned with the true goal.
  2. The system rewards cheating.
  3. It stays in use after underlying changes have made it irrelevant.
  - [MPRA](https://mpra.ub.uni-muenchen.de/98288/5/MPRA_paper_98288.pdf)

**Self-reports can be wrong**
- In METR's 2025 RCT, developers believed AI had sped them up by about 20% when it had actually slowed them by 19% (see Q4). — [METR](https://metr.org/blog/2025-07-10-early-2025-ai-experienced-os-dev-study/)

### Inferences
- **Metrics that tend to be meaningful for one person:**
  - A daily good-day or progress self-rating (Amabile; GitHub Good Day).
  - Length of uninterrupted focus blocks and number of context switches (Mark; Meyer).
  - Meeting count and fragmentation of the day.
  - Progress on the person's own stated goals.
  - Rework and verification time spent on AI output.
- **Metrics that tend to mislead when optimized:** raw activity counts (lines of code, PRs, emails sent, hours logged, number of AI tasks delegated). They are fine as context, but they should not be targets.
- **Design rules for the agent:**
  - Pair every activity metric with an outcome or perception metric (the SPACE pairing).
  - Never report a single composite score.
  - Retire metrics after their underlying conditions change (Manheim's third failure mode). For example, re-baseline when the AI toolset changes.
  - Self-reported speedups are not reliable evidence, especially for AI tools. Where possible, triangulate a self-report with objective timestamps.

### Gaps
- I found no primary source on Drucker's 1999 "Knowledge-Worker Productivity" (California Management Review) during this session. Its six factors, such as "what is the task?" and autonomy, are widely cited, but they are not verified here.
- I found no validated individual-level (as opposed to team-level) version of DORA. DORA measures team delivery, not personal work.

## 2. Self-tracking and personal informatics: what makes personal reflection lead to behavior change?

### Takeaway
Li et al.'s five-stage model (Preparation → Collection → Integration → Reflection → Action) shows that problems in early stages cascade into later ones. Many tools stop at a dashboard and never support the Action stage. The "lived informatics" work shows that people lapse and resume tracking, so a system should expect gaps. Workplace self-monitoring (Meyer et al.) and conversational reflection prompts (Kocielnik et al.) both raised awareness and motivated change in field studies. Reflection is a prerequisite for change, but it does not guarantee it.

### Cited Findings
**Stage models**
- Li, Dey, Forlizzi, "A Stage-Based Model of Personal Informatics Systems" (CHI 2010) has five stages: preparation, collection, integration, reflection, action. The stages have four properties:
  - Barriers cascade to later stages.
  - The stages are iterative.
  - They can be user-driven or system-driven.
  - They can be uni-faceted or multi-faceted.
  - [Paper PDF](https://courses.cs.washington.edu/courses/cse440/15au/readings/PersonalInformatics-Li2010.pdf); [Author page](https://ianli.owlstown.net/publications/17-a-stage-based-model-of-personal-informatics-systems)
- Epstein, Ping, Fogarty, Munson, "A Lived Informatics Model of Personal Informatics" (2015; UbiComp per the ACM DOI 10.1145/2750858.2804250): extends Li et al. with deciding to track, selecting tools, lapsing, and resuming. — [Paper PDF](https://my.eng.utah.edu/~cs5540/au16/readings/PersonalInformatics-Epstein2015.pdf); [ACM listing](https://mijn.bsl.nl/doi/10.1145/2750858.2804250)
- Follow-up work ("Beyond Abandonment to Next Steps"; survey of 193 people, 12 interviews) found six reasons people stop tracking and five perspectives on life after tracking. — [Epstein dissertation, UW](https://digital.lib.washington.edu/researchworks/items/e42bf659-7059-4e33-8d6e-e72ca7127617)

**Workplace self-monitoring**
- Meyer, Murphy, Zimmermann, Fritz, "Design Recommendations for Self-Monitoring in the Workplace" (PACM HCI, CSCW 2017):
  - Design elements came from iterative development with 20 users and a survey of 413.
  - The tool, WorkAnalytics/PersonalAnalytics, was field-tested with 43 developers for three weeks.
  - Self-monitoring combined with experience sampling (short self-reports) raised awareness and motivated many participants to improve.
  - A wide variety of metrics was needed to meet different developers' expectations.
  - [ZORA PDF](https://www.zora.uzh.ch/server/api/core/bitstreams/f6202968-ef6f-406e-8cec-df836cf77231/content); [UZH project page](https://www.ifi.uzh.ch/en/seal/people/meyer/personal-analytics.html)

**Conversational reflection**
- Kocielnik, Xiao, Avrahami, Hsieh, "Reflection Companion" (IMWUT 2018):
  - The system sent daily adaptive mini-dialogues with graphs.
  - Workshops produced 275 prompts, which were condensed into 25 mini-dialogues.
  - The dialogues triggered reflection, and reflection was linked to higher motivation, empowerment, and adoption of new behaviors.
  - The study was small and self-reported.
  - [Paper PDF](https://faculty.washington.edu/garyhs/docs/kocielnik-UBICOMP2018-reflection.pdf)

**Goals before data**
- Munson et al. (Frontiers in Digital Health, 2020) criticize "data-first" self-experiment tools that do not start from the person's goals. — [Frontiers PDF](https://www.frontiersin.org/journals/digital-health/articles/10.3389/fdgth.2020.00003/pdf)

### Inferences
- **The agent should cover the whole loop, not just show data:**
  - Preparation: elicit the person's goals.
  - Collection: gather data passively, such as calendar, app usage, and AI-session logs.
  - Integration: combine the sources.
  - Reflection: ask one or two short prompts instead of showing a dashboard.
  - Action: suggest a concrete change.
- **Ask, do not tell:** short conversational prompts ("What made today's best hour possible?") are better supported than static reports.
- **Expect lapses:** tolerate missing days, and resume tracking without guilt-inducing messages.

### Gaps
- I found no RCT showing that personal work analytics lead to long-term (more than 3 months) productivity gains. The evidence is mostly short field studies with self-reported outcomes.

## 3. Continuous-improvement loops (PDCA, kaizen, AAR, retrospectives, N-of-1): how to structure small, testable experiments for one person

### Takeaway
Structured debriefs (AAR-style) have solid meta-analytic support, roughly a 20–25% performance improvement and d of about 0.67–0.79. Single-case or N-of-1 designs show how to test one change at a time by alternating "on" and "off" periods. Systems like SleepCoacher already automate this loop for individuals. Behavior changes need weeks to become automatic, so judging an experiment after one night will produce noise.

### Cited Findings
**Debriefs and after-action reviews**
- Tannenbaum and Cerasoli's meta-analysis (Human Factors, 2013): properly conducted debriefs improve individual and team performance by about 20–25%, and they are a cheap, quick intervention. — [PubMed](https://pubmed.ncbi.nlm.nih.gov/23516804/); [CEBMA PDF](https://cebma.org/assets/Uploads/Tannenbaum-Cerasoli.pdf)
- Keiser and Arthur (2021; 61 studies): AARs gave an overall improvement of d = 0.79. Effectiveness rose when the review was aligned to the right level (individual vs. team) and used objective performance review media, such as recordings or data, instead of memory alone. — [Springer, J. Business & Psychology 2023 follow-up](https://link.springer.com/article/10.1007/s10869-023-09912-9); [summary](https://academicnewsletter.sufe.edu.cn/info/369459)

**Single-case and N-of-1 self-experiments**
- Karkar et al. (JAMIA 2015) give a framework for running single-case designs as self-experiments. In the TummyTrials case, each experiment produces a confidence value. — [PMC](https://pmc.ncbi.nlm.nih.gov/articles/PMC6095104)
- QuantifyMe (Taylor et al., Sensors 2018): well-designed single-case experiments are hard for novices to run on themselves, so tool support helps. — [MIT DSpace PDF](https://dspace.mit.edu/bitstream/1721.1/116285/1/sensors-18-01097.pdf)
- SleepCoacher (Daskalova et al., UIST 2016):
  - Recommendations came from 117 clinician-designed templates.
  - Users followed a recommendation for several nights, then ignored it for several nights.
  - Sleep improved as adherence increased (studies with 24 and 19 participants).
  - The follow-up SleepBandits (CHI 2020) supports flexible self-experiments.
  - [UIST PDF](https://jeffhuang.com/papers/SleepCoacher_UIST16.pdf); [Project site](https://sleepcoacher.cs.brown.edu)

**Habit formation**
- Lally et al. (Eur. J. Social Psychology, 2010; 96 participants): reaching automaticity took 66 days on average, ranging from 18 to a predicted 254 days. Missing one day did not derail habit formation, but very inconsistent practice did. — [UCL news](https://www.ucl.ac.uk/news/2009/aug/how-long-does-it-take-form-habit); [UCL BSH blog](https://blogs.ucl.ac.uk/bsh/2012/06/29/busting-the-21-days-habit-formation-myth/.)

### Inferences
**Experiment template for the agent** (my synthesis of AAR and N-of-1 practice, in PDCA style):
1. Observation with evidence, for example "3 of 5 days had no focus block of 90 minutes or more."
2. A hypothesis.
3. One change, for example "block 9–11 a.m., notifications off."
4. A primary outcome plus a guardrail metric, for example a good-day rating plus a check that response time to the team has not degraded.
5. A design: alternate ON and OFF days or weeks (A-B-A-B), or a baseline week then an intervention week.
6. A minimum duration of at least 1–2 weeks for tactical changes, and longer for habits.
7. A decision rule: adopt, adapt, or drop.

**Running experiments**
- Run only one or two experiments at a time so effects can be attributed.
- Use the AAR questions in the nightly reflection: "What was supposed to happen? What actually happened? Why the difference? What will I do differently?" The AAR evidence favors objective review media, so use logs and data, not just memory.

### Gaps
- I did not retrieve primary sources for PDCA (Shewhart/Deming), kaizen, or agile retrospectives (Derby and Larsen's five-stage retro). They are well-known practitioner frameworks, but no citations were gathered here.
- I found no empirical study of N-of-1 experiments applied specifically to knowledge-work methods, as opposed to health.

## 4. LLM self-reflection loops, and when delegating to AI helps or hurts (handoff costs, verification overhead)

### Takeaway
LLM self-reflection works when it is grounded in external feedback such as test results, outcomes, or user ratings. Reflection without that feedback often fails and can make results worse. So a nightly agent's reflections must be anchored in observed data and the person's feedback, not in the model's own judgment. On delegation, the evidence is "jagged":
- AI helps on tasks inside its capability frontier.
- It hurts on tasks outside it, and when experts work in familiar, complex contexts.
- It shifts human work from producing to verifying.
- People systematically overestimate how much AI speeds them up.

### Cited Findings
**LLM self-reflection**
- Reflexion (Shinn et al., 2023) turns feedback from the environment into a written reflection that the agent carries into its next attempt. It improved AlfWorld results by 22 points (absolute) over 12 iterations. — [arXiv](https://arxiv.org/abs/2303.11366v4)
- Huang et al., "Large Language Models Cannot Self-Correct Reasoning Yet": without external feedback, LLMs struggle to self-correct, and performance can degrade. The authors note that Reflexion-style gains rely on oracle or ground-truth feedback. — [arXiv](https://arxiv.org/html/2310.01798v1)
- Kamoi et al.'s survey, "When Can LLMs Actually Correct Their Own Mistakes?": self-correction works when reliable external feedback is available. It classes Reflexion as oracle-based, not intrinsic. — [arXiv](https://arxiv.org/html/2406.01297)

**When delegating to AI helps or hurts**
- Dell'Acqua et al., "Navigating the Jagged Technological Frontier" (758 BCG consultants, preregistered RCT, GPT-4):
  - Inside the frontier: 12.2% more tasks completed, quality up more than 30%, about 25% faster.
  - Outside the frontier (a problem with a clear right answer): AI users were 23% less likely to be correct.
  - The group trained on prompting over-relied on AI more.
  - [SSRN/MIT PDF](https://mitsloan.mit.edu/sites/default/files/2023-10/SSRN-id4573321.pdf); [BCG](https://www.bcg.com/publications/2023/how-people-create-and-destroy-value-with-gen-ai); [HBS](https://www.library.hbs.edu/working-knowledge/humans-vs-machines-untangling-the-tasks-ai-can-and-cant-handle)
- METR RCT (2025; 16 experienced open-source developers, 246 tasks, mostly Cursor with Claude 3.5/3.7):
  - AI made tasks 19% slower.
  - Developers had forecast a 24% speedup, and afterward still believed AI had saved them 20%.
  - Contributing factors: time spent prompting, waiting on, and reviewing AI output; deep familiarity with their own repositories; and large, complex codebases.
  - [METR](https://metr.org/blog/2025-07-10-early-2025-ai-experienced-os-dev-study/); [arXiv](https://arxiv.org/pdf/2507.09089)
- METR's February 2026 update: developers are likely sped up more in early 2026. The new estimate for newly recruited developers is -4% (confidence interval -15% to +9%), with selection-effect caveats. — [METR update](https://metr.org/blog/2026-02-24-uplift-update/)
- Peng et al. (2023) controlled experiment: Copilot users built an HTTP server in JavaScript 55.8% faster (CI 21–89%; 71 vs. 161 minutes). This was a well-bounded greenfield task. — [arXiv](https://arxiv.org/pdf/2302.06590)
- Cui et al.'s field experiments (4,867 developers; about 26% more completed tasks, larger gains for juniors) are reported only through a secondary source and not verified. — [secondary: arXiv 2502.13199](https://arxiv.org/pdf/2502.13199)
- Simkute, Tankelevitch et al., "Ironies of Generative AI" (Microsoft Research; IJHCI 2024), apply the "ironies of automation" to GenAI. They give four reasons for productivity loss:
  1. A role shift from producing to evaluating.
  2. Unhelpful restructuring of workflows.
  3. Interruptions.
  4. Automation making easy tasks easier and hard tasks harder.
  - Suggested mitigations: continuous feedback, personalization, task stabilization, and clear task allocation.
  - [arXiv](https://arxiv.org/abs/2402.11364v1); [Microsoft Research](https://www.microsoft.com/en-us/research/?p=1010463)

### Inferences
- **Ground the agent's reflection:** reflect only on logged facts (timestamps, task outcomes, rework, the user's ratings) and on whether past suggestions were adopted. Do not rely on the model critiquing itself without feedback. Keep a Reflexion-style "lessons memory" of what worked for this user and what did not.
- **Track delegation outcomes per task type:** delegation-to-done time, verification and rework time, and acceptance or rejection rate. Use these to map the user's personal "jagged frontier," meaning which task types to delegate to which AI and which to keep.
- **Flag the danger signs:**
  - Long review loops.
  - Repeated re-prompting.
  - Expert work on a familiar, complex codebase.
  - Tasks with a single correct answer where AI output goes unchecked.
- **Distrust felt speedups:** given the METR perception gap, test any "AI saves me time" belief with an ON/OFF comparison when it matters.

### Gaps
- I found no rigorous study of multi-AI orchestration, where one person delegates across several agents at once, or of its handoff and context-switching costs specifically. The closest evidence is the general interruption research and the "Ironies" paper.

## 5. Cadence (nightly vs. weekly), and keeping suggestions actionable and few

### Takeaway
Daily capture is well supported: daily diaries (Amabile), daily good-day ratings (GitHub), daily mini-dialogues (Kocielnik). Judging whether a change worked needs multi-day or multi-week windows (SleepCoacher's multi-night alternation; Lally's weeks to habit). The best structure is a light nightly check-in combined with a weekly evaluation and decision. Suggestions should be one or two small, concrete changes with a clear test, not long lists.

### Cited Findings
- Daily diary entries captured the "small wins" that drive motivation. — [HBS Working Knowledge](https://www.library.hbs.edu/working-knowledge/how-small-wins-unleash-creativity)
- The GitHub Good Day project used daily self-ratings to link interruptions and meetings to good days. — [GitHub blog](https://github.blog/news-insights/research/octoverse-spotlight-good-day-project/)
- Reflection Companion used daily adaptive mini-dialogues to trigger reflection. — [Kocielnik et al. PDF](https://faculty.washington.edu/garyhs/docs/kocielnik-UBICOMP2018-reflection.pdf)
- SleepCoacher's experiments alternated "several nights" on and off before drawing conclusions. — [UIST PDF](https://jeffhuang.com/papers/SleepCoacher_UIST16.pdf)
- Habit automaticity took 66 days on average, and one missed day did no harm. — [UCL](https://www.ucl.ac.uk/news/2009/aug/how-long-does-it-take-form-habit)
- Meyer et al.: different people want different metrics, so personalize. — [ZORA PDF](https://www.zora.uzh.ch/server/api/core/bitstreams/f6202968-ef6f-406e-8cec-df836cf77231/content)
- Interruptions are themselves a GenAI productivity cost, so the agent's own prompts must be minimal. — [Simkute et al.](https://arxiv.org/abs/2402.11364v1)

### Inferences
**Recommended cadence:**
- **Nightly (2 minutes or less, mostly automatic):**
  - Collect logs.
  - Ask for one good-day/progress rating and one reflection prompt.
  - Write down one observation.
  - Do not change the active experiment.
- **Weekly:**
  - Run an AAR-style review against objective data.
  - Decide whether to adopt, adapt, or drop each active experiment.
  - Propose at most one or two new experiments.
- **Monthly or quarterly:**
  - Re-check goals and the metric set.
  - Retire metrics that are being gamed or have gone stale.
  - Re-baseline after tool changes.

**Rules for suggestions:**
- Each suggestion must have an evidence line, one concrete action, a test window, and a success metric plus a guardrail.
- No more than one to three items at a time.
- Track adoption rate, and drop the kinds of suggestions the user ignores.

**Avoid the agent becoming a source of interruptions:** deliver suggestions at the end of the day or at the weekly review, not in the middle of focus time.

### Gaps
- I found no direct experimental comparison of nightly vs. weekly reflection cadence for knowledge-work improvement. The recommendation above is inferred from diary-study and self-experiment designs.
- I found no published guidance on the best number of simultaneous suggestions. "One to three" is an inference from single-case attribution logic and interruption-cost findings.
