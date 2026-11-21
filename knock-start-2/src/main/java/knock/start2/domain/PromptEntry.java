package knock.start2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** 세션 안에서 사용자가 보낸 요청(프롬프트) 1건. */
@Entity
@Table(indexes = @Index(columnList = "sessionId"))
public class PromptEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;
    private int seq;

    @Column(length = 1_000_000)
    private String text;

    private Instant timestamp;

    protected PromptEntry() {}

    public PromptEntry(String sessionId, int seq, String text, Instant timestamp) {
        this.sessionId = sessionId;
        this.seq = seq;
        this.text = text;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public int getSeq() { return seq; }
    public String getText() { return text; }
    public Instant getTimestamp() { return timestamp; }
}
