package a.gleb.producer.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ProducerAppException extends RuntimeException{

    private final HttpStatus httpStatus;

    public ProducerAppException(HttpStatus status, String message) {
        this.httpStatus = status;
        super(message);
    }
}
