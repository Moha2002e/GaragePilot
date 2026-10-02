package be.garagepilot.service;

import be.garagepilot.dto.client.*;
import be.garagepilot.exception.ResourceNotFoundException;
import be.garagepilot.model.Client;
import be.garagepilot.repository.*;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientService {
  private final ClientRepository clients;
  private final GarageRepository garages;

  public ClientService(ClientRepository clients, GarageRepository garages) {
    this.clients = clients;
    this.garages = garages;
  }

  @Transactional(readOnly = true)
  public Page<ClientResponse> findAll(UUID garageId, String search, Pageable pageable) {

    return clients.searchActive(garageId, normalizeSearch(search), pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public ClientResponse findById(UUID garageId, UUID id) {
    return toResponse(findEntity(garageId, id));
  }

  public ClientResponse create(UUID garageId, ClientRequest request) {
    Client client = new Client();
    client.setGarage(garages.getReferenceById(garageId));
    apply(client, request);
    return toResponse(clients.save(client));
  }

  public ClientResponse update(UUID garageId, UUID id, ClientRequest request) {
    Client client = findEntity(garageId, id);
    apply(client, request);
    return toResponse(clients.save(client));
  }

  public void deactivate(UUID garageId, UUID id) {
    Client client = findEntity(garageId, id);
    client.deactivate();
    clients.save(client);
  }

  public Client findEntity(UUID garageId, UUID id) {
    return clients
        .findByIdAndGarageId(id, garageId)
        .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
  }

  private void apply(Client c, ClientRequest r) {
    c.setType(r.type());
    c.setFirstName(r.firstName());
    c.setLastName(r.lastName());
    c.setCompanyName(r.companyName());
    c.setVatNumber(r.vatNumber());
    c.setEmail(r.email());
    c.setPhone(r.phone());
    c.setStreet(r.street());
    c.setPostalCode(r.postalCode());
    c.setCity(r.city());
    c.setCountry(r.country() == null ? "BE" : r.country().toUpperCase());
    c.setNotes(r.notes());
  }

  private ClientResponse toResponse(Client c) {
    return new ClientResponse(
        c.getId(),
        c.getType(),
        c.getFirstName(),
        c.getLastName(),
        c.getCompanyName(),
        c.getEmail(),
        c.getPhone(),
        c.getCity(),
        c.isActive(),
        c.getCreatedAt(),
        c.getUpdatedAt());
  }

  private String normalizeSearch(String search) {
    return search == null ? "" : search.trim();
  }
}
