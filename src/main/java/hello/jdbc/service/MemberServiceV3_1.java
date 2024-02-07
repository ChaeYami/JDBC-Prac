package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV2;
import hello.jdbc.repository.MemberRepositoryV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 트랜잭션 - 트랜잭션 매니저
 */

@Slf4j
@RequiredArgsConstructor
public class MemberServiceV3_1 {

    //    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;
    /*
    * 트랜잭션 매니저를 주입 받는다. 지금은 JDBC 기술을 사용하기 때문에 DataSourceTransactionManager 구현체를 주입 받아야 한다.
    * 물론 JPA 같은 기술로 변경되면 JpaTransactionManager 를 주입 받으면 된다
    * */

    private final MemberRepositoryV3 memberRepository;

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
//        Connection con = dataSource.getConnection();

        //트랜잭션 시작
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        /*
        * 트랜잭션을 시작한다.
        * TransactionStatus status 를 반환한다. 현재 트랜잭션의 상태 정보가 포함되어 있다.
        * 이후 트랜잭션을 커밋, 롤백할 때 필요하다.
        * */

        try {
            // 비즈니스 로직 수행
            bizLogic(fromId, toId, money);

            transactionManager.commit(status); // 성공시 커밋

        } catch (Exception e) {
            transactionManager.rollback(status); // 실패시 롤백

            throw new IllegalStateException(e);
        } // 매니저가 알아서 닫아주기 때문에 release 필요 없음

    }

    private void bizLogic(String fromId, String toId, int money) throws SQLException {
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        // fromId 회원의 돈을 money 만큼 감소한다. UPDATE SQL 실행
        memberRepository.update(fromId, fromMember.getMoney() - money);
        // 이체중 예외
        validation(toMember);
        // toId 회원의 돈을 money 만큼 증가한다. UPDATE SQL 실행
        memberRepository.update(toId, toMember.getMoney() + money);
    }

    private static void release(Connection con) {
        if (con != null) {
            try {
                con.setAutoCommit(true); //커넥션 풀 고려
                con.close();

            } catch (Exception e) {
                log.info("error", e);
            }
        }
    }

    private void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체중 예외 발생");
        }
    }
}
