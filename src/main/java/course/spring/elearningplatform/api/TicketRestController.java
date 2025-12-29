package course.spring.elearningplatform.api;

import course.spring.elearningplatform.dto.TicketDto;
import course.spring.elearningplatform.entity.Ticket;
import course.spring.elearningplatform.service.ActivityLogService;
import course.spring.elearningplatform.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketRestController {

    private final TicketService ticketService;
    private final ActivityLogService activityLogService;

    @Autowired
    public TicketRestController(TicketService ticketService, ActivityLogService activityLogService) {
        this.ticketService = ticketService;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public ResponseEntity<?> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @PostMapping
    public ResponseEntity<?> createTicket(@RequestParam Long courseId,
                                         @RequestBody TicketDto ticketDto,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        ticketService.saveTicket(ticketDto, courseId, userDetails.getUsername());
        activityLogService.logActivity("Ticket opened", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Ticket created successfully"));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<?> resolveTicket(@PathVariable Long id,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        Ticket resolvedTicket = ticketService.resolveTicket(id);
        activityLogService.logActivity("Ticket resolved", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Ticket resolved successfully", "ticket", resolvedTicket));
    }
}