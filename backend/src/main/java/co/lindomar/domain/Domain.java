package co.lindomar.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Agrupa las entidades y enumeraciones que conforman el dominio del hotel. */
public final class Domain {
  private Domain() {}

  /** Roles disponibles para controlar el acceso a las funciones del sistema. */
  public enum Role {
    GUEST,
    EMPLOYEE,
    ADMIN
  }

  /** Estados posibles durante el ciclo de vida de una reserva. */
  public enum ReservationStatus {
    CONFIRMED,
    CHECKED_IN,
    COMPLETED,
    CANCELLED
  }

  /** Estados posibles durante el ciclo de vida de una tarea operativa. */
  public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    DONE
  }

  /** Estados operativos de una habitación. */
  public enum RoomStatus {
    AVAILABLE,
    OCCUPIED,
    RESERVED,
    MAINTENANCE,
    OUT_OF_SERVICE
  }

  /** Estados posibles para una solicitud de atención. */
  public enum RequestStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED
  }

  /** Tipos de movimiento financiero registrados por el hotel. */
  public enum EntryType {
    INCOME,
    EXPENSE
  }

  /** Sesión opaca persistida: el servidor, no el navegador, decide quién crea la reserva. */
  @Entity
  @Table(name = "auth_sessions")
  public static class AuthSession {
    @Id private String token;
    private Long userId;
    private LocalDateTime expiresAt;

    public AuthSession() {}

    /**
     * Crea una sesión autenticada.
     *
     * @param token identificador opaco de la sesión
     * @param userId identificador del usuario autenticado
     * @param expiresAt fecha y hora de vencimiento
     */
    public AuthSession(String token, Long userId, LocalDateTime expiresAt) {
      this.token = token;
      this.userId = userId;
      this.expiresAt = expiresAt;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String v) {
      token = v;
    }

    public Long getUserId() {
      return userId;
    }

    public void setUserId(Long v) {
      userId = v;
    }

    public LocalDateTime getExpiresAt() {
      return expiresAt;
    }

    public void setExpiresAt(LocalDateTime v) {
      expiresAt = v;
    }
  }

  /** Estados de seguimiento de un recordatorio de gasto. */
  public enum ReminderStatus {
    PENDING,
    DONE
  }

  /** Cuenta con credenciales, datos personales y un rol de acceso. */
  @Entity
  @Table(name = "users")
  public static class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @com.fasterxml.jackson.annotation.JsonProperty(
        access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String phone;

    public UserAccount() {}

    /**
     * Crea una cuenta de usuario.
     *
     * @param name nombre completo
     * @param email correo usado para iniciar sesión
     * @param password contraseña cifrada
     * @param role rol asignado
     * @param phone teléfono de contacto
     */
    public UserAccount(String name, String email, String password, Role role, String phone) {
      this.name = name;
      this.email = email;
      this.password = password;
      this.role = role;
      this.phone = phone;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String v) {
      name = v;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String v) {
      email = v;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String v) {
      password = v;
    }

    public Role getRole() {
      return role;
    }

    public void setRole(Role v) {
      role = v;
    }

    public String getPhone() {
      return phone;
    }

    public void setPhone(String v) {
      phone = v;
    }
  }

  /** Habitación ofrecida por el hotel con sus características y estado operativo. */
  @Entity(name = "Room")
  @Table(name = "rooms")
  public static class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String number;

    private int floor;
    private String type;
    private int capacity;
    private BigDecimal price;
    private boolean balcony;
    private boolean petFriendly;
    private String services;

    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    public Room() {}

    /**
     * Crea una habitación.
     *
     * @param number número visible de la habitación
     * @param floor piso donde se encuentra
     * @param type tipo comercial
     * @param capacity número máximo de huéspedes
     * @param price tarifa por noche
     * @param balcony indica si tiene balcón
     * @param petFriendly indica si admite mascotas
     * @param services servicios incluidos
     * @param status estado operativo inicial
     */
    public Room(
        String number,
        int floor,
        String type,
        int capacity,
        BigDecimal price,
        boolean balcony,
        boolean petFriendly,
        String services,
        RoomStatus status) {
      this.number = number;
      this.floor = floor;
      this.type = type;
      this.capacity = capacity;
      this.price = price;
      this.balcony = balcony;
      this.petFriendly = petFriendly;
      this.services = services;
      this.status = status;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long v) {
      id = v;
    }

    public String getNumber() {
      return number;
    }

    public void setNumber(String v) {
      number = v;
    }

    public int getFloor() {
      return floor;
    }

    public void setFloor(int v) {
      floor = v;
    }

    public String getType() {
      return type;
    }

    public void setType(String v) {
      type = v;
    }

    public int getCapacity() {
      return capacity;
    }

    public void setCapacity(int v) {
      capacity = v;
    }

    public BigDecimal getPrice() {
      return price;
    }

    public void setPrice(BigDecimal v) {
      price = v;
    }

    public boolean isBalcony() {
      return balcony;
    }

    public void setBalcony(boolean v) {
      balcony = v;
    }

    public boolean isPetFriendly() {
      return petFriendly;
    }

    public void setPetFriendly(boolean v) {
      petFriendly = v;
    }

    public String getServices() {
      return services;
    }

    public void setServices(String v) {
      services = v;
    }

    public RoomStatus getStatus() {
      return status;
    }

    public void setStatus(RoomStatus v) {
      status = v;
    }
  }

  /** Reserva de una habitación asociada a un huésped y a un rango de fechas. */
  @Entity
  @Table(name = "reservations")
  public static class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long guestId;
    private Long roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int guests;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private String notes;

    public Reservation() {}

    public Long getId() {
      return id;
    }

    public void setId(Long v) {
      id = v;
    }

    public Long getGuestId() {
      return guestId;
    }

    public void setGuestId(Long v) {
      guestId = v;
    }

    public Long getRoomId() {
      return roomId;
    }

    public void setRoomId(Long v) {
      roomId = v;
    }

    public LocalDate getCheckIn() {
      return checkIn;
    }

    public void setCheckIn(LocalDate v) {
      checkIn = v;
    }

    public LocalDate getCheckOut() {
      return checkOut;
    }

    public void setCheckOut(LocalDate v) {
      checkOut = v;
    }

    public int getGuests() {
      return guests;
    }

    public void setGuests(int v) {
      guests = v;
    }

    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(BigDecimal v) {
      amount = v;
    }

    public ReservationStatus getStatus() {
      return status;
    }

    public void setStatus(ReservationStatus v) {
      status = v;
    }

    public String getNotes() {
      return notes;
    }

    public void setNotes(String v) {
      notes = v;
    }
  }

  /** Solicitud de atención creada por un huésped durante su estadía. */
  @Entity
  @Table(name = "service_requests")
  public static class ServiceRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long guestId;
    private String roomNumber;
    private String category;

    @Column(length = 1200)
    private String notes;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private LocalDateTime createdAt;

    public ServiceRequest() {}

    public Long getId() {
      return id;
    }

    public void setId(Long v) {
      id = v;
    }

    public Long getGuestId() {
      return guestId;
    }

    public void setGuestId(Long v) {
      guestId = v;
    }

    public String getRoomNumber() {
      return roomNumber;
    }

    public void setRoomNumber(String v) {
      roomNumber = v;
    }

    public String getCategory() {
      return category;
    }

    public void setCategory(String v) {
      category = v;
    }

    public String getNotes() {
      return notes;
    }

    public void setNotes(String v) {
      notes = v;
    }

    public RequestStatus getStatus() {
      return status;
    }

    public void setStatus(RequestStatus v) {
      status = v;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
      createdAt = v;
    }
  }

  /** Tarea operativa asignada a un empleado del hotel. */
  @Entity
  @Table(name = "staff_tasks")
  public static class StaffTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;
    private String title;

    @Column(length = 1200)
    private String description;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    private LocalDate dueDate;
    private String priority;

    @JsonIgnore
    @Column(columnDefinition = "bytea")
    private byte[] completionPhoto;

    private String completionPhotoName;
    private String completionPhotoContentType;
    private LocalDateTime completedAt;

    public StaffTask() {}

    /**
     * Crea una tarea operativa.
     *
     * @param employeeId identificador del empleado responsable
     * @param title título breve
     * @param description instrucciones de la tarea
     * @param status estado inicial
     * @param dueDate fecha límite
     * @param priority prioridad asignada
     */
    public StaffTask(
        Long employeeId,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        String priority) {
      this.employeeId = employeeId;
      this.title = title;
      this.description = description;
      this.status = status;
      this.dueDate = dueDate;
      this.priority = priority;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long v) {
      id = v;
    }

    public Long getEmployeeId() {
      return employeeId;
    }

    public void setEmployeeId(Long v) {
      employeeId = v;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String v) {
      title = v;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String v) {
      description = v;
    }

    public TaskStatus getStatus() {
      return status;
    }

    public void setStatus(TaskStatus v) {
      status = v;
    }

    public LocalDate getDueDate() {
      return dueDate;
    }

    public void setDueDate(LocalDate v) {
      dueDate = v;
    }

    public String getPriority() {
      return priority;
    }

    public void setPriority(String v) {
      priority = v;
    }

    public byte[] getCompletionPhoto() {
      return completionPhoto;
    }

    public void setCompletionPhoto(byte[] v) {
      completionPhoto = v;
    }

    public String getCompletionPhotoName() {
      return completionPhotoName;
    }

    public void setCompletionPhotoName(String v) {
      completionPhotoName = v;
    }

    public String getCompletionPhotoContentType() {
      return completionPhotoContentType;
    }

    public void setCompletionPhotoContentType(String v) {
      completionPhotoContentType = v;
    }

    public LocalDateTime getCompletedAt() {
      return completedAt;
    }

    public void setCompletedAt(LocalDateTime v) {
      completedAt = v;
    }

    @Transient
    public boolean getHasCompletionPhoto() {
      return completionPhoto != null && completionPhoto.length > 0;
    }
  }

  /** Movimiento de ingreso o gasto registrado por el hotel. */
  @Entity
  @Table(name = "finance_entries")
  public static class FinanceEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private EntryType type;

    private String concept;
    private BigDecimal amount;
    private LocalDate date;

    public FinanceEntry() {}

    /**
     * Crea un movimiento financiero.
     *
     * @param type tipo de movimiento
     * @param concept descripción del movimiento
     * @param amount valor monetario
     * @param date fecha contable
     */
    public FinanceEntry(EntryType type, String concept, BigDecimal amount, LocalDate date) {
      this.type = type;
      this.concept = concept;
      this.amount = amount;
      this.date = date;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long v) {
      id = v;
    }

    public EntryType getType() {
      return type;
    }

    public void setType(EntryType v) {
      type = v;
    }

    public String getConcept() {
      return concept;
    }

    public void setConcept(String v) {
      concept = v;
    }

    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(BigDecimal v) {
      amount = v;
    }

    public LocalDate getDate() {
      return date;
    }

    public void setDate(LocalDate v) {
      date = v;
    }
  }

  /** HU-15: recordatorios programados de gastos necesarios del hotel. */
  @Entity
  @Table(name = "expense_reminders")
  public static class ExpenseReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String concept;
    private String category;
    private BigDecimal estimatedAmount;
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private ReminderStatus status;

    public ExpenseReminder() {}

    /**
     * Crea un recordatorio de gasto.
     *
     * @param concept descripción del gasto esperado
     * @param category categoría presupuestal
     * @param estimatedAmount valor estimado
     * @param dueDate fecha límite de pago
     * @param status estado de seguimiento
     */
    public ExpenseReminder(
        String concept,
        String category,
        BigDecimal estimatedAmount,
        LocalDate dueDate,
        ReminderStatus status) {
      this.concept = concept;
      this.category = category;
      this.estimatedAmount = estimatedAmount;
      this.dueDate = dueDate;
      this.status = status;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long v) {
      id = v;
    }

    public String getConcept() {
      return concept;
    }

    public void setConcept(String v) {
      concept = v;
    }

    public String getCategory() {
      return category;
    }

    public void setCategory(String v) {
      category = v;
    }

    public BigDecimal getEstimatedAmount() {
      return estimatedAmount;
    }

    public void setEstimatedAmount(BigDecimal v) {
      estimatedAmount = v;
    }

    public LocalDate getDueDate() {
      return dueDate;
    }

    public void setDueDate(LocalDate v) {
      dueDate = v;
    }

    public ReminderStatus getStatus() {
      return status;
    }

    public void setStatus(ReminderStatus v) {
      status = v;
    }
  }
}
