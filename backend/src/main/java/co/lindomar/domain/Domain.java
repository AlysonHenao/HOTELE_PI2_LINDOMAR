package co.lindomar.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Domain {
    private Domain() {}

    public enum Role { GUEST, EMPLOYEE, ADMIN }
    public enum ReservationStatus { CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED }
    public enum TaskStatus { PENDING, IN_PROGRESS, DONE }
    public enum RoomStatus { AVAILABLE, OCCUPIED, MAINTENANCE }
    public enum RequestStatus { OPEN, IN_PROGRESS, RESOLVED }
    public enum EntryType { INCOME, EXPENSE }

    @Entity @Table(name = "users")
    public static class UserAccount {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        private String name;
        @Column(unique = true, nullable = false) private String email;
        private String password;
        @Enumerated(EnumType.STRING) private Role role;
        private String phone;
        public UserAccount() {}
        public UserAccount(String name, String email, String password, Role role, String phone) { this.name=name; this.email=email; this.password=password; this.role=role; this.phone=phone; }
        public Long getId(){return id;} public void setId(Long id){this.id=id;}
        public String getName(){return name;} public void setName(String v){name=v;}
        public String getEmail(){return email;} public void setEmail(String v){email=v;}
        public String getPassword(){return password;} public void setPassword(String v){password=v;}
        public Role getRole(){return role;} public void setRole(Role v){role=v;}
        public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    }

    @Entity @Table(name = "rooms")
    public static class Room {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(unique = true) private String number;
        private int floor;
        private String type;
        private int capacity;
        private BigDecimal price;
        private boolean balcony;
        private boolean petFriendly;
        private String services;
        @Enumerated(EnumType.STRING) private RoomStatus status;
        public Room() {}
        public Room(String number,int floor,String type,int capacity,BigDecimal price,boolean balcony,boolean petFriendly,String services,RoomStatus status){this.number=number;this.floor=floor;this.type=type;this.capacity=capacity;this.price=price;this.balcony=balcony;this.petFriendly=petFriendly;this.services=services;this.status=status;}
        public Long getId(){return id;} public void setId(Long v){id=v;}
        public String getNumber(){return number;} public void setNumber(String v){number=v;}
        public int getFloor(){return floor;} public void setFloor(int v){floor=v;}
        public String getType(){return type;} public void setType(String v){type=v;}
        public int getCapacity(){return capacity;} public void setCapacity(int v){capacity=v;}
        public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){price=v;}
        public boolean isBalcony(){return balcony;} public void setBalcony(boolean v){balcony=v;}
        public boolean isPetFriendly(){return petFriendly;} public void setPetFriendly(boolean v){petFriendly=v;}
        public String getServices(){return services;} public void setServices(String v){services=v;}
        public RoomStatus getStatus(){return status;} public void setStatus(RoomStatus v){status=v;}
    }

    @Entity @Table(name = "reservations")
    public static class Reservation {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        private Long guestId;
        private Long roomId;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private int guests;
        @Enumerated(EnumType.STRING) private ReservationStatus status;
        private String notes;
        public Reservation() {}
        public Long getId(){return id;} public void setId(Long v){id=v;}
        public Long getGuestId(){return guestId;} public void setGuestId(Long v){guestId=v;}
        public Long getRoomId(){return roomId;} public void setRoomId(Long v){roomId=v;}
        public LocalDate getCheckIn(){return checkIn;} public void setCheckIn(LocalDate v){checkIn=v;}
        public LocalDate getCheckOut(){return checkOut;} public void setCheckOut(LocalDate v){checkOut=v;}
        public int getGuests(){return guests;} public void setGuests(int v){guests=v;}
        public ReservationStatus getStatus(){return status;} public void setStatus(ReservationStatus v){status=v;}
        public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    }

    @Entity @Table(name = "service_requests")
    public static class ServiceRequest {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        private Long guestId;
        private String roomNumber;
        private String category;
        @Column(length=1200) private String notes;
        @Enumerated(EnumType.STRING) private RequestStatus status;
        private LocalDateTime createdAt;
        public ServiceRequest() {}
        public Long getId(){return id;} public void setId(Long v){id=v;}
        public Long getGuestId(){return guestId;} public void setGuestId(Long v){guestId=v;}
        public String getRoomNumber(){return roomNumber;} public void setRoomNumber(String v){roomNumber=v;}
        public String getCategory(){return category;} public void setCategory(String v){category=v;}
        public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
        public RequestStatus getStatus(){return status;} public void setStatus(RequestStatus v){status=v;}
        public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    }

    @Entity @Table(name = "staff_tasks")
    public static class StaffTask {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        private Long employeeId;
        private String title;
        @Column(length=1200) private String description;
        @Enumerated(EnumType.STRING) private TaskStatus status;
        private LocalDate dueDate;
        private String priority;
        public StaffTask() {}
        public StaffTask(Long employeeId,String title,String description,TaskStatus status,LocalDate dueDate,String priority){this.employeeId=employeeId;this.title=title;this.description=description;this.status=status;this.dueDate=dueDate;this.priority=priority;}
        public Long getId(){return id;} public void setId(Long v){id=v;}
        public Long getEmployeeId(){return employeeId;} public void setEmployeeId(Long v){employeeId=v;}
        public String getTitle(){return title;} public void setTitle(String v){title=v;}
        public String getDescription(){return description;} public void setDescription(String v){description=v;}
        public TaskStatus getStatus(){return status;} public void setStatus(TaskStatus v){status=v;}
        public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){dueDate=v;}
        public String getPriority(){return priority;} public void setPriority(String v){priority=v;}
    }

    @Entity @Table(name = "finance_entries")
    public static class FinanceEntry {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Enumerated(EnumType.STRING) private EntryType type;
        private String concept;
        private BigDecimal amount;
        private LocalDate date;
        public FinanceEntry() {}
        public FinanceEntry(EntryType type,String concept,BigDecimal amount,LocalDate date){this.type=type;this.concept=concept;this.amount=amount;this.date=date;}
        public Long getId(){return id;} public void setId(Long v){id=v;}
        public EntryType getType(){return type;} public void setType(EntryType v){type=v;}
        public String getConcept(){return concept;} public void setConcept(String v){concept=v;}
        public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
        public LocalDate getDate(){return date;} public void setDate(LocalDate v){date=v;}
    }
}

