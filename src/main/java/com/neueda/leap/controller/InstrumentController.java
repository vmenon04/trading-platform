package com.neueda.leap.controller;

import com.neueda.leap.dto.InstrumentResponseDTO;
import com.neueda.leap.entity.Instrument;
import com.neueda.leap.service.InstrumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping
    public ResponseEntity<List<InstrumentResponseDTO>> getInstruments() {
        List<InstrumentResponseDTO> response = instrumentService.getAllInstruments().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    private InstrumentResponseDTO toResponse(Instrument instrument) {
        return new InstrumentResponseDTO(instrument.getInstrumentId(), instrument.getTicker(), instrument.getName(),
                instrumentService.getCurrentPrice(instrument.getInstrumentId()));
    }
}
