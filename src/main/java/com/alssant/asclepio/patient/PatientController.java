package com.alssant.asclepio.patient;

import com.alssant.asclepio.patient.dto.CreatePatientRequest;
import com.alssant.asclepio.patient.dto.PatientResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
public class PatientController {
    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping()
    public ResponseEntity<List<PatientResponse>> getPatients() {
        return ResponseEntity.ok(patientService.findAll());
    }

    @PostMapping()
    public ResponseEntity<PatientResponse> createPatient(@RequestBody CreatePatientRequest request) {
        PatientResponse response = patientService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);

    }


}
