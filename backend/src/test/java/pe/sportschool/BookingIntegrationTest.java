package pe.sportschool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DATABASE_URL:jdbc:postgresql://localhost:55432/sportschool_test}"})
class BookingIntegrationTest {
    @Autowired BookingService service;
    @Autowired JdbcTemplate db;
    final LocalDate date=LocalDate.now(ZoneId.of("America/Lima")).plusDays(2);
    @BeforeEach void clear() {
        db.execute("TRUNCATE payments,reservations,request_history,requests,customers RESTART IDENTITY CASCADE");
        db.update("INSERT INTO user_sites(user_id,site_id) SELECT u.id,s.id FROM app_users u CROSS JOIN sites s ON CONFLICT DO NOTHING");
    }
    Map<String,Object> create(long space,String phone) { return service.create(new ApiController.NewRequest("Cliente "+phone,phone,"",space,date,18,19),null); }
    long id(Map<String,Object> value) { return ((Number)value.get("id")).longValue(); }
    ApiController.ConfirmRequest payment(long id) {
        return new ApiController.ConfirmRequest((BigDecimal)service.detail(id,"admin").get("agreed_price"),"YAPE","123456");
    }
    @Test void confirmsSectorAndClosesOverlappingConfigurationsOnly() {
        long a=id(create(1,"900000001")),half=id(create(5,"900000002")),full=id(create(7,"900000003")),other=id(create(3,"900000004"));
        service.confirm(a,payment(a),"admin");
        assertEquals("RESERVED",service.detail(a,"admin").get("status"));
        assertEquals("UNAVAILABLE",service.detail(half,"admin").get("status"));
        assertEquals("UNAVAILABLE",service.detail(full,"admin").get("status"));
        assertEquals("NEW",service.detail(other,"admin").get("status"));
        assertThrows(ResponseStatusException.class,()->create(7,"900000005"));
        assertDoesNotThrow(()->create(6,"900000006"));
        assertEquals(1,db.queryForObject("SELECT count(*) FROM payments",Integer.class));
    }
    @Test void concurrentOperatorsCannotDoubleBookSharedSectors() throws Exception {
        long a=id(create(1,"900000011")),half=id(create(5,"900000012"));
        var pa=payment(a);var ph=payment(half);var latch=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Future<Boolean> first=pool.submit(()->{latch.await();try{service.confirm(a,pa,"admin");return true;}catch(ResponseStatusException e){return false;}});
            Future<Boolean> second=pool.submit(()->{latch.await();try{service.confirm(half,ph,"operador");return true;}catch(ResponseStatusException e){return false;}});
            latch.countDown();
            assertNotEquals(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS));
        }
        assertEquals(1,db.queryForObject("SELECT count(*) FROM reservations",Integer.class));
        assertEquals(1,db.queryForObject("SELECT count(*) FROM requests WHERE status='UNAVAILABLE'",Integer.class));
    }
    @Test void reopeningKeepsHistoryAndCustomer() {
        long a=id(create(1,"900000021")),half=id(create(5,"900000022"));
        service.confirm(a,payment(a),"admin");
        service.update(half,new ApiController.UpdateRequest(6,date,19,20,"CONTACTED",null,"","Cliente acepta otra mitad"),"operador");
        var reopened=service.detail(half,"operador");
        assertEquals("CONTACTED",reopened.get("status"));
        assertEquals("900000022",reopened.get("phone"));
        assertEquals(3,db.queryForObject("SELECT count(*) FROM request_history WHERE request_id=?",Integer.class,half));
        assertEquals(new BigDecimal("220.00"),reopened.get("agreed_price"));
    }
    @Test void fullPaymentAdjustmentReasonAndSiteAccessAreEnforced() {
        long a=id(create(1,"900000031"));
        assertThrows(ResponseStatusException.class,()->service.confirm(a,new ApiController.ConfirmRequest(BigDecimal.ONE,"YAPE",""),"admin"));
        assertThrows(ResponseStatusException.class,()->service.update(a,new ApiController.UpdateRequest(1,date,18,19,"CONTACTED",BigDecimal.TEN,"",""),"admin"));
        service.update(a,new ApiController.UpdateRequest(1,date,18,19,"AWAITING_PAYMENT",BigDecimal.TEN,"Acuerdo comercial",""),"admin");
        assertDoesNotThrow(()->create(1,"900000032"));
        db.update("DELETE FROM user_sites WHERE site_id=1 AND user_id=(SELECT id FROM app_users WHERE username='operador')");
        assertThrows(ResponseStatusException.class,()->service.confirm(a,new ApiController.ConfirmRequest(BigDecimal.TEN,"YAPE",""),"operador"));
        service.confirm(a,new ApiController.ConfirmRequest(BigDecimal.TEN,"YAPE",""),"admin");
        assertEquals(new BigDecimal("130.00"),service.detail(a,"admin").get("quoted_price"));
    }
}
