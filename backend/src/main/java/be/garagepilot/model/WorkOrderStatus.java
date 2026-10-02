package be.garagepilot.model;

public enum WorkOrderStatus {
  DRAFT,
  WAITING_FOR_APPROVAL,
  APPROVED,
  IN_PROGRESS,
  WAITING_FOR_PARTS,
  COMPLETED,
  INVOICED,
  CANCELLED
}
