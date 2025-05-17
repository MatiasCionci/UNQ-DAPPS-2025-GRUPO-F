package com.dappstp.dappstp.model.queryhistory;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_logs")
@Data
@NoArgsConstructor
public class PredictionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData; // Los datos de entrada que se usaron para la predicción

    @Column(name = "prediction_result", columnDefinition = "TEXT")
    private String predictionResult; // El resultado de la predicción (podría ser un JSON del objeto Prediction)

    @Column(name = "prediction_type") // Para diferenciar si fue 'comprehensive' u otra
    private String predictionType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    // @ManyToOne
    // @JoinColumn(name = "user_id")
    // private User user;

    public PredictionLog(String requestData, String predictionResult, String predictionType) {
        this.requestData = requestData;
        this.predictionResult = predictionResult;
        this.predictionType = predictionType;
        this.createdAt = LocalDateTime.now();
    }

   
}