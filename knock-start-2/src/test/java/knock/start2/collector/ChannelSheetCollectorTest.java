package knock.start2.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import knock.start2.domain.ChannelMessage;
import org.junit.jupiter.api.Test;

class ChannelSheetCollectorTest {

    // 실제 "메시지" 탭과 같은 머리글. 열 순서를 바꾸고 새 열(업무ID)을 끼워 넣어도 읽혀야 한다.
    private static final String CSV = "﻿번호,시각 (KST),보낸 이,업무ID,받는 이,제목,내용,답장 대상 번호,상태\r\n"
            + "1,2026-11-22 00:58,캐드,,모두,채널 개설,\"규칙을 읽어주세요, 그리고\n\"\"답장\"\" 부탁\",,새 메시지\r\n"
            + "2,2026-11-22 02:05,캐대,T-1,zu,[결과] 대시보드,한 일: 수집기,1,완료\r\n"
            + ",,,,,,,,\r\n";

    @Test
    void parsesMessagesByHeaderName() {
        ChannelSheetCollector c = new ChannelSheetCollector(null, null, "", "0", "Asia/Seoul");
        List<ChannelMessage> list = c.parse(CSV);

        assertThat(list).hasSize(2);
        ChannelMessage first = list.get(0);
        assertThat(first.getNo()).isEqualTo(1);
        assertThat(first.getSender()).isEqualTo("캐드");
        assertThat(first.getReceiver()).isEqualTo("모두");
        assertThat(first.getContent()).isEqualTo("규칙을 읽어주세요, 그리고\n\"답장\" 부탁");
        assertThat(first.getReplyTo()).isNull();
        assertThat(first.getSentAt()).hasToString("2026-11-21T15:58:00Z"); // KST → UTC
        assertThat(first.getStatus()).isEqualTo("새 메시지");

        ChannelMessage second = list.get(1);
        assertThat(second.getReceiver()).isEqualTo("zu");
        assertThat(second.getReplyTo()).isEqualTo(1);
        assertThat(second.getStatus()).isEqualTo("완료");
        assertThat(c.enabled()).isFalse();
    }
}
