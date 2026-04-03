package com.baghadom.cross_border_payment_simulator.repository;

import com.baghadom.cross_border_payment_simulator.entity.NostroVostroAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NostroVostroRepository extends JpaRepository<NostroVostroAccount, UUID> {

    Optional<NostroVostroAccount> findByOwnerBicAndCorrespondentBicAndCurrency(
            String ownerBic, String correspondentBic, String currency);

    List<NostroVostroAccount> findByOwnerBic(String ownerBic);
}
