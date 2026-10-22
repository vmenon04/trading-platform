package com.neueda.leap.controller;

import com.neueda.leap.dto.InstrumentResponseDTO;
import com.neueda.leap.service.InstrumentCatalog;
import com.neueda.leap.service.InstrumentCatalog.CatalogInstrument;
import com.neueda.leap.service.InstrumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;
    private final InstrumentCatalog instrumentCatalog;

    public InstrumentController(InstrumentService instrumentService, InstrumentCatalog instrumentCatalog) {
        this.instrumentService = instrumentService;
        this.instrumentCatalog = instrumentCatalog;
    }

    // Instruments with their price to show (cached in Redis, then the API in batches; the last known price if there's
    // no fresh one). Without search: every instrument in the database. With search=ORCL: that symbol, looked up in
    // the database and then the market data API (which adds it to the database), or an empty list if it's unknown.
    @GetMapping
    public ResponseEntity<List<InstrumentResponseDTO>> getInstruments(@RequestParam(required = false) String search) {
        List<CatalogInstrument> instruments;
        if (search == null || search.isBlank()) {
            instruments = instrumentCatalog.all();
        } else {
            CatalogInstrument found = instrumentCatalog.lookup(search);
            instruments = found == null ? List.of() : List.of(found);
        }

        Map<String, BigDecimal> prices = instrumentService.getCurrentPrices(
                instruments.stream().map(CatalogInstrument::ticker).toList());
        return ResponseEntity.ok(instruments.stream()
                .map(instrument -> new InstrumentResponseDTO(instrument.instrumentId(), instrument.ticker(),
                        instrument.name(), prices.get(instrument.ticker()), instrument.assetClass()))
                .toList());
    }
}
