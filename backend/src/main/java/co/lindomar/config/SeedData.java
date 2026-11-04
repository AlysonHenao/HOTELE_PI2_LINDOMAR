package co.lindomar.config;

import co.lindomar.domain.Domain.EntryType;
import co.lindomar.domain.Domain.ExpenseReminder;
import co.lindomar.domain.Domain.FinanceEntry;
import co.lindomar.domain.Domain.ReminderStatus;
import co.lindomar.domain.Domain.Role;
import co.lindomar.domain.Domain.Room;
import co.lindomar.domain.Domain.RoomStatus;
import co.lindomar.domain.Domain.StaffTask;
import co.lindomar.domain.Domain.TaskStatus;
import co.lindomar.domain.Domain.UserAccount;
import co.lindomar.repository.FinanceRepository;
import co.lindomar.repository.ReminderRepository;
import co.lindomar.repository.ReservationRepository;
import co.lindomar.repository.RoomRepository;
import co.lindomar.repository.TaskRepository;
import co.lindomar.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Carga datos de demostración y sincroniza las habitaciones reales de Sincelejo. */
@Configuration
@ConditionalOnProperty(
    name = "hotel.demo-data.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SeedData {
  @Bean
  CommandLineRunner seed(
      UserRepository users,
      RoomRepository rooms,
      ReservationRepository reservations,
      TaskRepository tasks,
      FinanceRepository finance,
      ReminderRepository reminders,
      PasswordEncoder encoder) {
    return args -> {
      syncSincelejoRooms(rooms, reservations);

      if (users.count() > 0) {
        return;
      }
      users.save(
          new UserAccount(
              "Mariana Torres",
              "huesped@lindomar.co",
              encoder.encode("demo123"),
              Role.GUEST,
              "300 555 0142"));
      final var employee =
          users.save(
              new UserAccount(
                  "Santiago Ruiz",
                  "empleado@lindomar.co",
                  encoder.encode("demo123"),
                  Role.EMPLOYEE,
                  "301 555 0188"));
      users.save(
          new UserAccount(
              "Laura Méndez",
              "admin@lindomar.co",
              encoder.encode("demo123"),
              Role.ADMIN,
              "315 555 0101"));
      users.save(
          new UserAccount(
              "Ana Gómez",
              "ana@lindomar.co",
              encoder.encode("demo123"),
              Role.EMPLOYEE,
              "312 555 0165"));
      tasks.saveAll(
          List.of(
              new StaffTask(
                  employee.getId(),
                  "Preparar habitación 201",
                  "Limpieza completa y reposición de minibar.",
                  TaskStatus.PENDING,
                  LocalDate.now(),
                  "Alta"),
              new StaffTask(
                  employee.getId(),
                  "Revisar solicitud 102",
                  "Llevar dos toallas adicionales.",
                  TaskStatus.IN_PROGRESS,
                  LocalDate.now(),
                  "Media"),
              new StaffTask(
                  employee.getId(),
                  "Inventario de lencería",
                  "Registrar faltantes del piso 1.",
                  TaskStatus.PENDING,
                  LocalDate.now().plusDays(1),
                  "Baja")));
      finance.saveAll(
          List.of(
              new FinanceEntry(
                  EntryType.INCOME,
                  "Reservas de la semana",
                  new BigDecimal("4850000"),
                  LocalDate.now()),
              new FinanceEntry(
                  EntryType.EXPENSE,
                  "Proveedor de lavandería",
                  new BigDecimal("720000"),
                  LocalDate.now().minusDays(1)),
              new FinanceEntry(
                  EntryType.EXPENSE,
                  "Mantenimiento preventivo",
                  new BigDecimal("350000"),
                  LocalDate.now().minusDays(2))));
      reminders.saveAll(
          List.of(
              new ExpenseReminder(
                  "Reposición de insumos de aseo",
                  "Insumos",
                  new BigDecimal("480000"),
                  LocalDate.now().plusDays(2),
                  ReminderStatus.PENDING),
              new ExpenseReminder(
                  "Pago factura de energía",
                  "Servicios",
                  new BigDecimal("1250000"),
                  LocalDate.now().plusDays(5),
                  ReminderStatus.PENDING),
              new ExpenseReminder(
                  "Mantenimiento del aire acondicionado",
                  "Mantenimiento",
                  new BigDecimal("640000"),
                  LocalDate.now().plusDays(12),
                  ReminderStatus.PENDING),
              new ExpenseReminder(
                  "Compra de lencería piso 2",
                  "Insumos",
                  new BigDecimal("890000"),
                  LocalDate.now().plusDays(21),
                  ReminderStatus.PENDING)));
    };
  }

  private void syncSincelejoRooms(RoomRepository rooms, ReservationRepository reservations) {
    record TargetRoom(
        String number,
        int floor,
        String type,
        int capacity,
        BigDecimal price,
        boolean balcony,
        boolean petFriendly,
        String services,
        String imageUrl) {}

    final var targetRooms =
        List.of(
            new TargetRoom(
                "101",
                1,
                "Sencilla",
                1,
                new BigDecimal("80000"),
                false,
                false,
                "Wi-Fi · Aire acondicionado · TV · Baño privado",
                "/rooms/habitacion-1.jpg"),
            new TargetRoom(
                "102",
                1,
                "Sencilla",
                1,
                new BigDecimal("80000"),
                true,
                false,
                "Wi-Fi · Ventilador · TV · Baño privado",
                "/rooms/habitacion-2.jpg"),
            new TargetRoom(
                "201",
                2,
                "Doble",
                2,
                new BigDecimal("100000"),
                false,
                false,
                "Wi-Fi · Aire acondicionado · TV · Baño privado",
                "/rooms/habitacion-1.jpg"),
            new TargetRoom(
                "202",
                2,
                "Doble",
                2,
                new BigDecimal("100000"),
                true,
                false,
                "Wi-Fi · Ventilador · TV · Baño privado",
                "/hotel/lobby.jpg"),
            new TargetRoom(
                "203",
                2,
                "Familiar",
                3,
                new BigDecimal("180000"),
                false,
                false,
                "Wi-Fi · Aire acondicionado · TV · Baño privado",
                "/hotel/sala.jpg"),
            new TargetRoom(
                "301",
                3,
                "Familiar",
                4,
                new BigDecimal("240000"),
                true,
                false,
                "Wi-Fi · Aire acondicionado · TV · Baño privado",
                "/hotel/recepcion.jpg"),
            new TargetRoom(
                "302",
                3,
                "Familiar",
                6,
                new BigDecimal("360000"),
                true,
                false,
                "Wi-Fi · Aire acondicionado · TV · Baño privado",
                "/rooms/habitacion-2.jpg"));

    final var validNumbers = new HashSet<String>();
    for (var target : targetRooms) {
      validNumbers.add(target.number());
      var room = rooms.findByNumber(target.number()).orElseGet(Room::new);
      room.setNumber(target.number());
      room.setFloor(target.floor());
      room.setType(target.type());
      room.setCapacity(target.capacity());
      room.setPrice(target.price());
      room.setBalcony(target.balcony());
      room.setPetFriendly(target.petFriendly());
      room.setServices(target.services());
      room.setImageUrl(target.imageUrl());
      if (room.getStatus() == null || room.getStatus() == RoomStatus.OUT_OF_SERVICE) {
        room.setStatus(RoomStatus.AVAILABLE);
      }
      rooms.save(room);
    }

    for (var existing : rooms.findAll()) {
      if (!validNumbers.contains(existing.getNumber())) {
        if (!reservations.existsByRoomId(existing.getId())) {
          rooms.delete(existing);
        } else {
          existing.setStatus(RoomStatus.OUT_OF_SERVICE);
          rooms.save(existing);
        }
      }
    }
  }
}
