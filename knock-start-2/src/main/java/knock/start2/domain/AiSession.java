package knock.start2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;

/** AI 도구와의 대화 세션 1건 (= 요청 묶음). 수집기가 채우고, memo/tags/starred 는 사용자가 편집한다. */
@Entity
public class AiSession {

    @Id
    private String id;

    private String source;          // 예: claude-code
    private String project;         // 작업 디렉터리 전체 경로
    private String projectName;     // 경로의 마지막 폴더명
    private String gitBranch;
    private String model;

    @Column(length = 500)
    private String title;

    private int promptCount;
    private int assistantCount;
    private int toolCallCount;
    private long inputTokens;
    private long outputTokens;
    private long cacheReadTokens;

    @Column(length = 1_000_000)
    private String toolUsageJson;   // {"Bash":12,"Edit":3}

    @Column(length = 1_000_000)
    private String filesTouched;    // 줄바꿈 구분 파일 경로

    private Instant startedAt;
    private Instant endedAt;

    // 증분 수집용: 원본 파일이 바뀌었을 때만 다시 파싱
    @Column(length = 1000)
    private String sourceFile;
    private long sourceSize;
    private long sourceModified;

    // 사용자 편집 필드 (재수집 시 보존)
    @Column(length = 1_000_000)
    private String memo;
    private String tags;            // 쉼표 구분
    private boolean starred;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getGitBranch() { return gitBranch; }
    public void setGitBranch(String gitBranch) { this.gitBranch = gitBranch; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getPromptCount() { return promptCount; }
    public void setPromptCount(int promptCount) { this.promptCount = promptCount; }
    public int getAssistantCount() { return assistantCount; }
    public void setAssistantCount(int assistantCount) { this.assistantCount = assistantCount; }
    public int getToolCallCount() { return toolCallCount; }
    public void setToolCallCount(int toolCallCount) { this.toolCallCount = toolCallCount; }
    public long getInputTokens() { return inputTokens; }
    public void setInputTokens(long inputTokens) { this.inputTokens = inputTokens; }
    public long getOutputTokens() { return outputTokens; }
    public void setOutputTokens(long outputTokens) { this.outputTokens = outputTokens; }
    public long getCacheReadTokens() { return cacheReadTokens; }
    public void setCacheReadTokens(long cacheReadTokens) { this.cacheReadTokens = cacheReadTokens; }
    public String getToolUsageJson() { return toolUsageJson; }
    public void setToolUsageJson(String toolUsageJson) { this.toolUsageJson = toolUsageJson; }
    public String getFilesTouched() { return filesTouched; }
    public void setFilesTouched(String filesTouched) { this.filesTouched = filesTouched; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
    public long getSourceSize() { return sourceSize; }
    public void setSourceSize(long sourceSize) { this.sourceSize = sourceSize; }
    public long getSourceModified() { return sourceModified; }
    public void setSourceModified(long sourceModified) { this.sourceModified = sourceModified; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public boolean isStarred() { return starred; }
    public void setStarred(boolean starred) { this.starred = starred; }
}
