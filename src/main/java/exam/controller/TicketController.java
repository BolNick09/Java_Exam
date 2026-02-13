package exam.controller;

import exam.model.Category;
import exam.model.Comment;
import exam.model.Ticket;
import exam.model.User;
import exam.repository.CategoryRepository;
import exam.repository.CommentRepository;
import exam.repository.TicketRepository;
import exam.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
public class TicketController {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final CategoryRepository categoryRepository;

    public TicketController(TicketRepository ticketRepository, CommentRepository commentRepository, CategoryRepository categoryRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/tickets")
    public String tickets(Authentication authentication, Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        String role = user.getRole().getName();
        List<Ticket> tickets;

        switch (role) {
            case "USER" -> tickets = ticketRepository.findByUser(user);
            case "AGENT" -> tickets = ticketRepository.findByAgent(user);
            case "ADMIN" -> tickets = ticketRepository.findAll();
            default -> tickets = List.of();
        }

        model.addAttribute("tickets", tickets);
        model.addAttribute("role", role);

        return "tickets/list";
    }
    @GetMapping("/tickets/{id}")
    public String ticketDetails(
            @PathVariable Long id,
            Authentication authentication,
            Model model
    ) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userDetails.getUser();

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String role = currentUser.getRole().getName();

        boolean allowed = switch (role) {
            case "ADMIN" -> true;
            case "USER" ->
                    ticket.getUser().getId().equals(currentUser.getId());
            case "AGENT" ->
                    ticket.getAgent() != null &&
                            ticket.getAgent().getId().equals(currentUser.getId());
            default -> false;
        };

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        model.addAttribute("ticket", ticket);
        model.addAttribute("role", role);

        return "tickets/show";
    }


    @PostMapping("/tickets/{id}/comments")
    public String addComment(
            @PathVariable Long id,
            @RequestParam String text,
            Authentication authentication
    ) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userDetails.getUser();

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String role = currentUser.getRole().getName();

        boolean allowed = switch (role) {
            case "ADMIN" -> true;
            case "USER" ->
                    ticket.getUser().getId().equals(currentUser.getId());
            case "AGENT" ->
                    ticket.getAgent() != null &&
                            ticket.getAgent().getId().equals(currentUser.getId());
            default -> false;
        };

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(currentUser);
        comment.setText(text);

        commentRepository.save(comment);

        return "redirect:/tickets/" + id;
    }

    @GetMapping("/tickets/new")
    public String newTicket(Model model) {
        model.addAttribute("ticket", new Ticket());
        model.addAttribute("categories", categoryRepository.findAll());
        return "tickets/create";
    }

    @PostMapping("/tickets")
    public String createTicket(
            @ModelAttribute Ticket ticket,
            @RequestParam Long categoryId,
            Authentication authentication
    ) {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow();

        ticket.setUser(user);
        ticket.setCategory(category);
        ticket.setStatus("OPEN");

        ticketRepository.save(ticket);
        return "redirect:/tickets";
    }


}
