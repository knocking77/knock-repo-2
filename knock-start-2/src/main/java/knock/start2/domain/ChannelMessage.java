package knock.start2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;

/** "AI 공통 채널" 시트의 메시지 탭 한 줄. 시트가 원본이므로 수집할 때마다 통째로 교체된다. */
@Entity
public class ChannelMessage {

    @Id
    private Integer no;             // 번호

    private Instant sentAt;         // 시각 (KST)
    private String sender;          // 보낸 이
    private String receiver;        // 받는 이
    @Column(length = 1000)
    private String subject;         // 제목
    @Column(length = 1_000_000)
    private String content;         // 내용
    private Integer replyTo;        // 답장 대상 번호
    private String status;          // 새 메시지 / 읽음 / 완료

    protected ChannelMessage() {}

    public ChannelMessage(Integer no, Instant sentAt, String sender, String receiver, String subject,
                          String content, Integer replyTo, String status) {
        this.no = no;
        this.sentAt = sentAt;
        this.sender = sender;
        this.receiver = receiver;
        this.subject = subject;
        this.content = content;
        this.replyTo = replyTo;
        this.status = status;
    }

    public Integer getNo() { return no; }
    public Instant getSentAt() { return sentAt; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public Integer getReplyTo() { return replyTo; }
    public String getStatus() { return status; }
}
