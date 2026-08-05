package co.lindomar.config;

import co.lindomar.domain.Domain.*;
import co.lindomar.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Configuration
public class SeedData {
 @Bean CommandLineRunner seed(UserRepository users, RoomRepository rooms, TaskRepository tasks, FinanceRepository finance, ReminderRepository reminders){
  return args -> {
   if(users.count()>0) return;
   var guest=users.save(new UserAccount("Mariana Torres","huesped@lindomar.co","demo123",Role.GUEST,"300 555 0142"));
   var employee=users.save(new UserAccount("Santiago Ruiz","empleado@lindomar.co","demo123",Role.EMPLOYEE,"301 555 0188"));
   users.save(new UserAccount("Laura Méndez","admin@lindomar.co","demo123",Role.ADMIN,"315 555 0101"));
   users.save(new UserAccount("Ana Gómez","ana@lindomar.co","demo123",Role.EMPLOYEE,"312 555 0165"));
   rooms.saveAll(List.of(
    new Room("101",1,"Estándar",2,new BigDecimal("185000"),false,false,"Wi-Fi · TV · Desayuno",RoomStatus.AVAILABLE),
    new Room("102",1,"Estándar",2,new BigDecimal("195000"),true,true,"Wi-Fi · TV · Desayuno",RoomStatus.AVAILABLE),
    new Room("103",1,"Familiar",4,new BigDecimal("310000"),true,true,"Wi-Fi · TV · Minibar · Desayuno",RoomStatus.OCCUPIED),
    new Room("201",2,"Deluxe",2,new BigDecimal("285000"),true,false,"Wi-Fi · Smart TV · Jacuzzi",RoomStatus.AVAILABLE),
    new Room("202",2,"Familiar",5,new BigDecimal("360000"),true,true,"Wi-Fi · TV · Cocina · Desayuno",RoomStatus.AVAILABLE),
    new Room("203",2,"Deluxe",3,new BigDecimal("295000"),false,false,"Wi-Fi · Smart TV · Minibar",RoomStatus.MAINTENANCE),
    new Room("301",3,"Suite",2,new BigDecimal("480000"),true,true,"Wi-Fi · Jacuzzi · Minibar · Vista al mar",RoomStatus.AVAILABLE),
    new Room("302",3,"Suite",4,new BigDecimal("560000"),true,false,"Wi-Fi · Jacuzzi · Sala · Vista al mar",RoomStatus.AVAILABLE)
   ));
   tasks.saveAll(List.of(
    new StaffTask(employee.getId(),"Preparar habitación 201","Limpieza completa y reposición de minibar.",TaskStatus.PENDING,LocalDate.now(),"Alta"),
    new StaffTask(employee.getId(),"Revisar solicitud 102","Llevar dos toallas adicionales.",TaskStatus.IN_PROGRESS,LocalDate.now(),"Media"),
    new StaffTask(employee.getId(),"Inventario de lencería","Registrar faltantes del piso 1.",TaskStatus.PENDING,LocalDate.now().plusDays(1),"Baja")
   ));
   finance.saveAll(List.of(
    new FinanceEntry(EntryType.INCOME,"Reservas de la semana",new BigDecimal("4850000"),LocalDate.now()),
    new FinanceEntry(EntryType.EXPENSE,"Proveedor de lavandería",new BigDecimal("720000"),LocalDate.now().minusDays(1)),
    new FinanceEntry(EntryType.EXPENSE,"Mantenimiento preventivo",new BigDecimal("350000"),LocalDate.now().minusDays(2))
   ));
   reminders.saveAll(List.of(
    new ExpenseReminder("Reposición de insumos de aseo","Insumos",new BigDecimal("480000"),LocalDate.now().plusDays(2),ReminderStatus.PENDING),
    new ExpenseReminder("Pago factura de energía","Servicios",new BigDecimal("1250000"),LocalDate.now().plusDays(5),ReminderStatus.PENDING),
    new ExpenseReminder("Mantenimiento del aire acondicionado","Mantenimiento",new BigDecimal("640000"),LocalDate.now().plusDays(12),ReminderStatus.PENDING),
    new ExpenseReminder("Compra de lencería piso 2","Insumos",new BigDecimal("890000"),LocalDate.now().plusDays(21),ReminderStatus.PENDING)
   ));
  };
 }
}

