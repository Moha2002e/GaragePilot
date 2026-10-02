package be.garagepilot.dto.servicecatalog;

import be.garagepilot.model.ServiceCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record GarageServiceRequest(
    @NotBlank @Size(max = 120) String name,
    @NotNull ServiceCategory category,
    String description,
    @NotNull @Positive Integer durationMinutes,
    @NotNull @DecimalMin("0.00") BigDecimal price,
    @NotNull @DecimalMin("0.00") BigDecimal vatRate,
    @Size(max = 20) String color) {}
