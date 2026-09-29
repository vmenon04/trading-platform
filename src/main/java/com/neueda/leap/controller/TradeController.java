package com.neueda.leap.controller;

import com.neueda.leap.dto.OrderRequestDTO;
import com.neueda.leap.dto.OrderResponseDTO;
import com.neueda.leap.entity.AccountTrade;
import com.neueda.leap.repository.AccountTradeMapper;
import com.neueda.leap.service.ExecutionService;
import com.neueda.leap.service.InstrumentService;
import com.neueda.leap.service.OrderManagementService;
import com.neueda.leap.service.ValidationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/accounts/{accountId}/trades")
public class TradeController {

    private final InstrumentService instrumentService;
    private final OrderManagementService orderManagementService;
    private final ValidationService validationService;
    private final ExecutionService executionService;
    private final AccountTradeMapper accountTradeMapper;

    private static final String PENDING = "PENDING";
    private static final String ACCEPTED = "ACCEPTED";
    private static final String REJECTED = "REJECTED";
    private static final String FULFILLED = "FULFILLED";

    public TradeController(InstrumentService instrumentService, OrderManagementService orderManagementService, ValidationService validationService,
                           ExecutionService executionService, AccountTradeMapper accountTradeMapper) {
        this.instrumentService = instrumentService;
        this.orderManagementService = orderManagementService;
        this.validationService = validationService;
        this.executionService = executionService;
        this.accountTradeMapper = accountTradeMapper;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDTO> placeTrade(
            @PathVariable int accountId,
            @Valid @RequestBody OrderRequestDTO order) {

        OrderResponseDTO recievedDTO = OrderResponseDTO.builder().status("SUBMITTED").build();
        ResponseEntity.accepted().body(recievedDTO);
        BigDecimal price = instrumentService.getCurrentPrice(order.instrumentId());

        try {
            validationService.validate(order, price);
        } catch (IllegalStateException e) {
            orderManagementService.recordTrade(order, price, REJECTED);
            throw e;
        }


        int tradeId = orderManagementService.recordTrade(order, price, PENDING);
        accountTradeMapper.insertStatus(tradeId, ACCEPTED);
        OrderResponseDTO validatedDTO = OrderResponseDTO.builder().status("ACCEPTED").build();
        ResponseEntity.accepted().body(validatedDTO);

        try {
            executionService.execute(order, price);
        } catch (RuntimeException e) {
            accountTradeMapper.insertStatus(tradeId, REJECTED);
            throw e;
        }

        accountTradeMapper.insertStatus(tradeId, FULFILLED);
        BigDecimal executedPrice = accountTradeMapper.findById(tradeId).getPrice();
        BigDecimal executedQuantity = accountTradeMapper.findById(tradeId).getQuantity();
        OrderResponseDTO executedDTO = new OrderResponseDTO(tradeId, "EXECUTED", executedPrice, executedQuantity, null);

        return ResponseEntity.ok(executedDTO);

    }
}