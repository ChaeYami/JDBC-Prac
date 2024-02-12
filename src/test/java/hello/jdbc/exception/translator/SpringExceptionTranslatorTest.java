package hello.jdbc.exception.translator;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static hello.jdbc.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class SpringExceptionTranslatorTest {
    DataSource dataSource;
    @BeforeEach
    void init() {
        dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
    }

    @Test
    void sqlExceptionErrorCode() {
        String sql = "select bad grammar";
        try {

            Connection con = dataSource.getConnection(); PreparedStatement stmt = con.prepareStatement(sql);
            stmt.executeQuery();

        } catch (SQLException e) {

            assertThat(e.getErrorCode()).isEqualTo(42122);
            int errorCode = e.getErrorCode();
            log.info("errorCode={}", errorCode);
            //org.h2.jdbc.JdbcSQLSyntaxErrorException
            log.info("error", e);

        }
    }

    /*
    * 이전에 살펴봤던 SQL ErrorCode를 직접 확인하는 방법이다.
    * 이렇게 직접 예외를 확인하고 하나하나 스프링이 만들어준 예외로 변환하는 것은 현실성이 없다.
    * 이렇게 하려면 해당 오류 코드를 확인하고 스프링의 예외 체계에 맞추어 예외를 직접 변환해야 할 것이다.
    * 그리고 데이터베이스마다 오류 코드가 다르다는 점도 해결해야 한다.
    * 그래서 스프링은 예외 변환기를 제공한다.
    * */

    // 예외 변환기
    @Test
    void exceptionTranslator() {
        String sql = "select bad grammar";
        try {

            Connection con = dataSource.getConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.executeQuery();

        } catch (SQLException e) {

            assertThat(e.getErrorCode()).isEqualTo(42122);
            //org.springframework.jdbc.support.sql-error-codes.xml
            SQLExceptionTranslator exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
            //org.springframework.jdbc.BadSqlGrammarException

            DataAccessException resultEx = exTranslator.translate("select", sql, e);
            // translate() 메서드의 첫번째 파라미터는 읽을 수 있는 설명이고, 두번째는 실행한 sql, 마지막은 발생된 SQLException
            // 적절한 스프링 데이터 접근 계층의 예외로 변환해서 반환
            // 여기서는 BadSqlGrammarException 을 반환
            // 참고로 BadSqlGrammarException 은 최상위 타입인 DataAccessException 를 상속 받아서 만들어진다.

            log.info("resultEx", resultEx);
            assertThat(resultEx.getClass()).isEqualTo(BadSqlGrammarException.class);

        }
    }
}