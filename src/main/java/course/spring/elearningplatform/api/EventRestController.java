package course.spring.elearningplatform.api;

import course.spring.elearningplatform.dto.EventDto;
import course.spring.elearningplatform.dto.mapper.EntityMapper;
import course.spring.elearningplatform.entity.CustomUserDetails;
import course.spring.elearningplatform.entity.Event;
import course.spring.elearningplatform.service.ActivityLogService;
import course.spring.elearningplatform.service.impl.EventServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
public class EventRestController {

    private final EventServiceImpl eventService;
    private final ActivityLogService activityLogService;

    @Autowired
    public EventRestController(EventServiceImpl eventService, ActivityLogService activityLogService) {
        this.eventService = eventService;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public ResponseEntity<?> getAllEvents() {
        List<EventDto> events = eventService.getAllEvents().stream()
                .map(event -> EntityMapper.mapEntityToDto(event, EventDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(events);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingEvents() {
        List<Event> upcomingEvents = eventService.getAllEvents().stream()
                .filter(event -> event.getStartTime().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(Event::getStartTime))
                .limit(3)
                .collect(Collectors.toList());
        return ResponseEntity.ok(upcomingEvents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id) {
        Event event = eventService.getEventById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid event Id:" + id));
        event.setImageBase64(event.getImage().parseImage());
        return ResponseEntity.ok(event);
    }

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody EventDto eventDto,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        eventDto.setInstructor(userDetails.getUsername());
        eventService.saveEvent(eventDto);
        activityLogService.logActivity("New event created", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Event created successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        eventService.deleteEvent(id);
        activityLogService.logActivity("Deleted event", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Event deleted successfully"));
    }

    @GetMapping("/instructor")
    public ResponseEntity<?> getInstructorEvents(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Event> events = eventService.getAllEventsByUser(userDetails.getUsername());
        return ResponseEntity.ok(events);
    }
}