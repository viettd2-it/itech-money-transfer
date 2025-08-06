package ibank.tech.money.transfer.service;

import ibank.tech.feature.flag.aop.FeatureFlag;
import ibank.tech.feature.flag.dto.FlagResponse;
import ibank.tech.feature.flag.service.FeatureFlagService;
import ibank.tech.money.transfer.dto.TransferRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

  private final FeatureFlagService featureFlagService;

  @FeatureFlag(key = "transfer-domestic", namespace = "bep")
  public String transferDomestic(TransferRequest transferRequest) {
    return "Transferred " + transferRequest.getAmount();
  }

//  @FeatureFlag(key = "transfer-international", namespace = "bep")
  public String transferInternational(TransferRequest transferRequest) {
    boolean test = featureFlagService.isEnabled("bep", "bep-ff-1", transferRequest.getUserId());
    log.info("test: {}", test);
    List<FlagResponse> flagResponses = featureFlagService.getFlagsByNamespace("bep");
    log.info("flagResponses: {}", flagResponses);
    return "Transferred " + transferRequest.getAmount();
  }
}
