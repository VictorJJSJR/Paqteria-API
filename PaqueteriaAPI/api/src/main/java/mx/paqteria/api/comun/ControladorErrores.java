package mx.paqteria.api.comun;

import java.time.Instant;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControladorErrores {
    @ExceptionHandler(ExcepcionApi.class)
    ResponseEntity<ErrorApi> manejarApi(ExcepcionApi error, HttpServletRequest solicitud) {
        return respuesta(error.getEstado(), error.getMessage(), solicitud);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorApi> manejarValidacion(MethodArgumentNotValidException error, HttpServletRequest solicitud) {
        String mensaje = error.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + traducir(e.getCode())).distinct().collect(Collectors.joining("; "));
        return respuesta(HttpStatus.BAD_REQUEST, mensaje.isBlank() ? "Revisa los datos enviados." : mensaje, solicitud);
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorApi> manejarSolicitudInvalida(Exception error, HttpServletRequest solicitud) {
        return respuesta(HttpStatus.BAD_REQUEST, "El cuerpo o los parámetros de la solicitud tienen un formato inválido.", solicitud);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<ErrorApi> manejarDuplicado(Exception error, HttpServletRequest solicitud) {
        return respuesta(HttpStatus.CONFLICT, "Los datos entran en conflicto con un registro o una restricción de la base de datos.", solicitud);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<ErrorApi> manejarAccesoDenegado(Exception error, HttpServletRequest solicitud) {
        return respuesta(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta operación.", solicitud);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorApi> manejarError(Exception error, HttpServletRequest solicitud) {
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno al procesar la solicitud.", solicitud);
    }

    private ResponseEntity<ErrorApi> respuesta(HttpStatus estado, String mensaje, HttpServletRequest solicitud) {
        return ResponseEntity.status(estado).body(new ErrorApi(Instant.now(), estado.value(), estado.getReasonPhrase(), mensaje, solicitud.getRequestURI()));
    }

    private String traducir(String codigo) {
        if (codigo == null) return "revisa el valor enviado";
        return switch (codigo) {
            case "NotBlank", "NotNull", "NotEmpty" -> "es obligatorio";
            case "Email" -> "debe ser un correo electrónico válido";
            case "Size" -> "excede la longitud permitida";
            case "Positive" -> "debe ser mayor que cero";
            case "DecimalMin" -> "está por debajo del mínimo permitido";
            default -> "no tiene un valor válido";
        };
    }
}
