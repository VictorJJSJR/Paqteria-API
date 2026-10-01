package mx.paqteria.api.comun;

import org.springframework.http.HttpStatus;

public class ExcepcionApi extends RuntimeException {
    private final HttpStatus estado;

    public ExcepcionApi(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
