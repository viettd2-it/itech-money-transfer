package ibank.tech.money.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {

  private double amount;
  private String from;
  private String to;
  private String role;
  private String region;
  private String userId;
}
