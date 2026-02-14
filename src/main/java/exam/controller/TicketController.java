package exam.controller;

import exam.model.Category;
import exam.model.Comment;
import exam.model.Ticket;
import exam.model.User;
import exam.repository.CategoryRepository;
import exam.repository.CommentRepository;
import exam.repository.TicketRepository;
import exam.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class TicketController {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TicketController(TicketRepository ticketRepository, CommentRepository commentRepository, CategoryRepository categoryRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
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

    @PostMapping("/tickets/{id}/comments")
    public String addComment(
            @PathVariable Long id,
            @RequestParam String text,
            Authentication authentication
    ) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        String role = user.getRole().getName();

        // Защита доступа
        if ("USER".equals(role)) {
            if (!ticket.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }

        if ("AGENT".equals(role)) {
            if (ticket.getAgent() == null ||
                    !ticket.getAgent().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }

        Comment comment = new Comment();
        comment.setText(text);
        comment.setTicket(ticket);
        comment.setAuthor(user);
        comment.setCreatedAt(LocalDateTime.now());

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
    @GetMapping("/tickets/{id}")
    public String showTicket(
            @PathVariable Long id,
            Model model,
            Authentication authentication
    ) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        String role = user.getRole().getName();

        // USER: только свои тикеты
        if ("USER".equals(role)) {
            if (!ticket.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }

        // AGENT: только назначенные
        if ("AGENT".equals(role)) {
            if (ticket.getAgent() == null ||
                    !ticket.getAgent().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }

        if ("ADMIN".equals(role)) {
            model.addAttribute("agents",
                    userRepository.findByRole_Name("AGENT"));
        }

        model.addAttribute("ticket", ticket);
        model.addAttribute("comments", ticket.getComments());

        return "tickets/show";
    }

    @PostMapping("/tickets/{id}/assign")
    public String assignAgent(
            @PathVariable Long id,
            @RequestParam Long agentId,
            Authentication authentication
    ) {
        User admin = ((CustomUserDetails) authentication.getPrincipal()).getUser();

        if (!"ADMIN".equals(admin.getRole().getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!"AGENT".equals(agent.getRole().getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        ticket.setAgent(agent);
        ticket.setStatus("IN_PROGRESS");

        ticketRepository.save(ticket);

        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication
    ) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userDetails.getUser();

        String role = currentUser.getRole().getName();

        // --- проверка прав ---
        if (role.equals("AGENT")) {
            if (ticket.getAgent() == null ||
                    !ticket.getAgent().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied");
            }
        }

        if (role.equals("USER")) {
            throw new RuntimeException("Access denied");
        }

        // --- обновление ---
        ticket.setStatus(status);
        ticketRepository.save(ticket);

        return "redirect:/tickets/" + id;
    }




}
