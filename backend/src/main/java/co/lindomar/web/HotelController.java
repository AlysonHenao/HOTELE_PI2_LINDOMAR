package co.lindomar.web;

import co.lindomar.domain.Domain.AuthSession;
import co.lindomar.domain.Domain.EntryType;
import co.lindomar.domain.Domain.ExpenseReminder;
import co.lindomar.domain.Domain.FinanceEntry;
import co.lindomar.domain.Domain.ReminderStatus;
import co.lindomar.domain.Domain.RequestStatus;
import co.lindomar.domain.Domain.Reservation;
import co.lindomar.domain.Domain.ReservationStatus;
import co.lindomar.domain.Domain.Role;
import co.lindomar.domain.Domain.Room;
import co.lindomar.domain.Domain.RoomStatus;
import co.lindomar.domain.Domain.ServiceRequest;
import co.lindomar.domain.Domain.StaffTask;
import co.lindomar.domain.Domain.TaskStatus;
import co.lindomar.domain.Domain.UserAccount;
import co.lindomar.repository.AuthSessionRepository;
import co.lindomar.repository.FinanceRepository;
import co.lindomar.repository.ReminderRepository;
import co.lindomar.repository.RequestRepository;
import co.lindomar.repository.ReservationRepository;
import co.lindomar.repository.RoomRepository;
import co.lindomar.repository.TaskRepository;
import co.lindomar.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** API HTTP del prototipo Lindomar. */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HotelController {
  private final UserRepository users;
  private final RoomRepository rooms;
  private final ReservationRepository reservations;
  private final RequestRepository requests;
  private final TaskRepository tasks;
  private final FinanceRepository finance;
  private final ReminderRepository reminders;
  private final AuthSessionRepository sessions;
  private final PasswordEncoder passwords;

  /** Construye el controlador con los repositorios y servicios requeridos. */
  public HotelController(
      UserRepository u,
      RoomRepository r,
      ReservationRepository rs,
      RequestRepository rq,
      TaskRepository t,
      FinanceRepository f,
      ReminderRepository rm,
      AuthSessionRepository s,
      PasswordEncoder passwords) {
    users = u;
    rooms = r;
    reservations = rs;
    requests = rq;
    tasks = t;
    finance = f;
    reminders = rm;
    sessions = s;
    this.passwords = passwords;
  }

  /** Credenciales recibidas para iniciar sesión. */
  public record LoginRequest(String email, String password) {}

  /** Datos permitidos para registrar un huésped. */
  public record RegisterRequest(
      @NotBlank(message = "El nombre es obligatorio") @Size(max = 255) String name,
      @NotBlank(message = "El correo es obligatorio")
          @Email(message = "Ingrese un correo electrónico válido")
          @Size(max = 255)
          String email,
      @NotBlank(message = "La contraseña es obligatoria")
          @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
          String password,
      @Size(max = 255) String phone) {
    /** Normaliza los datos de identidad del huésped. */
    public RegisterRequest {
      if (name != null) {
        name = name.trim();
      }
      if (email != null) {
        email = email.trim().toLowerCase(Locale.ROOT);
      }
    }
  }

  /** Estado nuevo solicitado para una habitación. */
  public record RoomStatusRequest(
      @NotNull(message = "El estado es obligatorio") RoomStatus status) {}

  /** Datos validados para asignar una tarea a un empleado. */
  public record TaskCreateRequest(
      @NotNull(message = "El empleado es obligatorio") Long employeeId,
      @NotBlank(message = "El título es obligatorio")
          @Size(max = 120, message = "El título no puede superar 120 caracteres")
          String title,
      @NotBlank(message = "La descripción es obligatoria")
          @Size(max = 1200, message = "La descripción no puede superar 1200 caracteres")
          String description,
      @NotNull(message = "La fecha límite es obligatoria")
          @FutureOrPresent(message = "La fecha límite no puede estar en el pasado")
          LocalDate dueDate,
      @NotBlank(message = "La prioridad es obligatoria")
          @Pattern(regexp = "Alta|Media|Baja", message = "La prioridad debe ser Alta, Media o Baja")
          String priority) {
    /** Normaliza el texto antes de validar y persistir la tarea. */
    public TaskCreateRequest {
      if (title != null) {
        title = title.trim();
      }
      if (description != null) {
        description = description.trim();
      }
      if (priority != null) {
        priority = priority.trim();
      }
    }
  }

  /** Estado solicitado para una tarea del empleado. */
  public record TaskStatusRequest(
      @NotNull(message = "El estado es obligatorio") TaskStatus status) {}

  /** Representación pública de una cuenta, sin contraseña. */
  public record PublicUser(Long id, String name, String email, Role role, String phone) {}

  /** Resultado de autenticación con usuario y token de sesión. */
  public record AuthResponse(PublicUser user, String token) {}

  /** Reserva enriquecida con los datos visibles del huésped y de la habitación. */
  public record ReservationView(
      Long id,
      Long guestId,
      String guestName,
      Long roomId,
      String roomNumber,
      String roomType,
      LocalDate checkIn,
      LocalDate checkOut,
      int guests,
      ReservationStatus status,
      String notes,
      BigDecimal total) {}

  @PostMapping("/auth/login")
  ResponseEntity<?> login(@RequestBody LoginRequest input) {
    if (input.email() == null
        || input.password() == null
        || input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
      return ResponseEntity.status(401).body(Map.of("message", "Correo o contraseña incorrectos"));
    }
    return users
        .findByEmailIgnoreCase(input.email().trim())
        .filter(u -> passwords.matches(input.password(), u.getPassword()))
        .<ResponseEntity<?>>map(u -> ResponseEntity.ok(authResponse(u)))
        .orElseGet(
            () ->
                ResponseEntity.status(401)
                    .body(Map.of("message", "Correo o contraseña incorrectos")));
  }

  /** HU-01: registro de nuevos huéspedes. Siempre crea cuentas con rol GUEST. */
  @PostMapping("/auth/register")
  ResponseEntity<?> register(@Valid @RequestBody RegisterRequest input) {
    if (input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
      return ResponseEntity.badRequest()
          .body(Map.of("message", "La contraseña no puede superar 72 bytes UTF-8"));
    }
    if (users.findByEmailIgnoreCase(input.email()).isPresent()) {
      return ResponseEntity.status(409)
          .body(Map.of("message", "Ya existe una cuenta registrada con ese correo"));
    }
    UserAccount saved;
    try {
      saved =
          users.saveAndFlush(
              new UserAccount(
                  input.name(),
                  input.email(),
                  passwords.encode(input.password()),
                  Role.GUEST,
                  input.phone()));
    } catch (DataIntegrityViolationException ex) {
      return ResponseEntity.status(409)
          .body(Map.of("message", "Ya existe una cuenta registrada con ese correo"));
    }
    return ResponseEntity.ok(authResponse(saved));
  }

  @GetMapping("/auth/me")
  PublicUser currentUser(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return publicUser(requireUser(authorization, null));
  }

  @Transactional
  @PostMapping("/auth/logout")
  ResponseEntity<Void> logout(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    tokenFrom(authorization).ifPresent(sessions::deleteByToken);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/users")
  List<PublicUser> allUsers(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    requireUser(authorization, Role.ADMIN);
    return users.findAll().stream()
        .map(u -> new PublicUser(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getPhone()))
        .toList();
  }

  @PutMapping("/users/{id}")
  PublicUser updateUser(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody UserAccount data) {
    requireUser(authorization, Role.ADMIN);
    var u = users.findById(id).orElseThrow();
    if (data.getName() != null) {
      u.setName(data.getName());
    }
    if (data.getPhone() != null) {
      u.setPhone(data.getPhone());
    }
    if (data.getRole() != null) {
      u.setRole(data.getRole());
    }
    users.save(u);
    return new PublicUser(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getPhone());
  }

  @GetMapping("/rooms")
  List<Room> getRooms(
      @RequestParam(required = false) String type,
      @RequestParam(required = false) Integer capacity,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) Boolean balcony,
      @RequestParam(required = false) Boolean petFriendly,
      @RequestParam(required = false) LocalDate checkIn,
      @RequestParam(required = false) LocalDate checkOut) {
    if (checkIn != null || checkOut != null) {
      validateSearchDates(checkIn, checkOut);
    }
    return rooms.findAll().stream()
        .filter(r -> type == null || type.isBlank() || r.getType().equalsIgnoreCase(type))
        .filter(r -> capacity == null || r.getCapacity() >= capacity)
        .filter(r -> maxPrice == null || r.getPrice().compareTo(maxPrice) <= 0)
        .filter(r -> balcony == null || !balcony || r.isBalcony())
        .filter(r -> petFriendly == null || !petFriendly || r.isPetFriendly())
        .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
        .filter(
            r ->
                checkIn == null
                    || checkOut == null
                    || !reservations
                        .existsByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(
                            r.getId(), ReservationStatus.CANCELLED, checkOut, checkIn))
        .toList();
  }

  @GetMapping("/rooms/all")
  List<Room> allRooms() {
    return rooms.findAll();
  }

  @PostMapping("/rooms")
  ResponseEntity<?> createRoom(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody Room room) {
    requireUser(authorization, Role.ADMIN);
    room.setId(null);
    if (room.getStatus() == null) {
      room.setStatus(RoomStatus.AVAILABLE);
    }
    normalizeAndValidateRoom(room);
    try {
      return ResponseEntity.ok(rooms.saveAndFlush(room));
    } catch (DataIntegrityViolationException ex) {
      return roomConflict();
    }
  }

  @Transactional
  @PutMapping("/rooms/{id}")
  ResponseEntity<?> updateRoom(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody Room data) {
    requireUser(authorization, Role.ADMIN);
    rooms
        .findByIdForUpdate(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habitación no encontrada"));
    normalizeAndValidateRoom(data);
    data.setId(id);
    try {
      return ResponseEntity.ok(rooms.saveAndFlush(data));
    } catch (DataIntegrityViolationException ex) {
      return roomConflict();
    }
  }

  @Transactional
  @PatchMapping("/rooms/{id}/status")
  Room updateRoomStatus(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody RoomStatusRequest data) {
    requireUser(authorization, Role.ADMIN);
    var room =
        rooms
            .findByIdForUpdate(id)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Habitación no encontrada"));
    room.setStatus(data.status());
    return rooms.save(room);
  }

  @DeleteMapping("/rooms/{id}")
  void deleteRoom(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireUser(authorization, Role.ADMIN);
    rooms.deleteById(id);
  }

  private void validateSearchDates(LocalDate checkIn, LocalDate checkOut) {
    if (checkIn == null || checkOut == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Selecciona las fechas de llegada y salida");
    }
    if (checkIn.isBefore(LocalDate.now())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La llegada no puede ser anterior a hoy");
    }
    if (!checkOut.isAfter(checkIn)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La salida debe ser posterior a la llegada");
    }
  }

  private void normalizeAndValidateRoom(Room room) {
    if (room.getStatus() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
    }
    if (room.getNumber() == null
        || room.getNumber().isBlank()
        || room.getType() == null
        || room.getType().isBlank()
        || room.getFloor() < 1
        || room.getCapacity() < 1
        || room.getPrice() == null
        || room.getPrice().signum() < 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Revisa el número, tipo, piso, capacidad y precio de la habitación");
    }
    room.setNumber(room.getNumber().trim());
    room.setType(room.getType().trim());
    if (room.getServices() == null) {
      room.setServices("");
    } else {
      room.setServices(room.getServices().trim());
    }
  }

  private ResponseEntity<?> roomConflict() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("message", "Ya existe una habitación con ese número"));
  }

  @GetMapping("/reservations")
  List<ReservationView> getReservations(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) Long guestId) {
    requireUser(authorization, Role.ADMIN);
    var list =
        guestId == null
            ? reservations.findAll()
            : reservations.findByGuestIdOrderByCheckInDesc(guestId);
    return list.stream().map(this::view).toList();
  }

  @GetMapping("/reservations/mine")
  List<ReservationView> myReservations(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    final var guest = requireUser(authorization, Role.GUEST);
    return reservations.findByGuestIdOrderByCheckInDesc(guest.getId()).stream()
        .map(this::view)
        .toList();
  }

  @Transactional
  @PostMapping("/reservations")
  ResponseEntity<?> createReservation(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody Reservation r) {
    final var guest = requireUser(authorization, Role.GUEST);
    try {
      validateSearchDates(r.getCheckIn(), r.getCheckOut());
    } catch (ResponseStatusException ex) {
      return ResponseEntity.badRequest().body(Map.of("message", ex.getReason()));
    }
    var room =
        rooms
            .findByIdForUpdate(r.getRoomId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Habitación no encontrada"));
    if (room.getStatus() != RoomStatus.AVAILABLE) {
      return ResponseEntity.status(409).body(Map.of("message", "La habitación no está disponible"));
    }
    if (r.getGuests() < 1 || r.getGuests() > room.getCapacity()) {
      return ResponseEntity.badRequest()
          .body(Map.of("message", "La habitación no admite esa cantidad de huéspedes"));
    }
    if (reservations.existsByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(
        r.getRoomId(), ReservationStatus.CANCELLED, r.getCheckOut(), r.getCheckIn())) {
      return ResponseEntity.status(409)
          .body(Map.of("message", "La habitación ya no está disponible en esas fechas"));
    }
    r.setGuestId(guest.getId());
    r.setAmount(
        room.getPrice()
            .multiply(
                BigDecimal.valueOf(ChronoUnit.DAYS.between(r.getCheckIn(), r.getCheckOut()))));
    r.setStatus(ReservationStatus.CONFIRMED);
    if (!r.getCheckIn().isAfter(LocalDate.now()) && r.getCheckOut().isAfter(LocalDate.now())) {
      room.setStatus(RoomStatus.OCCUPIED);
      rooms.save(room);
    }
    var saved = view(reservations.save(r));
    // HU-14: al confirmarse la reserva el ingreso se suma automáticamente al balance del hotel.
    finance.save(
        new FinanceEntry(
            EntryType.INCOME,
            "Reserva #" + saved.id() + " · Habitación " + saved.roomNumber(),
            saved.total(),
            LocalDate.now()));
    return ResponseEntity.ok(saved);
  }

  @Transactional
  @PutMapping("/reservations/{id}")
  ReservationView updateReservation(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody Reservation data) {
    requireUser(authorization, Role.ADMIN);
    var r = reservations.findById(id).orElseThrow();
    if (data.getCheckIn() != null) {
      r.setCheckIn(data.getCheckIn());
    }
    if (data.getCheckOut() != null) {
      r.setCheckOut(data.getCheckOut());
    }
    if (data.getGuests() > 0) {
      r.setGuests(data.getGuests());
    }
    if (data.getStatus() != null) {
      r.setStatus(data.getStatus());
      var room = rooms.findByIdForUpdate(r.getRoomId()).orElseThrow();
      if (data.getStatus() == ReservationStatus.CHECKED_IN) {
        room.setStatus(RoomStatus.OCCUPIED);
      }
      if (data.getStatus() == ReservationStatus.COMPLETED
          || data.getStatus() == ReservationStatus.CANCELLED) {
        room.setStatus(RoomStatus.AVAILABLE);
      }
      rooms.save(room);
    }
    if (data.getNotes() != null) {
      r.setNotes(data.getNotes());
    }
    return view(reservations.save(r));
  }

  @Transactional
  @DeleteMapping("/reservations/{id}")
  void cancelReservation(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    var actor = requireUser(authorization, null);
    var r = reservations.findById(id).orElseThrow();
    if (actor.getRole() != Role.ADMIN && !Objects.equals(actor.getId(), r.getGuestId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes cancelar esta reserva");
    }
    if (r.getStatus() == ReservationStatus.CANCELLED) {
      return;
    }
    r.setStatus(ReservationStatus.CANCELLED);
    reservations.save(r);
    var room = rooms.findByIdForUpdate(r.getRoomId()).orElseThrow();
    if (room.getStatus() == RoomStatus.OCCUPIED) {
      room.setStatus(RoomStatus.AVAILABLE);
    }
    rooms.save(room);
    // HU-14: se registra el reembolso para que el balance siga siendo coherente tras la
    // cancelación.
    var v = view(r);
    finance.save(
        new FinanceEntry(
            EntryType.EXPENSE,
            "Reembolso reserva #" + v.id() + " · Habitación " + v.roomNumber(),
            v.total(),
            LocalDate.now()));
  }

  @GetMapping("/requests")
  List<ServiceRequest> getRequests(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) Long guestId) {
    var actor = requireUser(authorization, null);
    if (actor.getRole() == Role.GUEST) {
      if (guestId == null || !Objects.equals(guestId, actor.getId())) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Solo puedes consultar tus solicitudes");
      }
      return requests.findByGuestIdOrderByCreatedAtDesc(actor.getId());
    }
    return guestId == null
        ? requests.findAll()
        : requests.findByGuestIdOrderByCreatedAtDesc(guestId);
  }

  @PostMapping("/requests")
  ServiceRequest createRequest(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody ServiceRequest r) {
    var guest = requireUser(authorization, Role.GUEST);
    r.setGuestId(guest.getId());
    r.setStatus(RequestStatus.OPEN);
    r.setCreatedAt(LocalDateTime.now());
    return requests.save(r);
  }

  @PutMapping("/requests/{id}")
  ServiceRequest updateRequest(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody ServiceRequest data) {
    requireStaff(authorization);
    var r = requests.findById(id).orElseThrow();
    if (data.getStatus() != null) {
      r.setStatus(data.getStatus());
    }
    return requests.save(r);
  }

  @GetMapping("/tasks")
  List<StaffTask> getTasks(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) Long employeeId) {
    var actor = requireUser(authorization, null);
    if (actor.getRole() == Role.GUEST) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El rol no permite esta acción");
    }
    if (actor.getRole() == Role.EMPLOYEE) {
      if (employeeId != null && !Objects.equals(employeeId, actor.getId())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes consultar tus tareas");
      }
      return tasks.findByEmployeeIdOrderByDueDateAsc(actor.getId());
    }
    return employeeId == null
        ? tasks.findAll()
        : tasks.findByEmployeeIdOrderByDueDateAsc(employeeId);
  }

  /** HU-26: devuelve exclusivamente las tareas del empleado autenticado. */
  @GetMapping("/tasks/mine")
  List<StaffTask> getMyTasks(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    var employee = requireUser(authorization, Role.EMPLOYEE);
    return tasks.findByEmployeeIdOrderByDueDateAsc(employee.getId());
  }

  /** HU-25: asigna una tarea nueva a una cuenta de empleado válida. */
  @PostMapping("/tasks")
  StaffTask createTask(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody TaskCreateRequest input) {
    requireUser(authorization, Role.ADMIN);
    var employee =
        users
            .findById(input.employeeId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));
    if (employee.getRole() != Role.EMPLOYEE) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La tarea solo puede asignarse a un empleado");
    }
    return tasks.save(
        new StaffTask(
            employee.getId(),
            input.title(),
            input.description(),
            TaskStatus.PENDING,
            input.dueDate(),
            input.priority()));
  }

  /** Edición administrativa de los datos de una tarea existente. */
  @PutMapping("/tasks/{id}")
  StaffTask updateTask(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody StaffTask data) {
    requireUser(authorization, Role.ADMIN);
    var t =
        tasks
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
    if (data.getStatus() != null) {
      t.setStatus(data.getStatus());
    }
    if (data.getTitle() != null) {
      t.setTitle(data.getTitle());
    }
    if (data.getDescription() != null) {
      t.setDescription(data.getDescription());
    }
    if (data.getDueDate() != null) {
      t.setDueDate(data.getDueDate());
    }
    if (data.getPriority() != null) {
      t.setPriority(data.getPriority());
    }
    return tasks.save(t);
  }

  /** HU-27: permite que el empleado inicie una tarea pendiente sin saltarse el cierre. */
  @PatchMapping("/tasks/{id}/status")
  StaffTask updateMyTaskStatus(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody TaskStatusRequest input) {
    var employee = requireUser(authorization, Role.EMPLOYEE);
    var task =
        tasks
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
    if (!Objects.equals(task.getEmployeeId(), employee.getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "La tarea no está asignada a este empleado");
    }
    if (task.getStatus() == input.status()) {
      return task;
    }
    if (task.getStatus() != TaskStatus.PENDING || input.status() != TaskStatus.IN_PROGRESS) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Solo se puede cambiar una tarea pendiente a en progreso; "
              + "usa completar para finalizarla");
    }
    task.setStatus(TaskStatus.IN_PROGRESS);
    return tasks.save(task);
  }

  /** HU-28: finaliza una tarea en progreso y conserva una fotografía opcional como evidencia. */
  @PostMapping(value = "/tasks/{id}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<?> completeTask(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestPart(value = "photo", required = false) MultipartFile photo)
      throws IOException {
    var employee = requireUser(authorization, Role.EMPLOYEE);
    var task =
        tasks
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
    if (!Objects.equals(task.getEmployeeId(), employee.getId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("message", "La tarea no está asignada a este empleado"));
    }
    if (task.getStatus() != TaskStatus.IN_PROGRESS) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Solo se puede completar una tarea que esté en progreso");
    }
    if (photo != null && !photo.isEmpty()) {
      if (photo.getSize() > 5 * 1024 * 1024) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "La fotografía no puede superar 5 MB"));
      }
      if (photo.getContentType() == null || !photo.getContentType().startsWith("image/")) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "El archivo debe ser una imagen"));
      }
      task.setCompletionPhoto(photo.getBytes());
      task.setCompletionPhotoName(photo.getOriginalFilename());
      task.setCompletionPhotoContentType(photo.getContentType());
    }
    task.setStatus(TaskStatus.DONE);
    task.setCompletedAt(LocalDateTime.now());
    return ResponseEntity.ok(tasks.save(task));
  }

  @GetMapping("/tasks/{id}/photo")
  ResponseEntity<byte[]> taskPhoto(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireUser(authorization, Role.ADMIN);
    var task = tasks.findById(id).orElseThrow();
    if (!task.getHasCompletionPhoto()) {
      return ResponseEntity.notFound().build();
    }
    var type =
        task.getCompletionPhotoContentType() == null
            ? MediaType.APPLICATION_OCTET_STREAM
            : MediaType.parseMediaType(task.getCompletionPhotoContentType());
    return ResponseEntity.ok()
        .contentType(type)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "inline; filename=\"" + task.getCompletionPhotoName().replace("\"", "") + "\"")
        .body(task.getCompletionPhoto());
  }

  @GetMapping("/finance")
  List<FinanceEntry> getFinance(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    requireUser(authorization, Role.ADMIN);
    return finance.findAll();
  }

  @PostMapping("/finance")
  FinanceEntry createFinance(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody FinanceEntry e) {
    requireUser(authorization, Role.ADMIN);
    if (e.getDate() == null) {
      e.setDate(LocalDate.now());
    }
    return finance.save(e);
  }

  // HU-15: recordatorios programados de gastos necesarios.
  @GetMapping("/reminders")
  List<ExpenseReminder> getReminders(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    requireUser(authorization, Role.ADMIN);
    return reminders.findAllByOrderByDueDateAsc();
  }

  @PostMapping("/reminders")
  ExpenseReminder createReminder(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody ExpenseReminder r) {
    requireUser(authorization, Role.ADMIN);
    if (r.getStatus() == null) {
      r.setStatus(ReminderStatus.PENDING);
    }
    if (r.getDueDate() == null) {
      r.setDueDate(LocalDate.now());
    }
    return reminders.save(r);
  }

  @PutMapping("/reminders/{id}")
  ExpenseReminder updateReminder(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody ExpenseReminder data) {
    requireUser(authorization, Role.ADMIN);
    var r = reminders.findById(id).orElseThrow();
    if (data.getStatus() != null) {
      r.setStatus(data.getStatus());
    }
    if (data.getConcept() != null) {
      r.setConcept(data.getConcept());
    }
    if (data.getDueDate() != null) {
      r.setDueDate(data.getDueDate());
    }
    return reminders.save(r);
  }

  @DeleteMapping("/reminders/{id}")
  void deleteReminder(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireUser(authorization, Role.ADMIN);
    reminders.deleteById(id);
  }

  /** Marca el recordatorio como atendido y registra el gasto real en el balance. */
  @PostMapping("/reminders/{id}/pay")
  FinanceEntry payReminder(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireUser(authorization, Role.ADMIN);
    var r = reminders.findById(id).orElseThrow();
    r.setStatus(ReminderStatus.DONE);
    reminders.save(r);
    return finance.save(
        new FinanceEntry(
            EntryType.EXPENSE, r.getConcept(), r.getEstimatedAmount(), LocalDate.now()));
  }

  @GetMapping("/dashboard")
  Map<String, Object> dashboard(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    requireUser(authorization, Role.ADMIN);
    var allRooms = rooms.findAll();
    var allRes = reservations.findAll();
    var entries = finance.findAll();
    var income =
        entries.stream()
            .filter(e -> e.getType() == EntryType.INCOME)
            .map(FinanceEntry::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    var expense =
        entries.stream()
            .filter(e -> e.getType() == EntryType.EXPENSE)
            .map(FinanceEntry::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long available = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).count();
    long occupied = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.OCCUPIED).count();
    long reserved = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.RESERVED).count();
    long maintenance =
        allRooms.stream().filter(r -> r.getStatus() == RoomStatus.MAINTENANCE).count();
    long outOfService =
        allRooms.stream().filter(r -> r.getStatus() == RoomStatus.OUT_OF_SERVICE).count();
    long occupancyRate = allRooms.isEmpty() ? 0 : Math.round(occupied * 100.0 / allRooms.size());
    long active =
        allRes.stream()
            .filter(
                r ->
                    r.getStatus() != ReservationStatus.CANCELLED
                        && r.getStatus() != ReservationStatus.COMPLETED)
            .count();
    int suggested = Math.max(1, (int) Math.ceil((occupied + active) / 4.0));
    long openRequests =
        requests.findAll().stream().filter(x -> x.getStatus() != RequestStatus.RESOLVED).count();
    var pending =
        reminders.findAllByOrderByDueDateAsc().stream()
            .filter(x -> x.getStatus() == ReminderStatus.PENDING)
            .toList();
    long dueSoon =
        pending.stream()
            .filter(
                x -> x.getDueDate() != null && !x.getDueDate().isAfter(LocalDate.now().plusDays(7)))
            .count();
    var result = new LinkedHashMap<String, Object>();
    result.put("rooms", allRooms.size());
    result.put("available", available);
    result.put("occupied", occupied);
    result.put("reserved", reserved);
    result.put("maintenance", maintenance);
    result.put("outOfService", outOfService);
    result.put("occupancyRate", occupancyRate);
    result.put("activeReservations", active);
    result.put(
        "openTasks",
        tasks.findAll().stream().filter(t -> t.getStatus() != TaskStatus.DONE).count());
    result.put("income", income);
    result.put("expenses", expense);
    result.put("balance", income.subtract(expense));
    result.put("suggestedCleaners", suggested);
    result.put("openRequests", openRequests);
    result.put("pendingReminders", pending.size());
    result.put("remindersDueSoon", dueSoon);
    return result;
  }

  @GetMapping("/assistant")
  Map<String, String> assistant(@RequestParam String q) {
    String s = q.toLowerCase();
    String answer =
        s.contains("desayuno")
            ? "El desayuno se sirve de 6:30 a 10:00 a. m. en el restaurante del primer piso."
            : s.contains("mascota") || s.contains("pet")
                ? "Tenemos habitaciones pet-friendly. "
                    + "Puedes identificarlas con el filtro ‘Mascotas’ al buscar."
                : s.contains("check")
                    ? "El check-in es desde las 3:00 p. m. y el check-out hasta las 12:00 m."
                    : s.contains("wifi")
                        ? "El Wi-Fi está incluido en todas las habitaciones. "
                            + "La clave se entrega al hacer check-in."
                        : s.contains("piscina")
                            ? "La piscina está abierta todos los días de 8:00 a. m. a 8:00 p. m."
                            : "Puedo ayudarte con horarios, desayuno, Wi-Fi, piscina, mascotas, "
                                + "check-in y servicios del hotel.";
    return Map.of("answer", answer);
  }

  private ReservationView view(Reservation r) {
    var room = rooms.findById(r.getRoomId()).orElse(null);
    var guest = users.findById(r.getGuestId()).orElse(null);
    long nights =
        r.getCheckIn() != null && r.getCheckOut() != null
            ? Math.max(1, ChronoUnit.DAYS.between(r.getCheckIn(), r.getCheckOut()))
            : 1;
    BigDecimal calculated =
        room == null ? BigDecimal.ZERO : room.getPrice().multiply(BigDecimal.valueOf(nights));
    BigDecimal total = r.getAmount() == null ? calculated : r.getAmount();
    return new ReservationView(
        r.getId(),
        r.getGuestId(),
        guest == null ? "Huésped" : guest.getName(),
        r.getRoomId(),
        room == null ? "—" : room.getNumber(),
        room == null ? "—" : room.getType(),
        r.getCheckIn(),
        r.getCheckOut(),
        r.getGuests(),
        r.getStatus(),
        r.getNotes(),
        total);
  }

  private PublicUser publicUser(UserAccount u) {
    return new PublicUser(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getPhone());
  }

  private AuthResponse authResponse(UserAccount u) {
    var token = UUID.randomUUID().toString();
    sessions.save(new AuthSession(token, u.getId(), LocalDateTime.now().plusHours(12)));
    return new AuthResponse(publicUser(u), token);
  }

  private Optional<String> tokenFrom(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return Optional.empty();
    }
    return Optional.of(authorization.substring(7).trim()).filter(x -> !x.isBlank());
  }

  private UserAccount requireUser(String authorization, Role role) {
    var token =
        tokenFrom(authorization)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión requerida"));
    var session =
        sessions
            .findByToken(token)
            .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión venció"));
    var user =
        users
            .findById(session.getUserId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    if (role != null && user.getRole() != role) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El rol no permite esta acción");
    }
    return user;
  }

  private UserAccount requireStaff(String authorization) {
    var user = requireUser(authorization, null);
    if (user.getRole() == Role.GUEST) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El rol no permite esta acción");
    }
    return user;
  }
}
