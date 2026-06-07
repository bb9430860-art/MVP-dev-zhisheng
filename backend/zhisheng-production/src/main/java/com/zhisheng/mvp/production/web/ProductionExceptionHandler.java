package com.zhisheng.mvp.production.web;

import com.zhisheng.mvp.common.ApiResponse;
import com.zhisheng.mvp.production.exception.ProductionDispatchException;
import com.zhisheng.mvp.production.exception.ProductionStepExecutionException;
import com.zhisheng.mvp.production.exception.ProductionWorkOrderException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.zhisheng.mvp.production")
public class ProductionExceptionHandler {

    @ExceptionHandler(ProductionDispatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductionDispatch(ProductionDispatchException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(ProductionStepExecutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductionStepExecution(
            ProductionStepExecutionException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(ProductionWorkOrderException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductionWorkOrder(ProductionWorkOrderException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(exception.getMessage()));
    }
}
