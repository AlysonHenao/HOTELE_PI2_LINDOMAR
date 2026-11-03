package co.lindomar.web;

import java.time.LocalDate;

/**
 * Contrato de respuesta para la recomendación de personal de aseo según ocupación y estacionalidad.
 */
public record StaffRecommendation(
    LocalDate targetDate,
    int totalRooms,
    long occupiedRooms,
    long checkoutRooms,
    long activeReservations,
    long occupancyRate,
    String season,
    String seasonDescription,
    int roomsPerCleanerRatio,
    int recommendedCleaners,
    String rationale) {}
