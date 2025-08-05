package ibank.tech.money.transfer.controller;

import ibank.tech.feature.flag.aop.EntityContextHolder;
import ibank.tech.money.transfer.dto.TransferRequest;
import ibank.tech.money.transfer.service.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TransferController {

  private final TransferService transferService;

  @PostMapping("/transfer/domestic")
  public ResponseEntity<String> domestic(@RequestBody TransferRequest transferRequest) {
    return ResponseEntity.ok(transferService.transferDomestic(transferRequest));
  }

  @PostMapping("/transfer/international")
  public ResponseEntity<String> international(@RequestBody TransferRequest transferRequest,
    HttpServletRequest request) {
    String entityId = request.getHeader("X-Entity-Id");
    EntityContextHolder.setEntityId(entityId);
    try {
      return ResponseEntity.ok(transferService.transferInternational(transferRequest));
    } finally {
      EntityContextHolder.clear();
    }
  }
}
