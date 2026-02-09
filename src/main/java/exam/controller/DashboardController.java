package exam.controller;

import exam.model.User;
import exam.repository.TicketRepository;
import exam.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final TicketRepository ticketRepository;

    public DashboardController(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        String role = user.getRole().getName();
        model.addAttribute("role", role);

        if (role.equals("USER")) {
            model.addAttribute(
                    "ticketCount",
                    ticketRepository.countByUser(user)
            );
        }

        if (role.equals("AGENT")) {
            model.addAttribute(
                    "ticketCount",
                    ticketRepository.countByAgent(user)
            );
        }

        if (role.equals("ADMIN")) {
            model.addAttribute(
                    "totalTickets",
                    ticketRepository.count()
            );
            model.addAttribute(
                    "openTickets",
                    ticketRepository.countByStatus("OPEN")
            );
            model.addAttribute(
                    "inProgressTickets",
                    ticketRepository.countByStatus("IN_PROGRESS")
            );
            model.addAttribute(
                    "closedTickets",
                    ticketRepository.countByStatus("CLOSED")
            );
        }

        return "dashboard";
    }
}
