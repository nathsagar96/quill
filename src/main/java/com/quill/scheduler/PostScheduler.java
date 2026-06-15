package com.quill.scheduler;

import com.quill.model.Post;
import com.quill.model.PostStatus;
import com.quill.repository.PostRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostScheduler {

    private final PostRepository postRepository;

    @Scheduled(fixedRate = 60_000)
    @SchedulerLock(name = "publishScheduledPosts", lockAtMostFor = "2m", lockAtLeastFor = "1m")
    @Transactional
    public void publishScheduledPosts() {
        List<Post> due = postRepository.findByStatusAndScheduledAtBefore(PostStatus.SCHEDULED, Instant.now());
        for (Post post : due) {
            post.setStatus(PostStatus.PUBLISHED);
            post.setPublishedAt(Instant.now());
            post.setScheduledAt(null);
            log.info("Published scheduled post id={}, title='{}'", post.getId(), post.getTitle());
        }
        if (!due.isEmpty()) {
            log.info("Published {} scheduled post(s)", due.size());
        }
    }
}
