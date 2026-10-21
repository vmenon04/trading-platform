package com.neueda.leap.controller;

import com.neueda.leap.dto.InstrumentResponseDTO;
import com.neueda.leap.entity.Instrument;
import com.neueda.leap.service.InstrumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    // TODO: Add pagination

    // every instrument with its current price, priced in one go (Redis first, then the API in batches);
    // an instrument with no price right now is still listed, with a null price
    @GetMapping
    public ResponseEntity<List<InstrumentResponseDTO>> getInstruments() {
        List<Instrument> instruments = instrumentService.getAllInstruments();
        Map<String, BigDecimal> prices = instrumentService.getCurrentPrices(instruments);
        List<InstrumentResponseDTO> response = instruments.stream()
                .map(instrument -> toResponse(instrument, prices.get(instrument.getTicker())))
                .toList();
        return ResponseEntity.ok(response);
    }

    private InstrumentResponseDTO toResponse(Instrument instrument, BigDecimal price) {
        return new InstrumentResponseDTO(instrument.getInstrumentId(), instrument.getTicker(), instrument.getName(),
                price, instrument.getInstrumentType() == null ? null : instrument.getInstrumentType().name());
    }
}
