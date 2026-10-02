package be.garagepilot.dto.servicecatalog;

import be.garagepilot.model.ServiceCategory;
import java.math.BigDecimal;
import java.util.UUID;

public record GarageServiceResponse(
    UUID id,
    String name,
    ServiceCategory category,
    String description,
    Integer durationMinutes,
    BigDecimal price,
    BigDecimal vatRate,
    String color,
    boolean active) {}
