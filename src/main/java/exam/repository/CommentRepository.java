package exam.repository;

import exam.model.Comment;
import exam.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long>
{
    List<Comment> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}
