package com.univers.univers_backend.Service;


import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.EventApproval;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Repository.EventApprovalRepository;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EventApprovalService {

    private final EventApprovalRepository eventApprovalRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventApprovalService(EventApprovalRepository eventApprovalRepository,
                                EventRepository eventRepository,
                                UserRepository userRepository){
        this.eventApprovalRepository = eventApprovalRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;

    }
    public String approveEvent(Long eventId, Long approverId, String remarks){
        User user = userRepository.findById(approverId).orElseThrow();
        if(user.getRoles().toString().contains(Role.VENUE_OWNER.toString())){
            return  approveByVenueOwner(eventId, user, remarks);
        }else if(user.getRoles().toString().contains(Role.MSDO.toString())){
            return approveByMSDO(eventId, user, remarks);
        }else if(user.getRoles().toString().contains(Role.OPC.toString())){
            return approveByOPC(eventId, user, remarks);
        }else if(user.getRoles().toString().contains(Role.DEPT_HEAD.toString())){
            return approveByDepartmentHead(eventId, user, remarks);
        }else if(user.getRoles().toString().contains(Role.VP_ADMIN.toString())){
            return approveByVPAdmin(eventId, user, remarks);
        }else if(user.getRoles().toString().contains(Role.VPAA.toString())){
            return approveByVPAA(eventId, user, remarks);
        }else if(user.getRoles().toString().contains(Role.SSD.toString())){
            return approveBySSD(eventId, user, remarks);
        }else if(user.getRoles().toString().contains(Role.FAO.toString())){
            return approveByFAO(eventId, user, remarks);
        }else{
            return "You are not authorized to approve this event";
        }
    }

    public String approveByVenueOwner(Long eventId, User approver, String remarks){

        Optional<Event> eventOpt = eventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            return "Error: Event not found.";
        }
        Event event = eventOpt.get();
        Venue venue = event.getEventVenue();

        if (venue == null) {
            return "Error: No venue is assigned to this event.";
        }
        if (venue.getVenueOwner() == null || !venue.getVenueOwner().equals(approver)) {
            return "Error: You are not authorized to approve this event for this venue.";
        }
        if (!approver.getRoles().toString().contains(Role.VENUE_OWNER.toString())) {
            return "Error: User does not have the VENUE_OWNER role.";
        }

        EventApproval eventApproval = new EventApproval();
        eventApproval.setEvent(event);
        eventApproval.setSignedBy(approver);
        eventApproval.setRemarks(remarks);
        eventApproval.setStatus(Status.APPROVED);
        eventApproval.setDateSigned(LocalDateTime.now());

        eventApprovalRepository.save(eventApproval);

        return "Venue approved successfully by " + ": " + approver.getRoles() + ": " + approver.getFirstname();
    }
    public String approveByDepartmentHead(Long eventId, User approver, String remarks){
        Optional<Event> eventOpt = eventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            return "Error: Event not found.";
        }
        Event event = eventOpt.get();
        User deptHead = event.getOrganizer().getDepartment().getDeptHead();

        if (deptHead == null || !deptHead.equals(approver)) {
            return "Error: You are not authorized to approve this event as a department head of " + approver.getDepartment();
        }
        if (!approver.getRoles().toString().contains(Role.DEPT_HEAD.toString())) {
            return "Error: User does not have the DEPT_HEAD role.";
        }

        EventApproval eventApproval = new EventApproval();
        eventApproval.setEvent(event);
        eventApproval.setSignedBy(approver);
        eventApproval.setRemarks(remarks);
        eventApproval.setStatus(Status.APPROVED);
        eventApproval.setDateSigned(LocalDateTime.now());

        eventApprovalRepository.save(eventApproval);

        return "Approved successfully by " + approver.getRoles() + ": " + approver.getFirstname();
    }
    public String approveByMSDO(Long evenId, User approverId, String remarks){
        return approve(evenId, approverId, remarks, Role.MSDO.toString());
    }
    public String approveByOPC(Long evenId, User approverId, String remarks){
        return approve(evenId, approverId, remarks, Role.OPC.toString());
    }
    public String approveByVPAdmin(Long evenId, User approverId, String remarks){
        return approve(evenId, approverId, remarks, Role.VP_ADMIN.toString());
    }
    public String approveByVPAA(Long evenId, User approverId, String remarks){
        return approve(evenId, approverId, remarks, Role.VPAA.toString());
    }
    public String approveBySSD(Long evenId, User approverId, String remarks){
        return approve(evenId, approverId, remarks, Role.SSD.toString());
    }
    public String approveByFAO(Long evenId, User approverId, String remarks){
        return approve(evenId, approverId, remarks, Role.FAO.toString());
    }


    public String approve(Long eventId, User approver, String remarks, String role){
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if(eventOptional.isEmpty()){
            return "Error: Event or user not found";
        }

        Event event = eventOptional.get();

        if(!approver.getRoles().toString().contains(role)){
            return "Error: You are not authorized to approve this event";
        }

        EventApproval eventApproval = new EventApproval();
        eventApproval.setEvent(event);
        eventApproval.setSignedBy(approver);
        eventApproval.setRemarks(remarks);
        eventApproval.setStatus(Status.APPROVED);
        eventApproval.setDateSigned(LocalDateTime.now());

        eventApprovalRepository.save(eventApproval);

        return "Approved successfully by " + role;

    }

}
