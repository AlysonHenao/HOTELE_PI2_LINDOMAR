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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Carga datos de demostración cuando la base de datos está vacía. */
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
      syncHotelRooms(rooms, reservations);

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

  private static void syncHotelRooms(RoomRepository rooms, ReservationRepository reservations) {
    final var validNumbers = new HashSet<String>();
    for (Room target : createOfficialHotelRooms()) {
      validNumbers.add(target.getNumber());
      var existing = rooms.findByNumber(target.getNumber());
      if (existing.isPresent()) {
        Room r = existing.get();
        r.setFloor(target.getFloor());
        r.setType(target.getType());
        r.setCapacity(target.getCapacity());
        r.setPrice(target.getPrice());
        r.setBalcony(target.isBalcony());
        r.setPetFriendly(target.isPetFriendly());
        r.setServices(target.getServices());
        if (target.getImageUrl() != null) {
          r.setImageUrl(target.getImageUrl());
        }
        if (r.getStatus() == null || r.getStatus() == RoomStatus.OUT_OF_SERVICE) {
          r.setStatus(RoomStatus.AVAILABLE);
        }
        rooms.save(r);
      } else {
        rooms.save(target);
      }
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

  /**
   * Genera las 44 habitaciones oficiales del Hotel Lindomar según la distribución real: - Primer
   * piso: 100 (Familiar, 4 camas, escaleras) y 101 (Doble, 2 camas, recepción). - Piso intermedio:
   * 102 a 106 (Camas dobles). - Piso 2: 200 a 221 (201 y 205 con ventilador; 208, 214, 218, 219,
   * 220 con 2 camas; 221 con 3 camas; resto dobles). - Piso 3: 300 a 314 con aire acondicionado
   * (300 a 304 con 2 camas; 306 con 5 camas; 314 con 3 camas; resto dobles).
   */
  public static List<Room> createOfficialHotelRooms() {
    List<Room> list = new ArrayList<>();

    // Primer piso (al lado de recepción y escaleras)
    list.add(
        new Room(
            "100",
            1,
            "Familiar",
            4,
            new BigDecimal("320000"),
            false,
            true,
            "Aire acondicionado · Wi-Fi · TV · 4 camas · Junto a recepción y escaleras",
            RoomStatus.AVAILABLE,
            "/rooms/habitacion-1.jpg"));
    list.add(
        new Room(
            "101",
            1,
            "Estándar",
            2,
            new BigDecimal("185000"),
            false,
            false,
            "Aire acondicionado · Wi-Fi · TV · Cama doble · Al lado de recepción y escaleras",
            RoomStatus.AVAILABLE,
            "/rooms/habitacion-2.jpg"));

    // Piso intermedio: 102 a 106 (camas dobles)
    for (int i = 102; i <= 106; i++) {
      String img = (i % 2 == 0) ? "/rooms/habitacion-1.jpg" : "/rooms/habitacion-2.jpg";
      list.add(
          new Room(
              String.valueOf(i),
              1,
              "Estándar",
              2,
              new BigDecimal("195000"),
              false,
              false,
              "Aire acondicionado · Wi-Fi · TV · Cama doble · Piso intermedio",
              RoomStatus.AVAILABLE,
              img));
    }

    // Segundo piso: 200 a 221
    for (int i = 200; i <= 221; i++) {
      String num = String.valueOf(i);
      String img = (i % 2 == 0) ? "/rooms/habitacion-1.jpg" : "/rooms/habitacion-2.jpg";
      if (i == 201 || i == 205) {
        list.add(
            new Room(
                num,
                2,
                "Estándar",
                2,
                new BigDecimal("175000"),
                false,
                false,
                "Ventilador · Wi-Fi · TV · Cama doble",
                RoomStatus.AVAILABLE,
                img));
      } else if (i == 202) {
        list.add(
            new Room(
                num,
                2,
                "Deluxe",
                2,
                new BigDecimal("285000"),
                true,
                true,
                "Aire acondicionado · Wi-Fi · Smart TV · Jacuzzi · Cama doble",
                RoomStatus.AVAILABLE,
                "/hotel/sala.jpg"));
      } else if (i == 208 || i == 214 || i == 218 || i == 219 || i == 220) {
        list.add(
            new Room(
                num,
                2,
                "Estándar",
                2,
                new BigDecimal("220000"),
                false,
                false,
                "Aire acondicionado · Wi-Fi · TV · 2 camas",
                RoomStatus.AVAILABLE,
                img));
      } else if (i == 221) {
        list.add(
            new Room(
                num,
                2,
                "Familiar",
                3,
                new BigDecimal("310000"),
                true,
                true,
                "Aire acondicionado · Wi-Fi · TV · 3 camas · Familiar múltiple",
                RoomStatus.AVAILABLE,
                "/rooms/habitacion-1.jpg"));
      } else {
        list.add(
            new Room(
                num,
                2,
                "Estándar",
                2,
                new BigDecimal("210000"),
                false,
                false,
                "Aire acondicionado · Wi-Fi · TV · Cama doble",
                RoomStatus.AVAILABLE,
                img));
      }
    }

    // Tercer piso: 300 a 314 (todas con aire acondicionado)
    for (int i = 300; i <= 314; i++) {
      String num = String.valueOf(i);
      String img = (i % 2 == 0) ? "/rooms/habitacion-1.jpg" : "/rooms/habitacion-2.jpg";
      if (i == 301) {
        list.add(
            new Room(
                num,
                3,
                "Suite",
                2,
                new BigDecimal("480000"),
                true,
                true,
                "Aire acondicionado · Wi-Fi · Jacuzzi · Minibar · Vista al mar · 2 camas",
                RoomStatus.AVAILABLE,
                "/hotel/recepcion.jpg"));
      } else if (i == 302) {
        list.add(
            new Room(
                num,
                3,
                "Suite",
                2,
                new BigDecimal("490000"),
                true,
                false,
                "Aire acondicionado · Wi-Fi · Jacuzzi · Sala · Vista al mar · 2 camas",
                RoomStatus.AVAILABLE,
                "/hotel/lobby.jpg"));
      } else if (i == 306) {
        list.add(
            new Room(
                num,
                3,
                "Familiar",
                5,
                new BigDecimal("420000"),
                true,
                true,
                "Aire acondicionado · Wi-Fi · TV · 5 camas · Familiar múltiple · Balcón",
                RoomStatus.AVAILABLE,
                "/rooms/habitacion-1.jpg"));
      } else if (i == 314) {
        list.add(
            new Room(
                num,
                3,
                "Familiar",
                3,
                new BigDecimal("350000"),
                true,
                true,
                "Aire acondicionado · Wi-Fi · TV · 3 camas · Familiar múltiple · Balcón",
                RoomStatus.AVAILABLE,
                "/rooms/habitacion-2.jpg"));
      } else if (i <= 304) {
        list.add(
            new Room(
                num,
                3,
                "Estándar",
                2,
                new BigDecimal("230000"),
                true,
                false,
                "Aire acondicionado · Wi-Fi · TV · 2 camas · Balcón",
                RoomStatus.AVAILABLE,
                img));
      } else {
        list.add(
            new Room(
                num,
                3,
                "Estándar",
                2,
                new BigDecimal("240000"),
                true,
                false,
                "Aire acondicionado · Wi-Fi · TV · Cama doble · Balcón",
                RoomStatus.AVAILABLE,
                img));
      }
    }

    return list;
  }
}
