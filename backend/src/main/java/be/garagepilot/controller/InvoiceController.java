package be.garagepilot.controller;

import be.garagepilot.dto.invoice.InvoiceRequest;
import be.garagepilot.dto.invoice.InvoiceResponse;
import be.garagepilot.model.InvoiceStatus;
import be.garagepilot.security.CurrentUser;
import be.garagepilot.service.InvoicePdfService;
import be.garagepilot.service.InvoiceService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

  private final InvoiceService service;
  private final InvoicePdfService pdfService;

  public InvoiceController(InvoiceService service, InvoicePdfService pdfService) {
    this.service = service;
    this.pdfService = pdfService;
  }

  @GetMapping
  public Page<InvoiceResponse> list(
      @AuthenticationPrincipal CurrentUser user,
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) InvoiceStatus status,
      @PageableDefault(size = 20, sort = "issueDate", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return service.findAll(user.garageId(), search, status, pageable);
  }

  @GetMapping("/{id}")
  public InvoiceResponse get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
    return service.findById(user.garageId(), id);
  }

  @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> downloadPdf(
      @AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
    InvoiceResponse invoice = service.findById(user.garageId(), id);
    byte[] pdf = pdfService.generate(user.garageId(), id);
    String fileName = "facture-" + invoice.invoiceNumber() + ".pdf";

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(pdf.length)
        .body(pdf);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public InvoiceResponse create(
      @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody InvoiceRequest request) {
    return service.create(user.garageId(), request);
  }

  @PutMapping("/{id}")
  public InvoiceResponse update(
      @AuthenticationPrincipal CurrentUser user,
      @PathVariable UUID id,
      @Valid @RequestBody InvoiceRequest request) {
    return service.update(user.garageId(), id, request);
  }
}
