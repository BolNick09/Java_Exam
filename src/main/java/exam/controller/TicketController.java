package exam.controller;

import exam.model.Ticket;
import exam.model.User;
import exam.repository.TicketRepository;
import exam.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class TicketController {

    private final TicketRepository ticketRepository;

    public TicketController(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
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
}
