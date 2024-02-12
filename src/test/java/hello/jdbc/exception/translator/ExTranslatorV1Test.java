package hello.jdbc.exception.translator;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.ex.MyDbException;
import hello.jdbc.repository.ex.MyDuplicateKeyException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static hello.jdbc.connection.ConnectionConst.*;
import static org.springframework.jdbc.support.JdbcUtils.closeConnection;
import static org.springframework.jdbc.support.JdbcUtils.closeStatement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;


public class ExTranslatorV1Test {
    Repository repository;
    Service service;

    @BeforeEach
    void init() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        repository = new Repository(dataSource);
        service = new Service(repository);

    }

    @Test
    void duplicatedKeySave() {
        service.create("myID");
        service.create("myID");
    }

    @Slf4j
    @RequiredArgsConstructor
    static class Service {
        private final Repository repository;

        public void create(String memberId) {

            try {

                repository.save(new Member(memberId, 0));
                log.info("saveId = {}", memberId);

            } catch (MyDuplicateKeyException e) {
                log.info("키 중복, 복구 시도");
                String retryId = generateNewId(memberId);
                log.info("retryId={}", retryId);
                repository.save(new Member(retryId, 0));
            } catch (MyDbException e) {
                // 만약 복구할 수 없는 예외( MyDbException )면 로그만 남기고 다시 예외를 던진다
                log.info("데이터 접근 계층 예외", e);
                throw e;
            }

        }

        private String generateNewId(String memberId) {
            return memberId + new Random().nextInt(10000);
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    static class Repository {
        private final DataSource dataSource;

        public Member save(Member member) {
            String sql = "insert into member(member_id, money) values(?,?)";

            Connection con = null;
            PreparedStatement pstmt = null;

            try {
                con = dataSource.getConnection();
                pstmt = con.prepareStatement(sql);
                pstmt.setString(1, member.getMemberId());
                pstmt.setInt(2, member.getMoney());
                pstmt.executeUpdate();

                return member;

            } catch (SQLException e) {
                // h2 db
                if (e.getErrorCode() == 23505) { // 오류 코드가 키 중복 오류( 23505 )인 경우
                    throw new MyDuplicateKeyException(e); // MyDuplicateKeyException 을 새로 만들어서 서비스 계층에 던진다.
                }
                throw new MyDbException(e); // 나머지 경우 기존에 만들었던 MyDbException 을 던진다.

            }finally {

                closeStatement(pstmt);
                closeConnection(con);

            }
        }
    }

    /*
    * SQL ErrorCode로 데이터베이스에 어떤 오류가 있는지 확인할 수 있었다.
    * 예외 변환을 통해 SQLException 을 특정 기술에 의존하지 않는 직접 만든 예외인 MyDuplicateKeyException 로 변환 할 수 있었다.
    * 리포지토리 계층이 예외를 변환해준 덕분에 서비스 계층은 특정 기술에 의존하지 않는 MyDuplicateKeyException 을 사용해서 문제를 복구하고, 서비스 계층의 순수성도 유지할 수 있었다.
    * */
}
