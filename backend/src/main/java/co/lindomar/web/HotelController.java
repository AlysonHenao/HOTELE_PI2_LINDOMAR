package co.lindomar.web;

import co.lindomar.domain.Domain.*;
import co.lindomar.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins="*")
public class HotelController {
 private final UserRepository users; private final RoomRepository rooms; private final ReservationRepository reservations;
 private final RequestRepository requests; private final TaskRepository tasks; private final FinanceRepository finance;
 private final ReminderRepository reminders; private final AuthSessionRepository sessions;
 private final PasswordEncoder passwords;
 public HotelController(UserRepository u,RoomRepository r,ReservationRepository rs,RequestRepository rq,TaskRepository t,FinanceRepository f,ReminderRepository rm,AuthSessionRepository s,PasswordEncoder passwords){users=u;rooms=r;reservations=rs;requests=rq;tasks=t;finance=f;reminders=rm;sessions=s;this.passwords=passwords;}

 public record LoginRequest(String email,String password){}
 public record RegisterRequest(
  @NotBlank(message="El nombre es obligatorio") @Size(max=255) String name,
  @NotBlank(message="El correo es obligatorio") @Email(message="Ingrese un correo electrónico válido") @Size(max=255) String email,
  @NotBlank(message="La contraseña es obligatoria") @Size(min=6,max=72,message="La contraseña debe tener entre 6 y 72 caracteres") String password,
  @Size(max=255) String phone){
  public RegisterRequest { if(name!=null)name=name.trim(); if(email!=null)email=email.trim().toLowerCase(Locale.ROOT); }
 }
 public record RoomStatusRequest(@NotNull(message="El estado es obligatorio") RoomStatus status){}
 public record PublicUser(Long id,String name,String email,Role role,String phone){}
 public record AuthResponse(PublicUser user,String token){}
 public record ReservationView(Long id,Long guestId,String guestName,Long roomId,String roomNumber,String roomType,LocalDate checkIn,LocalDate checkOut,int guests,ReservationStatus status,String notes,BigDecimal total){}

 @PostMapping("/auth/login") ResponseEntity<?> login(@RequestBody LoginRequest input){
  if(input.email()==null||input.password()==null||input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)return ResponseEntity.status(401).body(Map.of("message","Correo o contraseña incorrectos"));
  return users.findByEmailIgnoreCase(input.email().trim()).filter(u->passwords.matches(input.password(),u.getPassword()))
   .<ResponseEntity<?>>map(u->ResponseEntity.ok(authResponse(u)))
   .orElseGet(()->ResponseEntity.status(401).body(Map.of("message","Correo o contraseña incorrectos")));
 }

 /** HU-01: registro de nuevos huéspedes. Siempre crea cuentas con rol GUEST. */
 @PostMapping("/auth/register") ResponseEntity<?> register(@Valid @RequestBody RegisterRequest input){
  if(input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)return ResponseEntity.badRequest().body(Map.of("message","La contraseña no puede superar 72 bytes UTF-8"));
  if(users.findByEmailIgnoreCase(input.email()).isPresent())return ResponseEntity.status(409).body(Map.of("message","Ya existe una cuenta registrada con ese correo"));
  UserAccount saved;
  try { saved=users.saveAndFlush(new UserAccount(input.name(),input.email(),passwords.encode(input.password()),Role.GUEST,input.phone())); }
  catch(DataIntegrityViolationException ex){return ResponseEntity.status(409).body(Map.of("message","Ya existe una cuenta registrada con ese correo"));}
  return ResponseEntity.ok(authResponse(saved));
 }
 @GetMapping("/auth/me") PublicUser currentUser(@RequestHeader(value="Authorization",required=false)String authorization){return publicUser(requireUser(authorization,null));}
 @Transactional
 @PostMapping("/auth/logout") ResponseEntity<Void> logout(@RequestHeader(value="Authorization",required=false)String authorization){tokenFrom(authorization).ifPresent(sessions::deleteByToken);return ResponseEntity.noContent().build();}

 @GetMapping("/users") List<PublicUser> allUsers(){return users.findAll().stream().map(u->new PublicUser(u.getId(),u.getName(),u.getEmail(),u.getRole(),u.getPhone())).toList();}
 @PutMapping("/users/{id}") PublicUser updateUser(@RequestHeader(value="Authorization",required=false)String authorization,@PathVariable Long id,@RequestBody UserAccount data){requireUser(authorization,Role.ADMIN);var u=users.findById(id).orElseThrow(); if(data.getName()!=null)u.setName(data.getName());if(data.getPhone()!=null)u.setPhone(data.getPhone());if(data.getRole()!=null)u.setRole(data.getRole());users.save(u);return new PublicUser(u.getId(),u.getName(),u.getEmail(),u.getRole(),u.getPhone());}

 @GetMapping("/rooms") List<Room> getRooms(@RequestParam(required=false)String type,@RequestParam(required=false)Integer capacity,@RequestParam(required=false)BigDecimal maxPrice,@RequestParam(required=false)Boolean balcony,@RequestParam(required=false)Boolean petFriendly,@RequestParam(required=false)LocalDate checkIn,@RequestParam(required=false)LocalDate checkOut){
  if(checkIn!=null||checkOut!=null)validateSearchDates(checkIn,checkOut);
  return rooms.findAll().stream().filter(r->type==null||type.isBlank()||r.getType().equalsIgnoreCase(type)).filter(r->capacity==null||r.getCapacity()>=capacity).filter(r->maxPrice==null||r.getPrice().compareTo(maxPrice)<=0).filter(r->balcony==null||!balcony||r.isBalcony()).filter(r->petFriendly==null||!petFriendly||r.isPetFriendly()).filter(r->r.getStatus()==RoomStatus.AVAILABLE).filter(r->checkIn==null||checkOut==null||!reservations.existsByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(r.getId(),ReservationStatus.CANCELLED,checkOut,checkIn)).toList();
 }
 @GetMapping("/rooms/all") List<Room> allRooms(){return rooms.findAll();}
 @PostMapping("/rooms") Room createRoom(@RequestHeader(value="Authorization",required=false)String authorization,@RequestBody Room room){requireUser(authorization,Role.ADMIN);room.setId(null);if(room.getStatus()==null)room.setStatus(RoomStatus.AVAILABLE);validateRoom(room);return rooms.save(room);}
 @Transactional
 @PutMapping("/rooms/{id}") Room updateRoom(@RequestHeader(value="Authorization",required=false)String authorization,@PathVariable Long id,@RequestBody Room data){
  requireUser(authorization,Role.ADMIN);
  rooms.findByIdForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Habitación no encontrada"));
  validateRoom(data);data.setId(id);return rooms.save(data);
 }
 @Transactional
 @PatchMapping("/rooms/{id}/status") Room updateRoomStatus(@RequestHeader(value="Authorization",required=false)String authorization,@PathVariable Long id,@Valid @RequestBody RoomStatusRequest data){
  requireUser(authorization,Role.ADMIN);
  var room=rooms.findByIdForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Habitación no encontrada"));
  room.setStatus(data.status());return rooms.save(room);
 }
 @DeleteMapping("/rooms/{id}") void deleteRoom(@RequestHeader(value="Authorization",required=false)String authorization,@PathVariable Long id){requireUser(authorization,Role.ADMIN);rooms.deleteById(id);}

 private void validateSearchDates(LocalDate checkIn,LocalDate checkOut){
  if(checkIn==null||checkOut==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecciona las fechas de llegada y salida");
  if(checkIn.isBefore(LocalDate.now()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La llegada no puede ser anterior a hoy");
  if(!checkOut.isAfter(checkIn))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La salida debe ser posterior a la llegada");
 }
 private void validateRoom(Room room){
  if(room.getStatus()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El estado es obligatorio");
  if(room.getNumber()==null||room.getNumber().isBlank()||room.getType()==null||room.getType().isBlank()||room.getFloor()<1||room.getCapacity()<1||room.getPrice()==null||room.getPrice().signum()<0)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Revisa el número, tipo, piso, capacidad y precio de la habitación");
 }

 @GetMapping("/reservations") List<ReservationView> getReservations(@RequestParam(required=false)Long guestId){var list=guestId==null?reservations.findAll():reservations.findByGuestIdOrderByCheckInDesc(guestId);return list.stream().map(this::view).toList();}
 @GetMapping("/reservations/mine") List<ReservationView> myReservations(@RequestHeader(value="Authorization",required=false)String authorization){var guest=requireUser(authorization,Role.GUEST);return reservations.findByGuestIdOrderByCheckInDesc(guest.getId()).stream().map(this::view).toList();}
 @Transactional
 @PostMapping("/reservations") ResponseEntity<?> createReservation(@RequestHeader(value="Authorization",required=false)String authorization,@RequestBody Reservation r){
  var guest=requireUser(authorization,Role.GUEST);
  if(r.getCheckIn()==null||r.getCheckOut()==null||!r.getCheckOut().isAfter(r.getCheckIn()))return ResponseEntity.badRequest().body(Map.of("message","Las fechas seleccionadas no son válidas"));
  var room=rooms.findByIdForUpdate(r.getRoomId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Habitación no encontrada"));
  if(room.getStatus()!=RoomStatus.AVAILABLE)return ResponseEntity.status(409).body(Map.of("message","La habitación no está disponible"));
  if(r.getGuests()<1||r.getGuests()>room.getCapacity())return ResponseEntity.badRequest().body(Map.of("message","La habitación no admite esa cantidad de huéspedes"));
  if(reservations.existsByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(r.getRoomId(),ReservationStatus.CANCELLED,r.getCheckOut(),r.getCheckIn()))return ResponseEntity.status(409).body(Map.of("message","La habitación ya no está disponible en esas fechas"));
  r.setGuestId(guest.getId());
  r.setAmount(room.getPrice().multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(r.getCheckIn(),r.getCheckOut()))));
  r.setStatus(ReservationStatus.CONFIRMED);
  if(!r.getCheckIn().isAfter(LocalDate.now())&&r.getCheckOut().isAfter(LocalDate.now())){room.setStatus(RoomStatus.OCCUPIED);rooms.save(room);}
  var saved=view(reservations.save(r));
  // HU-14: al confirmarse la reserva el ingreso se suma automáticamente al balance del hotel.
  finance.save(new FinanceEntry(EntryType.INCOME,"Reserva #"+saved.id()+" · Habitación "+saved.roomNumber(),saved.total(),LocalDate.now()));
  return ResponseEntity.ok(saved);
 }
 @Transactional
 @PutMapping("/reservations/{id}") ReservationView updateReservation(@PathVariable Long id,@RequestBody Reservation data){var r=reservations.findById(id).orElseThrow();if(data.getCheckIn()!=null)r.setCheckIn(data.getCheckIn());if(data.getCheckOut()!=null)r.setCheckOut(data.getCheckOut());if(data.getGuests()>0)r.setGuests(data.getGuests());if(data.getStatus()!=null){r.setStatus(data.getStatus());var room=rooms.findByIdForUpdate(r.getRoomId()).orElseThrow();if(data.getStatus()==ReservationStatus.CHECKED_IN)room.setStatus(RoomStatus.OCCUPIED);if(data.getStatus()==ReservationStatus.COMPLETED||data.getStatus()==ReservationStatus.CANCELLED)room.setStatus(RoomStatus.AVAILABLE);rooms.save(room);}if(data.getNotes()!=null)r.setNotes(data.getNotes());return view(reservations.save(r));}
 @Transactional
 @DeleteMapping("/reservations/{id}") void cancelReservation(@PathVariable Long id){
  var r=reservations.findById(id).orElseThrow();
  if(r.getStatus()==ReservationStatus.CANCELLED)return;
  r.setStatus(ReservationStatus.CANCELLED);reservations.save(r);
  var room=rooms.findByIdForUpdate(r.getRoomId()).orElseThrow();
  if(room.getStatus()==RoomStatus.OCCUPIED)room.setStatus(RoomStatus.AVAILABLE);
  rooms.save(room);
  // HU-14: se registra el reembolso para que el balance siga siendo coherente tras la cancelación.
  var v=view(r);
  finance.save(new FinanceEntry(EntryType.EXPENSE,"Reembolso reserva #"+v.id()+" · Habitación "+v.roomNumber(),v.total(),LocalDate.now()));
 }

 @GetMapping("/requests") List<ServiceRequest> getRequests(@RequestParam(required=false)Long guestId){return guestId==null?requests.findAll():requests.findByGuestIdOrderByCreatedAtDesc(guestId);}
 @PostMapping("/requests") ServiceRequest createRequest(@RequestBody ServiceRequest r){r.setStatus(RequestStatus.OPEN);r.setCreatedAt(LocalDateTime.now());return requests.save(r);}
 @PutMapping("/requests/{id}") ServiceRequest updateRequest(@PathVariable Long id,@RequestBody ServiceRequest data){var r=requests.findById(id).orElseThrow();if(data.getStatus()!=null)r.setStatus(data.getStatus());return requests.save(r);}

 @GetMapping("/tasks") List<StaffTask> getTasks(@RequestParam(required=false)Long employeeId){return employeeId==null?tasks.findAll():tasks.findByEmployeeIdOrderByDueDateAsc(employeeId);}
 @PostMapping("/tasks") StaffTask createTask(@RequestBody StaffTask task){if(task.getStatus()==null)task.setStatus(TaskStatus.PENDING);return tasks.save(task);}
 @PutMapping("/tasks/{id}") StaffTask updateTask(@PathVariable Long id,@RequestBody StaffTask data){var t=tasks.findById(id).orElseThrow();if(data.getStatus()!=null)t.setStatus(data.getStatus());if(data.getTitle()!=null)t.setTitle(data.getTitle());if(data.getDescription()!=null)t.setDescription(data.getDescription());if(data.getDueDate()!=null)t.setDueDate(data.getDueDate());if(data.getPriority()!=null)t.setPriority(data.getPriority());return tasks.save(t);}
 @PostMapping(value="/tasks/{id}/complete",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) ResponseEntity<?> completeTask(@RequestHeader(value="Authorization",required=false)String authorization,@PathVariable Long id,@RequestPart(value="photo",required=false)MultipartFile photo) throws IOException{
  var employee=requireUser(authorization,Role.EMPLOYEE);
  var task=tasks.findById(id).orElseThrow();
  if(!Objects.equals(task.getEmployeeId(),employee.getId()))return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","La tarea no está asignada a este empleado"));
  if(photo!=null&&!photo.isEmpty()){
   if(photo.getSize()>5*1024*1024)return ResponseEntity.badRequest().body(Map.of("message","La fotografía no puede superar 5 MB"));
   if(photo.getContentType()==null||!photo.getContentType().startsWith("image/"))return ResponseEntity.badRequest().body(Map.of("message","El archivo debe ser una imagen"));
   task.setCompletionPhoto(photo.getBytes());task.setCompletionPhotoName(photo.getOriginalFilename());task.setCompletionPhotoContentType(photo.getContentType());
  }
  task.setStatus(TaskStatus.DONE);task.setCompletedAt(LocalDateTime.now());
  return ResponseEntity.ok(tasks.save(task));
 }
 @GetMapping("/tasks/{id}/photo") ResponseEntity<byte[]> taskPhoto(@RequestHeader(value="Authorization",required=false)String authorization,@PathVariable Long id){
  requireUser(authorization,Role.ADMIN);
  var task=tasks.findById(id).orElseThrow();
  if(!task.getHasCompletionPhoto())return ResponseEntity.notFound().build();
  var type=task.getCompletionPhotoContentType()==null?MediaType.APPLICATION_OCTET_STREAM:MediaType.parseMediaType(task.getCompletionPhotoContentType());
  return ResponseEntity.ok().contentType(type).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\""+task.getCompletionPhotoName().replace("\"","")+"\"").body(task.getCompletionPhoto());
 }

 @GetMapping("/finance") List<FinanceEntry> getFinance(){return finance.findAll();}
 @PostMapping("/finance") FinanceEntry createFinance(@RequestBody FinanceEntry e){if(e.getDate()==null)e.setDate(LocalDate.now());return finance.save(e);}
 // HU-15: recordatorios programados de gastos necesarios.
 @GetMapping("/reminders") List<ExpenseReminder> getReminders(){return reminders.findAllByOrderByDueDateAsc();}
 @PostMapping("/reminders") ExpenseReminder createReminder(@RequestBody ExpenseReminder r){if(r.getStatus()==null)r.setStatus(ReminderStatus.PENDING);if(r.getDueDate()==null)r.setDueDate(LocalDate.now());return reminders.save(r);}
 @PutMapping("/reminders/{id}") ExpenseReminder updateReminder(@PathVariable Long id,@RequestBody ExpenseReminder data){var r=reminders.findById(id).orElseThrow();if(data.getStatus()!=null)r.setStatus(data.getStatus());if(data.getConcept()!=null)r.setConcept(data.getConcept());if(data.getDueDate()!=null)r.setDueDate(data.getDueDate());return reminders.save(r);}
 @DeleteMapping("/reminders/{id}") void deleteReminder(@PathVariable Long id){reminders.deleteById(id);}

 /** Marca el recordatorio como atendido y registra el gasto real en el balance. */
 @PostMapping("/reminders/{id}/pay") FinanceEntry payReminder(@PathVariable Long id){
  var r=reminders.findById(id).orElseThrow();
  r.setStatus(ReminderStatus.DONE);reminders.save(r);
  return finance.save(new FinanceEntry(EntryType.EXPENSE,r.getConcept(),r.getEstimatedAmount(),LocalDate.now()));
 }

 @GetMapping("/dashboard") Map<String,Object> dashboard(){
  var allRooms=rooms.findAll();var allRes=reservations.findAll();var entries=finance.findAll();
  var income=entries.stream().filter(e->e.getType()==EntryType.INCOME).map(FinanceEntry::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
  var expense=entries.stream().filter(e->e.getType()==EntryType.EXPENSE).map(FinanceEntry::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
  long occupied=allRooms.stream().filter(r->r.getStatus()==RoomStatus.OCCUPIED).count();
  long active=allRes.stream().filter(r->r.getStatus()!=ReservationStatus.CANCELLED&&r.getStatus()!=ReservationStatus.COMPLETED).count();
  int suggested=Math.max(1,(int)Math.ceil((occupied+active)/4.0));
  long openRequests=requests.findAll().stream().filter(x->x.getStatus()!=RequestStatus.RESOLVED).count();
  var pending=reminders.findAllByOrderByDueDateAsc().stream().filter(x->x.getStatus()==ReminderStatus.PENDING).toList();
  long dueSoon=pending.stream().filter(x->x.getDueDate()!=null&&!x.getDueDate().isAfter(LocalDate.now().plusDays(7))).count();
  var result=new LinkedHashMap<String,Object>();
  result.put("rooms",allRooms.size());result.put("occupied",occupied);result.put("activeReservations",active);
  result.put("openTasks",tasks.findAll().stream().filter(t->t.getStatus()!=TaskStatus.DONE).count());
  result.put("income",income);result.put("expenses",expense);result.put("balance",income.subtract(expense));
  result.put("suggestedCleaners",suggested);result.put("openRequests",openRequests);
  result.put("pendingReminders",pending.size());result.put("remindersDueSoon",dueSoon);
  return result;
 }

 @GetMapping("/assistant") Map<String,String> assistant(@RequestParam String q){String s=q.toLowerCase();String answer=s.contains("desayuno")?"El desayuno se sirve de 6:30 a 10:00 a. m. en el restaurante del primer piso.":s.contains("mascota")||s.contains("pet")?"Tenemos habitaciones pet-friendly. Puedes identificarlas con el filtro ‘Mascotas’ al buscar.":s.contains("check")?"El check-in es desde las 3:00 p. m. y el check-out hasta las 12:00 m.":s.contains("wifi")?"El Wi-Fi está incluido en todas las habitaciones. La clave se entrega al hacer check-in.":s.contains("piscina")?"La piscina está abierta todos los días de 8:00 a. m. a 8:00 p. m.":"Puedo ayudarte con horarios, desayuno, Wi-Fi, piscina, mascotas, check-in y servicios del hotel.";return Map.of("answer",answer);}

 private ReservationView view(Reservation r){var room=rooms.findById(r.getRoomId()).orElse(null);var guest=users.findById(r.getGuestId()).orElse(null);long nights=r.getCheckIn()!=null&&r.getCheckOut()!=null?Math.max(1,ChronoUnit.DAYS.between(r.getCheckIn(),r.getCheckOut())):1;BigDecimal calculated=room==null?BigDecimal.ZERO:room.getPrice().multiply(BigDecimal.valueOf(nights));BigDecimal total=r.getAmount()==null?calculated:r.getAmount();return new ReservationView(r.getId(),r.getGuestId(),guest==null?"Huésped":guest.getName(),r.getRoomId(),room==null?"—":room.getNumber(),room==null?"—":room.getType(),r.getCheckIn(),r.getCheckOut(),r.getGuests(),r.getStatus(),r.getNotes(),total);}
 private PublicUser publicUser(UserAccount u){return new PublicUser(u.getId(),u.getName(),u.getEmail(),u.getRole(),u.getPhone());}
 private AuthResponse authResponse(UserAccount u){var token=UUID.randomUUID().toString();sessions.save(new AuthSession(token,u.getId(),LocalDateTime.now().plusHours(12)));return new AuthResponse(publicUser(u),token);}
 private Optional<String> tokenFrom(String authorization){if(authorization==null||!authorization.startsWith("Bearer "))return Optional.empty();return Optional.of(authorization.substring(7).trim()).filter(x->!x.isBlank());}
 private UserAccount requireUser(String authorization,Role role){var token=tokenFrom(authorization).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sesión requerida"));var session=sessions.findByToken(token).filter(s->s.getExpiresAt().isAfter(LocalDateTime.now())).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"La sesión venció"));var user=users.findById(session.getUserId()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Usuario no encontrado"));if(role!=null&&user.getRole()!=role)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"El rol no permite esta acción");return user;}
}

