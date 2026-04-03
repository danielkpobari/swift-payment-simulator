package com.baghadom.cross_border_payment_simulator.service;

import com.baghadom.cross_border_payment_simulator.entity.NostroVostroAccount;
import com.baghadom.cross_border_payment_simulator.repository.NostroVostroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NostroVostroService {

    private final NostroVostroRepository repo;

    @Transactional
    public void debitNostro(String ownerBic, String correspondentBic, String currency, BigDecimal amount) {
        NostroVostroAccount account = repo
                .findByOwnerBicAndCorrespondentBicAndCurrency(ownerBic, correspondentBic, currency)
                .orElseThrow(() -> new RuntimeException(
                        "No nostro account: " + ownerBic + " @ " + correspondentBic + " [" + currency + "]"));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient nostro balance for " + ownerBic + " @ " + correspondentBic);
        }
        account.setBalance(account.getBalance().subtract(amount));
        repo.save(account);
    }

    @Transactional
    public void creditVostro(String ownerBic, String correspondentBic, String currency, BigDecimal amount) {
        repo.findByOwnerBicAndCorrespondentBicAndCurrency(ownerBic, correspondentBic, currency)
                .ifPresent(account -> {
                    account.setBalance(account.getBalance().add(amount));
                    repo.save(account);
                });
    }

    public List<NostroVostroAccount> getAccountsForBank(String bic) {
        return repo.findByOwnerBic(bic);
    }

    public List<NostroVostroAccount> getAll() {
        return repo.findAll();
    }
}
