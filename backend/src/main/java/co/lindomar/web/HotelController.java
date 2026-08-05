package co.lindomar.web;

import co.lindomar.domain.Domain.*;
import co.lindomar.repository.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
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
 private final ReminderRepository reminders;
 public HotelController(UserRepository u,RoomRepository r,ReservationRepository rs,RequestRepository rq,TaskRepository t,FinanceRepository f,ReminderRepository rm){users=u;rooms=r;reservations=rs;requests=rq;tasks=t;finance=f;reminders=rm;}

 public record LoginRequest(String email,String password){}
 public record RegisterRequest(String name,String email,String password,String phone){}
 public record PublicUser(Long id,String name,String email,Role role,String phone){}
 public record ReservationView(Long id,Long guestId,String guestName,Long roomId,String roomNumber,String roomType,LocalDate checkIn,LocalDate checkOut,int guests,ReservationStatus status,String notes,BigDecimal total){}

 @PostMapping("/auth/login") ResponseEntity<?> login(@RequestBody LoginRequest input){
  return users.findByEmailIgnoreCase(input.email()).filter(u->Objects.equals(u.getPassword(),input.password()))
   .<ResponseEntity<?>>map(u->ResponseEntity.ok(new PublicUser(u.getId(),u.getName(),u.getEmail(),u.getRole(),u.getPhone())))
   .orElseGet(()->ResponseEntity.status(401).body(Map.of("message","Correo o contraseña incorrectos")));
 }

 /** HU-02: registro de nuevos huéspedes. Siempre crea cuentas con rol GUEST. */
 @PostMapping("/auth/register") ResponseEntity<?> register(@RequestBody RegisterRequest input){
  if(input.name()==null||input.name().isBlank())return ResponseEntity.badRequest().body(Map.of("message","El nombre es obligatorio"));
  if(input.email()==null||!input.email().contains("@"))return ResponseEntity.badRequest().body(Map.of("message","Ingrese un correo electrónico válido"));
  if(input.password()==null||input.password().length()<6)return ResponseEntity.badRequest().body(Map.of("message","La contraseña debe tener al menos 6 caracteres"));
  if(users.findByEmailIgnoreCase(input.email()).isPresent())return ResponseEntity.status(409).body(Map.of("message","Ya existe una cuenta registrada con ese correo"));
  var saved=users.save(new UserAccount(input.name().trim(),input.email().trim(),input.password(),Role.GUEST,input.phone()));
  return ResponseEntity.ok(new PublicUser(saved.getId(),saved.getName(),saved.getEmail(),saved.getRole(),saved.getPhone()));
 }

 @GetMapping("/users") List<PublicUser> allUsers(){return users.findAll().stream().map(u->new PublicUser(u.getId(),u.getName(),u.getEmail(),u.getRole(),u.getPhone())).toList();}
 @PutMapping("/users/{id}") PublicUser updateUser(@PathVariable Long id,@RequestBody UserAccount data){var u=users.findById(id).orElseThrow(); if(data.getName()!=null)u.setName(data.getName());if(data.getPhone()!=null)u.setPhone(data.getPhone());if(data.getRole()!=null)u.setRole(data.getRole());users.save(u);return new PublicUser(u.getId(),u.getName(),u.getEmail(),u.getRole(),u.getPhone());}

 @GetMapping("/rooms") List<Room> getRooms(@RequestParam(required=false)String type,@RequestParam(required=false)Integer capacity,@RequestParam(required=false)BigDecimal maxPrice,@RequestParam(required=false)Boolean balcony,@RequestParam(required=false)Boolean petFriendly,@RequestParam(required=false)LocalDate checkIn,@RequestParam(required=false)LocalDate checkOut){
  return rooms.findAll().stream().filter(r->type==null||type.isBlank()||r.getType().equalsIgnoreCase(type)).filter(r->capacity==null||r.getCapacity()>=capacity).filter(r->maxPrice==null||r.getPrice().compareTo(maxPrice)<=0).filter(r->balcony==null||!balcony||r.isBalcony()).filter(r->petFriendly==null||!petFriendly||r.isPetFriendly()).filter(r->r.getStatus()==RoomStatus.AVAILABLE).filter(r->checkIn==null||checkOut==null||!reservations.existsByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(r.getId(),ReservationStatus.CANCELLED,checkOut,checkIn)).toList();
 }
 @GetMapping("/rooms/all") List<Room> allRooms(){return rooms.findAll();}
 @PostMapping("/rooms") Room createRoom(@RequestBody Room room){if(room.getStatus()==null)room.setStatus(RoomStatus.AVAILABLE);return rooms.save(room);}
 @PutMapping("/rooms/{id}") Room updateRoom(@PathVariable Long id,@RequestBody Room data){data.setId(id);return rooms.save(data);}
 @DeleteMapping("/rooms/{id}") void deleteRoom(@PathVariable Long id){rooms.deleteById(id);}

 @GetMapping("/reservations") List<ReservationView> getReservations(@RequestParam(required=false)Long guestId){var list=guestId==null?reservations.findAll():reservations.findByGuestIdOrderByCheckInDesc(guestId);return list.stream().map(this::view).toList();}
 @PostMapping("/reservations") ResponseEntity<?> createReservation(@RequestBody Reservation r){
  if(r.getCheckIn()==null||r.getCheckOut()==null||!r.getCheckOut().isAfter(r.getCheckIn()))return ResponseEntity.badRequest().body(Map.of("message","Las fechas seleccionadas no son válidas"));
  var room=rooms.findById(r.getRoomId()).orElseThrow();
  if(r.getGuests()>room.getCapacity())return ResponseEntity.badRequest().body(Map.of("message","La habitación no admite esa cantidad de huéspedes"));
  if(reservations.existsByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(r.getRoomId(),ReservationStatus.CANCELLED,r.getCheckOut(),r.getCheckIn()))return ResponseEntity.status(409).body(Map.of("message","La habitación ya no está disponible en esas fechas"));
  r.setStatus(ReservationStatus.CONFIRMED);
  var saved=view(reservations.save(r));
  // HU-14: al confirmarse la reserva el ingreso se suma automáticamente al balance del hotel.
  finance.save(new FinanceEntry(EntryType.INCOME,"Reserva #"+saved.id()+" · Habitación "+saved.roomNumber(),saved.total(),LocalDate.now()));
  return ResponseEntity.ok(saved);
 }
 @PutMapping("/reservations/{id}") ReservationView updateReservation(@PathVariable Long id,@RequestBody Reservation data){var r=reservations.findById(id).orElseThrow();if(data.getCheckIn()!=null)r.setCheckIn(data.getCheckIn());if(data.getCheckOut()!=null)r.setCheckOut(data.getCheckOut());if(data.getGuests()>0)r.setGuests(data.getGuests());if(data.getStatus()!=null)r.setStatus(data.getStatus());if(data.getNotes()!=null)r.setNotes(data.getNotes());return view(reservations.save(r));}
 @DeleteMapping("/reservations/{id}") void cancelReservation(@PathVariable Long id){
  var r=reservations.findById(id).orElseThrow();
  if(r.getStatus()==ReservationStatus.CANCELLED)return;
  r.setStatus(ReservationStatus.CANCELLED);reservations.save(r);
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

 private ReservationView view(Reservation r){var room=rooms.findById(r.getRoomId()).orElse(null);var guest=users.findById(r.getGuestId()).orElse(null);long nights=r.getCheckIn()!=null&&r.getCheckOut()!=null?Math.max(1,ChronoUnit.DAYS.between(r.getCheckIn(),r.getCheckOut())):1;BigDecimal total=room==null?BigDecimal.ZERO:room.getPrice().multiply(BigDecimal.valueOf(nights));return new ReservationView(r.getId(),r.getGuestId(),guest==null?"Huésped":guest.getName(),r.getRoomId(),room==null?"—":room.getNumber(),room==null?"—":room.getType(),r.getCheckIn(),r.getCheckOut(),r.getGuests(),r.getStatus(),r.getNotes(),total);}
}

