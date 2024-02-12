package hello.jdbc.repository.ex;

// 기존에 사용했던 MyDbException 을 상속받아서 의미있는 계층을 형성
// 데이터 중복 예외 - JDBC나 JPA 등 특정 DB 기술에 종속되지 않음
public class MyDuplicateKeyException extends MyDbException{
    public MyDuplicateKeyException() {
    }

    public MyDuplicateKeyException(String message) {
        super(message);
    }

    public MyDuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public MyDuplicateKeyException(Throwable cause) {
        super(cause);
    }
}
