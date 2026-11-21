package knock.start2.domain;

import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromptEntryRepository extends JpaRepository<PromptEntry, Long> {

    List<PromptEntry> findBySessionIdOrderBySeq(String sessionId);

    @Modifying
    @Query("delete from PromptEntry p where p.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") String sessionId);

    @Query("select distinct p.sessionId from PromptEntry p where lower(p.text) like lower(concat('%', :q, '%'))")
    Set<String> findSessionIdsByTextContaining(@Param("q") String q);
}
