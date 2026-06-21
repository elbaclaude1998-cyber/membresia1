package com.membership.reservation.web;

import com.membership.reservation.service.RedsysService;
import com.membership.reservation.service.RedsysService.CallbackResult;
import com.membership.reservation.service.ReservationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recibe la notificación servidor-a-servidor de Redsys (form-urlencoded) y
 * confirma la reserva. Es PÚBLICO (lo llama Redsys, sin JWT).
 */
@RestController
public class RedsysController {

    private final RedsysService redsysService;
    private final ReservationService reservationService;

    public RedsysController(RedsysService redsysService, ReservationService reservationService) {
        this.redsysService = redsysService;
        this.reservationService = reservationService;
    }

    @PostMapping(value = "/redsys/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> callback(
            @RequestParam(value = "Ds_SignatureVersion", required = false) String signatureVersion,
            @RequestParam("Ds_MerchantParameters") String merchantParameters,
            @RequestParam("Ds_Signature") String signature) {
        CallbackResult result = redsysService.verifyCallback(merchantParameters, signature);
        reservationService.confirmByOrder(result.order(), result.authorized());
        return ResponseEntity.ok("OK");
    }
}
